@echo off
rem Starts the Software-EKG workbench.
rem
rem Needs a Java 25 runtime with JavaFX (for example Liberica Full JDK). It is taken from the
rem "jre" directory of this distribution if it was built with one, else from JAVA_HOME, else
rem from the PATH.
rem
rem Environment:
rem   EKG_JAVA_OPTS   further options for the JVM, for example -Xmx8g
rem   EKG_SOLR_HOME   where the index with the time series lives (default: %USERPROFILE%\.ekg\solr)
rem
rem The data of the user is kept in %USERPROFILE%\.ekg, never in this directory.

setlocal
set "HERE=%~dp0.."

if exist "%HERE%\jre\bin\java.exe" (
    set "JAVA=%HERE%\jre\bin\java.exe"
) else if defined JAVA_HOME (
    set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA=java"
)

set "LOG_DIR=%USERPROFILE%\.ekg\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
cd /d "%LOG_DIR%"

"%JAVA%" %EKG_JAVA_OPTS% ^
    --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.swing ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
    --add-opens java.base/java.util=ALL-UNNAMED ^
    --add-opens java.base/java.io=ALL-UNNAMED ^
    --add-opens java.base/java.net=ALL-UNNAMED ^
    --enable-native-access=ALL-UNNAMED,javafx.graphics ^
    "-Dekg.solr.template=%HERE%\solr" ^
    -cp "%HERE%\lib\*" ^
    io.softwareecg.awb.application.base.EkgAvajeApplication %*
endlocal
