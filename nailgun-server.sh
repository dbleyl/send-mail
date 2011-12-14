#!/bin/bash
java -cp .:$CLOJURE_HOME/clojure.jar:$NAILGUN_HOME/server-2.3.0.jar vimclojure.nailgun.NGServer 127.0.0.1:2222
#java -cp .:$CLOJURE_HOME/clojure.jar:$CASCALOG_JAR:`lein classpath`:$NAILGUN_HOME/server-2.3.0.jar vimclojure.nailgun.NGServer 127.0.0.1

