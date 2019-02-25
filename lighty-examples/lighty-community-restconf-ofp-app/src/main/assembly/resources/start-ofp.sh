#!/bin/bash

# Starts application with default settings
# alpn-boot version depends on java version 
java -Xbootclasspath/p:lib/alpn-boot-8.1.13.v20181017.jar -jar lighty-community-restconf-ofp-app-9.1.0-SNAPSHOT.jar