#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$SCRIPT_DIR/resolve_next_prod_android_version.sh"
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

cat <<'EOF' >"$TMP_DIR/valid.gradle.kts"
android {
    productFlavors {
        prod {
            versionCode = 81 // ci:prod-version-code
            versionName = "2.8.1" // ci:prod-version-name
        }
    }
}
EOF

assert_equals \
  "2.8.2" \
  "$("$SCRIPT" "$TMP_DIR/valid.gradle.kts")" \
  "increments the prod patch version"

cat <<'EOF' >"$TMP_DIR/zero.gradle.kts"
android {
    productFlavors {
        prod {
            versionName = "0.0.0" // ci:prod-version-name
        }
    }
}
EOF

assert_equals \
  "0.0.1" \
  "$("$SCRIPT" "$TMP_DIR/zero.gradle.kts")" \
  "increments from zero"

cat <<'EOF' >"$TMP_DIR/invalid.gradle.kts"
android {
    productFlavors {
        prod {
            versionName = "2.8" // ci:prod-version-name
        }
    }
}
EOF

assert_fails \
  "rejects non-semver prod versions" \
  "$SCRIPT" "$TMP_DIR/invalid.gradle.kts"

cat <<'EOF' >"$TMP_DIR/missing.gradle.kts"
android {
    productFlavors {
        prod {
            versionCode = 81 // ci:prod-version-code
        }
    }
}
EOF

assert_fails \
  "rejects missing prod version markers" \
  "$SCRIPT" "$TMP_DIR/missing.gradle.kts"

echo "All tests passed"
