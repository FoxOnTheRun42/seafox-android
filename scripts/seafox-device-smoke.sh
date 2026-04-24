#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_ID="$(date +%Y%m%d-%H%M%S)"

APP_ID="com.seafox.nmea_dashboard"
MAIN_ACTIVITY=".MainActivity"
SERIAL=""
APK_PATH=""
OUT_DIR="/tmp/seafox-device-smoke/$RUN_ID"
WAIT_SECONDS=8
ALLOW_DOWNGRADE=false
FORCE_STOP=true
CAPTURE_SCREENSHOT=true
CAPTURE_LOGCAT=true
CAPTURE_UI_DUMP=true

usage() {
    cat <<'USAGE'
Usage: scripts/seafox-device-smoke.sh [options]

Non-destructive adb smoke test for seaFOX. The script never runs Gradle and
therefore does not trigger assemble/install/package build-number increments.

Options:
  --serial <id>        Target adb serial. Required when multiple devices are ready.
  --apk <path>         Install an existing APK before launch using adb install -r.
  --allow-downgrade    Add adb install -d when --apk is used.
  --out-dir <path>     Artifact directory. Default: /tmp/seafox-device-smoke/<timestamp>.
  --package <id>       App package id. Default: com.seafox.nmea_dashboard.
  --activity <name>    Launcher activity or full component. Default: .MainActivity.
  --wait <seconds>     Seconds to wait after launch before capture. Default: 8.
  --no-force-stop      Do not force-stop before launching.
  --skip-screenshot    Do not capture screencap PNG.
  --skip-logcat        Do not capture logcat.
  --skip-ui-dump       Do not capture uiautomator XML.
  -h, --help           Show this help.

Examples:
  scripts/seafox-device-smoke.sh --serial emulator-5554
  scripts/seafox-device-smoke.sh --apk app/build/outputs/apk/debug/app-debug.apk
USAGE
}

fatal() {
    printf 'ERROR: %s\n' "$1" >&2
    exit "${2:-1}"
}

warn() {
    printf 'WARN: %s\n' "$1" >&2
}

require_arg() {
    local option="$1"
    local value="${2:-}"
    if [[ -z "$value" || "$value" == --* ]]; then
        fatal "$option requires a value" 2
    fi
}

abs_path() {
    case "$1" in
        /*) printf '%s\n' "$1" ;;
        *) printf '%s\n' "$PWD/$1" ;;
    esac
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --serial)
            require_arg "$1" "${2:-}"
            SERIAL="$2"
            shift 2
            ;;
        --apk)
            require_arg "$1" "${2:-}"
            APK_PATH="$2"
            shift 2
            ;;
        --allow-downgrade)
            ALLOW_DOWNGRADE=true
            shift
            ;;
        --out-dir)
            require_arg "$1" "${2:-}"
            OUT_DIR="$2"
            shift 2
            ;;
        --package)
            require_arg "$1" "${2:-}"
            APP_ID="$2"
            shift 2
            ;;
        --activity)
            require_arg "$1" "${2:-}"
            MAIN_ACTIVITY="$2"
            shift 2
            ;;
        --wait)
            require_arg "$1" "${2:-}"
            WAIT_SECONDS="$2"
            shift 2
            ;;
        --no-force-stop)
            FORCE_STOP=false
            shift
            ;;
        --skip-screenshot)
            CAPTURE_SCREENSHOT=false
            shift
            ;;
        --skip-logcat)
            CAPTURE_LOGCAT=false
            shift
            ;;
        --skip-ui-dump)
            CAPTURE_UI_DUMP=false
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

if ! [[ "$WAIT_SECONDS" =~ ^[0-9]+$ ]]; then
    fatal "--wait must be a whole number of seconds" 2
fi

if ! command -v adb >/dev/null 2>&1; then
    fatal "adb was not found on PATH. Install Android SDK Platform Tools or add \$ANDROID_HOME/platform-tools to PATH." 2
fi

OUT_DIR="$(abs_path "$OUT_DIR")"
mkdir -p "$OUT_DIR"

DEVICE_LIST_FILE="$OUT_DIR/adb-devices.txt"
adb devices -l > "$DEVICE_LIST_FILE"

print_device_help() {
    printf 'Current adb devices:\n' >&2
    sed -n '1,40p' "$DEVICE_LIST_FILE" >&2
}

READY_DEVICES=()
while IFS= read -r device_serial; do
    if [[ -n "$device_serial" ]]; then
        READY_DEVICES+=("$device_serial")
    fi
done < <(awk 'NR > 1 && $2 == "device" { print $1 }' "$DEVICE_LIST_FILE")

if [[ -n "$SERIAL" ]]; then
    DEVICE_STATE="$(awk -v serial="$SERIAL" '$1 == serial { print $2; found=1 } END { if (!found) exit 1 }' "$DEVICE_LIST_FILE" || true)"
    if [[ -z "$DEVICE_STATE" ]]; then
        print_device_help
        fatal "Device '$SERIAL' is not listed by adb. Start an emulator or connect a USB device with debugging enabled." 2
    fi
    if [[ "$DEVICE_STATE" != "device" ]]; then
        print_device_help
        fatal "Device '$SERIAL' is '$DEVICE_STATE', not ready. Unlock/authorize it and rerun the smoke test." 2
    fi
    DEVICE_SERIAL="$SERIAL"
else
    if [[ "${#READY_DEVICES[@]}" -eq 0 ]]; then
        print_device_help
        fatal "No ready Android device found. Start an emulator or connect a device; unauthorized/offline entries cannot be used." 2
    fi
    if [[ "${#READY_DEVICES[@]}" -gt 1 ]]; then
        print_device_help
        fatal "Multiple ready devices found. Rerun with --serial <id> to avoid touching the wrong device." 2
    fi
    DEVICE_SERIAL="${READY_DEVICES[0]}"
fi

ADB_DEVICE=(adb -s "$DEVICE_SERIAL")
FAILURES=()

record_failure() {
    FAILURES+=("$1")
    warn "$1"
}

component_from_activity() {
    local package_id="$1"
    local activity="$2"
    if [[ "$activity" == */* ]]; then
        printf '%s\n' "$activity"
    else
        printf '%s/%s\n' "$package_id" "$activity"
    fi
}

get_app_pid() {
    local pid=""
    pid="$("${ADB_DEVICE[@]}" shell pidof -s "$APP_ID" 2>/dev/null | tr -d '\r' | tr -d '[:space:]' || true)"
    if [[ -z "$pid" ]]; then
        pid="$("${ADB_DEVICE[@]}" shell ps -A 2>/dev/null | tr -d '\r' | awk -v pkg="$APP_ID" '$NF == pkg { print $2; exit }' || true)"
    fi
    printf '%s\n' "$pid"
}

capture_logcat() {
    local pid="$1"
    local log_file="$OUT_DIR/logcat.txt"
    local log_err="$OUT_DIR/logcat.err"
    local crash_file="$OUT_DIR/logcat-crash.txt"
    local captured=false

    if [[ -n "$pid" ]]; then
        if "${ADB_DEVICE[@]}" logcat -d --pid "$pid" > "$log_file" 2> "$log_err"; then
            captured=true
        fi
    fi

    if [[ "$captured" != true ]]; then
        if "${ADB_DEVICE[@]}" logcat -d -t 1000 > "$log_file" 2>> "$log_err"; then
            captured=true
        fi
    fi

    if [[ "$captured" != true ]]; then
        record_failure "Could not capture logcat; see $log_err"
    fi

    if ! "${ADB_DEVICE[@]}" logcat -d -b crash -t 200 > "$crash_file" 2> "$OUT_DIR/logcat-crash.err"; then
        warn "Could not capture crash logcat buffer; see $OUT_DIR/logcat-crash.err"
    fi
}

echo "seaFOX adb smoke"
echo "Root: $ROOT_DIR"
echo "Device: $DEVICE_SERIAL"
echo "Package: $APP_ID"
echo "Artifacts: $OUT_DIR"

if [[ -n "$APK_PATH" ]]; then
    APK_PATH="$(abs_path "$APK_PATH")"
    if [[ ! -f "$APK_PATH" ]]; then
        fatal "APK not found: $APK_PATH" 2
    fi

    INSTALL_ARGS=(install -r)
    if [[ "$ALLOW_DOWNGRADE" == true ]]; then
        INSTALL_ARGS+=( -d )
    fi

    echo "Installing existing APK with adb ${INSTALL_ARGS[*]} (app data is preserved)."
    if ! "${ADB_DEVICE[@]}" "${INSTALL_ARGS[@]}" "$APK_PATH" > "$OUT_DIR/install.txt" 2>&1; then
        sed -n '1,80p' "$OUT_DIR/install.txt" >&2 || true
        fatal "APK install failed. The script did not clear app data; inspect $OUT_DIR/install.txt"
    fi
fi

PACKAGE_PATH_OUTPUT="$("${ADB_DEVICE[@]}" shell pm path "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
printf '%s\n' "$PACKAGE_PATH_OUTPUT" > "$OUT_DIR/package-path.txt"
if [[ "$PACKAGE_PATH_OUTPUT" != package:* ]]; then
    fatal "Package '$APP_ID' is not installed on $DEVICE_SERIAL. Install it first or pass --apk <existing-apk>." 2
fi

RESOLVE_OUTPUT="$("${ADB_DEVICE[@]}" shell cmd package resolve-activity --brief "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
printf '%s\n' "$RESOLVE_OUTPUT" > "$OUT_DIR/resolve-activity.txt"
RESOLVED_COMPONENT="$(printf '%s\n' "$RESOLVE_OUTPUT" | awk '/\// { value=$0 } END { print value }')"
if [[ -n "$RESOLVED_COMPONENT" ]]; then
    LAUNCH_COMPONENT="$RESOLVED_COMPONENT"
else
    LAUNCH_COMPONENT="$(component_from_activity "$APP_ID" "$MAIN_ACTIVITY")"
fi
echo "Launch component: $LAUNCH_COMPONENT"

"${ADB_DEVICE[@]}" shell log -t seafox-device-smoke "starting $APP_ID $RUN_ID" >/dev/null 2>&1 || true

if [[ "$FORCE_STOP" == true ]]; then
    echo "Force-stopping $APP_ID before launch (no data clear)."
    "${ADB_DEVICE[@]}" shell am force-stop "$APP_ID" > "$OUT_DIR/force-stop.txt" 2>&1 || record_failure "force-stop failed; see $OUT_DIR/force-stop.txt"
fi

echo "Launching app."
if ! "${ADB_DEVICE[@]}" shell am start \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER \
    -n "$LAUNCH_COMPONENT" > "$OUT_DIR/am-start.txt" 2>&1; then
    sed -n '1,120p' "$OUT_DIR/am-start.txt" >&2 || true
    record_failure "am start failed; see $OUT_DIR/am-start.txt"
fi

if grep -Eqi 'Error:|Exception|does not exist|not found|SecurityException' "$OUT_DIR/am-start.txt"; then
    record_failure "am start reported an error; see $OUT_DIR/am-start.txt"
fi

sleep "$WAIT_SECONDS"

APP_PID="$(get_app_pid)"
printf '%s\n' "$APP_PID" > "$OUT_DIR/app-pid.txt"
if [[ -z "$APP_PID" ]]; then
    record_failure "App process is not running after launch wait; capture artifacts may still help diagnose a crash."
else
    echo "App process: $APP_PID"
fi

if [[ "$CAPTURE_SCREENSHOT" == true ]]; then
    if "${ADB_DEVICE[@]}" exec-out screencap -p > "$OUT_DIR/screenshot.png"; then
        echo "Screenshot: $OUT_DIR/screenshot.png"
    else
        rm -f "$OUT_DIR/screenshot.png"
        record_failure "Screenshot capture failed."
    fi
fi

if [[ "$CAPTURE_UI_DUMP" == true ]]; then
    if "${ADB_DEVICE[@]}" exec-out uiautomator dump /dev/tty > "$OUT_DIR/ui.xml" 2> "$OUT_DIR/ui.err"; then
        echo "UI dump: $OUT_DIR/ui.xml"
    else
        rm -f "$OUT_DIR/ui.xml"
        warn "UI dump failed; this can happen on locked screens. See $OUT_DIR/ui.err"
    fi
fi

if [[ "$CAPTURE_LOGCAT" == true ]]; then
    capture_logcat "$APP_PID"
    echo "Logcat: $OUT_DIR/logcat.txt"
    echo "Crash logcat: $OUT_DIR/logcat-crash.txt"
    if [[ -n "$APP_PID" ]] && grep -Eqi 'FATAL EXCEPTION|AndroidRuntime.*FATAL|Process .* has died' "$OUT_DIR/logcat.txt"; then
        record_failure "Logcat contains a fatal app signal; see $OUT_DIR/logcat.txt"
    fi
fi

cat > "$OUT_DIR/summary.txt" <<SUMMARY
seaFOX adb smoke
run_id=$RUN_ID
root=$ROOT_DIR
device=$DEVICE_SERIAL
package=$APP_ID
component=$LAUNCH_COMPONENT
apk=${APK_PATH:-}
wait_seconds=$WAIT_SECONDS
app_pid=$APP_PID
SUMMARY

if [[ "${#FAILURES[@]}" -gt 0 ]]; then
    printf '\nSmoke test failed:\n' >&2
    for failure in "${FAILURES[@]}"; do
        printf '%s\n' "- $failure" >&2
    done
    printf 'Artifacts: %s\n' "$OUT_DIR" >&2
    exit 1
fi

echo "Smoke test passed."
echo "Summary: $OUT_DIR/summary.txt"
