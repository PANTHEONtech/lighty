#!/bin/bash

#start controller with java 8
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-10.2.1-SNAPSHOT.jar -n 3 #-c configNode02.json
