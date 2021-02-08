#!/bin/bash

# Set JAVA_HOME to point to a specific Java 11+ JDK
#export JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64

# If JAVA_HOME is not set, try to find it using java itself
if [ -z ${JAVA_HOME} ]; then
	command -v java >/dev/null 2>&1 || { echo >&2 "java is required, but it's not installed. Set JAVA_HOME or add java to your path."; exit 1; }
	JAVA_HOME=`java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | sed -e 's/^.*java.home = \(.*\)$/\1/'`
fi;

# Make sure we are using Java 11+
JAVA_VERSION=`${JAVA_HOME}/bin/java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1`
if [ -z ${JAVA_VERSION} ] || [ ${JAVA_VERSION} -lt 11 ]; then
        echo "Java 11+ is required to run this application!"
        exit -1
fi;

# Find out lighty application name and version number
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
JAR_FILE=`ls -1 "${SCRIPT_DIR}" | grep .jar | head -n1`
APP_NAME=`echo "${JAR_FILE}" | sed -e 's/^\(.*\)-\([0-9]\+\.[0-9]\+\.[0-9]\+\)\(-SNAPSHOT\)\?\(-javadoc\)\?\.jar$/\1/'`
APP_VERSION=`echo "${JAR_FILE}" | sed -e 's/^.*-\([0-9]\+\.[0-9]\+\.[0-9]\+\)\(-SNAPSHOT\)\?\(-javadoc\)\?\.jar$/\1\2/'`

# Run the application
( cd "${SCRIPT_DIR}" && ${JAVA_HOME}/bin/java -jar "${APP_NAME}-${APP_VERSION}.jar" $* )