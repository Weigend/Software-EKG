# wpcaccess - native access to the Windows performance counters

The source `win` of the collector reads Windows performance counters through a small native
library, `wpcaccess-5.1.0-x64.dll` (and `-x32.dll` for a 32 bit Java). The library is **not part
of this repository**; it is built from the C++ sources here.

## Building

1. Open `src/main/win/wpcaccess/wpcaccess.vcxproj` in Visual Studio (Desktop development with C++).
2. Set `JAVA_HOME` to a JDK, so that `jni.h` is found.
3. Build the configuration `Release` for the platform `x64` (and `Win32` if needed).

The functions of the library are named after the Java class that declares them,
`io.softwareecg.collector.source.wincount.WindowsPerformanceCounterAccess`. A library built before
that class got its present name does not fit it.

## Installing

Put the DLL into the directory `bin\dlls` of the collector distribution; `ekg-collector.cmd` adds
that directory to the library path. Without the library the collector starts as usual and logs
that the Windows counters are not available.
