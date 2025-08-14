Lighty Controller
=================
```LightyController``` is key component of Lighty project. It represents
runtime of all important ODL subsystems such as:
* __MD-SAL__ - model-driven service abstraction layer
* __controller__ - MD-SAL and related ODL services
* __yangtools__ - tooling and libraries providing support of NETCONF and YANG for Java
* __clustering__ - pekko actor system with clustering 

References to important ODL runtime services are provided by ```LightyController.getServices()``` method call.

How to use it
-------------
To use Lighty controller in your project:
1. Add dependency in your pom.xml file.
```
  <dependency>
    <groupId>io.lighty.core</groupId>
    <artifactId>lighty-controller</artifactId>
    <version>22.1.0-SNAPSHOT</version>
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
### Initial configuration data

Lighty can be started with arbitrary yang modeled configuration data, which will be imported on startup to config datastore
as a merge operation so other data loaded (e.g from some persistence mechanism) will also be kept.\
To use this feature, create json/xml file with data you want to load on startup, then put:
```
"initialConfigData": {
      "pathToInitDataFile": "path/to/data/file",
      "format": "json"
    }
```
in your lighty .json controller configuration.\
If something goes wrong (e.g file doesn't exist, initial data isn't valid ..) `lightyController.start()` returns false
 and your application should react to it, for example initialize shutdown procedure.

### Configuration Files

* __lightyControllerConfig.json__ - main configuration file containing LightyController config options and paths to other configuration files.
* __pekko.conf__ - configuration file for pekko actor system
* __factory-pekko.conf__
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
                { "usedBy": "CONTROLLER", "name": "cluster-admin", "revision": "2025-01-31", "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:md:sal:cluster:admin"},
                { "usedBy": "CONTROLLER", "name": "distributed-datastore-provider", "revision": "2025-01-30", "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:config:distributed-datastore-provider"},
                { "usedBy": "CONTROLLER", "name": "odl-entity-owners", "nameSpace": "urn:opendaylight:params:xml:ns:yang:controller:entity-owners"},
                { "usedBy": "CONTROLLER", "name": "ietf-yang-types", "revision": "2013-07-15", "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-yang-types"},
                { "usedBy": "CONTROLLER", "name": "odl-general-entity", "revision": "2015-09-30", "nameSpace": "urn:opendaylight:params:xml:ns:yang:mdsal:core:general-entity"},
                { "usedBy": "CONTROLLER", "name": "yang-ext", "revision": "2013-07-09", "nameSpace": "urn:opendaylight:yang:extension:yang-ext"},
                { "usedBy": "CONTROLLER", "name": "opendaylight-l2-types", "revision": "2013-08-27", "nameSpace": "urn:opendaylight:l2:types"},
                { "usedBy": "CONTROLLER", "name": "iana-if-type", "revision": "2023-01-26", "nameSpace": "urn:ietf:params:xml:ns:yang:iana-if-type"},
                { "usedBy": "CONTROLLER", "name": "ietf-interfaces", "revision": "2018-02-20", "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-interfaces"},
                { "usedBy": "CONTROLLER", "name": "network-topology", "revision": "2013-10-21", "nameSpace": "urn:TBD:params:xml:ns:yang:network-topology"},
                { "usedBy": "CONTROLLER", "name": "ietf-inet-types", "revision": "2013-07-15", "nameSpace": "urn:ietf:params:xml:ns:yang:ietf-inet-types"}
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
For proper clustering configuration of LightyController, appropriate __pekko.conf__ must be provided.
