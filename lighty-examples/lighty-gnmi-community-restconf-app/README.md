# lighty.io gNMI/RESTCONF application
This application provides gNMI south-bound utilized with RESTCONF interface to manage gNMI devices on the network.
Application works as standalone SDN controller. It is capable to connect to gNMI devices and expose connected devices
over RESTCONF north-bound APIs. In this application will be started gNMI simulator as gNMI target and all operation will
be performed on this device.

Application lighty.io gNMI/RESTCONF is pre-prepared with [Openconfig YANG models](src/main/assembly/resources/yangs).
These models are used by both gNMI application and gNMI device simulator. Device have already prepared state/config data
configured by [this](src/main/assembly/resources/simulator/initialJsonData.json) json file.
For communication with gNMI device is required to use TLS communication with certificates and authorized
by username and password.

This application starts:
* [Lighty.io Controller](https://github.com/PANTHEONtech/lighty/tree/master/lighty-core/lighty-controller)
* [lighty.io RESTCONF](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-restconf-nb-community)
* [lighty.io gNMI](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-gnmi/lighty-gnmi-sb)
* [lighty.io gNMI device simulator](https://github.com/PANTHEONtech/lighty/tree/14.0.x/lighty-modules/lighty-gnmi/lighty-gnmi-device-simulator)

## Prerequisites
In order to build and start and use the lighty.io gNMI/RESTCONF application locally, you need:
* Java 11 or later
* Maven 3.5.4 or later
* Postman v7.36.5. or later

## Build and start
To build and start the lighty.io gNMI/RESTCONF application in your local environment, follow these steps:
1. Build the application using maven
   `mvn clean install`

2. Unpack the application ZIP distribution created in the _lighty-gnmi-community-restconf-app/target_ called
   `lighty-gnmi-community-restconf-app-<version>-bin.zip`

3. Start the application by:
   - Running it's _.jar_ file `java -jar lighty-gnmi-community-restconf-app-<version>-SNAPSHOT.jar`
   - Or with provided script `./start-controller.sh`

## Example of using lighty.io gNMI/RESTCONF application
This example show how to connect gNMI device and perform basic CRUD operation on the device. All RESTCONF request
used in this example are provided in [postman-collection](lighty.io gNMI-RESTCONF application.postman_collection.json).

 - ### Add client certificates to lighty.io gNMI keystore
Used certificates can be found [here](src/main/assembly/resources/certificates). To keystore is added only client
certificates. Adding required certificates for gNMI device to lighty.io gNMI application is performed by
postman request `'Add Keystore'`.

 - ### Connect gNMI device
Device connection is performed by request `'Connet device'`. In the body of this request is contained identifier
for keystore, device information, extension parameters and basic authorization required by device. When device
is successfully connected to application, then these logs should be visible:
```
 INFO [gnmi_executor-1] (GnmiMountPointRegistrator.java:52) - Mount point for node gnmi-simulator created: {closed=false, instance=org.opendaylight.mdsal.dom.spi.SimpleDOMMountPoint@60112bc5}
 INFO [gnmi_executor-0] (GnmiNodeListener.java:105) - Connection with node Uri{_value=gnmi-simulator} established successfully
```

Device state can be also checked by request `'Get gnmi-simulator node'`. Inside this request can be found information about
created device. In section `gnmi-topology:node-state` can be found current state of device or information about error
if some occurs. If device is connected, then `node-status` in this response should have value `READY`.

 - ### Get data from gNMI device
In provided postman-collection few examples of getting data from gNMI device `'Get interfaces'`,`'Get system'` and
`'Get Authentication'`. In next section for modifying data, we will more focus on system data obtained from
request `'Get Authentication'`. This request can be modified to obtain required data.

 - ### ADD data to gNMI device with PUT request
In collection can be found PUT request example `'Put Authentication config/state'`. This request will replace
data in `config` container, remove `admin-user` container and add new container `state`. All set request on
gNMI simulator will be applied to CONFIGURATION datastore. For view changed data, is required to sent request with
`?content=nonconfig` query at the end of URL or execute request from postman collection `'Get Authentication from CONFIG'`.
To view original data on STATE datastore send request `'Get Authentication from STATE'`.

 - ### Update data on gNMI device with PATCH request
For updating data send request `'Update config data'`. This request will append new type to config authentication-method.
To validate request send GET request `'Get Authentication from CONFIG'`.

 - ### Delete data on gNMI device
For deleting `config` container send request `'Delete authentication config'`. To validate request send GET
request `'Get Authentication from CONFIG'`.

 - ### Remove gNMI device from lighty.io gNMI/RESTCONF application
When is required to restart connection or just remove device send request `'Remove device'`.
This will remove connected device. For restarting connection it will be required to send request `'Connet device'`