# NETCONF south-bound plugins
This module contains 3 plugins that provide following Opendaylight features functionality:

 - odl-netconf-topology - NetconfTopologyPluginImpl
 - odl-netconf-clustered-topology - NetconfClusteredTopologyPluginImpl
 - odl-netconf-callhome - NetconfCallhomePluginImpl

Detailed information about usage can be found in
[Netconf User Guide](https://docs.opendaylight.org/en/stable-fluorine/release-notes/projects/netconf.html).

## NETCONF topology plugin
Plugin listens on changes in config data store on path network-topology/topology/topology-netconf/.
When new NETCONF node is created, it initiates a connection to the NETCONF device.
It registers mount point which provides following services:

 - DOMRpcService
 - DOMDataBroker
 - DOMNotificationService

## Netconf clustered topology plugin
Clustered topology plugin provides the same functionality as the topology plugin, but it is cluster aware.
When a new node is written to data store, only one cluster member is elected as a master and only master
member creates a ssh connection to device. Other cluster members use master for communication with the device.
Slaves route all their requests to the device via master member.

Only one topology plugin must be used at the same time. Running both topology plugins is not supported.

## Netconf call home plugin
In most cases the client initiates a connection to a NETCONF server, running on the device. NETCONF call home
plugin allows the server(device) side to initiate a connection to a client(Lighty). Lighty must be configured
to accept device connection. It is done in the odl-netconf-callhome-server.yang module. Device public key is
added there together with credentials, which the plugin uses to authenticate the device. Call home server runs
on a port 4334, by default.

This plugin can be used together with one of the topology plugins.

## How to use it
This plugin requires LightyController instance in order to run.
To use NETCONF in your project:
1. Add dependency in your pom.xml file.
```
  <dependency>
    <groupId>io.lighty.modules</groupId>
    <artifactId>lighty-netconf-sb</artifactId>
    <version>19.4.0</version>
  </dependency>  
```
2. Initialize and start an instance of NETCONF SBP in your code:
```
NetconfConfiguration netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
    netconfSBPConfiguration, lightyController.getServices());
LightyModule netconfSouthboundPlugin = new NetconfTopologyPluginBuilder()
    .from(netconfSBPConfiguration, lightyController.getServices())
    .build();
netconfSouthboundPlugin.start();
```

### Configuration
1. Use NetconfConfigUtils to get default configuration.
```
  NetconfConfiguration netconfConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
  ...
```
2. Load configuration from JSON file.
```
  Path configPath = Paths.get("/path/to/lightyControllerConfig.json");
  InputStream is = Files.newInputStream(configPath);
  NetconfConfiguration netconfConfig = NetconfConfigUtils.createNetconfConfiguration(is);
  ...
```
3. Load configuration from classpath JSON resource.
```
  InputStream is = this.getClass().getClassLoader().getResourceAsStream("/resoures/netConfConfig.json");
  NetconfConfiguration netconfConfig = NetconfConfigUtils.createNetconfConfiguration(is);
  ...
```
4. Create configuration programmatically.
```
  NetconfConfiguration netconfConfig = new NetconfConfiguration();
  netconfConfig.setWriteTxTimeout(200);
  ...
```
5. Last step before using NetconfConfiguration is to inject LightyServices into configuration instance loaded previously.
```
NetconfConfiguration netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
    netconfSBPConfiguration, lightyController.getServices());
```

### Configuration file example
Example configuration file is located [here](src/main/resources/sampleConfigSingleNode.json)
