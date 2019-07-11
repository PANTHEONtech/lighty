#!/bin/bash

#start controller with java 8
java -ms128m -mx128m -XX:MaxMetaspaceSize=128m -jar lighty-cluster-app-11.0.0-SNAPSHOT.jar -n 3 #-c configNode02.json
