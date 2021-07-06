# gNMI device simulator
This simulator provides gNMI device driven by gNMI proto files, with datastore defined by set of YANG models.

## How to build and run gNMI device simulator

1. Add maven dependency to your pom.xml file.
```
   <dependency>
      <groupId>io.lighty.modules.gnmi</groupId>
      <artifactId>lighty-gnmi-device-simulator</artifactId>
      <version>15.0.0-SNAPSHOT</version>
   </dependency>
```

2. Initialize and start an instance of gNMI device simulator. Setting path to folder with yang models used by this
   simulator is required.

```
   final SimulatedGnmiDevice simulatedGnmiDevice
         = new SimulatedGnmiDeviceBuilder()
               .setYangsPath(SCHEMA_PATH)
               .build();
   simulatedGnmiDevice.start();
```

3. Close gNMI device simulator.

```
   simulatedGnmiDevice.stop();
```

## Additional configuration

Any additional configuration has to be done before starting simulator. This configuration is available in
SimulatedGnmiDeviceBuilder.

1. **setInitialConfigDataPath(String)** - (default: empty) Set configuration data-store for device. Path to file
   in JSON format is expected. Data inside this file should be modeled by YANG files provided in *setYangsPath(String)*

2. **setInitialStateDataPath(String)** - (default: empty) Set **?operational?** data-store for device. Path to
   file in JSON format is expected. Data inside this file should be modeled by YANG files provided in *setYangsPath(String)*

3. **setBossGroup(EventLoopGroup)** - (default: NioEventLoopGroup(1)) Provides the boss EventGroupLoop to the server.

4. **setWorkerGroup(EventLoopGroup)** - (default: NioEventLoopGroup(0)) Provides the worker EventGroupLoop to the server.

5. **setHost(String)** - (default: "0.0.0.0") Set host address.

6. **setPort(int)** - (default: 10161) Set port value (port >= 0 && port <= 65535).

7. **setMaxConnections(int)** - (default: 50) Determines the number of connections queued.

8. **setCertificatePath(String)** - (default: "certs/server.crt") Set certificate for gNMI device simulator.
   Path to certificate file is expected.

9. **setKeyPath(String)** - (default: "certs/server.key") Set certificate key for gNMI device simulator.
   Path to certificate key file is expected.

10. **setUsernamePasswordAuth(String, String)** - (default: empty) Setting username and password will allow
   authentication. All requests routed to gNMI simulator will have to provide username/password
   authentication in request metadata.

11. **usePlaintext()** - (default: turned off) This will Disable TLS validation. If this is enabled, then there is no need
   to specify TLS related options.

12. **setGsonInstance(Gson)** - (default: default instance of Gson)  Provides an option to customize Gson parser instance
   used in device.

##Example with gnmic client
This example will show how to execute basic operations on lighty.io gNMI device simulator. Request will be performed with
[gnmic](https://gnmic.kmrd.dev/) client.

1) Install gnmic client with online tutorial [here](https://gnmic.kmrd.dev/#installation)

2) Start gNMI device simulator with custom certificates and authentication. Data used for this example:
   - [YANG_MODELS_PATH](src/test/resources/test_schema)
   - [INITIAL_CONFIGURATION_PATH](src/test/resources/initData/config.json)
   - SERVER_CERTIFICATE / SERVER_PKCS8_KEY - was generated with script inside lighty-gnmi-connector module.
   ```
   SimulatedGnmiDevice device = new SimulatedGnmiDeviceBuilder()
            .setYangsPath(YANG_MODELS_PATH)
            .setInitialConfigDataPath(INITIAL_CONFIGURATION_PATH)
            .setHost("127.0.0.1")
            .setCertificatePath(SERVER_CERTIFICATE)
            .setKeyPath(SERVER_PKCS8_KEY)
            .setUsernamePasswordAuth("Admin", "Admin")
            .setPort(9090)
            .build();
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
