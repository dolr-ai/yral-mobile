#!/bin/bash

##################################################
# We call this from an Xcode run script.
##################################################

set -euo pipefail

if [[ -z "$PROJECT_DIR" ]]; then
  echo "Must provide PROJECT_DIR environment variable set to the Xcode project directory." 1>&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
MANIFEST_PATH="${SCRIPT_DIR}/../rust-agent/Cargo.toml"
export PATH="$HOME/.cargo/bin:$PATH"

# Workaround for macOS Big Sur / cargo-lipo
# https://github.com/TimNN/cargo-lipo/issues/41#issuecomment-774793892
if [[ -n "${DEVELOPER_SDK_DIR:-}" ]]; then
  export LIBRARY_PATH="${DEVELOPER_SDK_DIR}/MacOSX.sdk/usr/lib:${LIBRARY_PATH:-}"
fi

####################################################
# 1) Decide which architectures to build:
#    - CI => only aarch64-apple-ios
#    - Local => depends on PLATFORM_NAME (sim vs device).
####################################################
if [[ "$CI" == "true" ]]; then
  # For CI, build only device arch
  TARGETS="aarch64-apple-ios"
else
  # Local development
  if [[ "$PLATFORM_NAME" == "iphonesimulator" ]]; then
    TARGETS="aarch64-apple-ios-sim,x86_64-apple-ios"
  else
    TARGETS="aarch64-apple-ios,x86_64-apple-ios"
  fi
fi

echo "[rust-build] manifest : $MANIFEST_PATH"
echo "[rust-build] targets  : $TARGETS"

DEPLOY_VERSION="${IPHONEOS_DEPLOYMENT_TARGET:-${IPHONESIMULATOR_DEPLOYMENT_TARGET:-}}"
if [[ -n "$DEPLOY_VERSION" ]]; then
  export RUSTFLAGS="-C link-arg=-miphoneos-version-min=${DEPLOY_VERSION}"
  echo "[rust-build] using RUSTFLAGS=${RUSTFLAGS}"
fi

####################################################
# 2) Decide debug or release based on $CONFIGURATION
#    (set by Xcode for the active scheme)
####################################################
if [[ "$CONFIGURATION" == "Release" ]]; then
  echo "BUILDING RUST LIBRARY FOR RELEASE ($TARGETS)"
  cargo lipo --release \
    --manifest-path "$MANIFEST_PATH" \
    --targets "$TARGETS" \
    -p yral-mobile-swift-binding
else
  echo "BUILDING RUST LIBRARY FOR DEBUG ($TARGETS)"
  cargo lipo \
    --manifest-path "$MANIFEST_PATH" \
    --targets "$TARGETS" \
    -p yral-mobile-swift-binding
fi