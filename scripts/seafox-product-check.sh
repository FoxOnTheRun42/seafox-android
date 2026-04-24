#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CI_MODE=false
RUN_COMPILE=true
RUN_TESTS=true
RUN_LINT=true
RUN_RELEASE_R8=false
REQUIRE_DEVICE=false

usage() {
    cat <<'USAGE'
Usage: scripts/seafox-product-check.sh [options]

Options:
  --ci             Run with CI-oriented output.
  --skip-compile   Skip Kotlin compilation.
  --skip-tests     Skip JVM unit tests.
  --skip-lint      Skip Android lint.
  --release-r8     Also run the release R8/minify task without producing a signed artifact.
  --device         Require adb and at least one connected device.
  -h, --help       Show this help.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ci)
            CI_MODE=true
            shift
            ;;
        --skip-compile)
            RUN_COMPILE=false
            shift
            ;;
        --skip-tests)
            RUN_TESTS=false
            shift
            ;;
        --skip-lint)
            RUN_LINT=false
            shift
            ;;
        --release-r8)
            RUN_RELEASE_R8=true
            shift
            ;;
        --device)
            REQUIRE_DEVICE=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

section() {
    printf '\n== %s ==\n' "$1"
}

warn() {
    printf 'WARN: %s\n' "$1" >&2
}

require_file() {
    local path="$1"
    if [[ ! -f "$ROOT_DIR/$path" ]]; then
        echo "Missing required file: $path" >&2
        exit 1
    fi
}

section "seaFOX product gate"
echo "Root: $ROOT_DIR"
echo "CI mode: $CI_MODE"

if [[ -x "$ROOT_DIR/gradlew" ]]; then
    GRADLE_CMD=("$ROOT_DIR/gradlew" "--no-daemon")
else
    warn "gradlew is missing or not executable; falling back to system gradle"
    GRADLE_CMD=("gradle" "--no-daemon")
fi

section "Static project health"
require_file "settings.gradle.kts"
require_file "build.gradle.kts"
require_file "app/build.gradle.kts"
require_file "app/proguard-rules.pro"
require_file "app/src/main/AndroidManifest.xml"
require_file "app/src/main/java/com/boat/dashboard/MainActivity.kt"
require_file "app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt"
require_file "app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartWidget.kt"
require_file "app/src/main/java/com/boat/dashboard/ui/widgets/chart/NavigationOverlay.kt"
require_file "docs/PRODUCTION_READINESS.md"
require_file "docs/QA_MATRIX.md"
require_file "docs/RELEASE_CHECKLIST.md"
require_file "docs/BILLING_BACKEND_CONTRACT.md"
require_file "docs/PRIVACY_POLICY_DRAFT.md"
require_file "docs/SAFETY_DISCLAIMER_DRAFT.md"
require_file "scripts/seafox-device-smoke.sh"

if ! grep -q "android.intent.action.MAIN" "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
    echo "Launcher intent is missing in AndroidManifest.xml" >&2
    exit 1
fi

if ! grep -q "com.seafox.nmea_dashboard" "$ROOT_DIR/app/build.gradle.kts"; then
    echo "Application id or namespace check failed" >&2
    exit 1
fi

if [[ ! -d "$ROOT_DIR/app/src/test" ]]; then
    echo "Missing JVM test source set: app/src/test" >&2
    exit 1
fi

java_line="$(java -version 2>&1 | head -n 1 || true)"
echo "Java: ${java_line:-unknown}"
if [[ "$java_line" =~ \"([0-9]+) ]]; then
    java_major="${BASH_REMATCH[1]}"
    if (( java_major > 21 )); then
        warn "Java $java_major can trigger Kotlin daemon fallback. CI uses Java 17."
    fi
fi

section "Gradle wrapper"
"${GRADLE_CMD[@]}" --version

if [[ "$RUN_COMPILE" == true ]]; then
    section "Compile debug Kotlin"
    "${GRADLE_CMD[@]}" :app:compileDebugKotlin
fi

if [[ "$RUN_TESTS" == true ]]; then
    section "Run JVM unit tests"
    "${GRADLE_CMD[@]}" :app:testDebugUnitTest
fi

if [[ "$RUN_LINT" == true ]]; then
    section "Run Android lint"
    "${GRADLE_CMD[@]}" :app:lintDebug
fi

if [[ "$RUN_RELEASE_R8" == true ]]; then
    section "Run release R8/minify"
    "${GRADLE_CMD[@]}" :app:minifyReleaseWithR8
fi

section "Device readiness"
if command -v adb >/dev/null 2>&1; then
    connected_devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
    if [[ -n "$connected_devices" ]]; then
        echo "Connected Android device(s):"
        echo "$connected_devices"
    elif [[ "$REQUIRE_DEVICE" == true ]]; then
        echo "adb is installed, but no connected device is ready" >&2
        exit 1
    else
        warn "adb is installed, but no device is connected. Emulator QA was not run."
    fi
elif [[ "$REQUIRE_DEVICE" == true ]]; then
    echo "adb is required for --device but was not found" >&2
    exit 1
else
    warn "adb is not installed. Emulator/device QA was not run."
fi

section "Product gate complete"
echo "Passed compile/test/lint/static checks selected for this run."
