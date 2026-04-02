#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <flavor-marker> <build-gradle-file>" >&2
  exit 1
fi

FLAVOR_MARKER="$1"
BUILD_FILE="$2"
VERSION_MARKER="ci:${FLAVOR_MARKER}-version-name"

CURRENT_VERSION=$(
  sed -nE "s/^[[:space:]]*versionName = \"([^\"]+)\" \/\/ ${VERSION_MARKER}$/\\1/p" "$BUILD_FILE" \
    | head -n 1
)

if ! [[ "$CURRENT_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Error: could not read ${FLAVOR_MARKER} versionName semver from '$BUILD_FILE', got: '$CURRENT_VERSION'" >&2
  exit 1
fi

IFS=. read -r MAJOR MINOR PATCH <<<"$CURRENT_VERSION"
echo "$MAJOR.$MINOR.$((PATCH + 1))"
