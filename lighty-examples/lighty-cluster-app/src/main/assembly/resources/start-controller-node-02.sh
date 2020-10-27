#!/bin/bash

#start controller
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-12.2.1-SNAPSHOT.jar -n 2 #-c configNode02.json
