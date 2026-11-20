Software-EKG Workbench
======================

Start
-----
  Linux / macOS:   bin/software-ekg.sh
  Windows:         bin\software-ekg.bat

Needs Java 25 with JavaFX, for example the Liberica Full JDK. A distribution built with
"mvn package -Pdist" brings its own runtime in the directory "jre" and needs nothing else.

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
