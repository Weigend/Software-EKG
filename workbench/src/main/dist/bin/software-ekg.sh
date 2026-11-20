#!/usr/bin/env bash
#
# Starts the Software-EKG workbench.
#
# Needs a Java 25 runtime. It is taken from, in this order:
#   1. the "jre" directory of this distribution, if it was built with one (mvn package -Pdist)
#   2. JAVA_HOME
#   3. the PATH
# JavaFX comes with the runtime (for example Liberica Full JDK) or, failing that, from the
# JavaFX jars in lib/javafx, which fit the platform this distribution was built on.
#
# Environment:
#   EKG_JAVA_OPTS   further options for the JVM, for example -Xmx8g
#   EKG_SOLR_HOME   where the index with the time series lives (default: ~/.ekg/solr)
#
# The data of the user - projects, time series, credentials - is kept in ~/.ekg, never in
# this directory. A new version of the workbench can replace this directory as a whole.

set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)"

if [ -x "$HERE/jre/bin/java" ]; then
    JAVA="$HERE/jre/bin/java"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif command -v java > /dev/null; then
    JAVA="$(command -v java)"
else
    echo "No Java found. Install a JDK 25 with JavaFX (for example Liberica Full JDK) or set JAVA_HOME." >&2
    exit 1
fi

JAVA_MAJOR="$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p')"
if [ -z "$JAVA_MAJOR" ] || [ "${JAVA_MAJOR%%.*}" -lt 25 ]; then
    echo "Software-EKG needs Java 25 or newer; $JAVA is Java ${JAVA_MAJOR:-unknown}." >&2
    exit 1
fi

# JavaFX: from the runtime if it has it, otherwise from the jars shipped for this platform
FX_MODULE_PATH=()
if ! "$JAVA" --list-modules 2> /dev/null | grep -q '^javafx\.controls'; then
    case "$(uname -s)-$(uname -m)" in
        Linux-aarch64)  FX_PLATFORM="linux-aarch64" ;;
        Linux-*)        FX_PLATFORM="linux" ;;
        Darwin-arm64)   FX_PLATFORM="mac-aarch64" ;;
        Darwin-*)       FX_PLATFORM="mac" ;;
        *)              FX_PLATFORM="win" ;;
    esac
    FX_JARS="$(ls "$HERE"/lib/javafx/javafx-*-"$FX_PLATFORM".jar 2> /dev/null | paste -sd: - || true)"
    if [ -z "$FX_JARS" ]; then
        echo "This Java has no JavaFX and the distribution brings none for $FX_PLATFORM." >&2
        echo "Use a JDK 25 with JavaFX, for example Liberica Full JDK." >&2
        exit 1
    fi
    FX_MODULE_PATH=(--module-path "$FX_JARS")
fi

# logs are written relative to the working directory; keep them with the data of the user
LOG_DIR="$HOME/.ekg/logs"
mkdir -p "$LOG_DIR"
cd "$LOG_DIR"

# shellcheck disable=SC2086
exec "$JAVA" ${EKG_JAVA_OPTS:-} \
    "${FX_MODULE_PATH[@]}" \
    --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.swing \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    --add-opens java.base/java.net=ALL-UNNAMED \
    --enable-native-access=ALL-UNNAMED,javafx.graphics \
    -Dekg.solr.template="$HERE/solr" \
    -cp "$HERE/lib/*" \
    io.softwareecg.awb.application.base.EkgAvajeApplication "$@"
