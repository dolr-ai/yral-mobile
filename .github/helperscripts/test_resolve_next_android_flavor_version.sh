#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/resolve_next_android_flavor_version.sh"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}

trap cleanup EXIT

assert_equals() {
  local expected="$1"
  local actual="$2"
  local message="$3"

  if [[ "$expected" != "$actual" ]]; then
    echo "Assertion failed: $message" >&2
    echo "Expected: $expected" >&2
    echo "Actual:   $actual" >&2
    exit 1
  fi
}

assert_fails() {
  local message="$1"
  shift

  if "$@" >/dev/null 2>&1; then
    echo "Assertion failed: $message" >&2
    exit 1
  fi
}

cat <<'EOF' >"$TMP_DIR/androidApp.build.gradle.kts"
android {
    productFlavors {
        create("staging") {
            versionName = "2.8.1" // ci:staging-version-name
        }
        create("prod") {
            versionName = "3.4.9" // ci:prod-version-name
        }
    }
}
EOF

assert_equals \
  "2.8.2" \
  "$("$SCRIPT" "staging" "$TMP_DIR/androidApp.build.gradle.kts")" \
  "increments the staging patch version"

assert_equals \
  "3.4.10" \
  "$("$SCRIPT" "prod" "$TMP_DIR/androidApp.build.gradle.kts")" \
  "increments the prod patch version"

cat <<'EOF' >"$TMP_DIR/invalid.build.gradle.kts"
android {
    productFlavors {
        create("staging") {
            versionName = "2.8" // ci:staging-version-name
        }
    }
}
EOF

assert_fails \
  "rejects non-semver version names" \
  "$SCRIPT" "staging" "$TMP_DIR/invalid.build.gradle.kts"

cat <<'EOF' >"$TMP_DIR/missing.build.gradle.kts"
android {
    productFlavors {
        create("staging") {
            versionCode = 1597 // ci:staging-version-code
        }
    }
}
EOF

assert_fails \
  "rejects missing version name markers" \
  "$SCRIPT" "staging" "$TMP_DIR/missing.build.gradle.kts"

echo "All tests passed"
