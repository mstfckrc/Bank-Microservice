#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <release-manifest.tsv>" >&2
    exit 2
fi

script_dir=$(CDPATH= cd "$(dirname "$0")" && pwd)
catalog_file="$script_dir/release-services.txt"
catalog_count=$(sh "$script_dir/validate-release-catalog.sh" "$catalog_file")

awk -F '\t' -v catalog_file="$catalog_file" -v catalog_count="$catalog_count" '
function fail(message) {
    printf "Invalid release manifest at line %d: %s\n", FNR, message > "/dev/stderr"
    failed = 1
}

{
    sub(/\r$/, "")
}

FILENAME == catalog_file && FNR == 1 { next }

FILENAME == catalog_file && /^[[:space:]]*(#|$)/ { next }

FILENAME == catalog_file {
    count = split($0, fields, "|")
    if (count == 5) {
        order[++service_count] = fields[1]
        repository[fields[1]] = fields[3]
        image_env[fields[1]] = fields[5]
    }
    next
}

FILENAME != catalog_file && FNR == 1 {
    if ($0 != "service\timage\tdigest") fail("unexpected header")
    manifest_header = 1
    next
}

FILENAME != catalog_file {
    if (NF != 3) {
        fail("expected exactly three tab-separated fields")
        next
    }

    service = $1
    image = $2
    digest_value = $3

    if (!(service in repository)) {
        fail("service is not present in the trusted release catalog")
        next
    }
    if (service in seen) {
        fail("duplicate service entry")
        next
    }

    expected_prefix = "docker.io/mstfckrc/" repository[service] ":sha-"
    if (substr(image, 1, length(expected_prefix)) != expected_prefix) {
        fail("image repository or tag does not match the trusted release catalog")
        next
    }

    source_sha = substr(image, length(expected_prefix) + 1)
    if (length(source_sha) != 40 || source_sha ~ /[^0-9a-f]/) {
        fail("image tag must contain a full lowercase 40-character source commit")
        next
    }
    if (source_commit != "" && source_commit != source_sha) {
        fail("image tags refer to different source commits")
        next
    }
    source_commit = source_sha

    if (length(digest_value) != 71 || substr(digest_value, 1, 7) != "sha256:" || substr(digest_value, 8) ~ /[^0-9a-f]/) {
        fail("invalid sha256 image digest")
        next
    }

    seen[service] = 1
    digest[service] = digest_value
    manifest_count++
}

END {
    if (!manifest_header) {
        printf "Invalid release manifest: header is missing\n" > "/dev/stderr"
        failed = 1
    }
    if (service_count != catalog_count || manifest_count != catalog_count) {
        printf "Invalid release manifest: expected %s catalog services, found %d manifest entries\n", catalog_count, manifest_count > "/dev/stderr"
        failed = 1
    }
    for (i = 1; i <= service_count; i++) {
        service = order[i]
        if (!(service in seen)) {
            printf "Invalid release manifest: missing service %s\n", service > "/dev/stderr"
            failed = 1
        }
    }
    if (failed) exit 1

    for (i = 1; i <= service_count; i++) {
        service = order[i]
        printf "%s=docker.io/mstfckrc/%s@%s\n", image_env[service], repository[service], digest[service]
    }
    printf "Validated %d release images for source commit %s.\n", manifest_count, source_commit > "/dev/stderr"
}
' "$catalog_file" "$1"
