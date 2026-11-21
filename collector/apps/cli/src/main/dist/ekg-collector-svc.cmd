@echo off

set SCRIPTPATH=%~dp0

set PATH=%SCRIPTPATH%\bin\dlls;%SCRIPTPATH%\jre\bin;%PATH%

set JAVA_LIBRARY_PATH=%SCRIPTPATH%\bin\dlls

set CLASSPATH=%SCRIPTPATH%/config;%SCRIPTPATH%/lib/*

set LOGBACK=-Dlogback.configurationFile=file:%SCRIPTPATH%/config/logback.xml
rem what the JVM would warn about although it is intended (native code, the JavaScript engine)
set QUIET=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dpolyglot.engine.WarnInterpreterOnly=false

rem The display of the collector needs JavaFX, which wants to be loaded as modules. It is loaded
rem only if the display is switched on: io.softwareecg.collector.ui.enabled=true in
rem config\collector.properties.
set FX=
if exist "%SCRIPTPATH%lib\javafx\*.jar" set FX=--module-path "%SCRIPTPATH%lib\javafx" --add-modules javafx.controls --enable-native-access=javafx.graphics
rem not every Java runtime takes these modules; collecting must not depend on it
if defined FX java %FX% -version >nul 2>&1 || set FX=

rem java
java %QUIET% %FX% -javaagent:%SCRIPTPATH%/lib/jolokia-jvm-1.6.0-agent.jar=port=8778,hostName=localhost -Djava.library.path=%JAVA_LIBRARY_PATH% %LOGBACK% -classpath %CLASSPATH% io.softwareecg.collector.CollectorService %1 %2 %3 %4 %5
