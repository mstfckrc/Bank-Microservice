#!/usr/bin/env python3
"""Install the validate-only Bank host helper and immutable catalog snapshot.

The production invocation is intended to be run once by an administrator on the
Bank VM. ``--root`` is only for isolated tests; it never changes the default
installation paths used by the protocol.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import stat
import sys
import tempfile
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SNAPSHOT_FILES = (
    "release-services.txt",
    "validate-release-catalog.sh",
    "prepare-release-env.sh",
)
HELPER_FILES = ("protocol.py",)
WRAPPER_SOURCE = REPOSITORY_ROOT / "ci" / "deploy" / "bank-demo-deploy"


def fail(message: str) -> "NoReturn":
    print(message, file=sys.stderr)
    raise SystemExit(2)


def content_id(files: tuple[Path, ...]) -> str:
    digest = hashlib.sha256()
    for path in files:
        digest.update(path.name.encode("utf-8") + b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return "catalog-" + digest.hexdigest()[:16]


def check_directory(path: Path, *, create: bool, real_install: bool) -> None:
    try:
        path.lstat()
    except FileNotFoundError:
        if not create:
            fail(f"Required installation directory is missing: {path}")
        path.mkdir(mode=0o755)
    info = path.lstat()
    if not stat.S_ISDIR(info.st_mode):
        fail(f"Installation path is not a real directory: {path}")
    if info.st_mode & 0o022:
        fail(f"Installation directory is group/world writable: {path}")
    if real_install and info.st_uid != 0:
        fail(f"Installation directory is not owned by root: {path}")


def check_file(path: Path, expected: bytes, mode: int, *, real_install: bool) -> bool:
    """Return True for an exact already-installed file; refuse all conflicts."""
    try:
        info = path.lstat()
    except FileNotFoundError:
        return False
    if not stat.S_ISREG(info.st_mode):
        fail(f"Refusing to replace a non-regular installation path: {path}")
    if real_install and info.st_uid != 0:
        fail(f"Installed file is not owned by root: {path}")
    if info.st_mode & 0o022:
        fail(f"Installed file is group/world writable: {path}")
    if stat.S_IMODE(info.st_mode) != mode or path.read_bytes() != expected:
        fail(f"Refusing to overwrite a different existing file: {path}")
    return True


def install_file(path: Path, data: bytes, mode: int, *, real_install: bool) -> None:
    if check_file(path, data, mode, real_install=real_install):
        return
    fd, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fchmod(stream.fileno(), mode)
            if real_install:
                os.fchown(stream.fileno(), 0, 0)
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def install_snapshot(target: Path, files: tuple[Path, ...], *, real_install: bool) -> None:
    try:
        target.lstat()
    except FileNotFoundError:
        target_exists = False
    else:
        target_exists = True
    if target_exists:
        info = target.lstat()
        if not stat.S_ISDIR(info.st_mode):
            fail(f"Refusing to replace a non-directory catalog snapshot: {target}")
        if info.st_mode & 0o022 or (real_install and info.st_uid != 0):
            fail(f"Existing catalog snapshot has unsafe ownership or permissions: {target}")
        for source in files:
            check_file(target / source.name, source.read_bytes(), 0o644, real_install=real_install)
        if set(path.name for path in target.iterdir()) != set(source.name for source in files):
            fail(f"Existing catalog snapshot has unexpected files: {target}")
        return

    temporary = Path(tempfile.mkdtemp(prefix=".catalog-install-", dir=target.parent))
    try:
        os.chmod(temporary, 0o755)
        if real_install:
            os.chown(temporary, 0, 0)
        for source in files:
            destination = temporary / source.name
            destination.write_bytes(source.read_bytes())
            os.chmod(destination, 0o644)
            if real_install:
                os.chown(destination, 0, 0)
        os.replace(temporary, target)
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)


def install(root: Path) -> None:
    real_install = root == Path("/")
    if real_install and os.geteuid() != 0:
        fail("Run the production bootstrap as root (for example, with sudo).")

    helper_sources = tuple(REPOSITORY_ROOT / "ci" / "deploy" / name for name in HELPER_FILES)
    snapshot_sources = tuple(REPOSITORY_ROOT / "ci" / name for name in SNAPSHOT_FILES)
    for source in helper_sources + snapshot_sources + (WRAPPER_SOURCE,):
        if not source.is_file() or source.is_symlink():
            fail(f"Required bootstrap source is missing or not a regular file: {source.name}")

    config_id = content_id(helper_sources + snapshot_sources)
    etc_root = root / "etc"
    bank_root = etc_root / "bank-demo"
    configs_root = bank_root / "configs"
    libexec_root = root / "usr" / "local" / "libexec" / "bank-demo-deploy"
    sbin_root = root / "usr" / "local" / "sbin"

    for directory in (etc_root, bank_root, configs_root, root / "usr", root / "usr" / "local",
                      root / "usr" / "local" / "libexec", libexec_root, sbin_root):
        check_directory(directory, create=True, real_install=real_install)

    snapshot = configs_root / config_id
    install_snapshot(snapshot, snapshot_sources, real_install=real_install)

    for source in helper_sources:
        destination = libexec_root / source.name
        install_file(destination, source.read_bytes(), 0o644, real_install=real_install)
    install_file(sbin_root / "bank-demo-deploy", WRAPPER_SOURCE.read_bytes(), 0o755,
                 real_install=real_install)

    config_path = bank_root / "deploy.json"
    config_data = (json.dumps({"protocol_version": 1, "config_id": config_id},
                              separators=(",", ":"), sort_keys=True) + "\n").encode()
    install_file(config_path, config_data, 0o644, real_install=real_install)

    print(f"Installed validate-only Bank host helper; config_id={config_id}.")
    print("No SSH account, sudoers rule, key, runtime secret, Compose file, or container was changed.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("/"),
        help="Installation root; only use a temporary directory for tests.",
    )
    args = parser.parse_args()
    root = args.root.resolve()
    if not root.is_dir():
        fail("Installation root must be an existing directory.")
    install(root)


if __name__ == "__main__":
    main()
