#!/bin/sh

#start controller
cd /lighty-cluster-app-12.2.3
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-12.2.3.jar -n 0 -k
