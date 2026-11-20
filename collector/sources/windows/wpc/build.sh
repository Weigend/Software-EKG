#!/usr/bin/env bash
#
# Builds wpcaccess, the native library of the collector source "win", with mingw-w64.
#
#   build.sh <output directory>
#
# Runs on Windows (Git Bash, with the g++ of mingw-w64 on the PATH) and as a cross build on
# Linux and macOS (x86_64-w64-mingw32-g++). The Maven profile "wpc" of the module calls it.
#
# Environment:
#   WPC_JAVA_HOME   the JDK whose jni.h is used (default: JAVA_HOME)
#   WPC_CXX         the compiler, if it is not found by its usual name

set -euo pipefail

# the name the Java class WindowsPerformanceCounterAccess loads
DLL_NAME="wpcaccess-5.1.0-x64.dll"
SOURCE="io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess.cpp"
JNI_PREFIX="Java_io_softwareecg_collector_source_wincount_WindowsPerformanceCounterAccess_"
JNI_FUNCTIONS="init getCounter authenticate resolveWildcards queryCounter deleteQuery englishNames lookupName"

fail() {
    echo "build.sh: $1" >&2
    exit 1
}

# A path that bash and a native Windows program both understand (D:/a/b); elsewhere unchanged.
portable() {
    if command -v cygpath > /dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi
}

[ $# -eq 1 ] || fail "usage: build.sh <output directory>"
mkdir -p "$(portable "$1")"
OUT="$(portable "$(cd "$(portable "$1")" && pwd)")"
HERE="$(cd "$(dirname "$(portable "$0")")" && pwd)"
JDK="$(portable "${WPC_JAVA_HOME:-${JAVA_HOME:-}}")"
[ -n "$JDK" ] || fail "Set JAVA_HOME (or WPC_JAVA_HOME) to a JDK; its jni.h is needed."
[ -f "$JDK/include/jni.h" ] || fail "No include/jni.h in $JDK - is it a JDK?"

# The compiler has to build for 64 bit Windows, whatever it is called on this machine.
CXX=""
for candidate in "${WPC_CXX:-}" x86_64-w64-mingw32-g++ g++; do
    [ -n "$candidate" ] || continue
    command -v "$candidate" > /dev/null 2>&1 || continue
    case "$("$candidate" -dumpmachine 2> /dev/null)" in
        x86_64-w64-mingw32* | x86_64-w64-windows-gnu*) CXX="$candidate"; break ;;
    esac
done
[ -n "$CXX" ] || fail "No mingw-w64 compiler for 64 bit Windows found. Install it (Debian/Ubuntu: g++-mingw-w64-x86-64, Windows: mingw-w64) or name it in WPC_CXX."

# jni_md.h, the platform part of JNI: a Windows JDK brings it, any other JDK brings the one
# of its own platform. Then the one of this directory is taken.
if [ -f "$JDK/include/win32/jni_md.h" ]; then
    JNI_MD="$JDK/include/win32"
else
    JNI_MD="$(portable "$HERE/src/main/win/jni-win64")"
fi

echo "Building $DLL_NAME with $CXX ($("$CXX" -dumpversion)), jni.h of $JDK"
# -static: the library needs nothing but what Windows brings (no libstdc++, libgcc, winpthread)
(cd "$HERE/src/main/win/wpcaccess" && "$CXX" -shared -O2 -s -DUNICODE -D_UNICODE \
    -I"$JDK/include" -I"$JNI_MD" \
    -static -static-libgcc -static-libstdc++ \
    -o "$OUT/$DLL_NAME" "$SOURCE" -lpdh)

# Is it what the Java class expects? Only checked if the tools of the compiler are there.
OBJDUMP="${CXX%g++}objdump"
if command -v "$OBJDUMP" > /dev/null 2>&1; then
    # without the carriage returns a Windows program writes
    HEADERS="$("$OBJDUMP" -p "$OUT/$DLL_NAME" | tr -d '\r')"
    for function in $JNI_FUNCTIONS; do
        grep -q "[[:space:]]$JNI_PREFIX$function\$" <<< "$HEADERS" \
            || fail "$DLL_NAME does not export $JNI_PREFIX$function."
    done
    if grep "DLL Name:" <<< "$HEADERS" | grep -i -E "libstdc|libgcc|pthread|msys|cygwin"; then
        fail "$DLL_NAME needs libraries that Windows does not bring (see above)."
    fi
fi
echo "Built $OUT/$DLL_NAME"
