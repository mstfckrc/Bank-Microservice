#!/usr/bin/env python3
"""Create and inspect the bounded JSON protocol used by the Bank deploy helper.

This file is intentionally safe to run on the Jenkins agent. It never executes
Docker, SSH, shell fragments, or values received from the Bank host.
"""

from __future__ import annotations

import argparse
import base64
import binascii
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import tempfile
import uuid
from pathlib import Path
from typing import Any


MAX_REQUEST_BYTES = 96 * 1024
MAX_RESPONSE_BYTES = 16 * 1024
MAX_MANIFEST_BYTES = MAX_REQUEST_BYTES // 2
BUILD_NUMBER_RE = re.compile(r"^[1-9][0-9]{0,8}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REQUEST_ID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
)
CONFIG_ID_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")

HOST_CONFIG = Path("/etc/bank-demo/deploy.json")
HOST_CONFIGS_DIR = Path("/etc/bank-demo/configs")
MANIFEST_TEMP_DIR = Path("/var/tmp")
TRUSTED_OWNER_UID = 0

RESPONSE_KEYS = {
    "protocol_version",
    "request_id",
    "action",
    "release_build_number",
    "result",
    "source_commit",
    "manifest_sha256",
    "config_id",
    "message",
}
VALIDATE_RESULTS = {"validated", "rejected"}


def fail(message: str) -> "NoReturn":
    print(message, file=sys.stderr)
    raise SystemExit(2)


def read_bounded(path: Path, limit: int) -> bytes:
    try:
        with path.open("rb") as stream:
            data = stream.read(limit + 1)
    except OSError as exc:
        fail(f"Cannot read protocol file: {exc.strerror or 'I/O error'}")
    if len(data) > limit:
        fail("Protocol file exceeds the configured size limit.")
    return data


def parse_json(data: bytes, *, what: str) -> dict[str, Any]:
    def reject_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("duplicate JSON object key")
            result[key] = value
        return result

    try:
        value = json.loads(data.decode("utf-8"), object_pairs_hook=reject_duplicate_keys)
    except (UnicodeDecodeError, json.JSONDecodeError, ValueError):
        fail(f"{what} is not valid UTF-8 JSON.")
    if not isinstance(value, dict):
        fail(f"{what} must be a JSON object.")
    return value


def require_exact_keys(value: dict[str, Any], allowed: set[str], *, what: str) -> None:
    unknown = set(value) - allowed
    missing = allowed - set(value)
    if unknown or missing:
        fail(f"{what} contains unsupported fields.")


def build_request(args: argparse.Namespace) -> None:
    request: dict[str, Any] = {
        "protocol_version": 1,
        "request_id": str(uuid.uuid4()),
        "action": args.action,
        "release_build_number": None,
        "manifest_sha256": None,
        "manifest_base64": None,
    }

    if not BUILD_NUMBER_RE.fullmatch(args.release_build_number or ""):
        fail("A positive image-build-pilot build number is required.")
    if not args.manifest:
        fail("A release manifest path is required for validation.")
    manifest = read_bounded(Path(args.manifest), MAX_MANIFEST_BYTES)
    if not manifest:
        fail("Release manifest is empty.")
    request["release_build_number"] = args.release_build_number
    request["manifest_sha256"] = hashlib.sha256(manifest).hexdigest()
    request["manifest_base64"] = base64.b64encode(manifest).decode("ascii")

    encoded = (json.dumps(request, separators=(",", ":"), sort_keys=True) + "\n").encode("utf-8")
    if len(encoded) > MAX_REQUEST_BYTES:
        fail("Generated request exceeds the configured size limit.")
    try:
        Path(args.output).write_bytes(encoded)
    except OSError as exc:
        fail(f"Cannot write protocol request: {exc.strerror or 'I/O error'}")
    print(f"Prepared protocol v1 {args.action} request {request['request_id']}.")


def check_response(args: argparse.Namespace) -> None:
    response = parse_json(read_bounded(Path(args.input), MAX_RESPONSE_BYTES), what="Bank helper response")
    require_exact_keys(response, RESPONSE_KEYS, what="Bank helper response")

    if type(response.get("protocol_version")) is not int or response["protocol_version"] != 1:
        fail("Bank helper returned an unsupported protocol version.")
    if not isinstance(response.get("request_id"), str) or not re.fullmatch(
        r"[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",
        response["request_id"],
    ):
        fail("Bank helper returned an invalid request ID.")
    if response.get("request_id") != args.request_id:
        fail("Bank helper response does not match the submitted request.")
    if response.get("action") != args.action:
        fail("Bank helper response action does not match the submitted request.")

    release_build_number = response.get("release_build_number")
    if release_build_number is not None and (
        not isinstance(release_build_number, str)
        or not BUILD_NUMBER_RE.fullmatch(release_build_number)
    ):
        fail("Bank helper returned an invalid release build number.")
    if args.release_build_number is not None and release_build_number != args.release_build_number:
        fail("Bank helper response does not match the selected release build.")

    result = response.get("result")
    if not isinstance(result, str) or result not in VALIDATE_RESULTS:
        fail("Bank helper returned an unsupported result for this action.")

    source_commit = response.get("source_commit")
    if source_commit is not None and (
        not isinstance(source_commit, str) or not re.fullmatch(r"[0-9a-f]{40}", source_commit)
    ):
        fail("Bank helper returned an invalid source commit.")
    manifest_sha256 = response.get("manifest_sha256")
    if manifest_sha256 is not None and (
        not isinstance(manifest_sha256, str) or not SHA256_RE.fullmatch(manifest_sha256)
    ):
        fail("Bank helper returned an invalid manifest hash.")
    if args.manifest_sha256 is not None and manifest_sha256 != args.manifest_sha256:
        fail("Bank helper response does not match the submitted manifest.")
    config_id = response.get("config_id")
    if config_id is not None and (
        not isinstance(config_id, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,63}", config_id)
    ):
        fail("Bank helper returned an invalid configuration ID.")
    if result == "validated" and not (source_commit and manifest_sha256 and config_id):
        fail("Bank helper validation response is missing release or configuration identity.")
    if result == "validated" and not release_build_number:
        fail("Bank helper validation response is missing the selected release build number.")
    message = response.get("message")
    if not isinstance(message, str) or len(message) > 512 or any(ord(char) < 32 for char in message):
        fail("Bank helper returned an invalid status message.")

    print(f"Bank helper result: {result}")
    if source_commit:
        print(f"Source commit: {source_commit}")
    if release_build_number:
        print(f"Release build: #{release_build_number}")
    if manifest_sha256:
        print(f"Manifest SHA-256: {manifest_sha256}")
    if config_id:
        print(f"Configuration ID: {config_id}")
    print(f"Message: {message}")

    if result != "validated":
        raise SystemExit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    create = subparsers.add_parser("create-request")
    create.add_argument("--action", choices=["validate"], required=True)
    create.add_argument("--release-build-number", required=True)
    create.add_argument("--manifest", required=True)
    create.add_argument("--output", required=True)
    create.set_defaults(func=build_request)

    check = subparsers.add_parser("check-response")
    check.add_argument("--action", choices=["validate"], required=True)
    check.add_argument("--request-id", required=True)
    check.add_argument("--release-build-number")
    check.add_argument("--manifest-sha256")
    check.add_argument("--input", required=True)
    check.set_defaults(func=check_response)

    host = subparsers.add_parser(
        "host-validate",
        help="Root-side validate-only endpoint used by the restricted Bank host command.",
    )
    host.set_defaults(func=host_validate)

    args = parser.parse_args()
    try:
        args.func(args)
    except BrokenPipeError:
        raise SystemExit(1)


def require_trusted_file(path: Path) -> None:
    try:
        info = path.lstat()
    except OSError:
        fail("The Bank host deployment validation contract is not installed completely.")
    if not stat.S_ISREG(info.st_mode) or info.st_uid != TRUSTED_OWNER_UID or info.st_mode & 0o022:
        fail("A trusted Bank host deployment validation file has unsafe ownership or permissions.")


def load_host_config() -> tuple[str, Path]:
    for directory in (HOST_CONFIG.parent, HOST_CONFIGS_DIR):
        try:
            info = directory.lstat()
        except OSError:
            fail("The Bank host deployment configuration directories are not installed.")
        if not stat.S_ISDIR(info.st_mode) or info.st_uid != TRUSTED_OWNER_UID or info.st_mode & 0o022:
            fail("A Bank host deployment configuration directory has unsafe ownership or permissions.")

    require_trusted_file(HOST_CONFIG)
    config = parse_json(read_bounded(HOST_CONFIG, 4096), what="Bank host configuration")
    require_exact_keys(config, {"protocol_version", "config_id"}, what="Bank host configuration")
    if type(config.get("protocol_version")) is not int or config["protocol_version"] != 1:
        fail("The Bank host deployment configuration uses an unsupported protocol version.")
    config_id = config.get("config_id")
    if not isinstance(config_id, str) or not CONFIG_ID_RE.fullmatch(config_id):
        fail("The Bank host deployment configuration has an invalid config ID.")
    config_dir = HOST_CONFIGS_DIR / config_id
    try:
        info = config_dir.lstat()
    except OSError:
        fail("The configured Bank host deployment snapshot is missing.")
    if not stat.S_ISDIR(info.st_mode) or info.st_uid != TRUSTED_OWNER_UID or info.st_mode & 0o022:
        fail("The configured Bank host deployment snapshot has unsafe ownership or permissions.")
    return config_id, config_dir


def write_response(response: dict[str, Any]) -> None:
    encoded = (json.dumps(response, separators=(",", ":"), sort_keys=True) + "\n").encode("utf-8")
    if len(encoded) > MAX_RESPONSE_BYTES:
        fail("Generated Bank helper response exceeds the configured size limit.")
    sys.stdout.buffer.write(encoded)
    sys.stdout.buffer.flush()


def host_validate(_args: argparse.Namespace) -> None:
    raw_request = sys.stdin.buffer.read(MAX_REQUEST_BYTES + 1)
    if len(raw_request) > MAX_REQUEST_BYTES:
        fail("Protocol request exceeds the configured size limit.")

    request = parse_json(raw_request, what="Bank helper request")
    request_keys = {
        "protocol_version",
        "request_id",
        "action",
        "release_build_number",
        "manifest_sha256",
        "manifest_base64",
    }
    require_exact_keys(request, request_keys, what="Bank helper request")

    request_id = request.get("request_id")
    if not isinstance(request_id, str) or not REQUEST_ID_RE.fullmatch(request_id):
        fail("Bank helper request has an invalid request ID.")
    action = request.get("action")
    if action != "validate":
        fail("Bank helper request has an unsupported action.")
    if type(request.get("protocol_version")) is not int or request["protocol_version"] != 1:
        fail("Bank helper request uses an unsupported protocol version.")

    build_number = request.get("release_build_number")
    if not isinstance(build_number, str) or not BUILD_NUMBER_RE.fullmatch(build_number):
        fail("Bank helper request has an invalid release build number.")

    manifest_hash = request.get("manifest_sha256")
    encoded_manifest = request.get("manifest_base64")
    if not isinstance(manifest_hash, str) or not SHA256_RE.fullmatch(manifest_hash):
        fail("Bank helper request has an invalid manifest hash.")
    if not isinstance(encoded_manifest, str) or len(encoded_manifest) > 4 * MAX_MANIFEST_BYTES // 3 + 8:
        fail("Bank helper request has an invalid or oversized manifest.")
    try:
        manifest = base64.b64decode(encoded_manifest, validate=True)
    except (ValueError, binascii.Error):
        fail("Bank helper request manifest is not valid Base64.")
    if base64.b64encode(manifest).decode("ascii") != encoded_manifest:
        fail("Bank helper request manifest is not canonically encoded.")
    if not manifest or len(manifest) > MAX_MANIFEST_BYTES:
        fail("Bank helper request manifest is empty or oversized.")
    actual_hash = hashlib.sha256(manifest).hexdigest()
    if actual_hash != manifest_hash:
        fail("Bank helper request manifest hash does not match its content.")

    config_id: str | None = None
    source_commit: str | None = None
    result = "rejected"
    message = "Release manifest was rejected by the Bank host's trusted catalog. No change was made."
    try:
        config_id, config_dir = load_host_config()
        validator = config_dir / "prepare-release-env.sh"
        catalog_validator = config_dir / "validate-release-catalog.sh"
        for trusted_path in (validator, catalog_validator, config_dir / "release-services.txt"):
            require_trusted_file(trusted_path)

        # Verify the catalog itself before invoking the manifest validator.
        catalog_check = subprocess.run(
            ["/bin/sh", str(catalog_validator), str(config_dir / "release-services.txt")],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
            timeout=10,
            env={"PATH": "/usr/bin:/bin", "LC_ALL": "C"},
        )
        if catalog_check.returncode != 0:
            fail("The trusted Bank host release catalog failed validation.")

        with tempfile.NamedTemporaryFile(
            prefix="bank-deploy-manifest-", dir=MANIFEST_TEMP_DIR, delete=True
        ) as stream:
            os.fchmod(stream.fileno(), 0o600)
            stream.write(manifest)
            stream.flush()
            validation = subprocess.run(
                ["/bin/sh", str(validator), stream.name],
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                check=False,
                timeout=20,
                env={"PATH": "/usr/bin:/bin", "LC_ALL": "C"},
            )
        if validation.returncode == 0:
            match = re.search(
                rb"Validated [0-9]+ release images for source commit ([0-9a-f]{40})\.",
                validation.stderr,
            )
            if match is None:
                fail("Trusted Bank manifest validation returned no source commit identity.")
            source_commit = match.group(1).decode("ascii")
            result = "validated"
            message = "Release manifest matches the Bank host's trusted service catalog. No change was made."
    except subprocess.TimeoutExpired:
        result = "rejected"
        message = "Bank host release validation exceeded its time limit. No change was made."
    except SystemExit:
        raise
    except Exception:
        # Do not return paths, exception text, or potentially sensitive host detail.
        result = "rejected"
        message = "Bank host release validation could not complete. No change was made."

    write_response(
        {
            "protocol_version": 1,
            "request_id": request_id,
            "action": action,
            "release_build_number": build_number,
            "result": result,
            "source_commit": source_commit,
            "manifest_sha256": actual_hash,
            "config_id": config_id,
            "message": message,
        }
    )


if __name__ == "__main__":
    main()
