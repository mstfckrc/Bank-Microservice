#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <release-services.txt>" >&2
    exit 2
fi

awk -F '|' '
function fail(message) {
    printf "Invalid release catalog at line %d: %s\n", FNR, message > "/dev/stderr"
    failed = 1
}

FNR == 1 {
    if ($0 != "# compose-service|build-context|dockerhub-repository|dockerfile-target") {
        fail("unexpected header")
    }
    next
}

/^[[:space:]]*$/ { next }
/^[[:space:]]*#/ { next }

NF != 4 {
    fail("expected exactly four pipe-separated fields")
    next
}

{
    service = $1
    context = $2
    repository = $3
    target = $4

    if (service !~ /^[a-z][a-z0-9-]*$/) fail("invalid Compose service name")
    if (context !~ /^[A-Za-z0-9._/-]+$/ || context ~ /^\// || context ~ /(^|\/)\.\.(\/|$)/) fail("invalid build context")
    if (repository !~ /^[a-z0-9][a-z0-9-]*$/) fail("invalid registry repository")
    if (target !~ /^[a-z][a-z0-9-]*$/) fail("invalid Docker build target")

    if (service in services) fail("duplicate Compose service")
    if (repository in repositories) fail("duplicate registry repository")

    services[service] = 1
    repositories[repository] = 1
    count++
}

END {
    if (FNR == 0) {
        printf "Invalid release catalog: file is empty\n" > "/dev/stderr"
        failed = 1
    }
    if (count < 1) {
        printf "Invalid release catalog: no service entries\n" > "/dev/stderr"
        failed = 1
    }
    if (failed) exit 1
    print count
}
' "$1"
