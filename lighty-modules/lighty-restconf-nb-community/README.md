# RESTCONF north-bound plugin
```io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf``` provides northbound interface to
running Opendaylight services via HTTP and WebSocket protocol. CommunityRestConf requires initialized services
from LightyController. CommunityRestConf starts it's own servlet container and WebsSocket end-point, using
two ports.

## How to use it
This plugin requires LightyController instance in order to run.
To use RESTCONF in your project:
1. Add dependency in your pom.xml file.
```
  <dependency>
    <groupId>io.lighty.modules</groupId>
    <artifactId>lighty-restconf-nb-community</artifactId>
    <version>22.0.0</version>
  </dependency>
```

2. Initialize and start an instance of CommunityRestConf in your code:
```
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getDefaultRestConfConfiguration();
  CommunityRestConf communityRestConf = CommunityRestConfBuilder
      .from(restConfConfig)
      .build();
  communityRestConf.start();
```

## Configuration
1. Use RestConfConfigUtils to get default configuration.
```
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getDefaultRestConfConfiguration();
```

2. Load configuration from JSON file.
```
  Path configPath = Paths.get("/path/to/lightyControllerConfig.json");
  InputStream is = Files.newInputStream(configPath);
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getRestConfConfiguration(is);
```

3. Load configuration from classpath JSON resource.
```
  InputStream is = this.getClass().getClassLoader().getResourceAsStream("/resoures/restConfConfig.json");
  RestConfConfiguration restConfConfig
      = RestConfConfigUtils.getRestConfConfiguration(is);
```

4. Create configuration programmatically.
```
  RestConfConfiguration restConfConfig = new RestConfConfiguration();
  restConfConfiguration.setHttpPort(8888);
  restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());
  ...
```
5. Last step before using RestConfConfiguration is to inject LightyServices into configuration instance loaded previously.
```
  ...
  RestConfConfiguration restConfConfiguration = getRestConfConfiguration(restConfConfig, lightyServices);
  ...
```

### Configuration file restConfConfig.json
Example configuration file is located [here](src/main/resources/restConfConfig.json)

### Default Models
CommunityRestConf requires following models to be loaded by LightyController.
```
  "org.opendaylight.mdsal.model:rfc7895:1.0.1-SNAPSHOT",
  "org.opendaylight.netconf:ietf-restconf-monitoring:1.8.1-SNAPSHOT"
```

### Clustering
As RESTCONF provides northbound interface, clustering has no effect on this component.
