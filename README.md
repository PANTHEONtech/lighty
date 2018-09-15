[![Build Status](https://travis-ci.org/PantheonTechnologies/lighty-core.svg?branch=8.3.x)](https://travis-ci.org/PantheonTechnologies/lighty-core)
[![Coverity Scan Build Status](https://scan.coverity.com/projects/16327/badge.svg)](https://scan.coverity.com/projects/lighty-core) 


# lighty.io 8.3
__lighty.io__ is a Software Development Kit powered by [OpenDaylight](https://www.opendaylight.org/) to support, ease and accelerate development of
Software-Defined Networking (SDN) solutions in Java.
lighty.io is a toolkit for SDN application programmers and solution architects that can be used to build and integrate SDN controllers.
It utilizes core [OpenDaylight](https://www.opendaylight.org/) components, which are available as a set of libraries.
The final SDN application architecture is up to the user.

_This branch maintains compatibility with __OpenDaylight Oxygen SR3__ release._

## SDN controller architecture
![architecture](docs/lighty.io-controller-architecture.svg)

## Components
* __lighty-codecs__ - easy to use IO YANG data operations
* __lighty-core__ - [OpenDaylight](https://www.opendaylight.org/) [core services](lighty-core/lighty-controller/README.md): MD-SAL, controller, yangtools, clustering
* __lighty-examples__ - lighty.io [examples and applications](lighty-examples/controllers/README.md)
* __lighty-models__ - example [YANG models](lighty-models/README.md) and YANG model artifacts
* __lighty-modules__ - [OpenDaylight](https://www.opendaylight.org/) NorthBound (NB) and SouthBound (SB) plugin initializers
  - [RESTCONF north-bound plugin](lighty-modules/northbound-modules/lighty-restconf-nb-community)
  - [NETCONF south-bound plugin](lighty-modules/southbound-modules/lighty-netconf-sb) 
* __lighty-resources__ - resource artifacts

## Build and Install
In order to build and install lighty.io artifacts locally, follow the procedure below:
* __install JDK__ - make sure [JDK 8](http://openjdk.java.net/install/) is installed
* __install maven__ - make sure you have maven 3.5.0 or later installed
* __setup maven__ - make sure you have proper [settings.xml](https://github.com/opendaylight/odlparent/blob/master/settings.xml) in your ```~/.m2``` directory
* __build, install locally, and test__ - by running command: ``mvn clean install``

## Build SDN controller
lighty.io offers simplified SDN application development procedure. Follow [this](lighty-examples/controllers/README.md) manual to create your own SDN controller project.

![controller startup sequence](docs/lighty.io-controller-startup-sequence.svg)

## Support, FAQ and examples of use
If you are interested, technical support, blogs, FAQ, technical articles and more examples are available at
[lighty.io](https://lighty.io/)
