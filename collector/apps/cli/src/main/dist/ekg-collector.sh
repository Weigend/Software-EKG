#!/usr/bin/env bash
#
# Starts the Software-EKG collector in the foreground.
#
#   ekg-collector.sh [<config.yaml> [<log directory>]]
#   ekg-collector.sh --convert <config.xml> [<config.yaml>]
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
#
# Environment:
#   EKG_COLLECTOR_JAVA_OPTS   options for the JVM; replaces the default memory settings.
#                             To debug: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
#   and whatever the configuration refers to with ${ENV:NAME} - passwords, for example.

set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"

if [ -x "$HERE/jre/bin/java" ]; then
    JAVA="$HERE/jre/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif command -v java > /dev/null; then
    JAVA="$(command -v java)"
else
    echo "No Java found. Install a JDK 25 or set JAVA_HOME." >&2
    exit 1
fi

JAVA_OPTS="${EKG_COLLECTOR_JAVA_OPTS:--Xms196m -Xmx196m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=164m}"
CLASSPATH="$HERE/config:$HERE/lib/*"

# What the JVM would warn about although it is intended: native code is loaded by the
# JavaScript engine of the calculated metrics and by the Windows counters, and that engine
# uses sun.misc.Unsafe and runs without a compiler of its own.
QUIET="--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dpolyglot.engine.WarnInterpreterOnly=false"

if [ "${1:-}" = "--convert" ]; then
    shift
    # shellcheck disable=SC2086
    exec "$JAVA" -Dlogback.configurationFile="$HERE/config/logback.xml" -classpath "$CLASSPATH" \
        io.softwareecg.collector.config.ConvertConfiguration "$@"
fi

CONFIG="${1:-$HERE/config/default/config.yaml}"
LOGDIR="${2:-$HERE/logs}"

if [ ! -f "$CONFIG" ]; then
    echo "There is no configuration $CONFIG." >&2
    exit 1
fi

# shellcheck disable=SC2086
exec "$JAVA" $JAVA_OPTS $QUIET \
    -Dlogback.configurationFile="$HERE/config/logback.xml" \
    -Dcollector.log.dir="$LOGDIR" \
    -classpath "$CLASSPATH" \
    io.softwareecg.collector.StartCollector "$CONFIG" "$LOGDIR"
