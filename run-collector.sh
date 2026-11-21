#!/usr/bin/env bash
#
# Builds the collector from this source tree and starts it in the foreground.
#
#   ./run-collector.sh [<config.yaml> [<log directory>]]              build (without tests), then start
#   ./run-collector.sh --no-build [<config.yaml> [<log directory>]]   start what was built last
#
# Without a configuration the collector reads invented values (config/default/config.yaml of
# its distribution). The arguments and the environment are those of
# collector/apps/cli/src/main/dist/ekg-collector.sh, the start script of the distribution this uses.

set -euo pipefail

ROOT="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"

if [ "${1:-}" = "--no-build" ]; then
    shift
else
    # from the root, so that Maven reads .mvn/jvm.config there
    (cd "$ROOT" && mvn -q -pl collector/apps/cli -am package -DskipTests)
fi

# Builds of several versions may lie next to each other. The last one is told by its jar: the
# start script and the directories keep the date of their source and are the same age in all.
JAR="$(ls -t "$ROOT"/collector/apps/cli/product/software-ekg-collector-*/software-ekg-collector-*/lib/collector-core-*.jar 2> /dev/null | head -1)"
if [ -z "$JAR" ]; then
    echo "The collector is not built yet. Run this script without --no-build." >&2
    exit 1
fi
START="$(dirname "$(dirname "$JAR")")/ekg-collector.sh"
exec "$START" "$@"
