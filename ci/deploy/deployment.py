#!/usr/bin/env python3
"""Root-side, fail-closed Docker Compose deployment operations."""

from __future__ import annotations

import fcntl
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import tempfile
import time
import uuid
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Iterator

import protocol


STATE_ROOT = Path("/var/lib/bank-demo-deploy")
RUNTIME_ENV = Path("/etc/bank-demo/runtime.env")
PROJECT_DIRECTORY = Path("/home/mstfckrc/Bank-Microservice")
DOCKER_BIN = "/usr/bin/docker"
HEALTH_WAIT_SECONDS = 900
PULL_TIMEOUT_SECONDS = 2700
UP_TIMEOUT_SECONDS = 1200
ROLLBACK_TIMEOUT_SECONDS = 1200
DEPLOYMENT_ID_RE = re.compile(r"^deploy-[0-9a-f]{32}$")
IMAGE_REF_RE = re.compile(r"^docker\.io/mstfckrc/[a-z0-9][a-z0-9-]*@sha256:[0-9a-f]{64}$")
IMAGE_ID_RE = re.compile(r"^sha256:[0-9a-f]{64}$")
SAFE_PHASES = {"succeeded", "rolled_back", "failed_before_apply"}


class OperationError(Exception):
    pass


def _trusted_directory(path: Path, *, create: bool = False, mode: int = 0o700) -> None:
    try:
        info = path.lstat()
    except FileNotFoundError:
        if not create:
            raise OperationError("Bank deployment state directory is missing.")
        path.mkdir(mode=mode)
        info = path.lstat()
    if (not stat.S_ISDIR(info.st_mode) or info.st_uid != protocol.TRUSTED_OWNER_UID
            or info.st_mode & 0o077):
        raise OperationError("Bank deployment state directory has unsafe ownership or permissions.")


def _trusted_regular(path: Path, *, mode: int | None = None) -> os.stat_result:
    try:
        info = path.lstat()
    except OSError as exc:
        raise OperationError("A required Bank deployment file is unavailable.") from exc
    if (not stat.S_ISREG(info.st_mode) or info.st_uid != protocol.TRUSTED_OWNER_UID
            or info.st_mode & 0o077 or (mode is not None and stat.S_IMODE(info.st_mode) != mode)):
        raise OperationError("A required Bank deployment file has unsafe ownership or permissions.")
    return info


def _atomic_write(path: Path, data: bytes, mode: int = 0o600) -> None:
    fd, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fchmod(stream.fileno(), mode)
            if protocol.TRUSTED_OWNER_UID == 0:
                os.fchown(stream.fileno(), 0, 0)
            os.fsync(stream.fileno())
        os.replace(temporary, path)
        dir_fd = os.open(path.parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0))
        try:
            os.fsync(dir_fd)
        finally:
            os.close(dir_fd)
    finally:
        temporary.unlink(missing_ok=True)


def _load_state() -> dict[str, Any] | None:
    path = STATE_ROOT / "state.json"
    try:
        _trusted_regular(path, mode=0o600)
    except OperationError:
        if not path.exists() and not path.is_symlink():
            return None
        raise
    try:
        state = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise OperationError("Bank deployment state is unreadable; refusing another change.") from exc
    if (not isinstance(state, dict) or state.get("schema") != 1
            or state.get("phase") not in {
                "prepared", "pulling", "applying", "checking", "succeeded",
                "rolling_back", "rolled_back", "failed_before_apply", "recovery_required",
            }):
        raise OperationError("Bank deployment state is invalid; refusing another change.")
    return state


def _save_state(state: dict[str, Any]) -> None:
    _atomic_write(
        STATE_ROOT / "state.json",
        (json.dumps(state, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8"),
    )


@contextmanager
def _operation_lock() -> Iterator[None]:
    _trusted_directory(STATE_ROOT)
    lock_path = STATE_ROOT / "operation.lock"
    flags = os.O_CREAT | os.O_RDWR | getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(lock_path, flags, 0o600)
    try:
        info = os.fstat(fd)
        if (not stat.S_ISREG(info.st_mode) or info.st_uid != protocol.TRUSTED_OWNER_UID
                or stat.S_IMODE(info.st_mode) != 0o600):
            raise OperationError("Bank deployment lock has unsafe ownership or permissions.")
        deadline = time.monotonic() + 60
        while True:
            try:
                fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    raise OperationError("Another Bank deployment operation holds the host lock.")
                time.sleep(0.25)
        yield
    finally:
        os.close(fd)


def _run(args: list[str], *, timeout: int, input_data: bytes | None = None,
         stdout: int = subprocess.PIPE) -> subprocess.CompletedProcess[bytes]:
    try:
        result = subprocess.run(
            args, input=input_data, stdin=subprocess.DEVNULL if input_data is None else None,
            stdout=stdout, stderr=subprocess.PIPE, check=False, timeout=timeout,
            env={"PATH": "/usr/bin:/bin", "LC_ALL": "C", "HOME": "/root"},
            cwd=PROJECT_DIRECTORY,
        )
    except subprocess.TimeoutExpired as exc:
        raise OperationError("A bounded Docker deployment command timed out.") from exc
    except OSError as exc:
        raise OperationError("A required Docker deployment command could not start.") from exc
    if result.returncode != 0:
        raise OperationError("A Docker deployment command failed; host state was preserved for review.")
    return result


def _compose_base(config_dir: Path, images_env: Path) -> list[str]:
    _trusted_regular(RUNTIME_ENV, mode=0o600)
    _trusted_regular(images_env, mode=0o600)
    for filename in ("docker-compose.yml", "compose.images.yml"):
        protocol.require_trusted_file(config_dir / filename)
    return [
        DOCKER_BIN, "compose", "--project-name", protocol.PROJECT_NAME,
        "--project-directory", str(PROJECT_DIRECTORY),
        "--env-file", str(RUNTIME_ENV), "--env-file", str(images_env),
        "-f", str(config_dir / "docker-compose.yml"),
        "-f", str(config_dir / "compose.images.yml"),
    ]


def _parse_images_env(data: bytes, config_dir: Path) -> tuple[dict[str, str], bytes]:
    validator = config_dir / "prepare-release-env.sh"
    protocol.require_trusted_file(validator)
    try:
        result = subprocess.run(
            ["/bin/sh", str(validator), "/dev/stdin"], input=data,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False, timeout=20,
            env={"PATH": "/usr/bin:/bin", "LC_ALL": "C"},
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise OperationError("The trusted host release validator could not complete.") from exc
    if result.returncode != 0:
        raise OperationError("The release manifest did not pass the trusted host validator.")
    catalog = protocol.read_service_catalog(config_dir)
    expected_env: dict[str, str] = {}
    for line in (config_dir / "release-services.txt").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        service, _context, repository, _target, image_env = line.split("|")
        expected_env[service] = image_env
    try:
        values: dict[str, str] = {}
        for line in result.stdout.decode("ascii").splitlines():
            key, separator, value = line.partition("=")
            if not separator or key in values or not value:
                raise ValueError
            values[key] = value
        expected_keys = {expected_env[name] for name in catalog}
        if set(values) != expected_keys:
            raise ValueError
        images = {name: values[expected_env[name]] for name in catalog}
        if any(not IMAGE_REF_RE.fullmatch(value) for value in images.values()):
            raise ValueError
    except (UnicodeDecodeError, ValueError, KeyError) as exc:
        raise OperationError("The trusted host validator produced invalid image inputs.") from exc
    return images, result.stdout


def _write_private(path: Path, data: bytes) -> None:
    _atomic_write(path, data, 0o600)


def _env_bytes(images: dict[str, str], config_dir: Path) -> bytes:
    image_env: dict[str, str] = {}
    for line in (config_dir / "release-services.txt").read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            service, _context, _repository, _target, key = line.split("|")
            image_env[service] = key
    return ("".join(f"{image_env[name]}={images[name]}\n" for name in images)).encode("ascii")


def _validate_compose_model(config_dir: Path, images_env: Path,
                            expected_images: dict[str, str]) -> None:
    result = _run(
        _compose_base(config_dir, images_env) + ["config", "--format", "json"],
        timeout=30,
    )
    try:
        model = json.loads(result.stdout)
        services = model["services"]
        catalog = protocol.read_service_catalog(config_dir)
        for name in catalog:
            service = services[name]
            local_rollback = expected_images[name].startswith("localhost/bank-demo-rollback/")
            valid_image = (
                bool(re.fullmatch(
                    rf"localhost/bank-demo-rollback/{re.escape(name)}:[0-9a-f]{{32}}",
                    expected_images[name],
                ))
                if local_rollback else bool(IMAGE_REF_RE.fullmatch(expected_images[name]))
            )
            if (not valid_image or service.get("image") != expected_images[name]
                    or service.get("build") is not None
                    or not isinstance(service.get("healthcheck"), dict)
                    or not service["healthcheck"].get("test")):
                raise ValueError
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
        raise OperationError("The merged Compose model failed the deploy safety checks.") from exc


def _inspect_current_images(config_dir: Path) -> dict[str, str]:
    catalog = protocol.read_service_catalog(config_dir)
    listed = _run([
        DOCKER_BIN, "ps", "--all", "--filter",
        f"label=com.docker.compose.project={protocol.PROJECT_NAME}",
        "--format", '{{.Label "com.docker.compose.service"}}|{{.ID}}|{{.State}}',
    ], timeout=20)
    found: dict[str, tuple[str, str]] = {}
    for raw_line in listed.stdout.decode("ascii", "strict").splitlines():
        fields = raw_line.split("|")
        if len(fields) != 3:
            raise OperationError("Docker returned an invalid application container inventory.")
        name, container_id, state = fields
        if name not in catalog:
            continue
        if name in found or not protocol.CONTAINER_ID_RE.fullmatch(container_id):
            raise OperationError("Docker returned duplicate or invalid Bank application containers.")
        found[name] = (container_id, state)
    if set(found) != set(catalog) or any(state != "running" for _cid, state in found.values()):
        raise OperationError("All catalogued Bank application containers must be running before deploy.")

    images: dict[str, str] = {}
    for name, (container_id, _state) in found.items():
        inspected = _run([
            DOCKER_BIN, "inspect", "--format", "{{.Image}}", container_id,
        ], timeout=15)
        image_id = inspected.stdout.decode("ascii", "strict").strip()
        if not IMAGE_ID_RE.fullmatch(image_id):
            raise OperationError("Docker returned an invalid current application image ID.")
        images[name] = image_id
    return images


def _compose_apply(config_dir: Path, images_env: Path, *, timeout: int) -> None:
    services = protocol.read_service_catalog(config_dir)
    _run(
        _compose_base(config_dir, images_env) + [
            "up", "--no-build", "--no-deps", "--pull", "never",
            "--wait", "--wait-timeout", str(HEALTH_WAIT_SECONDS), *services,
        ],
        timeout=timeout,
        stdout=subprocess.DEVNULL,
    )


def _verify_live(config_dir: Path, expected_images: dict[str, str]) -> None:
    catalog = protocol.read_service_catalog(config_dir)
    listed = _run([
        DOCKER_BIN, "ps", "--all", "--filter",
        f"label=com.docker.compose.project={protocol.PROJECT_NAME}",
        "--format", '{{.Label "com.docker.compose.service"}}|{{.ID}}',
    ], timeout=20)
    containers: dict[str, str] = {}
    for line in listed.stdout.decode("ascii", "strict").splitlines():
        name, separator, container_id = line.partition("|")
        if not separator or name not in catalog:
            continue
        if name in containers or not protocol.CONTAINER_ID_RE.fullmatch(container_id):
            raise OperationError("Docker returned duplicate or invalid Bank application containers.")
        containers[name] = container_id
    if set(containers) != set(catalog):
        raise OperationError("Not all Bank application services are present after Compose apply.")
    for name, container_id in containers.items():
        inspected = _run([
            DOCKER_BIN, "inspect", "--format",
            "{{.Config.Image}}|{{.Image}}|{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}not_configured{{end}}",
            container_id,
        ], timeout=15)
        fields = inspected.stdout.decode("ascii", "strict").strip().split("|")
        if (len(fields) != 4 or fields[0] != expected_images[name]
                or not IMAGE_ID_RE.fullmatch(fields[1])
                or fields[2] != "running" or fields[3] != "healthy"):
            raise OperationError("A Bank application container did not reach its expected healthy image state.")


def _write_images_file(path: Path, images: dict[str, str], config_dir: Path) -> None:
    _write_private(path, _env_bytes(images, config_dir))


def _tag_baseline(images: dict[str, str], config_dir: Path, deployment_id: str) -> dict[str, str]:
    rollback_images: dict[str, str] = {}
    for service, image_id in images.items():
        reference = f"localhost/bank-demo-rollback/{service}:{deployment_id.removeprefix('deploy-')}"
        _run([DOCKER_BIN, "image", "tag", image_id, reference], timeout=30,
             stdout=subprocess.DEVNULL)
        rollback_images[service] = reference
    return rollback_images


def _read_request(raw_request: bytes, action: str) -> dict[str, Any]:
    if len(raw_request) > protocol.MAX_REQUEST_BYTES:
        protocol.fail("Protocol request exceeds the configured size limit.")
    request = protocol.parse_json(raw_request, what="Bank helper request")
    request_id = request.get("request_id")
    if not isinstance(request_id, str) or not protocol.REQUEST_ID_RE.fullmatch(request_id):
        protocol.fail("Bank helper request has an invalid request ID.")
    if (request.get("action") != action
            or type(request.get("protocol_version")) is not int
            or request["protocol_version"] != 1):
        protocol.fail("Bank helper request has an unsupported action or protocol version.")
    return request


def _response(request_id: str, action: str, *, result: str, message: str,
              config_id: str | None, release_build_number: str | None = None,
              source_commit: str | None = None, manifest_sha256: str | None = None,
              deployment_id: str | None = None) -> None:
    protocol.write_response({
        "protocol_version": 1,
        "request_id": request_id,
        "action": action,
        "release_build_number": release_build_number,
        "result": result,
        "source_commit": source_commit,
        "manifest_sha256": manifest_sha256,
        "config_id": config_id,
        "deployment_id": deployment_id,
        "message": message,
        "status_report": None,
    })


def _deploy(raw_request: bytes) -> None:
    info = protocol.validate_release_request(raw_request, "deploy")
    _trusted_regular(RUNTIME_ENV, mode=0o600)
    deployment_id = "deploy-" + uuid.uuid4().hex
    config_dir: Path = info["config_dir"]
    _trusted_directory(STATE_ROOT)
    deployment_root = STATE_ROOT / "deployments"
    _trusted_directory(deployment_root)
    deployment_dir = deployment_root / deployment_id
    expected_images, image_env_data = _parse_images_env(info["manifest"], config_dir)
    mutated = False
    with _operation_lock():
        previous = _load_state()
        if previous and previous.get("phase") not in SAFE_PHASES:
            raise OperationError("A prior Bank deployment is incomplete; inspect status before any new change.")
        deployment_dir.mkdir(mode=0o700)
        os.chmod(deployment_dir, 0o700)
        if protocol.TRUSTED_OWNER_UID == 0:
            os.chown(deployment_dir, 0, 0)
        _write_private(deployment_dir / "release-manifest.tsv", info["manifest"])
        images_env = deployment_dir / "images.env"
        _write_private(images_env, image_env_data)
        state: dict[str, Any] = {
            "schema": 1,
            "phase": "prepared",
            "deployment_id": deployment_id,
            "release_build_number": info["release_build_number"],
            "source_commit": info["source_commit"],
            "manifest_sha256": info["manifest_sha256"],
            "config_id": info["config_id"],
            "images": expected_images,
            "rollback_images": {},
            "started_at": int(time.time()),
        }
        baseline: dict[str, str] | None = None
        try:
            baseline = _inspect_current_images(config_dir)
            rollback_images = _tag_baseline(baseline, config_dir, deployment_id)
            state["rollback_images"] = rollback_images
            _write_images_file(deployment_dir / "rollback.env", rollback_images, config_dir)
            _save_state(state)

            _validate_compose_model(config_dir, images_env, expected_images)
            state["phase"] = "pulling"
            _save_state(state)
            _run(_compose_base(config_dir, images_env) + ["pull", *protocol.read_service_catalog(config_dir)],
                 timeout=PULL_TIMEOUT_SECONDS, stdout=subprocess.DEVNULL)
            state["phase"] = "applying"
            _save_state(state)
            mutated = True
            _compose_apply(config_dir, images_env, timeout=UP_TIMEOUT_SECONDS)
            state["phase"] = "checking"
            _save_state(state)
            _verify_live(config_dir, expected_images)
            state["phase"] = "succeeded"
            state["finished_at"] = int(time.time())
            _save_state(state)
        except Exception as exc:
            message = str(exc) if isinstance(exc, OperationError) else "Bank deployment failed safely."
            if mutated and baseline is not None:
                try:
                    state["phase"] = "rolling_back"
                    _save_state(state)
                    rollback_env = deployment_dir / "rollback.env"
                    _compose_apply(config_dir, rollback_env, timeout=ROLLBACK_TIMEOUT_SECONDS)
                    _verify_live(config_dir, rollback_images)
                    state["phase"] = "rolled_back"
                    state["finished_at"] = int(time.time())
                    state["failure"] = "deployment_failed_rolled_back"
                    _save_state(state)
                    _response(
                        info["request_id"], "deploy", result="rolled_back",
                        message="Deployment did not pass health checks; the previous application images were restored.",
                        config_id=info["config_id"],
                        release_build_number=info["release_build_number"],
                        source_commit=info["source_commit"],
                        manifest_sha256=info["manifest_sha256"], deployment_id=deployment_id,
                    )
                    return
                except Exception:
                    state["phase"] = "recovery_required"
                    state["finished_at"] = int(time.time())
                    _save_state(state)
                    message = "Deployment and automatic rollback could not be verified; recovery is required."
            else:
                state["phase"] = "failed_before_apply"
                state["finished_at"] = int(time.time())
                state["failure"] = "failed_before_apply"
                _save_state(state)
            _response(
                info["request_id"], "deploy", result="rejected", message=message,
                config_id=info["config_id"], release_build_number=info["release_build_number"],
                source_commit=info["source_commit"], manifest_sha256=info["manifest_sha256"],
                deployment_id=deployment_id,
            )
            return

    _response(
        info["request_id"], "deploy", result="deployed",
        message="All catalogued Bank application containers use the selected digest-pinned images and are healthy.",
        config_id=info["config_id"], release_build_number=info["release_build_number"],
        source_commit=info["source_commit"], manifest_sha256=info["manifest_sha256"],
        deployment_id=deployment_id,
    )


def _rollback(raw_request: bytes) -> None:
    request = _read_request(raw_request, "rollback")
    protocol.require_exact_keys(request, {"protocol_version", "request_id", "action", "deployment_id"},
                                what="Bank helper request")
    deployment_id = request.get("deployment_id")
    if not isinstance(deployment_id, str) or not DEPLOYMENT_ID_RE.fullmatch(deployment_id):
        protocol.fail("Bank helper request has an invalid deployment ID.")
    config_id, config_dir = protocol.load_host_config()
    _trusted_directory(STATE_ROOT)
    with _operation_lock():
        state = _load_state()
        if (not state or state.get("phase") != "succeeded"
                or state.get("deployment_id") != deployment_id
                or not isinstance(state.get("rollback_images"), dict)):
            raise OperationError("Only the currently active successful deployment can be rolled back.")
        rollback_images = state["rollback_images"]
        if set(rollback_images) != set(protocol.read_service_catalog(config_dir)):
            raise OperationError("The recorded rollback image set is incomplete.")
        deployment_dir = STATE_ROOT / "deployments" / deployment_id
        _trusted_directory(deployment_dir)
        rollback_env = deployment_dir / "rollback.env"
        _trusted_regular(rollback_env, mode=0o600)
        state["phase"] = "rolling_back"
        _save_state(state)
        try:
            _validate_compose_model(config_dir, rollback_env, rollback_images)
            _compose_apply(config_dir, rollback_env, timeout=ROLLBACK_TIMEOUT_SECONDS)
            _verify_live(config_dir, rollback_images)
            state["phase"] = "rolled_back"
            state["rolled_back_at"] = int(time.time())
            _save_state(state)
        except Exception as exc:
            state["phase"] = "recovery_required"
            state["failure"] = "explicit_rollback_failed"
            _save_state(state)
            message = str(exc) if isinstance(exc, OperationError) else "Rollback could not be verified."
            _response(request["request_id"], "rollback", result="rejected", message=message,
                      config_id=config_id, deployment_id=deployment_id)
            return
    _response(request["request_id"], "rollback", result="rolled_back",
              message="The previous application images were restored and passed health checks.",
              config_id=config_id, deployment_id=deployment_id)


def host_deployment_command(action: str, raw_request: bytes) -> None:
    try:
        if action == "deploy":
            _deploy(raw_request)
        elif action == "rollback":
            _rollback(raw_request)
        else:
            protocol.fail("Bank helper received an unsupported deployment action.")
    except protocol.ReleaseRejected:
        request = protocol.parse_json(raw_request, what="Bank helper request")
        try:
            config_id, _config_dir = protocol.load_host_config()
        except SystemExit:
            config_id = None
        _response(
            request.get("request_id", ""), action, result="rejected",
            message="Release manifest was rejected by the Bank host's trusted catalog. No change was made.",
            config_id=config_id,
            release_build_number=request.get("release_build_number"),
            manifest_sha256=request.get("manifest_sha256"),
        )
    except OperationError as exc:
        try:
            request = protocol.parse_json(raw_request, what="Bank helper request")
            config_id, _config_dir = protocol.load_host_config()
            _response(request.get("request_id", ""), action, result="rejected",
                      message=str(exc), config_id=config_id,
                      release_build_number=request.get("release_build_number"),
                      manifest_sha256=request.get("manifest_sha256"),
                      deployment_id=request.get("deployment_id"))
        except Exception:
            protocol.fail("Bank deployment could not complete safely; inspect host state before retrying.")


def current_deployment_state() -> tuple[str | None, str | None]:
    try:
        _trusted_directory(STATE_ROOT)
        state = _load_state()
    except OperationError:
        return None, "unknown"
    if state is None:
        return None, "not_deployed"
    return state.get("deployment_id"), state.get("phase")
