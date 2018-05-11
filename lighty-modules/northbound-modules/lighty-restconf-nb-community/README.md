Lighty RESTCONF NETCONF - Community
======================================
```io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf``` provides northbound interface to
running Opendaylight services via HTTP and WebSocket protocol. CommunityRestConf requires initialized services
from LightyController. CommunityRestConf starts it's own servlet container and WebsSocket end-point, using
two ports.

How to use it
-------------
To use RESTCONF in your project:
1. Add dependency in your pom.xml file.
```
  <dependency>
    <groupId>io.lighty.modules.northbound.restconf</groupId>
    <artifactId>lighty-restconf-nb-community</artifactId>
    <version>8.0.0-SNAPSHOT</version>
  </dependency>
```

2. Initialize and start an instance of CommunityRestConf in your code:
```
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getDefaultRestConfConfiguration();
  ODLRestConf odlRestConf = new ODLRestConfBuilder()
      .from(RestConfConfigUtils.getRestConfConfiguration(restConfConfig, odlController.getServices()))
      .build();
  odlRestConf.start();
```

Configuration
-------------
1. Use ControllerConfigUtils to get default configuration.
```
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getDefaultRestConfConfiguration();
```

2. Load configuration from JSON file.
```
  Path configPath = Paths.get("/path/to/odlControllerConfig.json");
  InputStream is = Files.newInputStream(configPath);
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getRestConfConfiguration();
```

3. Load configuration from classpath JSON resource.
```
  InputStream is = this.getClass().getClassLoader().getResourceAsStream("/resoures/restConfConfig.json");
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getRestConfConfiguration();
```

4. Create configuration programmatically
```
  RestConfConfiguration restConfConfiguration = new RestConfConfiguration();
  restConfConfiguration.setHttpPort(8888);
  restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());
  ...
```

### Configuration file restConfConfig.json
Example configuration file is located [here](src/main/resources/restConfConfig.json)

Default Models
--------------
CommunityRestConf requires following models to be loaded by LightyController.
```
  "mvn:org.opendaylight.netconf/ietf-yang-library/1.7.0-SNAPSHOT",
  "mvn:org.opendaylight.netconf/ietf-restconf-monitoring/1.7.0-SNAPSHOT"
```

Clustering
----------
As RESTCONF provides northbound interface, clustering has no effect on this component.
