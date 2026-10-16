"""Host deployment state-machine tests; Docker and SSH are never invoked."""

from __future__ import annotations

import contextlib
import io
import json
import os
import subprocess
import sys
import unittest
from pathlib import Path
from unittest import mock


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPOSITORY_ROOT / "ci" / "deploy"))
sys.path.insert(0, str(REPOSITORY_ROOT / "ci" / "deploy" / "tests"))

import deployment  # noqa: E402
import protocol  # noqa: E402


class DeploymentStateMachineTests(unittest.TestCase):
    def setUp(self) -> None:
        from test_protocol import DeployProtocolTests

        self.fixture = DeployProtocolTests("test_client_builds_bounded_request_with_manifest_hash")
        self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)
        self.state_root = self.fixture.root / "state"
        self.state_root.mkdir(mode=0o700)
        os.chmod(self.state_root, 0o700)
        (self.state_root / "deployments").mkdir(mode=0o700)
        os.chmod(self.state_root / "deployments", 0o700)
        runtime_env = self.fixture.root / "runtime.env"
        runtime_env.write_text("TEST_ONLY_VALUE=not-a-secret\n", encoding="utf-8")
        os.chmod(runtime_env, 0o600)
        self.patches = [
            mock.patch.object(deployment, "STATE_ROOT", self.state_root),
            mock.patch.object(deployment, "RUNTIME_ENV", runtime_env),
            mock.patch.object(deployment, "PROJECT_DIRECTORY", REPOSITORY_ROOT),
        ]
        for patcher in self.patches:
            patcher.start()
            self.addCleanup(patcher.stop)
        self.services = protocol.read_service_catalog(self.fixture.config_dir)
        self.baseline = {name: "sha256:" + str(index + 1) * 64 for index, name in enumerate(self.services)}
        self.rollback = {
            name: f"localhost/bank-demo-rollback/{name}:{'a' * 32}"
            for name in self.services
        }

    def raw_request(self, action: str) -> bytes:
        request = self.fixture.request()
        request["action"] = action
        return json.dumps(request).encode("utf-8")

    def invoke(self, action: str, raw_request: bytes) -> dict[str, object]:
        fake_stdout = type("BinaryStream", (), {"__init__": lambda self: setattr(self, "buffer", io.BytesIO())})()
        with mock.patch.object(protocol.sys, "stdout", fake_stdout):
            deployment.host_deployment_command(action, raw_request)
        return json.loads(fake_stdout.buffer.getvalue())

    def deployment_patches(self, *, apply_side_effect=None):
        patches = [
            mock.patch.object(
                deployment,
                "_run",
                return_value=subprocess.CompletedProcess([], 0, stdout=b"", stderr=b""),
            ),
            mock.patch.object(deployment, "_inspect_current_images", return_value=self.baseline),
            mock.patch.object(deployment, "_tag_baseline", return_value=self.rollback),
            mock.patch.object(deployment, "_validate_compose_model"),
            mock.patch.object(deployment, "_verify_live"),
        ]
        apply_patch = mock.patch.object(deployment, "_compose_apply", side_effect=apply_side_effect)
        patches.append(apply_patch)
        return patches

    def test_deploy_success_records_state_then_allows_exact_manual_rollback(self) -> None:
        patches = self.deployment_patches()
        for patcher in patches:
            patcher.start()
            self.addCleanup(patcher.stop)

        response = self.invoke("deploy", self.raw_request("deploy"))

        self.assertEqual(response["result"], "deployed")
        deployment_id = response["deployment_id"]
        self.assertRegex(deployment_id, r"^deploy-[0-9a-f]{32}$")
        state = json.loads((self.state_root / "state.json").read_text(encoding="utf-8"))
        self.assertEqual(state["phase"], "succeeded")
        self.assertEqual(state["deployment_id"], deployment_id)
        self.assertEqual(state["rollback_images"], self.rollback)

        rollback_request = {
            "protocol_version": 1,
            "request_id": self.fixture.request_id,
            "action": "rollback",
            "deployment_id": deployment_id,
        }
        rollback_response = self.invoke("rollback", json.dumps(rollback_request).encode("utf-8"))

        self.assertEqual(rollback_response["result"], "rolled_back")
        self.assertEqual(rollback_response["deployment_id"], deployment_id)
        final_state = json.loads((self.state_root / "state.json").read_text(encoding="utf-8"))
        self.assertEqual(final_state["phase"], "rolled_back")

    def test_failed_apply_attempts_automatic_rollback_and_reports_failed_release(self) -> None:
        patches = self.deployment_patches(
            apply_side_effect=[deployment.OperationError("apply failed"), None]
        )
        started = [patcher.start() for patcher in patches]
        for patcher in patches:
            self.addCleanup(patcher.stop)
        apply_mock = started[-1]

        response = self.invoke("deploy", self.raw_request("deploy"))

        self.assertEqual(response["result"], "rolled_back")
        self.assertEqual(apply_mock.call_count, 2)
        state = json.loads((self.state_root / "state.json").read_text(encoding="utf-8"))
        self.assertEqual(state["phase"], "rolled_back")

    def test_failed_automatic_rollback_sets_recovery_required_and_blocks_next_change(self) -> None:
        patches = self.deployment_patches(
            apply_side_effect=[deployment.OperationError("apply failed"), deployment.OperationError("rollback failed")]
        )
        for patcher in patches:
            patcher.start()
            self.addCleanup(patcher.stop)

        response = self.invoke("deploy", self.raw_request("deploy"))

        self.assertEqual(response["result"], "rejected")
        state = json.loads((self.state_root / "state.json").read_text(encoding="utf-8"))
        self.assertEqual(state["phase"], "recovery_required")

        blocked = self.invoke("deploy", self.raw_request("deploy"))
        self.assertEqual(blocked["result"], "rejected")

    def test_runtime_env_must_be_root_private_before_any_compose_action(self) -> None:
        os.chmod(deployment.RUNTIME_ENV, 0o644)
        patches = self.deployment_patches()
        for patcher in patches:
            patcher.start()
            self.addCleanup(patcher.stop)

        response = self.invoke("deploy", self.raw_request("deploy"))

        self.assertEqual(response["result"], "rejected")
        self.assertFalse((self.state_root / "state.json").exists())

    def compose_model(self, images: dict[str, str]) -> dict[str, object]:
        return {
            "services": {
                name: {
                    "image": images[name],
                    "healthcheck": {"test": ["CMD", "true"]},
                }
                for name in self.services
            }
        }

    def validate_compose_model(self, images: dict[str, str], model: dict[str, object]) -> None:
        images_env = self.fixture.root / "images.env"
        images_env.write_text("TEST_ONLY_VALUE=not-a-secret\n", encoding="utf-8")
        os.chmod(images_env, 0o600)
        with mock.patch.object(
            deployment,
            "_run",
            return_value=subprocess.CompletedProcess(
                [], 0, stdout=json.dumps(model).encode("utf-8"), stderr=b""
            ),
        ):
            deployment._validate_compose_model(self.fixture.config_dir, images_env, images)

    def test_compose_model_accepts_digest_images_and_required_healthchecks(self) -> None:
        images = {
            name: f"docker.io/mstfckrc/{name}@sha256:{'b' * 64}"
            for name in self.services
        }

        self.validate_compose_model(images, self.compose_model(images))

    def test_compose_model_accepts_exact_service_specific_rollback_tags(self) -> None:
        model = self.compose_model(self.rollback)

        self.validate_compose_model(self.rollback, model)

    def test_compose_model_rejects_wrong_service_rollback_tag(self) -> None:
        images = dict(self.rollback)
        images["backend"] = f"localhost/bank-demo-rollback/frontend:{'a' * 32}"

        with self.assertRaises(deployment.OperationError):
            self.validate_compose_model(images, self.compose_model(images))

    def test_compose_model_rejects_buildable_or_unhealthy_service_definition(self) -> None:
        images = {
            name: f"docker.io/mstfckrc/{name}@sha256:{'b' * 64}"
            for name in self.services
        }
        model = self.compose_model(images)
        model["services"]["backend"]["build"] = "."

        with self.assertRaises(deployment.OperationError):
            self.validate_compose_model(images, model)

        model = self.compose_model(images)
        model["services"]["backend"]["healthcheck"]["test"] = []
        with self.assertRaises(deployment.OperationError):
            self.validate_compose_model(images, model)


if __name__ == "__main__":
    unittest.main()
