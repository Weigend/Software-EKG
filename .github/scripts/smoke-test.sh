#!/usr/bin/env bash
#
# Starts the two distributions once, the way a user would after unpacking them: with the
# start script of the platform and with nothing but the Java runtime the ZIP brings.
#
#   smoke-test.sh <workbench.zip> <collector.zip>
#
# Runs on Linux, macOS and Windows (Git Bash). The workbench needs a display; on Linux call
# this through xvfb-run.

set -euo pipefail

WORKBENCH_ZIP="$1"
COLLECTOR_ZIP="$2"
WORKBENCH_SECONDS="${WORKBENCH_SECONDS:-60}"
COLLECTOR_SECONDS="${COLLECTOR_SECONDS:-25}"

case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) PLATFORM=windows ;;
    Darwin)                   PLATFORM=macos ;;
    *)                        PLATFORM=linux ;;
esac

WORK="$(mktemp -d)"
unzip -q "$WORKBENCH_ZIP" -d "$WORK"
unzip -q "$COLLECTOR_ZIP" -d "$WORK"
WB="$(echo "$WORK"/software-ekg-workbench-*)"
COL="$(echo "$WORK"/software-ekg-collector-*)"

# The distributions have to run with the runtime they bring. Take the JDK of the build out of
# the way, so that a broken runtime is not covered up by it.
if [ -n "${JAVA_HOME:-}" ]; then
    if [ "$PLATFORM" = windows ]; then
        JDK_BIN="$(cygpath -u "$JAVA_HOME")/bin"
    else
        JDK_BIN="$JAVA_HOME/bin"
    fi
    PATH="$(printf '%s' "$PATH" | tr ':' '\n' | grep -v -x -F "$JDK_BIN" | paste -sd: -)"
    export PATH
    unset JAVA_HOME
fi

native() {   # a path as the start scripts of the platform expect it
    if [ "$PLATFORM" = windows ]; then cygpath -w "$1"; else printf '%s' "$1"; fi
}

java_running() {   # Windows: the JVM is a child of cmd.exe and has no pid this shell knows
    tasklist //FI "IMAGENAME eq java.exe" 2> /dev/null | grep -qi "java.exe"
}

fail() {
    echo "::error::$1"
    exit 1
}

# --- collector -------------------------------------------------------------------------------

echo "::group::Collector ($PLATFORM)"
COLLECTOR_LOGS="$WORK/collector-logs"
COLLECTOR_OUT="$WORK/collector.out"
if [ "$PLATFORM" = windows ]; then
    (cd "$COL" && cmd.exe //c "ekg-collector.cmd $(native "$COL/config/mock/config.yaml") $(native "$COLLECTOR_LOGS")" > "$COLLECTOR_OUT" 2>&1) &
else
    "$COL/ekg-collector.sh" --version
    "$COL/ekg-collector.sh" --check | tee "$WORK/collector.check"
    grep -q -F "$COL/jre/bin/java" "$WORK/collector.check" || fail "The collector does not use the runtime of its distribution."
    "$COL/ekg-collector.sh" "$COL/config/mock/config.yaml" "$COLLECTOR_LOGS" > "$COLLECTOR_OUT" 2>&1 &
    COLLECTOR_PID=$!
fi
sleep "$COLLECTOR_SECONDS"
if [ "$PLATFORM" = windows ]; then
    java_running && COLLECTOR_ALIVE=1 || COLLECTOR_ALIVE=
    taskkill //F //IM java.exe //T > /dev/null 2>&1 || true
else
    kill -0 "$COLLECTOR_PID" 2> /dev/null && COLLECTOR_ALIVE=1 || COLLECTOR_ALIVE=
    kill "$COLLECTOR_PID" 2> /dev/null || true
fi
tail -n 30 "$COLLECTOR_OUT" || true
[ -n "$COLLECTOR_ALIVE" ] || fail "The collector stopped within $COLLECTOR_SECONDS seconds."
# the mock source produces values right away; they have to arrive in the CSV repository
CSV="$(find "$COLLECTOR_LOGS" -name '*.csv' | head -n 1)"
[ -n "$CSV" ] || fail "The collector wrote no measurements."
[ "$(wc -l < "$CSV")" -ge 3 ] || fail "The collector wrote no measurements into $CSV."
echo "The collector ran and wrote $(wc -l < "$CSV") lines into $(basename "$CSV")."
echo "::endgroup::"

# --- workbench -------------------------------------------------------------------------------

echo "::group::Workbench ($PLATFORM)"
WORKBENCH_OUT="$WORK/workbench.out"
if [ "$PLATFORM" = windows ]; then
    cmd.exe //c "$(native "$WB/bin/software-ekg.bat")" > "$WORKBENCH_OUT" 2>&1 &
else
    "$WB/bin/software-ekg.sh" --check | tee "$WORK/workbench.check"
    grep -q -F "$WB/jre/bin/java" "$WORK/workbench.check" || fail "The workbench does not use the runtime of its distribution."
    grep -q "JavaFX: *from the Java runtime" "$WORK/workbench.check" || fail "The runtime of the workbench has no JavaFX."
    if [ "$PLATFORM" = macos ]; then
        # the bash that macOS ships is version 3.2
        /bin/bash --version | head -n 1
        /bin/bash "$WB/bin/software-ekg.sh" --check > /dev/null || fail "The start script does not run with /bin/bash of macOS."
    fi
    "$WB/bin/software-ekg.sh" > "$WORKBENCH_OUT" 2>&1 &
    WORKBENCH_PID=$!
fi
sleep "$WORKBENCH_SECONDS"
if [ "$PLATFORM" = windows ]; then
    java_running && WORKBENCH_ALIVE=1 || WORKBENCH_ALIVE=
    taskkill //F //IM java.exe //T > /dev/null 2>&1 || true
else
    kill -0 "$WORKBENCH_PID" 2> /dev/null && WORKBENCH_ALIVE=1 || WORKBENCH_ALIVE=
    kill "$WORKBENCH_PID" 2> /dev/null || true
fi
tail -n 40 "$WORKBENCH_OUT" || true
[ -n "$WORKBENCH_ALIVE" ] || fail "The workbench stopped within $WORKBENCH_SECONDS seconds."
if grep -q -E "Exception in Application start method|Unable to open DISPLAY|Graphics Device initialization failed|Error initializing QuantumRenderer" "$WORKBENCH_OUT"; then
    fail "The workbench started but JavaFX reported an error."
fi
echo "The workbench was still running after $WORKBENCH_SECONDS seconds."
echo "::endgroup::"
