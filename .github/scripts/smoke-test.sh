#!/usr/bin/env bash
#
# Starts the two distributions once, the way a user would after unpacking them: with the
# start script of the platform and with nothing but the Java runtime the ZIP brings.
#
#   smoke-test.sh <workbench.zip> <collector.zip>
#
# Runs on Linux, macOS and Windows (Git Bash). The workbench needs a display; on Linux call
# this through xvfb-run.
#
# On Windows both distributions have to bring the native library of the Windows performance
# counters, and the collector has to read counters with it.

set -euo pipefail

WORKBENCH_ZIP="$1"
COLLECTOR_ZIP="$2"
WORKBENCH_SECONDS="${WORKBENCH_SECONDS:-60}"
COLLECTOR_SECONDS="${COLLECTOR_SECONDS:-25}"
COUNTER_SECONDS="${COUNTER_SECONDS:-20}"
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
# the native library of the Windows performance counters, in bin/dlls of a distribution
WPC_DLL="wpcaccess-5.1.0-x64.dll"
WPC_CHECK="io.softwareecg.collector.source.wincount.WindowsPerformanceCounterCheck"

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
    grep -q "Display: *available" "$WORK/collector.check" || fail "The collector cannot show its display with the runtime of its distribution."
    if [ "$PLATFORM" = macos ]; then
        # the bash that macOS ships is version 3.2
        /bin/bash "$COL/ekg-collector.sh" --check > /dev/null || fail "The start script of the collector does not run with /bin/bash of macOS."
    fi
    # with its display, which is off unless asked for: it has to come up beside the collecting
    "$COL/ekg-collector.sh" --ui "$COL/config/mock/config.yaml" "$COLLECTOR_LOGS" > "$COLLECTOR_OUT" 2>&1 &
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
if [ "$PLATFORM" != windows ]; then
    grep -q "The display of the collector is started" "$COLLECTOR_OUT" || fail "The collector did not start its display."
fi
echo "The collector ran and wrote $(wc -l < "$CSV") lines into $(basename "$CSV")."
echo "::endgroup::"

# --- Windows performance counters ------------------------------------------------------------

if [ "$PLATFORM" = windows ]; then
    echo "::group::Windows performance counters"
    [ -f "$COL/bin/dlls/$WPC_DLL" ] || fail "The collector distribution has no bin/dlls/$WPC_DLL."
    [ -f "$WB/bin/dlls/$WPC_DLL" ] || fail "The workbench distribution has no bin/dlls/$WPC_DLL."

    # the collector, started as a user would: it has to find the library and read counters
    COUNTER_LOGS="$WORK/counter-logs"
    COUNTER_OUT="$WORK/counters.out"
    (cd "$COL" && cmd.exe //c "ekg-collector.cmd $(native "$SCRIPTS/windows-counters.yaml") $(native "$COUNTER_LOGS")" > "$COUNTER_OUT" 2>&1) &
    sleep "$COUNTER_SECONDS"
    java_running && COUNTERS_ALIVE=1 || COUNTERS_ALIVE=
    taskkill //F //IM java.exe //T > /dev/null 2>&1 || true
    tail -n 40 "$COUNTER_OUT" || true
    [ -n "$COUNTERS_ALIVE" ] || fail "The collector stopped within $COUNTER_SECONDS seconds while reading Windows performance counters."
    grep -q "Native library ${WPC_DLL%.dll} loaded" "$COUNTER_OUT" || fail "The collector did not load $WPC_DLL."
    # the files of a job carry their day: <host>_<process>_windows_2026-11-21.csv
    CSV="$(find "$COUNTER_LOGS" -name '*_windows_*.csv' | head -n 1)"
    [ -n "$CSV" ] || fail "The collector wrote no Windows performance counters."
    head -n 4 "$CSV"
    head -n 1 "$CSV" | grep -q -F 'System\Threads' || fail "The counter \\System\\Threads is missing in $CSV."
    # the wildcard \Processor(_Total)\* has to be resolved into its counters
    head -n 1 "$CSV" | grep -q -F '% Processor Time' || fail "The wildcard \\Processor(_Total)\\* was not resolved in $CSV."
    # a line of measurements: the time, then one value for each counter; -1 says "no value"
    MEASURED="$(tr -d '\r' < "$CSV" | awk -F ';' 'NR > 1 { for (i = 2; i <= NF; i++) if ($i + 0 > 0) { n++; break } } END { print n + 0 }')"
    [ "$MEASURED" -ge 3 ] || fail "The collector read no values of the Windows performance counters ($MEASURED lines with a value in $CSV)."
    echo "The collector read Windows performance counters: $MEASURED lines with values."

    # The workbench embeds the collector. Its runtime and its libraries have to work with the
    # library as well; software-ekg.bat puts bin\dlls on the PATH, as it is done here.
    (cd "$WB" && PATH="$WB/bin/dlls:$PATH" "$WB/jre/bin/java" --enable-native-access=ALL-UNNAMED \
        -cp "$(native "$WB/lib")\\*" "$WPC_CHECK" '\System\Threads' '\Memory\Available Bytes') \
        || fail "The runtime of the workbench cannot read Windows performance counters."
    echo "::endgroup::"
fi

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
cat "$WORKBENCH_OUT" || true
[ -n "$WORKBENCH_ALIVE" ] || fail "The workbench stopped within $WORKBENCH_SECONDS seconds."
if grep -q -E "Exception in Application start method|Unable to open DISPLAY|Graphics Device initialization failed|Error initializing QuantumRenderer" "$WORKBENCH_OUT"; then
    fail "The workbench started but JavaFX reported an error."
fi
if grep -q "Can not start module" "$WORKBENCH_OUT"; then
    fail "The workbench started, but not all of its modules: $(grep -c "Can not start module" "$WORKBENCH_OUT") failed."
fi
# the line the workbench logs when all modules are loaded and the main window opens
grep -q "show main window" "$WORKBENCH_OUT" || fail "The workbench did not get as far as its main window within $WORKBENCH_SECONDS seconds."
echo "The workbench opened its main window and was still running after $WORKBENCH_SECONDS seconds."
echo "::endgroup::"
