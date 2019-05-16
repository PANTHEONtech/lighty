[![Build Status](https://travis-ci.org/PantheonTechnologies/lighty-core.svg?branch=master)](https://travis-ci.org/PantheonTechnologies/lighty-core)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.lighty.core/lighty-bom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.lighty.core/lighty-bom)
[![License](https://img.shields.io/badge/License-EPL%201.0-blue.svg)](https://opensource.org/licenses/EPL-1.0)

# lighty.io 11
__lighty.io__ is a Software Development Kit powered by [OpenDaylight](https://www.opendaylight.org/) to support, ease and accelerate development of
Software-Defined Networking (SDN) solutions in Java.
lighty.io is a toolkit for SDN application programmers and solution architects that can be used to build and integrate SDN controllers.
It utilizes core [OpenDaylight](https://www.opendaylight.org/) components, which are available as a set of libraries.

_This branch maintains compatibility with __OpenDaylight Sodium__ release._

## SDN controller architecture
![architecture](docs/lighty.io-controller-architecture.svg)

## Components
* __lighty-codecs__ - easy to use IO YANG data operations
* __lighty-core__ - [OpenDaylight](https://www.opendaylight.org/) [core services](lighty-core/lighty-controller/README.md): MD-SAL, controller, yangtools, clustering
  - [google guice extension](lighty-core/lighty-controller-guice-di/README.md) - dependency injection extension for [google guice](https://github.com/google/guice)
  - [spring extension](lighty-core/lighty-controller-spring-di/README.md) - dependency injection extension for [spring.io](https://spring.io/)  
* __lighty-examples__ - lighty.io [examples and applications](lighty-examples/README.md)
* __lighty-models__ - example [YANG models](lighty-models/README.md) and YANG model artifacts
* __lighty-modules__ - [OpenDaylight](https://www.opendaylight.org/) NorthBound (NB) and SouthBound (SB) plugin initializers
  - [RESTCONF north-bound plugin](lighty-modules/northbound-modules/lighty-restconf-nb-community)
  - [NETCONF south-bound plugin](lighty-modules/lighty-netconf-sb) 
  - [OPENFLOW south-bound plugin](lighty-modules/lighty-openflow-sb)
* __lighty-resources__ - resource artifacts

## Build and Install
In order to build and install lighty.io artifacts locally, follow the procedure below:
* __install JDK__ - make sure [JDK 8](http://openjdk.java.net/install/) or [JDK 11](https://jdk.java.net/11/) is installed
* __install maven__ - make sure you have maven 3.5.4 or later installed
* __setup maven__ - make sure you have proper [settings.xml](https://github.com/opendaylight/odlparent/blob/master/settings.xml) in your ```~/.m2``` directory
* __build and install locally__ - by running command: ``mvn clean install -DskipTests``

## Build SDN controller
lighty.io offers simplified SDN application development procedure. Follow [this](lighty-examples/README.md) manual to create your own SDN controller project.

![controller startup sequence](docs/lighty.io-controller-startup-sequence.svg)

## How-To migrate from ODL to lighty.io
[This guide](docs/ODL-migration-guide.md) describes migration procedure from ODL/Karaf application to lighty.io.
It contains summary of practical experiences based on real-life ODL project migrations.

## Run JUnit and IT tests
lighty.io project contains JUnit tests and integration tests. IT tests are special, because complete ODL/lighty.io controller is started often with south-bound and north-bound plugins.
IT tests are comparable with single-feature tests in ODL/Karaf environment, but much faster.
_Before starting IT tests, please make sure that ports 8080, 8888, 8185, 2550 are free on localhost._

To run unit tests and integration tests, use command:

```mvn clean install```

## Support, FAQ and examples of use
If you are interested, technical support, blogs, FAQ, technical articles and more examples are available at 
[lighty.io](https://lighty.io/)
