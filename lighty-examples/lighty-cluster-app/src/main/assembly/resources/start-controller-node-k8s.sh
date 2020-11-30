#!/bin/sh

#start controller
cd /lighty-cluster-app-13.1.0-SNAPSHOT
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-13.1.0-SNAPSHOT.jar -n 0 -k
