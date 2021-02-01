Lighty Controller
=================
```LightyController``` is key component of Lighty project. It represents
runtime of all important ODL subsystems such as:
* __MD-SAL__ - model-driven service abstraction layer
* __controller__ - MD-SAL and related ODL services
* __yangtools__ - tooling and libraries providing support of NETCONF and YANG for Java
* __clustering__ - akka actor system with clustering 

References to important ODL runtime services are provided by ```LightyController.getServices()``` method call.

How to use it
-------------
To use Lighty controller in your project:
1. Add dependency in your pom.xml file.
```
  <dependency>
    <groupId>io.lighty.core</groupId>
    <artifactId>lighty-controller</artifactId>
    <version>12.3.1-SNAPSHOT</version>
  </dependency>
```

2. Initialize and start an instance of LightyController in your code:
```
  ControllerConfiguration defaultSingleNodeConfiguration
     = ControllerConfigUtils.getDefaultSingleNodeConfiguration();
  LightyController lightyController = new LightyControllerBuilder()
     .from(defaultSingleNodeConfiguration)
     .build();
  lightyController.start();
```

3. Use LightyServices in your application
```
  LightyServices lightyServices = lightyController.getServices();
  DataBroker dataBroker = lightyServices.getBindingDataBroker();
  WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
  ...
  writeTransaction.submit();
```

Configuration
-------------
There are several ways how to obtain proper configuration for your
instance of LightyController.

1. Use ControllerConfigUtils to get default configuration.
```
  ControllerConfiguration odlControllerConfiguration
    = ControllerConfigUtils.getSingleNodeConfiguration();
```

2. Load configuration from JSON file.
```
  Path configPath = Paths.get("/path/to/testLightyControllerConfig.json");
  InputStream is = Files.newInputStream(configPath);
  ControllerConfiguration singleNodeConfiguration
                          = ControllerConfigUtils.getSingleNodeConfiguration(is);
```

3. Load configuration from classpath JSON resource.
```
  InputStream is = this.getClass().getClassLoader().getResourceAsStream("/resoures/testLightyControllerConfig.json");
  ControllerConfiguration singleNodeConfiguration
                          = ControllerConfigUtils.getSingleNodeConfiguration(is);
```

4. Create configuration programmatically
```
  ControllerConfiguration controllerConfiguration = new ControllerConfiguration();
  controllerConfiguration.setRestoreDirectoryPath("custom/path");
  ...
```

### Configuration Files

* __lightyControllerConfig.json__ - main configuration file containing LightyController config options and paths to other configuration files.
* __akka.conf__ - configuration file for akka actor system
* __factory-akka.conf__
* __module-shards.conf__
* __modules.conf__

#### testLightyControllerConfig.json
Example configuration file is located [here](src/test/resources/testLightyControllerConfig.json)
LightyController configuration is expected under JSON "controller" element.

Default Models
--------------
It is recommended to use this default yang model set when initializing
an instance of your ODL controller. Your application may extend this default model set
or completely override it if required.

```
        "schemaServiceConfig":{
            "topLevelModels":[
                { "nameSpace": "urn:TBD:params:xml:ns:yang:network:isis-topology", "name": "isis-topology", "revision": "2013-07-12" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:core:general-entity", "name": "general-entity", "revision": "2015-08-20" },
                { "nameSpace": "subscribe:to:notification", "name": "subscribe-to-notification", "revision": "2016-10-28" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:cluster:admin", "name": "cluster-admin", "revision": "2015-10-13" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-lisp-address-types", "name": "ietf-lisp-address-types", "revision": "2015-11-05" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:aaa", "name": "aaa", "revision": "2016-12-14" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:config:actor-system-provider:impl", "name": "actor-system-provider-impl", "revision": "2015-10-05" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ospf-topology", "name": "ospf-topology", "revision": "2013-07-12" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:config:distributed-datastore-provider", "name": "distributed-datastore-provider", "revision": "2014-06-12" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-yang-library", "name": "ietf-yang-library", "revision": "2016-06-21" },
                { "nameSpace": "urn:TBD:params:xml:ns:yang:network:isis-topology", "name": "isis-topology", "revision": "2013-10-21" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:inmemory-datastore-provider", "name": "opendaylight-inmemory-datastore-provider", "revision": "2014-06-17" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-restconf", "name": "ietf-restconf", "revision": "2013-10-19" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:iana-afn-safi", "name": "iana-afn-safi", "revision": "2013-07-04" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:config:concurrent-data-broker", "name": "odl-concurrent-data-broker-cfg", "revision": "2014-11-24" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:clustering:entity-owners", "name": "entity-owners", "revision": "2015-08-04" },
                { "nameSpace": "urn:sal:restconf:event:subscription", "name": "sal-remote-augment", "revision": "2014-07-08" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-access-control-list", "name": "ietf-access-control-list", "revision": "2016-02-18" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:pingpong", "name": "opendaylight-pingpong-broker", "revision": "2014-11-07" },
                { "nameSpace": "instance:identifier:patch:module", "name": "instance-identifier-patch-module", "revision": "2015-11-21" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-network-topology", "name": "ietf-network-topology", "revision": "2015-06-08" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-yang-types", "name": "ietf-yang-types", "revision": "2010-09-24" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:mdsal:core:general-entity", "name": "odl-general-entity", "revision": "2015-09-30" },
                { "nameSpace": "urn:opendaylight:yang:extension:yang-ext", "name": "yang-ext", "revision": "2013-07-09" },
                { "nameSpace": "urn:opendaylight:l2:types", "name": "opendaylight-l2-types", "revision": "2013-08-27" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:md:sal:config:impl:cluster-singleton-service", "name": "cluster-singleton-service-impl", "revision": "2016-07-18" },
                { "nameSpace": "urn:TBD:params:xml:ns:yang:ospf-topology", "name": "ospf-topology", "revision": "2013-10-21" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-restconf-monitoring", "name": "ietf-restconf-monitoring", "revision": "2017-01-26" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:clustering:prefix-shard-configuration", "name": "prefix-shard-configuration", "revision": "2017-01-10" },
                { "nameSpace": "urn:opendaylight:aaa:app:config", "name": "aaa-app-config", "revision": "2017-06-19" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-restconf", "name": "ietf-restconf", "revision": "2017-01-26" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:config:legacy-entity-ownership-service-provider", "name": "opendaylight-legacy-entity-ownership-service-provider", "revision": "2016-02-26" },
                { "nameSpace": "urn:ietf:params:xml:ns:yang:iana-if-type", "name": "iana-if-type", "revision": "2014-05-08" },
                { "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding:impl", "name": "opendaylight-sal-binding-broker-impl", "revision": "2013-10-28" }
            ]
        }
    },
    
```

### How to add your own model
By simply extending LightyController configuration. Please note, that jar file containing
model and java bindings must be on JVM's classpath as well as accessible on file system
as java archive. You can add your model by:
1. Specifying your custom model in JSON configuration file.
2. Creating configuration programmatically.

Clustering
----------
For proper clustering configuration of LightyController, appropriate __akka.conf__ must be provided.
