[![Build Status](https://travis-ci.org/PantheonTechnologies/lighty-core.svg?branch=8.2.0)](https://travis-ci.org/PantheonTechnologies/lighty-core)

# lighty.io 8.2
__lighty.io__ is a Software Development Kit powered by [OpenDaylight](https://www.opendaylight.org/) to support, ease and accelerate development of
Software-Defined Networking (SDN) solutions in Java.
lighty.io is a toolkit for SDN application programmers and solution architects that can be used to build and integrate SDN controllers.
It utilizes core [OpenDaylight](https://www.opendaylight.org/) components, which are available as a set of libraries.
The final SDN application architecture is up to the user.

_This branch maintains compatibility with __OpenDaylight Oxygen SR2__ release._

## Example SDN controller architecture
![architecture](docs/lighty.io-controller-architecture.svg)

## Components
* __lighty-codecs__ - easy to use IO YANG data operations
* __lighty-core__ - [OpenDaylight](https://www.opendaylight.org/) [core initializer](lighty-core/lighty-controller/README.md): MD-SAL, controller, yangtools, clustering
* __lighty-models__ - example YANG models and YANG model artifacts
* __lighty-modules__ - [OpenDaylight](https://www.opendaylight.org/) NorthBound (NB) and SouthBound (SB) plugin initializers
* __lighty-resources__ - resource artifacts
* __lighty-examples__ - lighty.io [examples and applications](lighty-examples/controllers/README.md)

## Build and Install
In order to build and install lighty.io artifacts, follow the procedure below:
* __maven installation__ - make sure you have maven 3.5.0 or later installed
* __maven setup__ - make sure you have proper [settings.xml](https://github.com/opendaylight/odlparent/blob/master/settings.xml) in your ~/.m2 directory
* __build, install and test__ - by running command: ``mvn clean install``
* __quick rebuild__ - by running command: ``mvn clean install -DskipTests``

## Support and use cases
If you are interested, technical support, blogs, technical articles and more examples are available at 
[lighty.io](https://lighty.io/)  

