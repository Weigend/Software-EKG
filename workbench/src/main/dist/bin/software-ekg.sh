#!/usr/bin/env bash
#
# Starts the Software-EKG workbench.
#
#   software-ekg.sh           start the workbench
#   software-ekg.sh --check   only check what the workbench needs on this machine and report
#
# Needs a Java 25 runtime. It is taken from, in this order:
#   1. the "jre" directory of this distribution, if it was built with one (mvn package -Pdist)
#   2. JAVA_HOME
#   3. the PATH
# A runtime that does not start on this machine or is older than Java 25 is skipped.
# JavaFX comes with the runtime (for example Liberica Full JDK) or, failing that, from the
# JavaFX jars in lib/javafx, which fit the platform this distribution was built on.
#
# Environment:
#   EKG_JAVA_OPTS     further options for the JVM, for example -Xmx8g
#   EKG_SOLR_HOME     where the index with the time series lives (default: ~/.ekg/solr)
#   EKG_SKIP_CHECKS   set to 1 to start although a check of display or libraries fails
#
# The data of the user - projects, time series, credentials - is kept in ~/.ekg, never in
# this directory. A new version of the workbench can replace this directory as a whole.

set -euo pipefail

MIN_JAVA=25

# Reports why the workbench cannot start. Started from a file manager there is no terminal
# to read it in, so the message is shown in a dialog as well, if there is a way to.
fail() {
    printf '%b\n' "Software-EKG cannot start.\n\n$*" >&2
    if [ ! -t 2 ] && [ -n "${DISPLAY:-}" ]; then
        local text
        text="$(printf '%b' "Software-EKG cannot start.\n\n$*")"
        if command -v zenity > /dev/null 2>&1; then
            zenity --error --no-markup --title "Software-EKG" --text "$text" 2> /dev/null || true
        elif command -v kdialog > /dev/null 2>&1; then
            kdialog --title "Software-EKG" --error "$text" 2> /dev/null || true
        elif command -v xmessage > /dev/null 2>&1; then
            xmessage -center "$text" 2> /dev/null || true
        fi
    fi
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
    (cd "$(dirname "$file")/.." && pwd)
}

HERE="$(distribution_dir "$0")"
OS="$(uname -s)"
ARCH="$(uname -m)"

# --- Java ------------------------------------------------------------------------------------

JAVA=""
JAVA_VERSION=""
JAVA_RUNTIME_HOME=""
REJECTED=""

# Takes the Java given if it starts and is new enough, and notes why not otherwise.
try_java() {
    local java="$1" origin="$2" settings version
    if [ -n "$JAVA" ]; then
        return 0
    fi
    if [ ! -x "$java" ]; then
        return 0
    fi
    settings="$("$java" -XshowSettings:properties -version 2>&1 || true)"
    version="$(printf '%s\n' "$settings" | sed -n 's/^ *java\.specification\.version = //p' | head -n 1)"
    version="${version#1.}"
    version="${version%%.*}"
    case "$version" in
        '' | *[!0-9]*)
            REJECTED="$REJECTED\n  $java ($origin): does not start on this machine ($OS $ARCH)"
            if [ "$OS" = "Darwin" ] && [ "$java" = "$HERE/jre/bin/java" ]; then
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
                JAVA_RUNTIME_HOME="$(printf '%s\n' "$settings" | sed -n 's/^ *java\.home = //p' | head -n 1)"
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
    fail "It needs Java $MIN_JAVA or newer with JavaFX. Found:$REJECTED\n\nInstall a JDK $MIN_JAVA with JavaFX, for example the Liberica Full JDK\n(https://bell-sw.com/pages/downloads/), and set JAVA_HOME to it."
fi

# --- JavaFX ----------------------------------------------------------------------------------

# JavaFX: from the runtime if it has it, otherwise from the jars shipped for this platform
FX_MODULE_PATH=()
FX_ORIGIN="the Java runtime"
if ! "$JAVA" --list-modules 2> /dev/null | grep -q '^javafx\.controls'; then
    case "$OS-$ARCH" in
        Linux-aarch64)  FX_PLATFORM="linux-aarch64" ;;
        Linux-*)        FX_PLATFORM="linux" ;;
        Darwin-arm64)   FX_PLATFORM="mac-aarch64" ;;
        Darwin-*)       FX_PLATFORM="mac" ;;
        *)              FX_PLATFORM="win" ;;
    esac
    FX_JARS="$(ls "$HERE"/lib/javafx/javafx-*-"$FX_PLATFORM".jar 2> /dev/null | paste -sd: - || true)"
    if [ -z "$FX_JARS" ]; then
        fail "$JAVA (Java $JAVA_VERSION) has no JavaFX, and this distribution brings none for $OS $ARCH.\n\nUse a JDK $MIN_JAVA with JavaFX, for example the Liberica Full JDK\n(https://bell-sw.com/pages/downloads/), and set JAVA_HOME to it."
    fi
    FX_MODULE_PATH=(--module-path "$FX_JARS")
    FX_ORIGIN="lib/javafx of this distribution"
fi

# --- display and libraries -------------------------------------------------------------------

# What is certain to fail later with a stack trace instead of a reason. EKG_SKIP_CHECKS=1
# starts the workbench anyway.
PROBLEMS=""
if [ "$OS" = "Linux" ]; then
    # JavaFX draws through X11, under Wayland through XWayland
    if [ -z "${DISPLAY:-}" ]; then
        PROBLEMS="$PROBLEMS\n- There is no graphical display (DISPLAY is not set). The workbench is a desktop\n  application: start it in a desktop session, or over SSH with X forwarding (ssh -X)."
    fi
    # the libraries the window toolkit of JavaFX is linked against
    GLASS="$JAVA_RUNTIME_HOME/lib/libglassgtk3.so"
    if [ -f "$GLASS" ] && command -v ldd > /dev/null 2>&1; then
        MISSING="$(ldd "$GLASS" 2> /dev/null | awk '/not found/ { print $1 }' | sort -u | paste -sd' ' - || true)"
        if [ -n "$MISSING" ]; then
            PROBLEMS="$PROBLEMS\n- System libraries that JavaFX needs are not installed: $MISSING\n  Install GTK 3, for example with\n    sudo apt install libgtk-3-0 libxtst6 libgl1     (Debian, Ubuntu)\n    sudo dnf install gtk3 libXtst mesa-libGL        (Fedora, RHEL)"
        fi
    fi
fi

# logs are written relative to the working directory; keep them with the data of the user
if [ -z "${HOME:-}" ]; then
    fail "The environment variable HOME is not set. The workbench keeps its data in \$HOME/.ekg."
fi
LOG_DIR="$HOME/.ekg/logs"
if ! mkdir -p "$LOG_DIR" 2> /dev/null || [ ! -w "$LOG_DIR" ]; then
    fail "The data directory $HOME/.ekg cannot be written. The workbench keeps projects,\ntime series and logs there."
fi

if [ "${1:-}" = "--check" ]; then
    echo "Software-EKG workbench in $HERE"
    echo "  Machine:  $OS $ARCH"
    echo "  Java:     $JAVA (Java $JAVA_VERSION)"
    echo "  JavaFX:   from $FX_ORIGIN"
    echo "  Data:     $HOME/.ekg"
    if [ -n "$REJECTED" ]; then
        printf '%b\n' "  Skipped:$REJECTED"
    fi
    if [ -n "$PROBLEMS" ]; then
        printf '%b\n' "\nThe workbench will not start:$PROBLEMS"
        exit 1
    fi
    echo "Everything the workbench needs is there."
    exit 0
fi

if [ -n "$PROBLEMS" ] && [ "${EKG_SKIP_CHECKS:-}" != "1" ]; then
    fail "${PROBLEMS#\\n}\n\nSet EKG_SKIP_CHECKS=1 to start anyway."
fi

cd "$LOG_DIR"

# shellcheck disable=SC2086
exec "$JAVA" ${EKG_JAVA_OPTS:-} \
    ${FX_MODULE_PATH[@]+"${FX_MODULE_PATH[@]}"} \
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
