"""Isolated checks for the deploy protocol; no Docker, SSH, or VM is used."""

from __future__ import annotations

import argparse
import base64
import contextlib
import hashlib
import io
import json
import os
import shutil
import sys
import tempfile
import unittest
import uuid
from pathlib import Path
from unittest import mock


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPOSITORY_ROOT / "ci" / "deploy"))

import protocol  # noqa: E402


class BinaryStream:
    def __init__(self, data: bytes = b"") -> None:
        self.buffer = io.BytesIO(data)


class DeployProtocolTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory(prefix="bank-deploy-protocol-test-")
        self.addCleanup(self.temp_dir.cleanup)
        self.root = Path(self.temp_dir.name)
        self.request_id = str(uuid.uuid4())
        self.build_number = "6"
        self.source_commit = "a" * 40
        self.config_id = "test-config-v1"
        self.config_root = self.root / "etc" / "bank-demo"
        self.config_dir = self.config_root / "configs" / self.config_id
        self.config_dir.mkdir(parents=True)
        for directory in (self.config_root, self.config_root / "configs", self.config_dir):
            os.chmod(directory, 0o755)
        self.manifest_temp_dir = self.root / "tmp"
        self.manifest_temp_dir.mkdir()

        for filename in (
            "release-services.txt",
            "validate-release-catalog.sh",
            "prepare-release-env.sh",
        ):
            copied_file = self.config_dir / filename
            shutil.copy2(REPOSITORY_ROOT / "ci" / filename, copied_file)
            os.chmod(copied_file, 0o644)

        host_config = self.config_root / "deploy.json"
        host_config.write_text(
            json.dumps({"protocol_version": 1, "config_id": self.config_id}) + "\n",
            encoding="utf-8",
        )
        os.chmod(host_config, 0o644)
        self.manifest = self.make_manifest()
        self.manifest_sha256 = hashlib.sha256(self.manifest).hexdigest()

        self.patches = [
            mock.patch.object(protocol, "HOST_CONFIG", self.config_root / "deploy.json"),
            mock.patch.object(protocol, "HOST_CONFIGS_DIR", self.config_root / "configs"),
            mock.patch.object(protocol, "MANIFEST_TEMP_DIR", self.manifest_temp_dir),
            mock.patch.object(protocol, "TRUSTED_OWNER_UID", os.getuid()),
        ]
        for patcher in self.patches:
            patcher.start()
            self.addCleanup(patcher.stop)

    def make_manifest(self, *, first_repository: str | None = None) -> bytes:
        lines = ["service\timage\tdigest"]
        for index, raw_line in enumerate(
            (REPOSITORY_ROOT / "ci" / "release-services.txt").read_text(encoding="utf-8").splitlines()
        ):
            if index == 0 or not raw_line or raw_line.startswith("#"):
                continue
            service, _context, repository, _target, _image_env = raw_line.split("|")
            if first_repository and not any(line.startswith(service + "\t") for line in lines):
                repository = first_repository
                first_repository = None
            lines.append(
                f"{service}\tdocker.io/mstfckrc/{repository}:sha-{self.source_commit}\t"
                f"sha256:{'b' * 64}"
            )
        return ("\n".join(lines) + "\n").encode("utf-8")

    def request(self, *, manifest: bytes | None = None, **overrides: object) -> dict[str, object]:
        contents = self.manifest if manifest is None else manifest
        request: dict[str, object] = {
            "protocol_version": 1,
            "request_id": self.request_id,
            "action": "validate",
            "release_build_number": self.build_number,
            "manifest_sha256": hashlib.sha256(contents).hexdigest(),
            "manifest_base64": base64.b64encode(contents).decode("ascii"),
        }
        request.update(overrides)
        return request

    def host_call(self, raw_request: bytes) -> bytes:
        fake_stdout = BinaryStream()
        with contextlib.redirect_stderr(io.StringIO()), mock.patch.object(
            protocol.sys, "stdin", BinaryStream(raw_request)
        ), mock.patch.object(protocol.sys, "stdout", fake_stdout):
            protocol.host_validate(argparse.Namespace())
        return fake_stdout.buffer.getvalue()

    def test_client_builds_bounded_request_with_manifest_hash(self) -> None:
        manifest_path = self.root / "release-manifest.tsv"
        request_path = self.root / "request.json"
        manifest_path.write_bytes(self.manifest)

        with contextlib.redirect_stdout(io.StringIO()):
            protocol.build_request(
                argparse.Namespace(
                    action="validate",
                    release_build_number=self.build_number,
                    manifest=str(manifest_path),
                    output=str(request_path),
                )
            )

        request = json.loads(request_path.read_text(encoding="utf-8"))
        self.assertEqual(request["action"], "validate")
        self.assertEqual(request["release_build_number"], self.build_number)
        self.assertEqual(request["manifest_sha256"], self.manifest_sha256)
        self.assertEqual(base64.b64decode(request["manifest_base64"], validate=True), self.manifest)
        self.assertLessEqual(request_path.stat().st_size, protocol.MAX_REQUEST_BYTES)

    def test_host_accepts_release_matching_its_trusted_catalog(self) -> None:
        response = json.loads(self.host_call(json.dumps(self.request()).encode("utf-8")))

        self.assertEqual(response["result"], "validated")
        self.assertEqual(response["request_id"], self.request_id)
        self.assertEqual(response["release_build_number"], self.build_number)
        self.assertEqual(response["source_commit"], self.source_commit)
        self.assertEqual(response["manifest_sha256"], self.manifest_sha256)
        self.assertEqual(response["config_id"], self.config_id)
        self.assertIn("No change was made", response["message"])

    def test_host_rejects_catalog_mismatch_without_mutating_application(self) -> None:
        invalid_manifest = self.make_manifest(first_repository="unexpected-repository")
        response = json.loads(self.host_call(json.dumps(self.request(manifest=invalid_manifest)).encode("utf-8")))

        self.assertEqual(response["result"], "rejected")
        self.assertIn("No change was made", response["message"])

    def test_host_rejects_manifest_hash_mismatch_before_catalog_access(self) -> None:
        request = self.request()
        request["manifest_sha256"] = "0" * 64

        with self.assertRaises(SystemExit):
            self.host_call(json.dumps(request).encode("utf-8"))

    def test_host_rejects_duplicate_json_keys(self) -> None:
        with self.assertRaises(SystemExit):
            self.host_call(b'{"protocol_version":1,"protocol_version":1}')

    def test_host_rejects_unknown_request_fields(self) -> None:
        request = self.request(untrusted_command="docker compose down")

        with self.assertRaises(SystemExit):
            self.host_call(json.dumps(request).encode("utf-8"))

    def test_host_rejects_unsupported_actions(self) -> None:
        request = self.request(action="deploy")

        with self.assertRaises(SystemExit):
            self.host_call(json.dumps(request).encode("utf-8"))

    def test_host_rejects_group_or_world_writable_trusted_config(self) -> None:
        os.chmod(self.config_root / "deploy.json", 0o666)

        with self.assertRaises(SystemExit):
            self.host_call(json.dumps(self.request()).encode("utf-8"))

    def test_client_rejects_response_for_another_release_build(self) -> None:
        response = {
            "protocol_version": 1,
            "request_id": self.request_id,
            "action": "validate",
            "release_build_number": "7",
            "result": "validated",
            "source_commit": self.source_commit,
            "manifest_sha256": self.manifest_sha256,
            "config_id": self.config_id,
            "message": "validated",
        }
        response_path = self.root / "response.json"
        response_path.write_text(json.dumps(response), encoding="utf-8")

        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            protocol.check_response(
                argparse.Namespace(
                    action="validate",
                    request_id=self.request_id,
                    release_build_number=self.build_number,
                    manifest_sha256=self.manifest_sha256,
                    input=str(response_path),
                )
            )

    def test_client_rejects_response_for_another_manifest(self) -> None:
        response = {
            "protocol_version": 1,
            "request_id": self.request_id,
            "action": "validate",
            "release_build_number": self.build_number,
            "result": "validated",
            "source_commit": self.source_commit,
            "manifest_sha256": "0" * 64,
            "config_id": self.config_id,
            "message": "validated",
        }
        response_path = self.root / "response-wrong-manifest.json"
        response_path.write_text(json.dumps(response), encoding="utf-8")

        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            protocol.check_response(
                argparse.Namespace(
                    action="validate",
                    request_id=self.request_id,
                    release_build_number=self.build_number,
                    manifest_sha256=self.manifest_sha256,
                    input=str(response_path),
                )
            )


if __name__ == "__main__":
    unittest.main()
