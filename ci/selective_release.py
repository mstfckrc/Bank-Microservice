#!/usr/bin/env python3
"""Plan selective image publication and assemble a complete release manifest.

This program intentionally uses only the Python standard library. It does not
build images, contact a registry, or mutate the source checkout.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable


CATALOG_HEADER = "# compose-service|build-context|dockerhub-repository|dockerfile-target"
MANIFEST_HEADER = "service\timage\tdigest"
SERVICE_RE = re.compile(r"^[a-z][a-z0-9-]*$")
CONTEXT_RE = re.compile(r"^[A-Za-z0-9._/-]+$")
REPOSITORY_RE = re.compile(r"^[a-z0-9][a-z0-9-]*$")
TARGET_RE = re.compile(r"^[a-z][a-z0-9-]*$")
SHA_RE = re.compile(r"^[0-9a-f]{40}$")
DIGEST_RE = re.compile(r"^sha256:[0-9a-f]{64}$")

# These files control validation/orchestration, not bytes in an image context.
# Jenkinsfile.release is deliberately *not* in this set: changes to the trusted
# publisher definition conservatively rebuild the catalog.
CONTROL_ONLY_PATHS = {
    "Jenkinsfile",
    "ci/selective_release.py",
    "ci/tests/test_selective_release.py",
    "ci/validate-release-catalog.sh",
}
DOC_PATHS = {"README.md", "README"}


class ReleaseError(ValueError):
    """Raised when source, baseline, or generated release data is unsafe."""


@dataclass(frozen=True)
class Service:
    service: str
    context: str
    repository: str
    target: str


@dataclass(frozen=True)
class ManifestEntry:
    service: str
    image: str
    digest: str


def parse_catalog_text(text: str, label: str, allow_empty: bool = False) -> list[Service]:
    lines = text.splitlines()
    if not lines or lines[0] != CATALOG_HEADER:
        raise ReleaseError(f"{label}: unexpected or missing catalog header")

    rows: list[Service] = []
    services: set[str] = set()
    repositories: set[str] = set()
    for line_number, line in enumerate(lines[1:], start=2):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        fields = line.split("|")
        if len(fields) != 4:
            raise ReleaseError(f"{label}:{line_number}: expected four pipe-separated fields")
        row = Service(*fields)
        if not SERVICE_RE.fullmatch(row.service):
            raise ReleaseError(f"{label}:{line_number}: invalid service name")
        if (
            not CONTEXT_RE.fullmatch(row.context)
            or row.context.startswith("/")
            or (
                row.context != "."
                and any(part in {"", ".", ".."} for part in row.context.split("/"))
            )
        ):
            raise ReleaseError(f"{label}:{line_number}: invalid build context")
        if not REPOSITORY_RE.fullmatch(row.repository):
            raise ReleaseError(f"{label}:{line_number}: invalid registry repository")
        if not TARGET_RE.fullmatch(row.target):
            raise ReleaseError(f"{label}:{line_number}: invalid Docker target")
        if row.service in services:
            raise ReleaseError(f"{label}:{line_number}: duplicate service")
        if row.repository in repositories:
            raise ReleaseError(f"{label}:{line_number}: duplicate repository")
        services.add(row.service)
        repositories.add(row.repository)
        rows.append(row)

    if not rows and not allow_empty:
        raise ReleaseError(f"{label}: catalog has no service entries")
    return rows


def parse_manifest_text(text: str, label: str) -> dict[str, ManifestEntry]:
    lines = text.splitlines()
    if not lines or lines[0] != MANIFEST_HEADER:
        raise ReleaseError(f"{label}: unexpected or missing manifest header")
    entries: dict[str, ManifestEntry] = {}
    for line_number, line in enumerate(lines[1:], start=2):
        if not line:
            continue
        fields = line.split("\t")
        if len(fields) != 3:
            raise ReleaseError(f"{label}:{line_number}: expected three tab-separated fields")
        service, image, digest = fields
        if not SERVICE_RE.fullmatch(service):
            raise ReleaseError(f"{label}:{line_number}: invalid service name")
        if service in entries:
            raise ReleaseError(f"{label}:{line_number}: duplicate service")
        if not DIGEST_RE.fullmatch(digest):
            raise ReleaseError(f"{label}:{line_number}: invalid image digest")
        if not re.fullmatch(
            r"docker\.io/mstfckrc/[a-z0-9][a-z0-9-]*:sha-[0-9a-f]{40}", image
        ):
            raise ReleaseError(f"{label}:{line_number}: image reference is not a source-SHA tag")
        entries[service] = ManifestEntry(service, image, digest)
    return entries


def manifest_text(entries: Iterable[ManifestEntry]) -> str:
    return MANIFEST_HEADER + "\n" + "".join(
        f"{entry.service}\t{entry.image}\t{entry.digest}\n" for entry in entries
    )


def validate_manifest_catalog(
    manifest: dict[str, ManifestEntry], catalog: list[Service], label: str
) -> None:
    by_service = {row.service: row for row in catalog}
    if set(manifest) != set(by_service):
        missing = sorted(set(by_service) - set(manifest))
        extra = sorted(set(manifest) - set(by_service))
        raise ReleaseError(f"{label}: service set mismatch (missing={missing}, extra={extra})")
    for service, entry in manifest.items():
        repository = by_service[service].repository
        if not entry.image.startswith(f"docker.io/mstfckrc/{repository}:sha-"):
            raise ReleaseError(f"{label}: image repository does not match catalog for {service}")


def path_is_within(path: str, context: str) -> bool:
    if context == ".":
        return True
    return path == context or path.startswith(context.rstrip("/") + "/")


def compute_plan(
    current_catalog: list[Service],
    previous_catalog: list[Service] | None,
    previous_manifest: dict[str, ManifestEntry] | None,
    changed_paths: list[str],
    baseline_sha: str | None,
) -> dict:
    current_by_service = {row.service: row for row in current_catalog}
    if baseline_sha is None:
        selected = {
            row.service: "no successful publication baseline; publish full catalog"
            for row in current_catalog
        }
        return {
            "schema": 1,
            "baseline_sha": None,
            "selected": selected,
            "reused": {},
            "removed": [],
            "changed_paths": changed_paths,
        }

    if not SHA_RE.fullmatch(baseline_sha):
        raise ReleaseError("baseline source SHA is invalid")
    if previous_catalog is None or previous_manifest is None:
        raise ReleaseError("successful baseline is incomplete; refusing an implicit full/empty fallback")
    validate_manifest_catalog(previous_manifest, previous_catalog, "previous manifest")
    previous_by_service = {row.service: row for row in previous_catalog}
    selected: dict[str, str] = {}

    for row in current_catalog:
        old = previous_by_service.get(row.service)
        if old is None:
            selected[row.service] = "service added to release catalog"
        elif old != row:
            selected[row.service] = "service build mapping changed in release catalog"

    for path in changed_paths:
        display_path = json.dumps(path, ensure_ascii=True)
        matched_current = [
            row for row in current_catalog if path_is_within(path, row.context)
        ]
        if matched_current:
            for row in matched_current:
                selected.setdefault(row.service, f"build-context input changed: {display_path}")
            continue

        if path == "ci/release-public.sh":
            if "frontend" in current_by_service:
                selected.setdefault("frontend", "public frontend build settings changed")
            continue

        if path == "ci/release-services.txt":
            # Mapping changes were handled row-by-row above; comments/order are inert.
            continue

        if path == "Jenkinsfile.release":
            for row in current_catalog:
                selected.setdefault(row.service, "trusted publisher definition changed")
            continue

        if path in CONTROL_ONLY_PATHS or path in DOC_PATHS or path.startswith("docs/"):
            continue

        # A removed context is still a known old image input; it cannot affect a
        # current image unless it is also covered by a current context above.
        if any(path_is_within(path, row.context) for row in previous_catalog):
            continue

        # Unknown paths are never silently dropped. Conservatively select all.
        for row in current_catalog:
            selected.setdefault(row.service, f"unclassified repository input changed: {display_path}")

    removed = sorted(set(previous_by_service) - set(current_by_service))
    reused: dict[str, dict[str, str]] = {}
    for row in current_catalog:
        if row.service not in selected:
            entry = previous_manifest.get(row.service)
            if entry is None:
                raise ReleaseError(f"previous successful manifest is missing {row.service}")
            if not entry.image.startswith(f"docker.io/mstfckrc/{row.repository}:sha-"):
                raise ReleaseError(f"cannot reuse image from a different repository for {row.service}")
            reused[row.service] = asdict(entry)

    return {
        "schema": 1,
        "baseline_sha": baseline_sha,
        "selected": selected,
        "reused": reused,
        "removed": removed,
        "changed_paths": changed_paths,
    }


def _git_text(root: Path, *args: str) -> str:
    try:
        return subprocess.check_output(["git", *args], cwd=root, text=True, stderr=subprocess.PIPE)
    except subprocess.CalledProcessError as exc:
        detail = exc.stderr.strip() if exc.stderr else "git command failed"
        raise ReleaseError(detail) from exc


def changed_paths(root: Path, baseline: str, source: str) -> list[str]:
    try:
        raw = subprocess.check_output(
            ["git", "diff", "--no-renames", "--name-only", "-z", baseline, source, "--"],
            cwd=root,
            stderr=subprocess.PIPE,
        )
    except subprocess.CalledProcessError as exc:
        detail = exc.stderr.decode(errors="replace").strip()
        raise ReleaseError(detail or "git diff failed") from exc
    return [os.fsdecode(item) for item in raw.split(b"\0") if item]


def write_atomic(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(content)
        os.replace(temporary, path)
    except BaseException:
        try:
            os.unlink(temporary)
        except FileNotFoundError:
            pass
        raise


def run_plan(args: argparse.Namespace) -> None:
    root = Path(args.repo_root).resolve()
    source_sha = args.source_sha
    if not SHA_RE.fullmatch(source_sha):
        raise ReleaseError("source SHA is invalid")
    current = parse_catalog_text(Path(args.current_catalog).read_text(encoding="utf-8"), args.current_catalog)

    if args.baseline_sha:
        if not SHA_RE.fullmatch(args.baseline_sha):
            raise ReleaseError("baseline source SHA is invalid")
        previous_text = _git_text(root, "show", f"{args.baseline_sha}:ci/release-services.txt")
        previous_catalog = parse_catalog_text(previous_text, "baseline release catalog")
        manifest_path = Path(args.previous_manifest)
        if not manifest_path.is_file():
            raise ReleaseError("successful baseline is missing release-manifest.tsv")
        previous_manifest = parse_manifest_text(
            manifest_path.read_text(encoding="utf-8"), str(manifest_path)
        )
        changes = changed_paths(root, args.baseline_sha, source_sha)
        plan = compute_plan(current, previous_catalog, previous_manifest, changes, args.baseline_sha)
    else:
        plan = compute_plan(current, None, None, [], None)
    plan["source_sha"] = source_sha

    output_dir = Path(args.output_dir)
    selected_rows = [row for row in current if row.service in plan["selected"]]
    build_catalog = CATALOG_HEADER + "\n" + "".join(
        f"{row.service}|{row.context}|{row.repository}|{row.target}\n" for row in selected_rows
    )
    reuse_entries = [
        ManifestEntry(**plan["reused"][row.service])
        for row in current
        if row.service in plan["reused"]
    ]
    write_atomic(output_dir / "build-catalog.txt", build_catalog)
    write_atomic(output_dir / "reuse-manifest.tsv", manifest_text(reuse_entries))
    write_atomic(output_dir / "build-manifest.tsv", MANIFEST_HEADER + "\n")
    write_atomic(output_dir / "plan.json", json.dumps(plan, indent=2, sort_keys=True) + "\n")

    print(f"Selected {len(selected_rows)} of {len(current)} images; reusing {len(reuse_entries)}.")
    for row in current:
        if row.service in plan["selected"]:
            print(f"BUILD {row.service}: {plan['selected'][row.service]}")
        else:
            print(f"REUSE {row.service}: unchanged image digest")
    for service in plan["removed"]:
        print(f"REMOVE {service}: absent from current catalog")


def run_complete(args: argparse.Namespace) -> None:
    current = parse_catalog_text(Path(args.current_catalog).read_text(encoding="utf-8"), args.current_catalog)
    selected = parse_catalog_text(
        Path(args.build_catalog).read_text(encoding="utf-8"), args.build_catalog, allow_empty=True
    )
    selected_by_service = {row.service: row for row in selected}
    current_by_service = {row.service: row for row in current}
    if any(current_by_service.get(name) != row for name, row in selected_by_service.items()):
        raise ReleaseError("build catalog is not an exact subset of the current catalog")

    if not SHA_RE.fullmatch(args.source_sha):
        raise ReleaseError("source SHA is invalid")
    reuse = parse_manifest_text(Path(args.reuse_manifest).read_text(encoding="utf-8"), args.reuse_manifest)
    built = parse_manifest_text(Path(args.built_manifest).read_text(encoding="utf-8"), args.built_manifest)
    expected_reuse = set(current_by_service) - set(selected_by_service)
    if set(reuse) != expected_reuse:
        raise ReleaseError("reuse manifest does not exactly cover unselected current services")
    if set(built) != set(selected_by_service):
        raise ReleaseError("built manifest does not exactly cover selected services")

    plan = json.loads(Path(args.plan_json).read_text(encoding="utf-8"))
    if (
        not isinstance(plan, dict)
        or plan.get("schema") != 1
        or plan.get("source_sha") != args.source_sha
        or not isinstance(plan.get("selected"), dict)
        or set(plan["selected"]) != set(selected_by_service)
        or not isinstance(plan.get("reused"), dict)
        or not isinstance(plan.get("removed"), list)
    ):
        raise ReleaseError("plan and build catalog do not agree")
    if set(plan["reused"]) != expected_reuse:
        raise ReleaseError("plan and reuse manifest do not agree")
    for service, entry in reuse.items():
        if plan["reused"].get(service) != asdict(entry):
            raise ReleaseError(f"reuse manifest differs from planned prior digest for {service}")
    if any(not isinstance(reason, str) or not reason for reason in plan["selected"].values()):
        raise ReleaseError("selected image is missing a build reason")

    merged: list[ManifestEntry] = []
    summary = ["source_sha\tbaseline_sha\tservice\taction\treason\timage\tdigest\n"]
    baseline_sha = plan["baseline_sha"] or "none"
    for row in current:
        if row.service in selected_by_service:
            entry = built[row.service]
            expected_image = f"docker.io/mstfckrc/{row.repository}:sha-{args.source_sha}"
            if entry.image != expected_image:
                raise ReleaseError(f"built image reference does not match source/catalog for {row.service}")
            action = "build"
            reason = plan["selected"][row.service]
        else:
            entry = reuse[row.service]
            if not entry.image.startswith(f"docker.io/mstfckrc/{row.repository}:sha-"):
                raise ReleaseError(f"reused image repository does not match catalog for {row.service}")
            action = "reuse"
            reason = "unchanged image inputs; prior successful digest retained"
        merged.append(entry)
        summary.append(
            f"{args.source_sha}\t{baseline_sha}\t{row.service}\t{action}\t{reason}\t{entry.image}\t{entry.digest}\n"
        )
    for service in plan["removed"]:
        summary.append(
            f"{args.source_sha}\t{baseline_sha}\t{service}\tremoved\tabsent from current catalog\t\t\n"
        )

    write_atomic(Path(args.output_manifest), manifest_text(merged))
    write_atomic(Path(args.output_source_sha), args.source_sha + "\n")
    write_atomic(Path(args.output_summary), "".join(summary))
    print(f"Assembled complete manifest: {len(merged)} current services; source baseline {args.source_sha}.")


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)
    plan = commands.add_parser("plan", help="select builds and prepare reuse inputs")
    plan.add_argument("--repo-root", required=True)
    plan.add_argument("--source-sha", required=True)
    plan.add_argument("--baseline-sha", default="")
    plan.add_argument("--current-catalog", required=True)
    plan.add_argument("--previous-manifest", default="")
    plan.add_argument("--output-dir", required=True)
    plan.set_defaults(func=run_plan)

    complete = commands.add_parser("complete", help="validate build/reuse rows and write full release outputs")
    complete.add_argument("--source-sha", required=True)
    complete.add_argument("--current-catalog", required=True)
    complete.add_argument("--build-catalog", required=True)
    complete.add_argument("--plan-json", required=True)
    complete.add_argument("--reuse-manifest", required=True)
    complete.add_argument("--built-manifest", required=True)
    complete.add_argument("--output-manifest", required=True)
    complete.add_argument("--output-source-sha", required=True)
    complete.add_argument("--output-summary", required=True)
    complete.set_defaults(func=run_complete)
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        args.func(args)
    except (OSError, ReleaseError, json.JSONDecodeError) as exc:
        print(f"Selective image release failed closed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
