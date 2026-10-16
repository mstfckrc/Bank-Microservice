"""Safe-install tests for the Bank host deployment bootstrap."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
BOOTSTRAP = REPOSITORY_ROOT / "ci" / "deploy" / "bootstrap_host_validation.py"


class HostBootstrapTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory(prefix="bank-host-bootstrap-test-")
        self.addCleanup(self.temp_dir.cleanup)
        self.root = Path(self.temp_dir.name)
        self.runtime_env_source = self.root / "source.env"
        self.runtime_env_source.write_text("TEST_ONLY_VALUE=not-a-secret\n", encoding="utf-8")
        os.chmod(self.runtime_env_source, 0o600)

    def run_bootstrap(self) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(BOOTSTRAP), "--root", str(self.root),
             "--runtime-env-source", str(self.runtime_env_source)],
            check=False,
            capture_output=True,
            text=True,
            env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"},
        )

    def test_installs_deploy_contract_and_runtime_env_with_restrictive_modes(self) -> None:
        result = self.run_bootstrap()

        self.assertEqual(result.returncode, 0, result.stderr)
        config_path = self.root / "etc/bank-demo/deploy.json"
        config = json.loads(config_path.read_text(encoding="utf-8"))
        snapshot = self.root / "etc/bank-demo/configs" / config["config_id"]
        self.assertEqual(config["protocol_version"], 1)
        self.assertEqual(
            {path.name for path in snapshot.iterdir()},
            {
                "release-services.txt",
                "validate-release-catalog.sh",
                "prepare-release-env.sh",
                "docker-compose.yml",
                "compose.images.yml",
            },
        )
        self.assertEqual(
            (self.root / "usr/local/libexec/bank-demo-deploy/protocol.py").read_bytes(),
            (REPOSITORY_ROOT / "ci/deploy/protocol.py").read_bytes(),
        )
        self.assertEqual(
            (self.root / "usr/local/libexec/bank-demo-deploy/deployment.py").read_bytes(),
            (REPOSITORY_ROOT / "ci/deploy/deployment.py").read_bytes(),
        )
        self.assertEqual(
            (self.root / "usr/local/sbin/bank-demo-deploy").read_bytes(),
            (REPOSITORY_ROOT / "ci/deploy/bank-demo-deploy").read_bytes(),
        )
        for path in (config_path, *snapshot.iterdir(),
                     self.root / "usr/local/libexec/bank-demo-deploy/protocol.py"):
            self.assertEqual(path.stat().st_mode & 0o777, 0o644)
        self.assertEqual(
            (self.root / "usr/local/sbin/bank-demo-deploy").stat().st_mode & 0o777,
            0o755,
        )
        runtime_env = self.root / "etc/bank-demo/runtime.env"
        self.assertEqual(runtime_env.read_text(encoding="utf-8"), "TEST_ONLY_VALUE=not-a-secret\n")
        self.assertEqual(runtime_env.stat().st_mode & 0o777, 0o600)
        self.assertEqual((self.root / "var/lib/bank-demo-deploy").stat().st_mode & 0o777, 0o700)
        self.assertEqual((self.root / "var/lib/bank-demo-deploy/operation.lock").stat().st_mode & 0o777, 0o600)

    def test_repeated_install_is_idempotent(self) -> None:
        first = self.run_bootstrap()
        second = self.run_bootstrap()

        self.assertEqual(first.returncode, 0, first.stderr)
        self.assertEqual(second.returncode, 0, second.stderr)

    def test_upgrades_known_catalog_snapshot_and_repeats_idempotently(self) -> None:
        first = self.run_bootstrap()
        self.assertEqual(first.returncode, 0, first.stderr)
        config_path = self.root / "etc/bank-demo/deploy.json"
        current = json.loads(config_path.read_text(encoding="utf-8"))
        old_id = "catalog-test-old"
        old_snapshot = self.root / "etc/bank-demo/configs" / old_id
        old_snapshot.mkdir()
        os.chmod(old_snapshot, 0o755)
        for filename in ("release-services.txt", "validate-release-catalog.sh", "prepare-release-env.sh"):
            source = REPOSITORY_ROOT / "ci" / filename
            (old_snapshot / filename).write_bytes(source.read_bytes())
            os.chmod(old_snapshot / filename, 0o644)
        config_path.write_text(
            json.dumps({"protocol_version": 1, "config_id": old_id}, separators=(",", ":")) + "\n",
            encoding="utf-8",
        )
        os.chmod(config_path, 0o644)

        second = self.run_bootstrap()
        third = self.run_bootstrap()

        self.assertEqual(second.returncode, 0, second.stderr)
        self.assertEqual(third.returncode, 0, third.stderr)
        updated = json.loads(config_path.read_text(encoding="utf-8"))
        self.assertNotEqual(updated["config_id"], old_id)
        self.assertEqual(updated, current)

    def test_refuses_to_replace_unknown_config_pointer(self) -> None:
        first = self.run_bootstrap()
        self.assertEqual(first.returncode, 0, first.stderr)
        config_path = self.root / "etc/bank-demo/deploy.json"
        config_path.write_text('{"protocol_version":1,"config_id":"other"}\n', encoding="utf-8")
        os.chmod(config_path, 0o644)

        second = self.run_bootstrap()

        self.assertEqual(second.returncode, 2)
        self.assertIn("Refusing to replace", second.stderr)


if __name__ == "__main__":
    unittest.main()
