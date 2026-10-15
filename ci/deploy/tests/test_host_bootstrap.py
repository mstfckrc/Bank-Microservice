"""Safe-install tests for the validate-only Bank host bootstrap."""

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

    def run_bootstrap(self) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(BOOTSTRAP), "--root", str(self.root)],
            check=False,
            capture_output=True,
            text=True,
            env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"},
        )

    def test_installs_only_validate_contract_with_restrictive_modes(self) -> None:
        result = self.run_bootstrap()

        self.assertEqual(result.returncode, 0, result.stderr)
        config_path = self.root / "etc/bank-demo/deploy.json"
        config = json.loads(config_path.read_text(encoding="utf-8"))
        snapshot = self.root / "etc/bank-demo/configs" / config["config_id"]
        self.assertEqual(config["protocol_version"], 1)
        self.assertEqual(
            {path.name for path in snapshot.iterdir()},
            {"release-services.txt", "validate-release-catalog.sh", "prepare-release-env.sh"},
        )
        self.assertEqual(
            (self.root / "usr/local/libexec/bank-demo-deploy/protocol.py").read_bytes(),
            (REPOSITORY_ROOT / "ci/deploy/protocol.py").read_bytes(),
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

    def test_repeated_install_is_idempotent(self) -> None:
        first = self.run_bootstrap()
        second = self.run_bootstrap()

        self.assertEqual(first.returncode, 0, first.stderr)
        self.assertEqual(second.returncode, 0, second.stderr)

    def test_refuses_to_overwrite_different_existing_config(self) -> None:
        first = self.run_bootstrap()
        self.assertEqual(first.returncode, 0, first.stderr)
        config_path = self.root / "etc/bank-demo/deploy.json"
        original = config_path.read_bytes()
        config_path.write_text('{"protocol_version":1,"config_id":"other"}\n', encoding="utf-8")

        second = self.run_bootstrap()

        self.assertEqual(second.returncode, 2)
        self.assertIn("Refusing to overwrite", second.stderr)
        self.assertNotEqual(config_path.read_bytes(), original)


if __name__ == "__main__":
    unittest.main()
