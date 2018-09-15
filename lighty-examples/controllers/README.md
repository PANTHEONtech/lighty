# Example SDN controller applications

The best way how to start consume lighty.io services is to check example applications in this project, usage of lighty core components as well as RESTCONF and NETCONF plugins.

 * Simple [RESTCONF-NETCONF SDN controller](lighty-community-restconf-netconf-app/README.md).
 * Simple [Spring Boot NETCONF SDN controller](lighty-controller-springboot-netconf/README.md).

## Setup controller project
Typical controller project requires initialization of [ODL core services](../../lighty-core/lighty-controller/src/main/java/io/lighty/core/controller/api/LightyServices.java), south-bound plugins and optionally RESTCONF north-bound plugin.
ODL core services represent MD-SAL layer, controller, DataStore, global schema context and all related services.

1. Add dependency on lighty.io core services  
```
<dependencies>
   <dependency>
      <groupId>io.lighty.core.parents</groupId>
      <artifactId>lighty-dependency-artifacts</artifactId>
      <version>8.3.0</version>
      <type>pom</type>
      <scope>import</scope>
   </dependency>
</dependencies>        
```

2. Optionally add dependencies on south-bound or north-bound plugins.
 * [RESTCONF NBP](../../lighty-modules/northbound-modules/lighty-restconf-nb-community/README.md)
 * [NETCONF SBP](../../lighty-modules/southbound-modules/lighty-netconf-sb/README.md)

3. Initialize and start LightyController
```
LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
lightyController.start().get();
```
  
## Controller initialization process
Controller startup sequence consists of 5 easy steps.
Step #4 is optional.

![startup-sequence](../../docs/lighty.io-controller-startup-sequence.svg)

## Controller types
lighty.io supports development of two basic controller types:
* __Standalone controller__ - runs in own JVM as micro service
* __Embedded controller__ - runs with other application components in single JVM
 