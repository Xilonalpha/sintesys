#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)

if [ -z "$JAVA_HOME" ] ; then
    JAVACMD=java
else
    JAVACMD="$JAVA_HOME/bin/java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
