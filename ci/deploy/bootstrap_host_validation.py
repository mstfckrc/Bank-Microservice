#!/usr/bin/env python3
"""Install the Bank host deploy helper, runtime env copy, and immutable snapshot.

The production invocation is intended to be run once by an administrator on the
Bank VM. ``--root`` is only for isolated tests; it never changes the default
installation paths used by the protocol.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import stat
import sys
import tempfile
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SNAPSHOT_FILES = (
    REPOSITORY_ROOT / "ci" / "release-services.txt",
    REPOSITORY_ROOT / "ci" / "validate-release-catalog.sh",
    REPOSITORY_ROOT / "ci" / "prepare-release-env.sh",
    REPOSITORY_ROOT / "docker-compose.yml",
    REPOSITORY_ROOT / "ci" / "deploy" / "compose.images.yml",
)
HELPER_FILES = ("protocol.py", "deployment.py")
WRAPPER_SOURCE = REPOSITORY_ROOT / "ci" / "deploy" / "bank-demo-deploy"
RUNTIME_ENV_SOURCE = REPOSITORY_ROOT / ".env"
CONFIG_ID_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")


def fail(message: str) -> "NoReturn":
    print(message, file=sys.stderr)
    raise SystemExit(2)


def content_id(files: tuple[Path, ...]) -> str:
    digest = hashlib.sha256()
    for path in files:
        digest.update(path.name.encode("utf-8") + b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return "deploy-" + digest.hexdigest()[:16]


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


def replace_managed_file(path: Path, data: bytes, mode: int, *, real_install: bool) -> None:
    """Atomically upgrade a previously installed, safely owned helper file."""
    try:
        info = path.lstat()
    except FileNotFoundError:
        install_file(path, data, mode, real_install=real_install)
        return
    if not stat.S_ISREG(info.st_mode):
        fail(f"Refusing to replace a non-regular managed file: {path}")
    if real_install and info.st_uid != 0:
        fail(f"Managed file is not owned by root: {path}")
    if info.st_mode & 0o022 or stat.S_IMODE(info.st_mode) != mode:
        fail(f"Managed file has unsafe ownership or permissions: {path}")
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


def update_config_pointer(path: Path, data: bytes, *, bank_root: Path,
                          real_install: bool) -> None:
    """Switch only from a valid existing snapshot, and do so as the final step."""
    try:
        info = path.lstat()
    except FileNotFoundError:
        install_file(path, data, 0o644, real_install=real_install)
        return
    if not stat.S_ISREG(info.st_mode):
        fail(f"Refusing to replace a non-regular deployment config: {path}")
    if real_install and info.st_uid != 0:
        fail(f"Existing deployment config is not owned by root: {path}")
    if info.st_mode & 0o022 or stat.S_IMODE(info.st_mode) != 0o644:
        fail(f"Existing deployment config has unsafe ownership or permissions: {path}")
    try:
        current = json.loads(path.read_text(encoding="utf-8"))
        if (set(current) != {"protocol_version", "config_id"}
                or type(current["protocol_version"]) is not int
                or current["protocol_version"] != 1
                or not isinstance(current["config_id"], str)
                or not CONFIG_ID_RE.fullmatch(current["config_id"])):
            fail("Refusing to replace an unknown existing deployment config.")
        if current["config_id"].startswith("deploy-"):
            if path.read_bytes() == data:
                return
            old_snapshot = bank_root / "configs" / current["config_id"]
            if not _verify_previous_deploy(current["config_id"], old_snapshot,
                                            bank_root=bank_root, real_install=real_install):
                fail("Refusing to replace an unverified existing deployment config.")
        elif not current["config_id"].startswith("catalog-"):
            fail("Refusing to replace a deployment config outside the supported upgrade path.")
        else:
            old_snapshot = bank_root / "configs" / current["config_id"]
            old_info = old_snapshot.lstat()
            if (not stat.S_ISDIR(old_info.st_mode) or old_info.st_mode & 0o022
                    or (real_install and old_info.st_uid != 0)):
                fail("Refusing to replace a deployment config with an unsafe prior snapshot.")
            old_files = {
                "release-services.txt", "validate-release-catalog.sh", "prepare-release-env.sh"
            }
            if {item.name for item in old_snapshot.iterdir()} != old_files:
                fail("Refusing to replace a deployment config with an unknown prior snapshot.")
            for filename in old_files:
                old_file = old_snapshot / filename
                old_file_info = old_file.lstat()
                if (not stat.S_ISREG(old_file_info.st_mode) or old_file_info.st_mode & 0o022
                        or stat.S_IMODE(old_file_info.st_mode) != 0o644
                        or (real_install and old_file_info.st_uid != 0)):
                    fail("Refusing to replace a deployment config with an unsafe prior snapshot.")
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, KeyError, TypeError):
        fail("Refusing to replace an invalid existing deployment config.")

    fd, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fchmod(stream.fileno(), 0o644)
            if real_install:
                os.fchown(stream.fileno(), 0, 0)
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def _verify_previous_deploy(config_id: str, snapshot: Path, *, bank_root: Path,
                            real_install: bool) -> bool:
    try:
        info = snapshot.lstat()
        if (not stat.S_ISDIR(info.st_mode) or info.st_mode & 0o022
                or (real_install and info.st_uid != 0)):
            return False
        snapshot_names = {source.name for source in SNAPSHOT_FILES}
        if {item.name for item in snapshot.iterdir()} != snapshot_names:
            return False
        for source in [snapshot / item.name for item in SNAPSHOT_FILES]:
            source_info = source.lstat()
            if (not stat.S_ISREG(source_info.st_mode) or source_info.st_mode & 0o022
                    or stat.S_IMODE(source_info.st_mode) != 0o644
                    or (real_install and source_info.st_uid != 0)):
                return False
        return config_id.startswith("deploy-")
    except OSError:
        return False


def install_runtime_env(root: Path, *, real_install: bool, source: Path) -> None:
    try:
        source_info = source.lstat()
    except OSError:
        fail("The Bank Compose .env file is not available for the root-only runtime copy.")
    if (not stat.S_ISREG(source_info.st_mode) or source.is_symlink()
            or source_info.st_mode & 0o077 or source_info.st_size < 1
            or source_info.st_size > 1024 * 1024):
        fail("The Bank Compose .env file must be a private regular file of at most 1 MiB.")
    try:
        contents = source.read_bytes()
    except OSError:
        fail("The Bank Compose .env file could not be read safely.")
    if b"\0" in contents:
        fail("The Bank Compose .env file contains an invalid byte.")
    target = root / "etc" / "bank-demo" / "runtime.env"
    try:
        target_info = target.lstat()
    except FileNotFoundError:
        install_file(target, contents, 0o600, real_install=real_install)
        return
    except OSError:
        fail("The Bank runtime environment file could not be inspected safely.")
    if (not stat.S_ISREG(target_info.st_mode) or target_info.st_mode & 0o077
            or stat.S_IMODE(target_info.st_mode) != 0o600
            or (real_install and target_info.st_uid != 0)):
        fail("The installed Bank runtime environment has unsafe ownership or permissions.")
    if target.read_bytes() != contents:
        fail("The Bank runtime environment differs; review and update it as a separate admin action.")


def install_state_directory(root: Path, *, real_install: bool) -> None:
    path = root / "var" / "lib" / "bank-demo-deploy"
    try:
        path.lstat()
    except FileNotFoundError:
        path.mkdir(mode=0o700)
        if real_install:
            os.chown(path, 0, 0)
    info = path.lstat()
    if (not stat.S_ISDIR(info.st_mode) or stat.S_IMODE(info.st_mode) != 0o700
            or (real_install and info.st_uid != 0)):
        fail("The Bank deployment state directory has unsafe ownership or permissions.")
    lock = path / "operation.lock"
    try:
        lock_info = lock.lstat()
    except FileNotFoundError:
        fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        try:
            if real_install:
                os.fchown(fd, 0, 0)
        finally:
            os.close(fd)
    except OSError:
        fail("The Bank deployment lock could not be inspected safely.")
    else:
        if (not stat.S_ISREG(lock_info.st_mode) or stat.S_IMODE(lock_info.st_mode) != 0o600
                or (real_install and lock_info.st_uid != 0)):
            fail("The Bank deployment lock has unsafe ownership or permissions.")


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


def install(root: Path, runtime_env_source: Path = RUNTIME_ENV_SOURCE) -> None:
    real_install = root == Path("/")
    if real_install and os.geteuid() != 0:
        fail("Run the production bootstrap as root (for example, with sudo).")

    helper_sources = tuple(REPOSITORY_ROOT / "ci" / "deploy" / name for name in HELPER_FILES)
    snapshot_sources = SNAPSHOT_FILES
    for source in helper_sources + snapshot_sources + (WRAPPER_SOURCE,):
        if not source.is_file() or source.is_symlink():
            fail(f"Required bootstrap source is missing or not a regular file: {source.name}")

    config_id = content_id(helper_sources + snapshot_sources + (WRAPPER_SOURCE,))
    etc_root = root / "etc"
    bank_root = etc_root / "bank-demo"
    configs_root = bank_root / "configs"
    libexec_root = root / "usr" / "local" / "libexec" / "bank-demo-deploy"
    sbin_root = root / "usr" / "local" / "sbin"

    for directory in (etc_root, bank_root, configs_root, root / "usr", root / "usr" / "local",
                      root / "usr" / "local" / "libexec", libexec_root, sbin_root):
        check_directory(directory, create=True, real_install=real_install)

    for directory in (root / "var", root / "var" / "lib"):
        check_directory(directory, create=True, real_install=real_install)
    install_runtime_env(root, real_install=real_install, source=runtime_env_source)
    install_state_directory(root, real_install=real_install)

    snapshot = configs_root / config_id
    install_snapshot(snapshot, snapshot_sources, real_install=real_install)

    for source in helper_sources:
        destination = libexec_root / source.name
        replace_managed_file(destination, source.read_bytes(), 0o644, real_install=real_install)
    replace_managed_file(sbin_root / "bank-demo-deploy", WRAPPER_SOURCE.read_bytes(), 0o755,
                         real_install=real_install)

    config_path = bank_root / "deploy.json"
    config_data = (json.dumps({"protocol_version": 1, "config_id": config_id},
                              separators=(",", ":"), sort_keys=True) + "\n").encode()
    update_config_pointer(config_path, config_data, bank_root=bank_root, real_install=real_install)

    print(f"Installed Bank deployment helper and root-only runtime environment; config_id={config_id}.")
    print("No SSH account, sudoers rule, key, live Compose configuration, or container was changed.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("/"),
        help="Installation root; only use a temporary directory for tests.",
    )
    parser.add_argument("--runtime-env-source", type=Path, default=RUNTIME_ENV_SOURCE,
                        help=argparse.SUPPRESS)
    args = parser.parse_args()
    root = args.root.resolve()
    if not root.is_dir():
        fail("Installation root must be an existing directory.")
    install(root, args.runtime_env_source.resolve())


if __name__ == "__main__":
    main()
