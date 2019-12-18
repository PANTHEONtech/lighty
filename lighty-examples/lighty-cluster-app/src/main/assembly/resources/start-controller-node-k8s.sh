#!/bin/sh

#start controller with java 8
cd /lighty-cluster-app-10.2.0-SNAPSHOT
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-10.2.0-SNAPSHOT.jar -n 0 -k
