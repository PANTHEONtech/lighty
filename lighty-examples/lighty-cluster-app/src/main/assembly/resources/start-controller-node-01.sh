#!/bin/bash

#start controller
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-12.2.1-SNAPSHOT.jar -n 1 #-c configNode01.json
