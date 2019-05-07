#!/bin/bash

#start controller with java 8
java -jar lighty-community-restconf-netconf-app-10.0.1-SNAPSHOT.jar sampleConfigSingleNode.json


#start controller with java 11 or later
#java --add-modules java.xml.bind -jar lighty-community-restconf-netconf-app-10.0.1-SNAPSHOT.jar sampleConfigSingleNode.json
