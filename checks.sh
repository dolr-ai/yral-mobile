#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# ── Prerequisites ─────────────────────────────────────────────────────────────

_require_android_tools() {
    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
    if ! command -v adb &>/dev/null; then
        if [[ -f "$sdk/platform-tools/adb" ]]; then
            export PATH="$sdk/platform-tools:$PATH"
        else
            echo "ERROR: adb not found. Set ANDROID_HOME or install Android SDK platform-tools."
            return 1
        fi
    fi
    if ! command -v emulator &>/dev/null; then
        if [[ -f "$sdk/emulator/emulator" ]]; then
            export PATH="$sdk/emulator:$PATH"
        fi
    fi
}

_require_kubeconfig() {
    if [[ -z "${KUBECONFIG:-}" ]]; then
        if [[ -f "ci-e2e-reader.kubeconfig" ]]; then
            export KUBECONFIG="$(pwd)/ci-e2e-reader.kubeconfig"
        else
            echo "ERROR: KUBECONFIG is not set and ci-e2e-reader.kubeconfig was not found in the repo root."
            echo "  Get the kubeconfig from a team member and place it at: $(pwd)/ci-e2e-reader.kubeconfig"
            return 1
        fi
    fi
    if [[ ! -f "$KUBECONFIG" ]]; then
        echo "ERROR: KUBECONFIG is set but the file does not exist: $KUBECONFIG"
        return 1
    fi
}

# ── Tooling ───────────────────────────────────────────────────────────────────

install_tools() {
    brew install swiftlint cocoapods xcbeautify
    brew tap mobile-dev-inc/tap
    brew install mobile-dev-inc/tap/maestro
}

# ── Android ───────────────────────────────────────────────────────────────────

android_ktlint() {
    ./gradlew ktlintFormat
    ./gradlew ktlintCheck || {
        find . -path "*/build/reports/ktlint/*.txt" | xargs cat 2>/dev/null
        return 1
    }
}

android_detekt() {
    ./gradlew detekt reportMerge --continue
}

android_unit_tests() {
    ./gradlew test -x :maestro:e2e-assert:test
}

android_build_apk() {
    ./gradlew :androidApp:assembleStagingDebug
}

android_start_emulator() {
    _require_android_tools
    if adb devices | grep -q $'\tdevice'; then
        echo "Android emulator already running."
        return 0
    fi
    local avd
    avd=$(emulator -list-avds | head -1)
    if [[ -z "$avd" ]]; then
        echo "ERROR: No AVDs found. Create one in Android Studio first."
        return 1
    fi
    echo "Starting emulator: $avd"
    emulator -avd "$avd" -no-audio -no-snapshot -no-boot-anim &
    echo "Waiting for emulator to finish booting..."
    adb wait-for-device
    until adb shell getprop sys.boot_completed 2>/dev/null | grep -q '1'; do sleep 2; done
    echo "Emulator ready."
}

android_install_apk() {
    _require_android_tools
    adb install -r androidApp/build/outputs/apk/staging/debug/androidApp-staging-debug.apk
}

android_e2e() {
    _require_android_tools
    _require_kubeconfig
    KAFKA_PASSWORD="${KAFKA_PASSWORD:-$(kubectl get secret kafka-console -n kafka -o jsonpath='{.data.password}' | base64 -d)}"
    E2E_PLATFORM=android \
    E2E_APP_ID=com.yral.android \
    KAFKA_BOOTSTRAP=localhost:9092 \
    KAFKA_PASSWORD="$KAFKA_PASSWORD" \
    ./gradlew :maestro:e2e-assert:test
}

# ── iOS ───────────────────────────────────────────────────────────────────────

ios_lint() {
    (cd iosApp && swiftlint)
}

ios_pin_cocoapods() {
    grep -q 'kotlin.apple.cocoapods.bin' local.properties \
        || printf 'kotlin.apple.cocoapods.bin=%s\n' "$(which pod)" >> local.properties
}

ios_build_kmm() {
    ./gradlew :iosSharedUmbrella:podInstall --stacktrace
    ./gradlew :iosSharedUmbrella:linkPodDebugFrameworkIosSimulatorArm64
}

ios_pod_install() {
    (cd iosApp && pod install)
}

_sim_name() {
    xcrun simctl list devices available | grep -m 1 'iPhone' | sed 's/^[[:space:]]*//' | sed 's/ (.*//'
}

_sim_udid() {
    xcrun simctl list devices available -j | python3 -c "
import json, sys
data = json.load(sys.stdin)
for runtime, devices in data['devices'].items():
    if 'iOS' in runtime:
        for d in devices:
            if 'iPhone' in d['name']:
                print(d['udid']); sys.exit(0)
sys.exit(1)"
}

ios_unit_tests() {
    set -o pipefail
    (cd iosApp && xcodebuild test \
        -workspace iosApp.xcworkspace \
        -scheme iosApp \
        -destination "platform=iOS Simulator,name=$(_sim_name)" \
        -configuration Debug \
        -test-timeouts-enabled YES \
        -maximum-test-execution-time-allowance 120 \
        CODE_SIGNING_ALLOWED=NO \
        ENABLE_USER_SCRIPT_SANDBOXING=NO \
        | xcbeautify)
}

ios_build_simulator() {
    (cd iosApp && xcodebuild build \
        -workspace iosApp.xcworkspace \
        -scheme iosApp \
        -configuration Debug \
        -destination "platform=iOS Simulator,name=$(_sim_name)" \
        -derivedDataPath "$(pwd)/../build/DerivedData" \
        CODE_SIGNING_ALLOWED=NO \
        ENABLE_USER_SCRIPT_SANDBOXING=NO)
}

ios_boot_install_simulator() {
    local udid
    udid="$(_sim_udid)"
    xcrun simctl boot "$udid" 2>/dev/null || true
    xcrun simctl bootstatus "$udid" -b
    open -a Simulator
    xcrun simctl install booted build/DerivedData/Build/Products/Debug-iphonesimulator/iosApp.app
}

ios_e2e() {
    _require_kubeconfig
    KAFKA_PASSWORD="${KAFKA_PASSWORD:-$(kubectl get secret kafka-console -n kafka -o jsonpath='{.data.password}' | base64 -d)}"
    E2E_PLATFORM=ios \
    E2E_APP_ID=com.yral.iosApp.staging \
    KAFKA_BOOTSTRAP=localhost:9092 \
    KAFKA_PASSWORD="$KAFKA_PASSWORD" \
    ./gradlew :maestro:e2e-assert:test
}

# ── Kafka ─────────────────────────────────────────────────────────────────────

kafka_port_forward() {
    _require_kubeconfig
    kubectl port-forward -n kafka svc/kafka-cluster-kafka-bootstrap 9092:9092
}

# ── Full suite ────────────────────────────────────────────────────────────────

all() {
    android_ktlint
    android_detekt
    android_unit_tests

    ios_pin_cocoapods
    ios_build_kmm
    ios_pod_install
    ios_lint
    ios_unit_tests

    # Start Kafka port-forward in background, kill it when this script exits
    kafka_port_forward &
    local kpf_pid=$!
    trap "kill $kpf_pid 2>/dev/null || true" EXIT
    sleep 3

    android_build_apk
    android_start_emulator
    android_install_apk
    android_e2e

    ios_build_simulator
    ios_boot_install_simulator
    ios_e2e
}

# ── Dispatch ──────────────────────────────────────────────────────────────────

if [[ $# -eq 0 ]]; then
    all
else
    "$@"
fi
