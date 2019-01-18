#!/bin/bash

#start controller with java 8
java -jar lighty-community-restconf-netconf-app-9.1.1.jar sampleConfigSingleNode.json


#start controller with java 10 or later
#java --add-modules java.xml.bind -jar lighty-community-restconf-netconf-app-9.1.1.jar sampleConfigSingleNode.json
