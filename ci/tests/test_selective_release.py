import json
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

from ci.selective_release import (
    CATALOG_HEADER,
    MANIFEST_HEADER,
    ManifestEntry,
    ReleaseError,
    Service,
    changed_paths,
    compute_plan,
    manifest_text,
    parse_catalog_text,
    parse_manifest_text,
    run_complete,
)


SOURCE = "a" * 40
OLD_SOURCE = "b" * 40
DIGEST = "sha256:" + "1" * 64


def rows():
    return [
        Service("backend", "Bank-Demo-Backend", "backend", "runtime"),
        Service("api-gateway", "api-gateway", "api-gateway", "runtime"),
        Service("frontend", "Bank-Demo-Frontend", "frontend", "runner"),
    ]


def previous_manifest(catalog=None):
    catalog = catalog or rows()
    return {
        row.service: ManifestEntry(
            row.service,
            f"docker.io/mstfckrc/{row.repository}:sha-{OLD_SOURCE}",
            DIGEST,
        )
        for row in catalog
    }


class SelectiveReleaseTests(unittest.TestCase):
    def test_missing_baseline_selects_full_catalog(self):
        plan = compute_plan(rows(), None, None, [], None)
        self.assertEqual(set(plan["selected"]), {row.service for row in rows()})
        self.assertFalse(plan["reused"])

    def test_readme_change_reuses_every_image(self):
        catalog = rows()
        for path in (
            "README.md",
            "JENKINS-PR-SMOKE.md",
            "Detaylı Yüksek Seviye Sistem Mimarisi (Kuşbakışı).png",
            "Güvenlik ve Veri Süzgeci Akışı (Zero-Trust JWT).png",
            "Keycloak Sistem Mimarisi.png",
            "Yüksek Seviye Sistem Mimarisi (Kuşbakışı).png",
            "İç Hatlar (Endpoint & FeignClient) İletişim Ağı.png",
        ):
            with self.subTest(path=path):
                plan = compute_plan(catalog, catalog, previous_manifest(), [path], OLD_SOURCE)
                self.assertFalse(plan["selected"])
                self.assertEqual(set(plan["reused"]), {row.service for row in catalog})

    def test_docs_only_completion_advances_source_and_preserves_all_digests(self):
        catalog = rows()
        old_manifest = previous_manifest(catalog)
        plan = compute_plan(catalog, catalog, old_manifest, ["README.md"], OLD_SOURCE)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "catalog.txt").write_text(
                CATALOG_HEADER + "\n" + "".join(
                    f"{row.service}|{row.context}|{row.repository}|{row.target}\n" for row in catalog
                ), encoding="utf-8"
            )
            (root / "build-catalog.txt").write_text(CATALOG_HEADER + "\n", encoding="utf-8")
            (root / "reuse.tsv").write_text(manifest_text(old_manifest.values()), encoding="utf-8")
            (root / "built.tsv").write_text(MANIFEST_HEADER + "\n", encoding="utf-8")
            plan["source_sha"] = SOURCE
            (root / "plan.json").write_text(json.dumps(plan), encoding="utf-8")
            args = type("Args", (), {
                "source_sha": SOURCE,
                "current_catalog": str(root / "catalog.txt"),
                "build_catalog": str(root / "build-catalog.txt"),
                "plan_json": str(root / "plan.json"),
                "reuse_manifest": str(root / "reuse.tsv"),
                "built_manifest": str(root / "built.tsv"),
                "output_manifest": str(root / "final.tsv"),
                "output_source_sha": str(root / "source.sha"),
                "output_summary": str(root / "summary.tsv"),
            })()
            from ci.selective_release import run_complete

            run_complete(args)
            final = parse_manifest_text((root / "final.tsv").read_text(encoding="utf-8"), "final")
            self.assertEqual(final, old_manifest)
            self.assertEqual((root / "source.sha").read_text(encoding="utf-8"), SOURCE + "\n")

    def test_one_java_context_selects_only_that_image(self):
        catalog = rows()
        plan = compute_plan(catalog, catalog, previous_manifest(), ["Bank-Demo-Backend/src/main/App.java"], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {"backend"})
        self.assertEqual(set(plan["reused"]), {"api-gateway", "frontend"})

    def test_root_runtime_settings_reuse_every_image(self):
        catalog = rows()
        for path in (
            ".gitignore",
            ".env.example",
            "alertmanager.yml",
            "docker-compose.yml",
            "nginx.conf",
            "prometheus/alert.rules.yml",
            "prometheus/prometheus.yml",
            "grafana/provisioning/datasources/bank-demo.yml",
            "grafana/dashboards/node-exporter-full.json",
            "keycloak/bank-realm.json",
            "scripts/prepare-local-env.py",
        ):
            with self.subTest(path=path):
                plan = compute_plan(catalog, catalog, previous_manifest(), [path], OLD_SOURCE)
                self.assertFalse(plan["selected"])
                self.assertEqual(plan["reused"], {
                    service: vars(entry) for service, entry in previous_manifest().items()
                })

    def test_runtime_settings_do_not_expand_a_real_service_change(self):
        catalog = rows()
        plan = compute_plan(catalog, catalog, previous_manifest(), [
            ".env.example", "docker-compose.yml", "nginx.conf",
            "Bank-Demo-Backend/src/main/App.java",
        ], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {"backend"})
        self.assertEqual(set(plan["reused"]), {"api-gateway", "frontend"})

    def test_root_context_still_selects_runtime_files_as_image_inputs(self):
        catalog = [Service("backend", ".", "backend", "runtime")]
        for path in (".env.example", "docker-compose.yml", "nginx.conf"):
            with self.subTest(path=path):
                plan = compute_plan(catalog, catalog, previous_manifest(catalog), [path], OLD_SOURCE)
                self.assertEqual(set(plan["selected"]), {"backend"})
                self.assertFalse(plan["reused"])

    def test_move_between_service_contexts_selects_both_images(self):
        catalog = rows()
        plan = compute_plan(
            catalog,
            catalog,
            previous_manifest(),
            ["Bank-Demo-Backend/src/shared/Contract.java", "api-gateway/src/shared/Contract.java"],
            OLD_SOURCE,
        )
        self.assertEqual(set(plan["selected"]), {"backend", "api-gateway"})

    def test_public_frontend_setting_selects_only_frontend(self):
        catalog = rows()
        plan = compute_plan(catalog, catalog, previous_manifest(), ["ci/release-public.sh"], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {"frontend"})

    def test_publisher_definition_change_is_conservative_full_catalog(self):
        catalog = rows()
        plan = compute_plan(catalog, catalog, previous_manifest(), ["Jenkinsfile.release"], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {row.service for row in catalog})

    def test_catalog_reordering_does_not_rebuild(self):
        catalog = rows()
        previous = [catalog[2], catalog[0], catalog[1]]
        plan = compute_plan(catalog, previous, previous_manifest(previous), ["ci/release-services.txt"], OLD_SOURCE)
        self.assertFalse(plan["selected"])

    def test_added_service_builds_only_new_service(self):
        old = rows()
        current = old + [Service("bill-service", "bill-service", "bill-service", "runtime")]
        plan = compute_plan(current, old, previous_manifest(old), ["ci/release-services.txt"], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {"bill-service"})
        self.assertEqual(set(plan["reused"]), {row.service for row in old})

    def test_removed_service_is_removed_without_rebuilding_survivors(self):
        old = rows()
        current = [row for row in old if row.service != "api-gateway"]
        plan = compute_plan(current, old, previous_manifest(old), ["api-gateway/Dockerfile", "ci/release-services.txt"], OLD_SOURCE)
        self.assertFalse(plan["selected"])
        self.assertEqual(plan["removed"], ["api-gateway"])
        self.assertEqual(set(plan["reused"]), {"backend", "frontend"})

    def test_catalog_mapping_change_builds_only_mapped_service(self):
        old = rows()
        current = [Service("backend", "Bank-Demo-Backend", "backend-v2", "runtime"), *old[1:]]
        plan = compute_plan(current, old, previous_manifest(old), ["ci/release-services.txt"], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {"backend"})

    def test_unknown_repository_path_fails_conservatively_to_full_catalog(self):
        catalog = rows()
        plan = compute_plan(catalog, catalog, previous_manifest(), ["ci/new-shared-build-input.txt"], OLD_SOURCE)
        self.assertEqual(set(plan["selected"]), {row.service for row in catalog})

    def test_nul_delimited_git_paths_preserve_newline_names(self):
        with patch("ci.selective_release.subprocess.check_output", return_value=b"README.md\0ci/new\nfile\0") as run:
            paths = changed_paths(Path("."), OLD_SOURCE, SOURCE)
        self.assertEqual(paths, ["README.md", "ci/new\nfile"])
        self.assertEqual(
            run.call_args.args[0][:6],
            ["git", "diff", "--no-renames", "--name-only", "-z", OLD_SOURCE],
        )

    def test_incomplete_or_invalid_previous_manifest_fails_closed(self):
        catalog = rows()
        partial = previous_manifest(catalog[:-1])
        with self.assertRaises(ReleaseError):
            compute_plan(catalog, catalog, partial, [], OLD_SOURCE)
        with self.assertRaises(ReleaseError):
            parse_manifest_text(MANIFEST_HEADER + "\nbackend\tdocker.io/mstfckrc/backend:latest\tbad\n", "test")

    def test_catalog_header_and_duplicate_validation(self):
        with self.assertRaises(ReleaseError):
            parse_catalog_text("bad header\nbackend|x|backend|runtime\n", "test")
        duplicate = CATALOG_HEADER + "\nbackend|one|backend|runtime\nbackend|two|backend-two|runtime\n"
        with self.assertRaises(ReleaseError):
            parse_catalog_text(duplicate, "test")

    def test_manifest_completion_requires_exact_built_and_reuse_sets(self):
        catalog = rows()
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "catalog.txt").write_text(
                CATALOG_HEADER + "\n" + "".join(
                    f"{row.service}|{row.context}|{row.repository}|{row.target}\n" for row in catalog
                ), encoding="utf-8"
            )
            (root / "build-catalog.txt").write_text(
                CATALOG_HEADER + f"\nbackend|{catalog[0].context}|backend|runtime\n", encoding="utf-8"
            )
            (root / "reuse.tsv").write_text(manifest_text([previous_manifest()["api-gateway"], previous_manifest()["frontend"]]), encoding="utf-8")
            (root / "built.tsv").write_text(
                manifest_text([ManifestEntry("backend", f"docker.io/mstfckrc/backend:sha-{SOURCE}", DIGEST)]),
                encoding="utf-8",
            )
            completion_plan = compute_plan(
                catalog, catalog, previous_manifest(), ["Bank-Demo-Backend/src/main/App.java"], OLD_SOURCE
            )
            completion_plan["source_sha"] = SOURCE
            (root / "plan.json").write_text(json.dumps(completion_plan), encoding="utf-8")
            args = type("Args", (), {
                "source_sha": SOURCE,
                "current_catalog": str(root / "catalog.txt"),
                "build_catalog": str(root / "build-catalog.txt"),
                "plan_json": str(root / "plan.json"),
                "reuse_manifest": str(root / "reuse.tsv"),
                "built_manifest": str(root / "built.tsv"),
                "output_manifest": str(root / "final.tsv"),
                "output_source_sha": str(root / "source.sha"),
                "output_summary": str(root / "summary.tsv"),
            })()
            run_complete(args)
            completed = parse_manifest_text((root / "final.tsv").read_text(encoding="utf-8"), "final")
            self.assertEqual(set(completed), {row.service for row in catalog})
            self.assertEqual((root / "source.sha").read_text(encoding="utf-8"), SOURCE + "\n")
            self.assertIn("\tapi-gateway\treuse\t", (root / "summary.tsv").read_text(encoding="utf-8"))

            (root / "built.tsv").write_text(MANIFEST_HEADER + "\n", encoding="utf-8")
            (root / "final.tsv").unlink()
            with self.assertRaises(ReleaseError):
                run_complete(args)
            self.assertFalse((root / "final.tsv").exists())
            self.assertEqual((root / "source.sha").read_text(encoding="utf-8"), SOURCE + "\n")

            (root / "built.tsv").write_text(
                manifest_text([ManifestEntry("backend", f"docker.io/mstfckrc/backend:sha-{SOURCE}", DIGEST)]),
                encoding="utf-8",
            )
            altered_reuse = [
                ManifestEntry("api-gateway", f"docker.io/mstfckrc/api-gateway:sha-{OLD_SOURCE}", "sha256:" + "9" * 64),
                previous_manifest()["frontend"],
            ]
            (root / "reuse.tsv").write_text(manifest_text(altered_reuse), encoding="utf-8")
            with self.assertRaises(ReleaseError):
                run_complete(args)
            self.assertFalse((root / "final.tsv").exists())
            self.assertEqual((root / "source.sha").read_text(encoding="utf-8"), SOURCE + "\n")


if __name__ == "__main__":
    unittest.main()
