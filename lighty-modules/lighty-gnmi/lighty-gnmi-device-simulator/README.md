# gNMI device simulator
This simulator provides gNMI device driven by gNMI proto files, with datastore defined by set of YANG models.

## How to build and run gNMI device simulator

1. Add maven dependency to your pom.xml file.
```
   <dependency>
      <groupId>io.lighty.modules.gnmi</groupId>
      <artifactId>lighty-gnmi-device-simulator</artifactId>
      <version>20.3.0</version>
   </dependency>
```

2. Initialize and start an instance of gNMI device simulator. Setting configuration for gNMI device simulator 
   is required. Use ```GnmiSimulatorConfUtils``` for loading configuration. There are two options: load configuration
   from file or load default configuration. Specifying the yang models for simulator si required. Yang models can be load 
   from class path with ```schemaServiceConfig``` or added as path to folder containing yang models with json element ```yangsPath```.
   Both option for loading models can be used at same time. Default models configuration points to the ```resources/yangs``` folder. 
   Load default configuration:
```
    GnmiSimulatorConfiguration gnmiSimulatorConfiguration = GnmiSimulatorConfUtils.loadDefaultGnmiSimulatorConfiguration();
```
   Load configuration from file:
```
   GnmiSimulatorConfiguration gnmiSimulatorConfiguration = GnmiSimulatorConfUtils
        .loadGnmiSimulatorConfiguration(Files.newInputStream(Path.of(CONFIG_PATH)));
```
   Example configuration file:
```
   {
       "gnmi_simulator":{
           "targetAddress": "0.0.0.0",
           "targetPort": 3333,
           "initialStateDataPath": "./simulator/initialJsonData.json",
           "initialConfigDataPath": "./simulator/initialJsonData.json",
           "certPath": "./simulator/certs/server.crt",
           "certKeyPath": "./simulator/certs/server.key",
           "yangsPath": "./yangs",
           "username": "admin",
           "password": "admin",
           "maxConnections": 50,
           "schemaServiceConfig": {
              "topLevelModels": [
                { "nameSpace":"http://openconfig.net/yang/aaa","name":"openconfig-aaa","revision":"2020-07-30"},
                { "nameSpace":"http://openconfig.net/yang/aaa","name":"openconfig-aaa-radius","revision":"2020-07-30"},
                { "nameSpace":"http://openconfig.net/yang/aaa","name":"openconfig-aaa-tacacs","revision":"2020-07-30"},
                { "nameSpace":"http://openconfig.net/yang/aaa/types","name":"openconfig-aaa-types","revision":"2018-11-21"},
                { "nameSpace":"http://openconfig.net/yang/alarms/types","name":"openconfig-alarm-types","revision":"2018-11-21"},
                { "nameSpace":"http://openconfig.net/yang/alarms","name":"openconfig-alarms","revision":"2019-07-09"},
                { "nameSpace":"http://openconfig.net/yang/openconfig-ext","name":"openconfig-extensions","revision":"2020-06-16"},
                { "nameSpace":"http://openconfig.net/yang/types/inet","name":"openconfig-inet-types","revision":"2021-01-07"},
                { "nameSpace":"http://openconfig.net/yang/interfaces","name":"openconfig-interfaces","revision":"2021-04-06"},
                { "nameSpace":"http://openconfig.net/yang/interfaces/aggregate","name":"openconfig-if-aggregate","revision":"2020-05-01"},
                { "nameSpace":"http://openconfig.net/yang/openconfig-if-types","name":"openconfig-if-types","revision":"2018-11-21"},
                { "nameSpace":"http://openconfig.net/yang/interfaces/ethernet","name":"openconfig-if-ethernet","revision":"2021-06-09"},
                { "nameSpace":"http://openconfig.net/yang/license","name":"openconfig-license","revision":"2020-04-22"},
                { "nameSpace":"http://openconfig.net/yang/messages","name":"openconfig-messages","revision":"2018-08-13"},
                { "nameSpace":"http://openconfig.net/yang/openflow","name":"openconfig-openflow","revision":"2018-11-21"},
                { "nameSpace":"http://openconfig.net/yang/openflow/types","name":"openconfig-openflow-types","revision":"2020-06-30"},
                { "nameSpace":"http://openconfig.net/yang/platform","name":"openconfig-platform","revision":"2021-01-18"},
                { "nameSpace":"http://openconfig.net/yang/platform-types","name":"openconfig-platform-types","revision":"2021-01-18"},
                { "nameSpace":"http://openconfig.net/yang/system/procmon","name":"openconfig-procmon","revision":"2019-03-15"},
                { "nameSpace":"http://openconfig.net/yang/system","name":"openconfig-system","revision":"2020-04-13"},
                { "nameSpace":"http://openconfig.net/yang/system/logging","name":"openconfig-system-logging","revision":"2018-11-21"},
                { "nameSpace":"http://openconfig.net/yang/system/terminal","name":"openconfig-system-terminal","revision":"2018-11-21"},
                { "nameSpace":"http://openconfig.net/yang/openconfig-types","name":"openconfig-types","revision":"2019-04-16"},
                { "nameSpace":"http://openconfig.net/yang/vlan","name":"openconfig-vlan","revision":"2019-04-16"},
                { "nameSpace":"http://openconfig.net/yang/vlan-types","name":"openconfig-vlan-types","revision":"2020-06-30"},
                { "nameSpace":"http://openconfig.net/yang/types/yang","name":"openconfig-yang-types","revision":"2021-03-02"}
              ]
           }
       }
   }
```
   Build and start gNMI simulator device:
```
   final SimulatedGnmiDevice simulatedGnmiDevice
         = new SimulatedGnmiDevice()
               .from(gnmiSimulatorConfiguration)
               .build();
   simulatedGnmiDevice.start();
```

3. Close gNMI device simulator.

```
   simulatedGnmiDevice.stop();
```

## Additional configuration

Any additional configuration has to be done before starting simulator. This configuration is available in
[GnmiSimulatorConfiguration](src/main/java/io/lighty/modules/gnmi/simulatordevice/config/GnmiSimulatorConfiguration.java).

1. **setTargetAddress(String)** - (default: "0.0.0.0") Set host address.

2. **setTargetPort(int)** - (default: 10161) Set port value (port >= 0 && port <= 65535).

3. **setInitialConfigDataPath(String)** - (default: empty) Set configuration data-store for device. Path to file
   in JSON format is expected. Data inside this file should be modeled by YANG files provided in *setYangsPath(String)*

4. **setMaxConnections(int)** - (default: 50) Determines the number of connections queued.

5. **setCertPath(String)** - (default: "certs/server.crt") Set certificate for gNMI device simulator.
   Path to certificate file is expected.

6. **setCertKeyPath(String)** - (default: "certs/server.key") Set certificate key for gNMI device simulator.
   Path to certificate key file is expected.

7. **setYangsPath(String)** - Load provided yang models from file.

8. **setUsername(String)** - default: empty) Setting username will allow authentication. All requests routed to gNMI
   simulator will have to provide username/password authentication in request metadata.

9. **setPassword(String)** - default: empty) Setting password will allow authentication. All requests routed to gNMI
   simulator will have to provide username/password authentication in request metadata.

10. **setUsePlaintext(boolean)** - (default: turned off) This will Disable TLS validation. If this is enabled, then there is no need
    to specify TLS related options.

11. **setBossGroup(EventLoopGroup)** - (default: NioEventLoopGroup(1)) Provides the boss EventGroupLoop to the server.

12. **setWorkerGroup(EventLoopGroup)** - (default: NioEventLoopGroup(0)) Provides the worker EventGroupLoop to the server.

13. **setInitialStateDataPath(String)** - (default: empty) Set **?operational?** data-store for device. Path to
    file in JSON format is expected. Data inside this file should be modeled by YANG models provided in *setYangsPath(String)*
    or *setYangModulesInfo(Set<YangModuleInfo>)*

15. **setBossGroup(EventLoopGroup)** - (default: NioEventLoopGroup(1)) Provides the boss EventGroupLoop to the server.

16. **setWorkerGroup(EventLoopGroup)** - (default: NioEventLoopGroup(0)) Provides the worker EventGroupLoop to the server.

17. **setSupportedEncodings(EnumSet<Gnmi.Encoding>)** - overwrites default encoding set {JSON_IETF, JSON}
        returned in CapabilityResponse.

18. **setGsonInstance(Gson)** - (default: default instance of Gson)  Provides an option to customize Gson parser instance
    used in device.

19. **setYangModulesInfo(Set<YangModuleInfo>)** - (default: empty) Load provided yang models from classpath.

##Example with gnmic client
This example will show how to execute basic operations on lighty.io gNMI device simulator. Request will be performed with
[gnmic](https://gnmic.kmrd.dev/) client.

1) Install gnmic client with online tutorial [here](https://gnmic.kmrd.dev/#installation)

2) Start gNMI device simulator with custom certificates and authentication. Data used for this example:
   - [YANG_MODELS_PATH](src/test/resources/test_schema)
   - [INITIAL_CONFIGURATION_PATH](src/test/resources/initData/config.json)
   - SERVER_CERTIFICATE / SERVER_PKCS8_KEY - was generated with script inside lighty-gnmi-connector module.
   ```
   GnmiSimulatorConfiguration gnmiSimulatorConfiguration = new GnmiSimulatorConfiguration()
            .setYangsPath(YANG_MODELS_PATH)
            .setInitialConfigDataPath(INITIAL_CONFIGURATION_PATH)
            .setTargetAddress("127.0.0.1")
            .setCertPath(SERVER_CERTIFICATE)
            .setCertKeyPath(SERVER_PKCS8_KEY)
            .setUsername("Admin")
            .setPassword("Admin")
            .setTargetPort(9090)
            .build();

   SimulatedGnmiDevice device = new SimulatedGnmiDevice(gnmiSimulatorConfiguration);
          device.start();
   ```

3) Get capabilities from gNMI device simulator
   ```
   gnmic -a 127.0.0.1:9090 capabilities --tls-ca CA_CERTIFICATE --tls-cert CLIENT_CERTIFICATE --tls-key CLIENT_KEY \
       -u Admin -p Admin

   gNMI version: 0.7.0
   supported models:
     - iana-if-type, IANA,
     - openconfig-alarm-types, OpenConfig working group, 0.2.1
     - openconfig-alarms, OpenConfig working group, 0.3.2
     - openconfig-extensions, OpenConfig working group,
     - openconfig-if-aggregate, OpenConfig working group, 2.4.3
     - openconfig-if-ethernet, OpenConfig working group, 2.8.1
     - openconfig-if-types, OpenConfig working group, 0.2.1
     - openconfig-inet-types, OpenConfig working group, 0.3.2
     - openconfig-interfaces, OpenConfig working group, 2.4.3
     - openconfig-platform, OpenConfig working group, 0.12.2
     - openconfig-platform-types, OpenConfig working group, 1.0.0
     - openconfig-types, OpenConfig working group, 0.5.1
     - openconfig-vlan, OpenConfig working group, 3.2.0
     - openconfig-vlan-types, OpenConfig working group, 3.1.1
     - openconfig-yang-types, OpenConfig working group, 0.2.1
   supported encodings:
     - JSON
     - JSON_IETF
     ```

4) Get data from gNMI device simulator
   ```
   gnmic -a 127.0.0.1:9090 --tls-ca CA_CERTIFICATE --tls-cert CLIENT_CERTIFICATE --tls-key CLIENT_KEY \
   --path interfaces/interface[name=br0]/ethernet/config --encoding json_ietf -u Admin -p Admin
   [
     {
       "timestamp": 1621604733259,
       "time": "1969-12-31T16:27:01.604733259-08:00",
       "updates": [
         {
           "Path": "interfaces/interface[name=br0]/ethernet/config",
           "values": {
             "interfaces/interface/ethernet/config": {
               "openconfig-if-ethernet:config": {
                 "auto-negotiate": true,
                 "enable-flow-control": true,
                 "openconfig-if-aggregate:aggregate-id": "admin",
                 "port-speed": "openconfig-if-ethernet:SPEED_10MB"
               }
             }
           }
         }
       ]
     }
   ]
   ```

5) Set data at gNMI device simulator
   - Data inside file updateFile.json:
      ```
      {"enable-flow-control": false,"openconfig-if-aggregate:aggregate-id": "admin","auto-negotiate": true,"port-speed": "openconfig-if-ethernet:SPEED_10MB"}
      ```
   ```
   $ gnmic -a 127.0.0.1:9090 --tls-ca CA_CERTIFICATE --tls-cert CLIENT_CERTIFICATE --tls-key CLIENT_KEY \
     set --update-path interfaces/interface[name=br0]/ethernet/config --update-file updateFile.json \
     --encoding json_ietf -u Admin -p Admin
   {
     "time": "1969-12-31T16:00:00-08:00",
     "results": [
       {
         "operation": "UPDATE",
         "path": "interfaces/interface[name=br0]/ethernet/config"
       }
     ]
   }
   ```
   
   ## gNOI - gRPC Network Operations Interface
   Simulator implements the following [gNOI](https://github.com/openconfig/gnoi) gRPCs:
   - file.proto:
     - get - downloads dummy file
     - stat - returns stats of file on path
   - system.proto:
     - time - returns current time
   
   [Other](src/main/java/io/lighty/modules/gnmi/simulatordevice/gnoi) gNOI grRPCs are also supported, but they have no logic behind them. They just returns some predefined response.
