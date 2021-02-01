#!/bin/sh

#start controller
cd /lighty-cluster-app-12.3.1-SNAPSHOT
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-12.3.1-SNAPSHOT.jar -n 0 -k
