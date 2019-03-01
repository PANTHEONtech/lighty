#!/bin/bash

#start controller with java 8
java -jar lighty-community-restconf-netconf-app-9.2.1-SNAPSHOT.jar sampleConfigSingleNode.json


#start controller with java 11
#java --add-modules java.xml.bind -jar lighty-community-restconf-netconf-app-9.2.1-SNAPSHOT.jar sampleConfigSingleNode.json
