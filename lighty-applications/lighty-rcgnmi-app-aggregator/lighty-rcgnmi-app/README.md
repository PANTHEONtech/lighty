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
    1. Open custom configuration example [here](src/main/resources/example-config/example_config.json).

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
* unzip example application bundle ```unzip  lighty-rcgnmi-app-15.0.0-SNAPSHOT-bin.zip```
* go to unzipped application directory ```cd lighty-rcgnmi-app-15.0.0-SNAPSHOT```
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

## Build and start with the docker
To build and start the RCgNMI lighty.io application using docker in the local environment follow these steps:

1. Build the application using this maven command:
   `mvn clean install -P docker`

2. Start the application using following docker command.
   `docker run -it --name lighty-rcgnmi --network host --rm lighty-rcgnmi`

3. To start the application with custom lighty configuration( -c ) and custom initial log4j config file( -l ) use command:
  ```
   docker run -it --name lighty-rcgnmi --network host
   -v /absolute_path/to/config-file/configuration.json:/lighty-rcgnmi/configuration.json
   --rm lighty-rcgnmi -c configuration.json
  ```

   If your configuration.json file specifies path to initial configuration data to load on start up
   (for more information, check
   [lighty-controller](https://github.com/PANTHEONtech/lighty/tree/master/lighty-core/lighty-controller))
   you need to mount the json/xml file as well:
   `-v /absolute/path/to/file/initData.xml:/lighty-rcgnmi/initData.xml`
   , then your path to this file in configuration.json becomes just "./initData.xml":
   ```
    "initialConfigData": {
          "pathToInitDataFile": "./initData.xml",
          "format": "xml"
    }
   ```
   Example configuration files are located on following path:
   `lighty-rcgnmi-app/src/resources/example-config/*`
   For additional configurable parameters and their explanation see previous chapters.

4. If the application was started successfully, then a log similar should be present in the console:
   ` INFO [main] (RCgNMIApp.java:98) - RCgNMI lighty.io application started in 10.10 s`

5. Test the RCgNMI lighty.io application. Default RESTCONF port is `8888`

## Deployment via helm chart
### Prerequisites
* kubernetes cluster 1.15.11 (minikube / microk8s /..)
* helm 2.17
### Deploy
To easily deploy Lighty RcGNMI application to kubernetes we provide custom helm chart located in /lighty-rcgnmi-app-helm/helm/.
To install, make sure that the docker image defined in `values.yaml` is accessible, then run command:
`microk8s helm install --name lighty-rcgnmi-app ./lighty-rcgnmi-app-helm/`
in `/lighty-rnc-app-helm/helm/` directory.
### Providing startup configuration
By default, the deployed application is started with custom configuration.json
(for more information check [lighty-controller](https://github.com/PANTHEONtech/lighty/tree/master/lighty-core/lighty-controller)).
We supply this configuration file by passing kubernetes configmap (`configmaps.yaml`), which you can modify to your needs.
To use the functionality of loading configuration data on startup, add new entry to configmaps.yaml:
`initData: |
     your initial yang modeled json/xml data
`
Then add:
` "initialConfigData": {
       "pathToInitDataFile": "{{ .Values.lighty.configDirectoryName }}/initData",
       "format": "xml"/"json" depending on format
      }`
entry to controller json node in lighty-config.json in `configmaps.yaml`.
If everything was set up corectly, then your data will be loaded to controller on startup and appropriate listeners should be triggered.
For example, if your initial json data contains node in gnmi topology:
 ```
{
  "network-topology:network-topology": {
    "topology": [
      {
        "topology-id": "gnmi-topology",
        "node": [
          {
            "node-id": "device1",
            "connection-parameters": {
              "host": "127.0.0.1",
              "port": 9090,
              "connection-type" : "INSECURE"
            }
          }
        ]
      }
    ]
  }
}
```
and the gNMI device is running, the connection should be established upon startup.
For testing purposes, you can use lighty-gnmi-device-simulator as a gNMI device.

## JMX debugging
Java Management Extensions is a tool enabled by default which makes it easy to change runtime
configuration of the application.
1. Start the application (see previous sections)
2. Connect the JXM client
  We recommend using `jconsole` because it is part of the standard java JRE.
  The command for connecting jconsole to JMX server is:
    `jconsole <ip-of-running-lighty>:<JMX-port>`, the default JMX-port is 1099.

This approach works only if the application is running locally.

If you want to connect the JMX client to application running remotely or in a container (k8s deployment or/and docker),
you need to start the application using following JAVA_OPTS:
```
JAVA_OPTS = -Dcom.sun.management.jmxremote
             -Dcom.sun.management.jmxremote.authenticate=false
             -Dcom.sun.management.jmxremote.ssl=false
             -Dcom.sun.management.jmxremote.local.only=false
             -Dcom.sun.management.jmxremote.port=<JMX_PORT>
             -Dcom.sun.management.jmxremote.rmi.port=<JMX_PORT>
             -Djava.rmi.server.hostname=127.0.0.1
```
Then run `java $JAVA_OPTS -jar lighty-rcgnmi-app-<version> ...`
## Connecting JMX client to application running in docker
1. As we said, if we want to be able to connect the JMX, we need to start the app with JAVA_OPTS described in
 previous chapter.
 In docker the most convenient way to do this is to create env.file and run the docker run with `--env-file env.file` argument
 The env.file must contain the definition of the described JAVA_OPTS environment variable.
 We also need to publish the container JMX_PORT to host, this is done via `-p <JMX_PORT>:<JMX_PORT>` argument.
 So the docker run command becomes:
  `docker run -it --name lighty-rcgnmi --env-file env.file -p <JMX_PORT>:<JMX_PORT> ...`
 The rest of the command stays the same as explained in previous chapters.
 2. Connect the JMX client via command `jconsole <ip-of-container>:<JMX_PORT>`.
 ## Connecting JMX client to application deployed in kubernetes
Once you have deployed the application via our provided helm chart in which you enabled jmxRemoting,
you just need to forward the JMX port of the pod in which the instance of the application you want to debug is running.
In kubernetes this is done via `kubectl port-forward` command.
1. Forward the pod's JMX port, run `kubectl port-forward <name-of-the-pod> <JMX_PORT>`
2. Connect JMX client, run `jconsole <pod-ip>:<JMX-port>`
