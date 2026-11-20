#!/bin/bash
#
# Install the WFX framework into the local Maven repository.
#
# WFX (https://github.com/Weigend/wfx) is no longer a module of the aggregator
# build. The workbench depends on io.softwareecg.wfx:* in the version given by
# <wfxVersion> in pom.xml, so the jars have to be present in ~/.m2
# before the first "mvn clean install". This script clones the wfx repository
# (or reuses an existing checkout), checks out the matching release tag and runs
# "mvn install" on it.
#
# Usage:
#   ./install-wfx.sh            install the version from pom.xml if missing
#   ./install-wfx.sh --force    rebuild and reinstall even if already present
#   ./install-wfx.sh --tests    run the wfx test suite too (needs a display)
#   ./install-wfx.sh --version 1.1.2   install a specific version instead
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WFX_REPO="https://github.com/Weigend/wfx.git"
WFX_DIR="${WFX_DIR:-$SCRIPT_DIR/wfx}"

# Where Maven installs to. Maven itself is asked: settings.xml may move the local repository,
# and an environment variable M2_REPO, as some CI images set it, says nothing about that.
M2_REPO=""
if command -v mvn > /dev/null 2>&1; then
    M2_REPO="$(cd "${TMPDIR:-/tmp}" && mvn -q -B help:evaluate -Dexpression=settings.localRepository -DforceStdout 2> /dev/null || true)"
fi
if [ -z "$M2_REPO" ] || [ ! -d "$M2_REPO" ]; then
    M2_REPO="$HOME/.m2/repository"
fi

FORCE=0
RUN_TESTS=0
VERSION=""

while [ $# -gt 0 ]; do
    case "$1" in
        --force)   FORCE=1 ;;
        --tests)   RUN_TESTS=1 ;;
        --version) shift; VERSION="${1:-}" ;;
        -h|--help) awk 'NR>1 && !/^#/ {exit} NR>1 {sub(/^# ?/, ""); print}' "$0"; exit 0 ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
    shift
done

# --- Determine the required version from the root pom -----------------------
if [ -z "$VERSION" ]; then
    VERSION="$(sed -n 's:.*<wfxVersion>\([^<]*\)</wfxVersion>.*:\1:p' "$SCRIPT_DIR/pom.xml" | head -1)"
fi
if [ -z "$VERSION" ]; then
    echo "✗ Could not determine wfxVersion from $SCRIPT_DIR/pom.xml" >&2
    exit 1
fi
TAG="v${VERSION}"

echo "=== SoftwareECG: Installing WFX ${VERSION} into ${M2_REPO} ==="
echo ""

# --- Skip if already installed ---------------------------------------------
MARKER_JAR="$M2_REPO/io/softwareecg/wfx/wfx-all/${VERSION}/wfx-all-${VERSION}.jar"
if [ -f "$MARKER_JAR" ] && [ "$FORCE" -eq 0 ]; then
    echo "✓ WFX ${VERSION} is already installed ($MARKER_JAR)"
    echo "  Use --force to rebuild and reinstall."
    exit 0
fi

# --- Prerequisites ----------------------------------------------------------
for tool in git mvn; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "✗ '$tool' not found on PATH" >&2
        exit 1
    fi
done

# --- Clone or update the wfx checkout ---------------------------------------
if [ -d "$WFX_DIR/.git" ]; then
    echo "→ Using existing checkout $WFX_DIR"
    if [ -n "$(git -C "$WFX_DIR" status --porcelain)" ]; then
        echo "✗ $WFX_DIR has uncommitted changes; commit, stash or remove them first." >&2
        exit 1
    fi
    git -C "$WFX_DIR" fetch --tags --quiet origin
elif [ -e "$WFX_DIR" ] && [ -n "$(ls -A "$WFX_DIR")" ]; then
    echo "✗ $WFX_DIR exists but is not a git repository" >&2
    exit 1
else
    echo "→ Cloning $WFX_REPO into $WFX_DIR"
    git clone --quiet "$WFX_REPO" "$WFX_DIR"
fi

if ! git -C "$WFX_DIR" rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
    echo "✗ Tag ${TAG} not found in $WFX_REPO" >&2
    echo "  Available tags:" >&2
    git -C "$WFX_DIR" tag -l 'v*' | sed 's/^/    /' >&2
    exit 1
fi

echo "→ Checking out ${TAG}"
git -C "$WFX_DIR" checkout --quiet --detach "$TAG"

# --- Build and install ------------------------------------------------------
MVN_ARGS=(-B clean install)
if [ "$RUN_TESTS" -eq 0 ]; then
    # The wfx test suite needs a display (JavaFX toolkit); skip by default.
    MVN_ARGS+=(-DskipTests)
fi

echo "→ Running: mvn ${MVN_ARGS[*]}  (in $WFX_DIR)"
echo ""
(cd "$WFX_DIR" && mvn "${MVN_ARGS[@]}")

echo ""
if [ -f "$MARKER_JAR" ]; then
    echo "✓ WFX ${VERSION} installed: $MARKER_JAR"
    echo ""
    echo "You can now build SoftwareECG:  mvn clean install"
else
    echo "✗ Build finished but $MARKER_JAR is missing" >&2
    exit 1
fi
