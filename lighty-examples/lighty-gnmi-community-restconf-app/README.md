# lighty.io gNMI/RESTCONF use-case example
This application provides gNMI south-bound utilized with RESTCONF interface to manage gNMI devices on the network.
Application works as standalone SDN controller. It is capable to connect to gNMI devices and expose connected devices
over RESTCONF north-bound APIs. In this application gNMI simulator starts as gNMI target and all operations performs
on this device.

Application lighty.io gNMI/RESTCONF is pre-prepared with [Openconfig YANG models](yangs).
These models are used by both gNMI application and gNMI device simulator. Device has already prepared state/config data
configured by [this](simulator/initialJsonData.json) json file.
For communication with gNMI device there is required to use TLS communication with certificates and authorized
by username and password.

This application starts:
* [Lighty.io Controller](https://github.com/PANTHEONtech/lighty/tree/master/lighty-core/lighty-controller)
* [lighty.io RESTCONF](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-restconf-nb-community)
* [lighty.io gNMI](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-gnmi/lighty-gnmi-sb)
* [lighty.io gNMI device simulator](https://github.com/PANTHEONtech/lighty/tree/14.0.x/lighty-modules/lighty-gnmi/lighty-gnmi-device-simulator)

![lighty.io gNMI / RESTCONF architecture](docs/lighty-gnmi-restconf-architecture.png)

## Prerequisites
In order to build and start and use the lighty.io gNMI/RESTCONF application locally, you need:
* Java 11 or later
* Maven 3.5.4 or later
* Postman v7.36.5. or later
* Linux-based system with bash

## How to run use-case
This example shows how to start [RCgNMI](../../lighty-applications/lighty-rcgnmi-app-aggregator/lighty-rcgnmi-app)
app and [gNMI simulator](../../lighty-modules/lighty-gnmi/lighty-gnmi-device-simulator) app. Next step shows how to
perform basic CRUD operations on the gNMI device using RCgNMI app. All RESTCONF requests used in this
example are provided in [postman-collection](lighty.io gNMI-RESTCONF application.postman_collection.json).

Clone lighty repository and build it with maven. Then move to lighty-gnmi-community-restconf-app folder.
```
git clone git@github.com:PANTHEONtech/lighty.git
cd lighty
mvn clean install
cd lighty-examples/lighty-gnmi-community-restconf-app
```

### Start RCgNMI controller app
Unzip lighty-rcgnmi-app to current location
```
unzip ../../lighty-applications/lighty-rcgnmi-app-aggregator/lighty-rcgnmi-app/target/lighty-rcgnmi-app-14.2.2-SNAPSHOT-bin.zip
```
Start application with pre-prepared configuration [example_config.json](example_config.json).
```
java -jar lighty-rcgnmi-app-14.2.2-SNAPSHOT/lighty-rcgnmi-app-14.2.2-SNAPSHOT.jar -c example_config.json
```

### Start lighty.io gNMI device simulator
Unzip gNMI simulator app to current folder.
```
unzip ../../lighty-modules/lighty-gnmi/lighty-gnmi-device-simulator/target/lighty-gnmi-device-simulator-14.2.2-SNAPSHOT-bin.zip
```

Start the application with pre-prepared configuration [simulator_config.json](simulator/simulator_config.json)
```
java -jar lighty-gnmi-device-simulator-14.2.2-SNAPSHOT/lighty-gnmi-device-simulator-14.2.2-SNAPSHOT.jar  -c simulator/simulator_config.json 
```

### Add client certificates to lighty.io gNMI keystore
Certificates used in this example can be found [here](certificates). Only the client certificates are added
to keystore with RPC. This RPC stores certificates in configuration data-store and encrypt their private key and passphrase.
Adding required certificates for gNMI device to lighty.io gNMI application is performs by postman request `'Add Keystore'`.
```
curl --request POST 'http://127.0.0.1:8888/restconf/operations/gnmi-certificate-storage:add-keystore-certificate' \
--header 'Content-Type: application/json' \
--data-raw "{
    \"input\": {
        \"keystore-id\": \"keystore-id-1\",
        \"ca-certificate\": \"$(cat certificates/ca.crt)\",
        \"client-key\": \"$(cat certificates/client.key)\",
        \"client-cert\": \"$(cat certificates/client.crt)\"
    }
}"
```


### Connect simulator to controller
Simulated gNMI device can be connected with `'Connect device'` request located in postman collection.
```
curl --request PUT 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator' \
--header 'Content-Type: application/json' \
--data-raw '{
    "node": [
        {
            "node-id": "gnmi-simulator",
            "connection-parameters": {
                "host": "127.0.0.1",
                "port": 10161,
                "keystore-id": "keystore-id-1",
                "credentials": {
                    "username": "admin",
                    "password": "admin"
                }
            },
            "extensions-parameters": {
                "gnmi-parameters": {
                    "use-model-name-prefix": true
                }
            }
        }
    ]
}'
```
In the body of this request is contained identifier
for keystore, device information, extension parameters and basic authorization required by device. When device
is successfully connected to application, then these logs should be visible:
```
 INFO [gnmi_executor-1] (GnmiMountPointRegistrator.java:52) - Mount point for node gnmi-simulator created: {closed=false, instance=org.opendaylight.mdsal.dom.spi.SimpleDOMMountPoint@60112bc5}
 INFO [gnmi_executor-0] (GnmiNodeListener.java:105) - Connection with node Uri{_value=gnmi-simulator} established successfully
```

Device state can be also checked by request `'Get gnmi-simulator node'`. Information about created device can be found
inside this request. In section `gnmi-topology:node-state`, current state of device or information about occured errors
can be found. If device is connected, then `node-status` in this response should have value `READY`.

### Read configuration from device
In provided postman-collection are few examples of getting data from gNMI device `'Get interfaces'`,`'Get system'` and
`'Get Authentication'`. In next section for modifying data, we focus on system data obtained from
request `'Get Authentication'`. This request can be modified to obtain required data.
```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication'
```

### Write configuration to device
To write authentication data use PUT request example `'Put Authentication config/state'` in postman collection.
```
curl --request PUT 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication' \
--header 'Content-Type: application/json' \
--data-raw '{
    "openconfig-system:authentication": {
        "config": {
            "authentication-method": [
                "openconfig-aaa-types:TACACS_ALL"
            ]
        },
        "state": {
            "authentication-method": [
                "openconfig-aaa-types:RADIUS_ALL"
            ]
        }
    }
}'
```
This request replace data in `config` container, remove `admin-user` container and add new container `state`. All set request on
gNMI simulator are applying into CONFIGURATION datastore. For view changed data, is required to sent request with
`?content=nonconfig` query at the end of URL or execute request from postman collection `'Get Authentication from CONFIG'`.
```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication?content=config'
```
To view original data on STATE datastore send request `'Get Authentication from STATE'`.
```
curl --location --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication?content=nonconfig'
```

### Update configuration on device
For updating data send request `'Update config data'`. This request append new type to config authentication-method.
```
curl --request PATCH 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication/config' \
--header 'Content-Type: application/json' \
--data-raw '{
    "openconfig-system:config": {
        "authentication-method": [
            "openconfig-aaa-types:RADIUS_ALL"
        ]
    }
}'
```
To validate request send GET request `'Get Authentication from CONFIG'`.
```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication?content=config'
```

### Delete configuration from device
For deleting `config` container send request `'Delete authentication config'`.
```
curl --location --request DELETE 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication/config'
```
To validate request send GET request `'Get Authentication from CONFIG'`.
```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator/yang-ext:mount/openconfig-system:system/aaa/authentication?content=config'
```

### Disconnect the device from controller
When is required restart of connection or removal of device, just send request `'Remove device'`.
```
curl --request DELETE 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=gnmi-simulator
```
For restarting connection it is required to send request `'Connect device'`
