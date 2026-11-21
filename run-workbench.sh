#!/usr/bin/env bash
#
# Builds the workbench from this source tree and starts it.
#
#   ./run-workbench.sh              build (without tests), then start
#   ./run-workbench.sh --no-build   start what was built last
#
# Further arguments go to the workbench. EKG_JAVA_OPTS and EKG_SOLR_HOME work as described in
# workbench/src/main/dist/bin/software-ekg.sh, the start script of the distribution this uses.

set -euo pipefail

ROOT="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"

if [ "${1:-}" = "--no-build" ]; then
    shift
else
    # from the root, so that Maven reads .mvn/jvm.config there
    (cd "$ROOT" && mvn -q -pl workbench -am package -DskipTests)
fi

# Builds of several versions may lie next to each other. The last one is told by its jar: the
# start script and the directories keep the date of their source and are the same age in all.
JAR="$(ls -t "$ROOT"/workbench/target/software-ekg-workbench-*/software-ekg-workbench-*/lib/workbench-*.jar 2> /dev/null | head -1)"
if [ -z "$JAR" ]; then
    echo "The workbench is not built yet. Run this script without --no-build." >&2
    exit 1
fi
START="$(dirname "$(dirname "$JAR")")/bin/software-ekg.sh"
exec "$START" "$@"
