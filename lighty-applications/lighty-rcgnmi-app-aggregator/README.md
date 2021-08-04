# RESTCONF gNMI (RCgNMI) application
Lighty application which starts and wires the following components:

- [lighty.io controller](https://github.com/PANTHEONtech/lighty/tree/master/lighty-core/lighty-controller)
  provides core ODL services (like MDSAL, yangtools, global schema context,...) that are required
  for other services.
- [lighty.io RESTCONF northbound](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-restconf-nb-community)
  provides the RESTCONF interface that is used to communicate with the application using the RESTCONF protocol over the HTTP.
- [lighty.io gNMI south-bound](https://github.com/PANTHEONtech/lighty/tree/master/lighty-modules/lighty-gnmi/lighty-gnmi-sb)
  acts as a gNMI client, manages connections to gNMI devices and gNMI communication. Currently, only gNMI Capabilities,
  gNMI Get and gNMI Set are supported.

![RCgNMI lighty.io architecture](docs/lighty-rcgnmi-architecture.png)

## Prerequisites
In order to build and start the rcgnmi application locally, you need:
* Java 11 or later
* maven 3.5.4 or later

## Build and start
To build and start the RCgNMI application in your local environment, follow these steps:
1. Build the application using maven  
    `mvn clean install -pl light-rcgnmi-app -am`

2. Unpack the application ZIP distribution created in the _lighty-rcgnmi-app/target_ called  
   `lighty-rcgnmi-app-<version>-bin.zip`

3. Start the application by running it's _.jar_ file:    
   `java -jar lighty-rcgnmi-app-<version>.jar`

4. To start the application with custom lighty configuration, use arg -c and for custom initial log4j configuration use argument -l:  
   `java -jar lighty-rcgnmi-app-<version>.jar -c /path/to/config-file -l /path/to/log4j-config-file`  

   Example configuration files are located on following path:  
   `lighty-rcgnmi-app-docker/example-config/*`

5. If the application was started successfully, then a log similar should be present in the console:  
  ` INFO [main] (RCgNMIApp.java:98) - RCgNMI lighty.io application started in 10.10 s`

6. Test the RCgNMI lighty.io application. Default RESTCONF port is `8888`  
   The default credential for http requests is login:`admin`, password: `admin`. 

## How to use RCgNMI example app
In this section we explore all necessities of connecting gNMI device and provide some examples on how to use the RESTCONF interface
for communicating with the device. Quick start example with pre-prepared gNMI/RESTCONF application and gNMI device simulator
can be found [here](../../lighty-examples/lighty-gnmi-community-restconf-app/README.md).

### Yang models for schema context of the gNMI device
Before the lighty gNMI southbound creates mount point for the communicating with the gNMI device, it is necessary to create schema context.
Schema context is created based on the yang files which device implements. These models are obtained via gNMI Capability response, but
only model name and model version are actually returned. This means that we need to somehow provide the content of the yang model.
The way of providing content of the yang file, so lighty gNMI can correctly use it for creating schema context, is to add
a field to .json configuration of the RCgNMI app or to use `upload-yang-model` RPC once the application is already running.
Both of these options will load the yang files into datastore, from which the ligthy gNMI reads the content of the model based
on it's name and version obtained in gNMI Capability response.

1) Provide yang model configuration as a parameter to RCgNMI app
    1. Open custom configuration example [here](lighty-rcgnmi-app/src/main/resources/example-config/example_config.json).

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

In the case when device does not report all of its capabilities, for example: the device sends a list of
capabilities without yang models that ```augment``` other models reported in the capabilities, use the
```force-capability``` parameter, which replaces the list of capabilities received in the gNMI Capabilities response
with the custom capabilities defined in this parameter.
If the ```force-capability``` is specified, each ```GetRequest``` request contains ```use_models``` field.
The ```use_models``` field specifying the list of capabilities that the target should use when creating a
response to the Get RPC call. When specified, the target MUST only consider data elements within the defined set of
schema models.

```
curl -X PUT \
  http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1 \
  -H 'Content-Type: application/json' \
  -d '{
    "node": [
        {
            "node-id": "node-id-1",
            "connection-parameters": {
                "host": "172.0.0.1",
                "port": 9090,
                "connection-type": "INSECURE"
            },
            "extensions-parameters": {
                "force-capability": [
                    {
                        "name": "openconfig-if-ethernet",
                        "version": "2.6.2"
                    },
                    {
                        "name": "openconfig-if-ip",
                        "version": "2.3.1"
                    }
                ]
            }
        }
    ]
}'
```

### gNMI southbound Authentication
In this section we explore different options of client's (gNMI southbound) authentication with examples on how to use them.

#### TLS authentication
According to [gNMI-authentication](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-authentication.md)
client(lighty gNMI southbound) and server (gNMI device) must create TLS-secure gRPC channel before doing any form of gNMI communication.  
Lighty gNMI southbound stores each of it's TLS client certificates in MD-SAL datastore under unique id `keystore-id`.  
##### Add certificate
To add a client certificate execute RPC `gnmi-certificate-storage:add-keystore-certificate`, for example:
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
        "client-cert": "-----BEGIN CERTIFICATE-----
                              CLIENT_CERT
                        -----END CERTIFICATE-----"
    }
}'
```
If your client key is encrypted by passphrase add field `"passphrase" : "example-passphrase"` to above.  
By using this RPC, the client-key and passphrase will be encrypted using OpenDaylight's AAA encryption service.
##### Remove certificate
To remove a certificate, execute RPC `gnmi-certificate-storage:remove-keystore-certificate`, for example to delete certificate
stored under id `keystore-id-1`:
```
curl --location --request POST 'http://127.0.0.1:8888/restconf/operations/gnmi-certificate-storage:remove-keystore-certificate' \
--header 'Content-Type: application/json' \
--data-raw '{
    "input": {
        "keystore-id": "keystore-id-1"
    }
}
```
##### Update certificates
To update already existing certificate, use the request for registering new certificate with the `keystore-id` you wish to update.

#### Username and password
If the server (gNMI device) requires also username and password [call-credentials](https://grpc.github.io/grpc-java/javadoc/io/grpc/CallCredentials.html)
that must be present in metadata of every gRPC request, use field `credentials`.   
Example request are located in [connection-requests](#connecting-gnmi-device).

#### Insecure connection
If you wish to use insecure connection (which we do not recommend), configure field `connection-type`, there are two possible values:
1. INSECURE - Skips TLS validation of the certificates. (Equivalent to --skip-verify flag for `gnmic` gNMI client)
2. PLAINTEXT - Indicates that the client wishes to use non secure (non TLS) connection with the target.  
Example request are located in [connection-requests](#connecting-gnmi-device).

### Additional parameters
If the target requires the gNMI requests sent by lighty gNMI southbound to have some specific configuration, we provide
the ability to overwrite default behaviour of constructing these request by exposing some parameters.
Those parameters are:
- `overwrite-data-type` is used to overwrite the type field of gNMI GetRequest. (if not used, then the type will be
   filled based on the value passed to read transaction).
- `use-model-name-prefix` is used when device requires a module prefix in first element name of gNMI request path.
   (e.g interfaces becomes openconfig-interfaces:interfaces, based on the identifier passed to transaction)
- `path-target` is used to specify the context of a particular stream of data and is only set in prefix for a path.

### Connecting gNMI device
To establish connection and communication with gNMI device via RESTCONF, one needs to add new node to gnmi-topology.
This is done by sending appropriate request (examples below) with unique node-id.
#### Insecure/plaintext connection
To connect to a device with [INSECURE/PLAINTEXT](#insecure-connection) connection, execute following RESTCONF request:
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
As already described [credentials](#username-and-password) is optional.

#### TLS-Certificate connection
To connect to a device with TLS, one needs to provide `keystore-id` under which the certificates are stored in datastore.
See [add-certificate](#tls-authentication) on how to add a client's certificate.

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
As already described [credentials](#username-and-password) is optional.

### Disconnect gNMI device
To disconnect the device and to delete the mount point, simply DELETE the node from `gnmi-topology`
```
curl --request DELETE 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1'
```

### State of registered gNMI device
Upon sending the [connection-request](#connecting-gnmi-device), lighty gNMI southbound writes the status of the connection
to the node in datastore. To see the status, execute RESTCONF command: 
```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1'
```
If the device's mount point was created successfully, one should see `"node-status":"READY"` and all the capabilities from
which the schema context was created, in the response.  

If something went wrong while creating the mount point, one should `failure-details` with the reason of the failure.
Upon fixing the issue, [disconnect](#disconnect-gnmi-device) the node and [connect](#connecting-gnmi-device) again.
### Example RESTCONF gNMI Requests
Once the device's mount point is successfully created, one can issue RESTCONF requests which are translated to gNMI GetRequest and SetRequest
according to [mapping](#restconf-gnmi-operations-mapping).
For convenience, we provide [postman-collection](lighty-rcgnmi-app/lighty-rcgnmi-app.postman_collection.json) which contains some
example requests for described operations.
#### Example RESTCONF gNMI GetRequest
```
curl --location --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1/yang-ext:mount/openconfig-interfaces:interfaces'
```

#### Example RESTCONF gNMI SetRequest
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

## RESTCONF -> gNMI operations mapping

Following table lists supported HTTP methods.

YANG node type         | HTTP methods
------------------------|-----------------
Configuration data      | POST, PUT, PATCH, DELETE, GET
Non configuration data  | GET
YANG RPC                | POST

For each REST request the lighty gNMI southbound invokes appropriate gNMI operation GnmiSet/GnmiGet to process the request.
Below is the mapping of HTTP operations to gNMI operations:

HTTP Method | gNMI operation     | Request data  | Response data
-------------|------------------|---------------|---------------
GET         | GnmiGet     | path          | status, payload
POST        | GnmiSet  | path, payload | status
PATCH       | GnmiSet  | path, payload | status
PUT         | GnmiSet | path, payload | status
DELETE      | GnmiSet  | path          | status

### RESTCONF GET method mapping
As described, the RESTCONF GET is mapped to [gNMI-GetRequest](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#331-the-getrequest-message)
which by default will have the type field set to CONFIG/STATE based on the content requested in RESTCONF request, there are two possible kinds of RESTCONF GET requests which
results in different gNMI GetRequests:
1. **RESTCONF GET request without query parameter content** - results in two gNMI GetRequests, one with type set to CONFIG and second with type STATE.
   Responses of both requests are then merged and returned as payload of RESTCONF response.

2. **RESTCONF GET request with query parameter `content`** - results in one gNMI GetRequst, with type set to CONFIG/STATE based on the RESTCONF
   `content` query parameter, which might be set to one of config/non-config. To use this option, append `?content=config/non-config` to
    your RESTCONF GET request.

To override this behaviour to always use some predefined value for the `type` field of the resulting gNMI GetRequest, use `overwrite-data-type` parameter
specified in [additional-parameters](#additional-parameters).

### RESTCONF PUT/POST/PATCH/DELETE method mapping
RESTCONF state modifying requests all results in gNMI SetRequest which, based on the operation invokes, contains Update/Replace/Delete fields.

- **PUT/POST** results in gNMI SetRequest with **update** and **replace** fields.
- **PATCH** results in gNMI SetRequest with **update** field.
- **DELETE** results in gNMI SetRequest with **delete** field.

## Build and start with docker
To build and start the RCgNMI lighty.io application using docker in the local environment follow these steps:

1. Build the application using this maven command:
   `mvn clean install -P docker`

2. Start the application using following docker command.
   `docker run -it --name lighty-rcgnmi --network host --rm lighty-rcgnmi`

3. To start the application with custom lighty configuration( -c ) and custom initial log4j config file( -l ) use command:
  ```
   docker run -it --name lighty-rcgnmi --network host
   -v /absolute_path/to/config-file/configuration.json:/lighty-rcgnmi/configuration.json
   -v /absolute_path/to/log4j-file/log4j.properties:/lighty-rcgnmi/log4j.properties
   --rm lighty-rcgnmi -c configuration.json -l log4j.properties
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

   Example configuration files are located [here](lighty-rcgnmi-app/src/main/resources/example-config/)
   For additional configurable parameters and their explanation see previous chapters.

4. If the application was started successfully, then a log similar should be present in the console:
   ` INFO [main] (RCgNMIApp.java:98) - RCgNMI lighty.io application started in 10.10 s`

5. Test the RCgNMI lighty.io application. Default RESTCONF port is `8888`

## Deployment via helm chart
### Prerequisites
* kubernetes cluster 1.15.11 (minikube / microk8s /..)
* helm 2.17
### Deploy
To easily deploy Lighty RcGNMI application to kubernetes we provide custom helm chart located [here](lighty-rcgnmi-app-helm/helm).

To install, make sure that the docker image defined in `values.yaml` is accessible in your kubernetes (for microk8s you can use
 [docker-microk8s-script](lighty-rcgnmi-app-helm/helm/microk8s-uploadDocker.sh)), then run command:
`microk8s helm install --name lighty-rcgnmi-app ./lighty-rcgnmi-app-helm/`
in `/lighty-rcgnmi-app-helm/helm/` directory.  
Once the deployment is started and all pods and services are ready, [save-logs-script](lighty-rcgnmi-app-helm/helm/microk8s-saveLightyLogs.sh)
can be used to save logs from pods.
To uninstall the deployment, run command:  
`microk8s del --purge lighty-rcgnmi-app`
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
