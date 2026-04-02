#!/usr/bin/env bash

set -euo pipefail

BUILD_FILE="${1:-androidApp/build.gradle.kts}"

CURRENT_VERSION=$(
  sed -nE 's/^[[:space:]]*versionName = "([^"]+)" \/\/ ci:prod-version-name$/\1/p' "$BUILD_FILE" \
    | head -n 1
)

if ! [[ "$CURRENT_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Error: could not read prod versionName semver from '$BUILD_FILE', got: '$CURRENT_VERSION'" >&2
  exit 1
fi

IFS=. read -r MAJOR MINOR PATCH <<<"$CURRENT_VERSION"
NEXT_VERSION="$MAJOR.$MINOR.$((PATCH + 1))"

echo "$NEXT_VERSION"
