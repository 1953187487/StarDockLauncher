#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$DIR"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVA="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}/bin/java"
exec "$JAVA" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
