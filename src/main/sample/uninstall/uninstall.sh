#!/bin/bash
# find java
if [ "x$JAVA_HOME" = "x" ] ; then
    JAVA="$( which java 2>/dev/null )"
else
    JAVA="$JAVA_HOME"/bin/java
fi
# check results
if [ "x$JAVA" = "x" ] ; then
    echo "Cannot find java executable, check JAVA_HOME, aborting" 1>&2
    exit 1
else
    echo "Using java executable: $JAVA"
fi
# get dir
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# run
"$JAVA" -jar "$SCRIPT_DIR"/uninstall.jar
