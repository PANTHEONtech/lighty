#!/bin/sh

#start controller
cd /lighty-cluster-app-13.1.1
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-13.1.1.jar -n 0 -k
