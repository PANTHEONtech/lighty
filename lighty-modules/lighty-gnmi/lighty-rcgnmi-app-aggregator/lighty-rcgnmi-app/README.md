#RESTCONF gNMI application
Lighty application which starts and wires the following components:

- [lighty.io controller](https://github.com/PANTHEONtech/lighty/tree/master/lighty-core/lighty-controller)
  provides core ODL services (like MDSAL, yangtools, global schema context,...) that are required
  for other services.
- [lighty.io RESTCONF northbound](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-restconf-nb-community)
  provides the RESTCONF interface that is used to communicate with the application using the RESTCONF protocol over the HTTP.
- [lighty.io gNMI south-bound](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-gnmi-sb)
  acts as a gNMI client, manages connections to gNMI devices and gNMI communication. Currently, only gNMI Capabilities,
  gNMI Get and gNMI Set are supported.

## Prerequisites
In order to build and start the rcgnmi application locally, you need:
* Java 11 or later
* maven 3.5.4 or later

##Custom configuration
Before the lighty gNMI creates mount point for the communicating with the gNMI device, it is necessary to create schema context.
This schema context is created based on the yang files which device implements. These models are obtained via gNMI Capability response, but
only model name and model version are actually returned, thus we need some way of providing the content of the yang model.
The way of providing content of the yang file, so lighty gNMI can correctly create schema context, is to add a parameter to RCGNMI app
.json configuration or use `upload-yang-model` RPC, both of these options will load the yang files into datastore, from which
the ligthy gNMI reads the model based on it's name and version obtained in gNMI Capability response.

1) Provide yang model configuration as a parameter to example app
    1. Open custom configuration example [here](src/main/resources/example_config.json).

    2. Add custom gNMI configuration (into root, next to controller or restconf configuration):
    ```
      "gnmi": {
        "initialYangsPaths" : [
          "INITIAL_FOLDER_PATH"
        ]
      }
    ```
    3. Change `INITIAL_FOLDER_PATH`, from JSON block above, to folder path, which contains YANG models you wish to
    load into datastore. These models will be then automatically loaded on startup.

2) Add yang model with RPC request to running app
- 'YANG_MODEL' - Should have included escape characters before each double quotation marks character.
```
curl --request POST 'http://127.0.0.1:8888/restconf/operations/gnmi-yang-storage:upload-yang-model' \
--header 'Content-Type: application/json' \
--data-raw '{
    "input": {
        "name": "openconfig-interfaces",
        "semver": "2.4.3",
        "body": "YANG_MODEL"
    }
}'
```

##How to start rcgnmi example app
* build the project using ```mvn clean install```
* go to target directory ```cd lighty-rcgnmi-app/target```
* unzip example application bundle ```unzip  lighty-rcgnmi-app-14.0.1-SNAPSHOT-bin.zip```
* go to unzipped application directory ```cd lighty-rcgnmi-app-14.0.1-SNAPSHOT```
* To start the application with custom lighty configuration, use arg -c and for custom initial log4j configuration use argument -l:
  `start-controller.sh -c /path/to/config-file -l /path/to/log4j-config-file`

##How to use rcgnmi example app

###Certificates
####Register certificates
Certificates used for connecting to device can be stored inside lighty-gnmi datastore. Certificates key and passphrase is
encrypted before storing inside data-store. After registering certificates key and passphrase, it is not possible
to get decrypted data back from data-store.
```
curl --request POST 'http://127.0.0.1:8888/restconf/operations/gnmi-certificate-storage:add-keystore-certificate' \
--header 'Content-Type: application/json' \
--data-raw '{
    "input": {
        "keystore-id": "keystore-id-1",
        "ca-certificate": "-----BEGIN CERTIFICATE-----
                              CA-CERTIFICATE
                          -----END CERTIFICATE-----",
        "client-key": "-----BEGIN RSA PRIVATE KEY-----
                                CLIENT-KEY
                      -----END RSA PRIVATE KEY-----",
        "passphrase": "key-passphrase",
        "client-cert": "-----BEGIN CERTIFICATE-----
                              CLIENT_CERT
                        -----END CERTIFICATE-----"
    }
}'
```

####Remove certificates
```
curl --location --request POST 'http://127.0.0.1:8888/restconf/operations/gnmi-certificate-storage:remove-keystore-certificate' \
--header 'Content-Type: application/json' \
--data-raw '{
    "input": {
        "keystore-id": "keystore-id-1"
    }
}'
```
####Update certificates
To update already existing certificate, use the request for registering new certificate with the keystore-id you wish to update.

###Connecting gNMI device
   To establish connection and communication with gNMI device via RESTCONF, one needs to add new node to gnmi-topology.
   This is done by sending appropriate request (examples below) with unique node-id.
   The connection-parameters are used to specify connection parameters and client's (lighty gNMI) way of authenticating. 
   Property `connection-type` is enum and can be set to two values:
   - INSECURE : Skip TLS validation with certificates.
   - PLAINTEXT :  Disable TLS validation.

  If the device requires the client to authenticate with registered certificates, remove `connection-type` property. Then add `keystore-id` property
  with id of registered certificates.

  If the device requires username/password validation, then fill `username` and `password` in `credentials` container.
  This container is optional.

  If the device requires additional parameters in gNMI request/response, there is a container `extensions-parameters`
  where is defined a set of parameters that can be optionally included in the gNMI request and response.
  Those parameters are:
  - `overwrite-data-type` is used to overwrite the type field of gNMI GetRequest.
  - `use-model-name-prefix` is used when device requires a module prefix in first element name of gNMI request path
  - `path-target` is used to specify the context of a particular stream of data and is only set in prefix for a path.

```
curl --request PUT 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1' \
--header 'Content-Type: application/json' \
--data-raw '{
    "node": [
        {
            "node-id": "node-id-1",
            "connection-parameters": {
                "host": "127.0.0.1",
                "port": 9090,
                "connection-type": "INSECURE",
                "credentials": {
                    "username": "admin",
                    "password": "admin"
                }
            }
        }
    ]
}'
```
Example of creating mountpoint with custom certificates:
```
curl --request PUT 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1' \
--header 'Content-Type: application/json' \
--data-raw '{
    "node": [
        {
            "node-id": "node-id-1",
            "connection-parameters": {
                "host": "127.0.0.1",
                "port": 9090,
                "keystore-id": "keystore-id-1",
                "credentials": {
                    "username": "admin",
                    "password": "admin"
                }
            }
        }
    ]
}'
```

### Get state of registered gNMI device
```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1'
```

### Example RESTCONF gNMI GetRequest
```
curl --location --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1/yang-ext:mount/openconfig-interfaces:interfaces'
```

### Example RESTCONF gNMI SetRequest
```
curl --request PUT 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1/yang-ext:mount/interfaces/interface=br0/ethernet/config' \
--header 'Content-Type: application/json' \
--data-raw '{
    "openconfig-if-ethernet:config": {
        "enable-flow-control": false,
        "openconfig-if-aggregate:aggregate-id": "admin",
        "auto-negotiate": true,
        "port-speed": "openconfig-if-ethernet:SPEED_10MB"
    }
}'
```

### Disconnect gNMI device
```
curl --request DELETE 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1'
```


##### All request performed on rcgnmi app can be found in provided [postman-collection](lighty-rcgnmi-app.postman_collection.json)

## RESTCONF - gNMI operations mapping

Following table lists supported HTTP methods.

YANG node type         | HTTP methods
------------------------|-----------------
Configuration data      | POST, PUT, PATCH, DELETE, GET
Non configuration data  | GET
YANG RPC                | POST

For each REST request the lighty gNMI invokes appropriate gNMI operation GnmiSet/GnmiGet to process the request.
Below is the mapping of HTTP operations to gNMI operations:

HTTP Method | gNMI operation     | Request data  | Response data
-------------|------------------|---------------|---------------
GET         | GnmiGet     | path          | status, payload
POST        | GnmiSet  | path, payload | status
PATCH       | GnmiSet  | path, payload | status
PUT         | GnmiSet | path, payload | status
DELETE      | GnmiSet  | path          | status

### RESTCONF GET method mapping
- Reading data from the operational datastore invokes readOperationalData() in [GnmiGet](src/main/java/io/lighty/gnmi/southbound/mountpoint/ops/GnmiGet.java).
- Reading data from the configuration datastore invokes readConfigurationData() in [GnmiGet](src/main/java/io/lighty/gnmi/southbound/mountpoint/ops/GnmiGet.java).

### RESTCONF PUT/POST/PATCH/DELETE method mapping
- Sending data to operational/configuration datastore invokes method set() in [GnmiSet](src/main/java/io/lighty/gnmi/southbound/mountpoint/ops/GnmiSet.java).
- List of input parameters come from method request in the form of fields of update messages: update, replace and delete fields.

**PUT/POST** request method sends update messages through two fields: **update** and **replace fields**.
**PATCH** request method sends update messages through the **update field**.
**DELETE** request method sends update messages through the **delete field**.
