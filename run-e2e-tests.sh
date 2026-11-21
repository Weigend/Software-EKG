#!/usr/bin/env bash
#
# Runs the end-to-end tests of the workbench: they start the whole application and operate it
# through its user interface - the menu Import, the dialog of every importer, the import - and
# then ask the repository what arrived. See workbench/src/test/java/io/softwareecg/awb/e2e.
#
#   ./run-e2e-tests.sh              all end-to-end tests
#   ./run-e2e-tests.sh --show       on the display of this session, to watch them
#   ./run-e2e-tests.sh -Dtest=ImporterE2ETest#importsTheTradesOfBinance
#
# The tests need a display. Without --show they use a virtual one (xvfb-run), where there is
# one, so that no window appears on the screen. The pictures they take of the dialogs are in
# workbench/target/e2e/screenshots, the data of the workbench under test in
# workbench/target/e2e/home. The home directory of the person running them is not touched.

set -euo pipefail

ROOT="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"

SHOW=0
if [ "${1:-}" = "--show" ]; then
    SHOW=1
    shift
fi

RUN=()
if [ "$SHOW" = 0 ] && command -v xvfb-run > /dev/null 2>&1; then
    RUN=(xvfb-run -a --server-args="-screen 0 1920x1200x24")
elif [ -z "${DISPLAY:-}" ] && [ "$(uname -s)" = "Linux" ]; then
    echo "The end-to-end tests need a display: install xvfb (xvfb-run), or run them in a desktop session." >&2
    exit 1
fi

cd "$ROOT"
# -am builds what the workbench needs; the profile e2e runs the end-to-end tests and only them
exec ${RUN[@]+"${RUN[@]}"} mvn -B -pl workbench -am test -Pe2e -Dskip.core.tests=true "$@"
