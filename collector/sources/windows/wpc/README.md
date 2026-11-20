# wpcaccess - native access to the Windows performance counters

The source `win` of the collector reads Windows performance counters through a small native
library, `wpcaccess-5.1.0-x64.dll`. The library is **not part of this repository**; the build
makes it from the C++ sources here, with [mingw-w64](https://www.mingw-w64.org/).

## Building

The Maven profile `wpc` of the module `collector/sources/windows` builds the library:

    mvn -Pwpc -pl collector/sources/windows -am verify

It calls `build.sh`, which needs

- `bash` - on Windows the one of Git for Windows (`-Dwpc.bash=...` if another one comes first
  on the `PATH`),
- the `g++` of mingw-w64 for 64 bit Windows on the `PATH` - on Windows `g++`, on Linux and macOS
  the cross compiler `x86_64-w64-mingw32-g++` (Debian/Ubuntu: `apt install g++-mingw-w64-x86-64`,
  macOS: `brew install mingw-w64`). `WPC_CXX` names a compiler that is called differently.

The result is `collector/sources/windows/target/native/wpcaccess-5.1.0-x64.dll`. It is linked
statically and needs nothing but what Windows brings.

Without the profile the build needs no C++ compiler and makes no library.

The functions of the library are named after the Java class that declares them,
`io.softwareecg.collector.source.wincount.WindowsPerformanceCounterAccess`. A library built before
that class got its present name does not fit it. `build.sh` checks that the library exports the
functions of the class.

There is no 32 bit library any more: Software-EKG needs Java 25, and that has no 32 bit Windows.
The Visual Studio project in `src/main/win` is the former way to build the library and is not
used by the build.

## Testing

The tests of the module read counters of the machine they run on. They are skipped on other
systems than Windows, and on Windows if the library is not built. With `-Pwpc` a library that
cannot be loaded is a failure.

On GitHub the job "Windows performance counters" of the workflow "Build and Test" builds the
library on a Windows runner and runs these tests. The workflow "Release" starts the Windows
distributions and lets the collector read counters (`.github/scripts/smoke-test.sh`).

## Where the library ends up

A build with `-Pwpc` puts the library into both distributions, the release workflow does so for
Windows:

| distribution | library | found through |
|---|---|---|
| collector | `bin\dlls\wpcaccess-5.1.0-x64.dll` | `ekg-collector.cmd` sets `java.library.path` |
| workbench (embedded collector) | `bin\dlls\wpcaccess-5.1.0-x64.dll` | `bin\software-ekg.bat` puts the directory on the `PATH` |

Without the library a collector starts as usual and logs that the Windows counters are not
available.

To see whether a distribution can read counters, call in its directory:

    jre\bin\java -cp "lib\*" -Djava.library.path=bin\dlls io.softwareecg.collector.source.wincount.WindowsPerformanceCounterCheck \System\Threads
