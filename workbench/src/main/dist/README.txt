Software-EKG Workbench
======================

Start
-----
  Linux / macOS:   bin/software-ekg.sh
  Windows:         bin\software-ekg.bat

Needs Java 25 with JavaFX, for example the Liberica Full JDK. A distribution built with
"mvn package -Pdist" brings its own runtime in the directory "jre"; it fits the operating
system and processor it was built on. On another machine the start script uses the Java in
JAVA_HOME or in the PATH instead.

On Linux the workbench needs a desktop session and GTK 3. The start script checks all of
this and says what is missing. To only run the checks:

  bin/software-ekg.sh --check

More memory for large analyses:   EKG_JAVA_OPTS=-Xmx8g bin/software-ekg.sh

Your data
---------
Projects, time series and stored credentials live in the directory ".ekg" of your home
directory, not in this one:

  ~/.ekg/metadata.mv.db   projects, repositories, bookmarks, data sources
  ~/.ekg/solr             the time series (set EKG_SOLR_HOME to move them)
  ~/.ekg/secrets          credentials of data sources
  ~/.ekg/logs             log files

To update the workbench, replace this directory with the new version. Your data stays.

Contents
--------
  bin/     start scripts
  lib/     the workbench and its libraries
  solr/    configuration of the embedded Solr; copied to ~/.ekg/solr on every start
  jre/     the Java runtime, if this distribution was built with one
