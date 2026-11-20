@echo off

set SCRIPTPATH=%~dp0

set PATH=%SCRIPTPATH%\bin\dlls;%SCRIPTPATH%\jre\bin;%PATH%

set JAVA_LIBRARY_PATH=%SCRIPTPATH%\bin\dlls

rem config will be the execution directory

if %1!==! (set CONFIG=%SCRIPTPATH%/config/default/config.yaml) else (set CONFIG=%1)
if %2!==! (set LOGDIR=%SCRIPTPATH%/logs) else (set LOGDIR=%2)

set CLASSPATH=%SCRIPTPATH%/config;%SCRIPTPATH%/lib/*
set LOGBACK=-Dlogback.configurationFile=file:%SCRIPTPATH%/config/logback.xml
rem what the JVM would warn about although it is intended (native code, the JavaScript engine)
set QUIET=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Dpolyglot.engine.WarnInterpreterOnly=false


rem java
rem java -javaagent:%SCRIPTPATH%/lib/jolokia-jvm-1.6.0-agent.jar=port=8778,hostName=localhost  -Djava.library.path=%JAVA_LIBRARY_PATH% %LOGBACK% -classpath %CLASSPATH% io.softwareecg.collector.StartCollector %CONFIG% %LOGDIR%
java %QUIET% %LOGBACK% -Dcollector.log.dir=%LOGDIR% -Dlog.directory=%LOGDIR%\ekg -Djava.library.path=%JAVA_LIBRARY_PATH% -classpath %CLASSPATH% io.softwareecg.collector.StartCollector %CONFIG% %LOGDIR%
