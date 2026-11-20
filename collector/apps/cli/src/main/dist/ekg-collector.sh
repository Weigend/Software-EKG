#!/usr/bin/env bash
#
# Starts the Software-EKG collector in the foreground.
#
#   ekg-collector.sh [<config.yaml> [<log directory>]]
#   ekg-collector.sh --convert <config.xml> [<config.yaml>]
#   ekg-collector.sh --check      only check what the collector needs on this machine and report
#   ekg-collector.sh --version
#   ekg-collector.sh --help
#
# Without a configuration it runs config/default/config.yaml, which reads invented values.
# The examples in the directory config show the real sources; config/current/config-template.yaml
# explains every setting. --convert turns a configuration in the XML format of earlier
# versions into YAML.
#
# Needs a Java 25 runtime. It is taken from, in this order:
#   1. the "jre" directory of this distribution, if it was built with one (mvn package -Pdist)
#   2. JAVA_HOME
#   3. the PATH
# A runtime that does not start on this machine or is older than Java 25 is skipped.
#
# Environment:
#   EKG_COLLECTOR_JAVA_OPTS   options for the JVM; replaces the default memory settings.
#                             To debug: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
#   and whatever the configuration refers to with ${ENV:NAME} - passwords, for example.

set -euo pipefail

MIN_JAVA=25

fail() {
    printf '%b\n' "The Software-EKG collector cannot start.\n\n$*" >&2
    exit 1
}

# The directory of the distribution. $0 may be a link; readlink -f is not available everywhere.
distribution_dir() {
    local file="$1" dir
    while [ -L "$file" ]; do
        dir="$(cd "$(dirname "$file")" && pwd)"
        file="$(readlink "$file")"
        case "$file" in
            /*) ;;
            *)  file="$dir/$file" ;;
        esac
    done
    (cd "$(dirname "$file")" && pwd)
}

HERE="$(distribution_dir "$0")"

case "${1:-}" in
    -h | --help)
        # the usage is the comment at the top of this file
        sed -n '3,25p' "$0" | sed 's/^# \{0,1\}//'
        exit 0
        ;;
    --version)
        version="$(ls "$HERE"/lib/collector-core-*.jar 2> /dev/null | head -n 1)"
        version="${version##*/collector-core-}"
        echo "Software-EKG collector ${version%.jar}"
        exit 0
        ;;
esac

# --- Java ------------------------------------------------------------------------------------

JAVA=""
JAVA_VERSION=""
REJECTED=""

# Takes the Java given if it starts and is new enough, and notes why not otherwise.
try_java() {
    local java="$1" origin="$2" version
    if [ -n "$JAVA" ]; then
        return 0
    fi
    if [ ! -x "$java" ]; then
        return 0
    fi
    version="$("$java" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p' | head -n 1 || true)"
    version="${version#1.}"
    version="${version%%.*}"
    case "$version" in
        '' | *[!0-9]*)
            REJECTED="$REJECTED\n  $java ($origin): does not start on this machine ($(uname -s) $(uname -m))"
            if [ "$(uname -s)" = "Darwin" ] && [ "$java" = "$HERE/jre/bin/java" ]; then
                # Gatekeeper: what a browser downloaded is quarantined, and the runtime is not notarised
                REJECTED="$REJECTED\n    If the ZIP was downloaded with a browser, macOS may be blocking this runtime. Allow it with:\n      xattr -dr com.apple.quarantine \"$HERE\""
            fi
            ;;
        *)
            if [ "$version" -lt "$MIN_JAVA" ]; then
                REJECTED="$REJECTED\n  $java ($origin): is Java $version"
            else
                JAVA="$java"
                JAVA_VERSION="$version"
            fi
            ;;
    esac
}

try_java "$HERE/jre/bin/java" "the runtime of this distribution"
if [ -n "${JAVA_HOME:-}" ]; then
    if [ -x "$JAVA_HOME/bin/java" ]; then
        try_java "$JAVA_HOME/bin/java" "JAVA_HOME"
    else
        REJECTED="$REJECTED\n  JAVA_HOME=$JAVA_HOME: there is no bin/java in it"
    fi
fi
if command -v java > /dev/null 2>&1; then
    try_java "$(command -v java)" "PATH"
fi

if [ -z "$JAVA" ]; then
    if [ -z "$REJECTED" ]; then
        REJECTED="\n  none: no \"jre\" directory in $HERE, JAVA_HOME is not set, no java in the PATH"
    fi
    fail "It needs Java $MIN_JAVA or newer. Found:$REJECTED\n\nInstall a JDK $MIN_JAVA and set JAVA_HOME to it."
fi

JAVA_OPTS="${EKG_COLLECTOR_JAVA_OPTS:--Xms196m -Xmx196m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=164m}"
CLASSPATH="$HERE/config:$HERE/lib/*"

# What the JVM would warn about although it is intended: native code is loaded by the
# JavaScript engine of the calculated metrics and by the Windows counters, and that engine
# uses sun.misc.Unsafe and runs without a compiler of its own.
QUIET="--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dpolyglot.engine.WarnInterpreterOnly=false"

if [ "${1:-}" = "--convert" ]; then
    shift
    if [ $# -lt 1 ]; then
        fail "--convert needs the configuration to convert: ekg-collector.sh --convert <config.xml> [<config.yaml>]"
    fi
    if [ ! -f "$1" ]; then
        fail "There is no configuration $1."
    fi
    exec "$JAVA" -Dlogback.configurationFile="$HERE/config/logback.xml" -classpath "$CLASSPATH" \
        io.softwareecg.collector.config.ConvertConfiguration "$@"
fi

CHECK=""
if [ "${1:-}" = "--check" ]; then
    CHECK=1
    shift
fi

case "${1:-}" in
    -*)
        fail "Unknown option $1. See ekg-collector.sh --help."
        ;;
esac

CONFIG="${1:-$HERE/config/default/config.yaml}"
LOGDIR="${2:-$HERE/logs}"

if [ ! -f "$CONFIG" ]; then
    fail "There is no configuration $CONFIG."
fi
if ! mkdir -p "$LOGDIR" 2> /dev/null || [ ! -w "$LOGDIR" ]; then
    fail "The log directory $LOGDIR cannot be written. Give another one as the second argument:\n  ekg-collector.sh <config.yaml> <log directory>"
fi

if [ -n "$CHECK" ]; then
    echo "Software-EKG collector in $HERE"
    echo "  Machine:        $(uname -s) $(uname -m)"
    echo "  Java:           $JAVA (Java $JAVA_VERSION)"
    echo "  Configuration:  $CONFIG"
    echo "  Logs:           $LOGDIR"
    if [ -n "$REJECTED" ]; then
        printf '%b\n' "  Skipped:$REJECTED"
    fi
    echo "Everything the collector needs is there."
    exit 0
fi

# shellcheck disable=SC2086
exec "$JAVA" $JAVA_OPTS $QUIET \
    -Dlogback.configurationFile="$HERE/config/logback.xml" \
    -Dcollector.log.dir="$LOGDIR" \
    -classpath "$CLASSPATH" \
    io.softwareecg.collector.StartCollector "$CONFIG" "$LOGDIR"
