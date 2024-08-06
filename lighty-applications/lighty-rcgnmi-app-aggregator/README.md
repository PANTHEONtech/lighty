# RESTCONF gNMI (RCgNMI) Application
A lighty.io application, which starts and wires the following components:

- [lighty.io Controller](../../lighty-core/lighty-controller)
  provides core OpenDaylight services (like MD-SAL, YANG Tools, global schema context, etc.), which are required for other services.
- [lighty.io RESTCONF Northbound](../../lighty-modules/lighty-restconf-nb-community)
  provides the RESTCONF interface, that is used to communicate with the application using the RESTCONF protocol over HTTP.
- [lighty.io gNMI Southbound](../../lighty-modules/lighty-gnmi/lighty-gnmi-sb) acts as a gNMI client, manages connections to gNMI devices and gNMI communication. Currently, only _gNMI Capabilities, gNMI Get_ & _gNMI Set_ are supported.

![RCgNMI lighty.io architecture](docs/lighty-rcgnmi-architecture.png)

## Prerequisites
In order to build & start the RCgNMI application locally, you need:
* Java 21 (or later)
* Maven 3.8.5 (or later)

## Build & Start
To build and start the RCgNMI application in your local environment, follow these steps:

1. Build the application using Maven  
    `mvn clean install -pl light-rcgnmi-app -am`

2. Unpack the application .zip distribution, created in the _lighty-rcgnmi-app/target_ location, called  
   `lighty-rcgnmi-app-<version>-bin.zip`

3. Start the application by running it's _.jar_ file:    
   `java -jar lighty-rcgnmi-app-<version>.jar`

4. To start the application with a custom lighty.io configuration, use the argument _`-c`:
   `java -jar lighty-rcgnmi-app-<version>.jar -c /path/to/config-file`

   To extend lighty modules time-out use `"modules": {"moduleTimeoutSeconds": SECONDS },` property inside JSON configuration. This property increases the time after which the exception is thrown if the module is not successfully initialized by then (Default 60).
   Example configuration files are located on following folder [example-config](lighty-rcgnmi-app/src/main/resources/example-config).

5. If the application was started successfully, then a log similar should be present in the console:  
  ` INFO [main] (RCgNMIApp.java:98) - RCgNMI lighty.io application started in 10.10 s`

6. Test the RCgNMI lighty.io application. Default RESTCONF port is `8888`  
   The default credential for http requests is login:`admin`, password: `admin`. 

## How to Use the RCgNMI Example App
In this section we explore all necessities of connecting gNMI device and provide some examples on how to use the RESTCONF interface for communicating with the device.
Quick start example with pre-prepared gNMI/RESTCONF application and gNMI device simulator can be found [here](../../lighty-examples/lighty-gnmi-community-restconf-app/README.md).

### YANG Models for Schema Context of the gNMI Device
Before **lighty.io gNMI Southbound** creates a mount point for communication with the gNMI device, it is necessary to create a **schema context**. A schema context is created based on the YANG files, which device implements. 

These models are obtained via the *gNMI Capability* response, but only the model name and version are actually returned. This means, that we need to somehow provide the content of the YANG model. 

The way of providing content of the YANG models, so **lighty.io gNMI** can correctly use it for creating the schema context, is to add a field to the .json configuration of the RCgNMI app, or to use the `upload-yang-model` RPC, once the application is already running. 

Both of these options will load the YANG files into data-store, from which **ligthy.io gNMI** reads the content of the model, based on it's name and version - obtained from the *gNMI Capability* response.

1. Provide the YANG model configuration as a parameter to the **RCgNMI app**
    1. Open the custom configuration example [here](lighty-rcgnmi-app/src/main/resources/example-config/example_config.json).

    2. Add the custom gNMI configuration (into root, next to the controller or RESTCONF configuration):
    ```
      "gnmi": {
        "initialYangsPaths" : [
          "INITIAL_FOLDER_PATH"
        ],
        "initialYangModels": [
            { "nameSpace":"MODEL_NAMESPACE","name":"MODEL_NAME","revision":"MODEL_REVISION"}
        ]
      }
    ```
    3. Use one or both [`initialYangsPaths`,`initialYangModels`] option for adding YANG models to gNMI module. 
       1. `initialYangsPaths`: Change `INITIAL_FOLDER_PATH`, from the JSON block above, to a folder paths, which contain YANG models you wish to load into the datastore. 
       2. `initialYangModels`: Add all required models which can be found in classpath of application in format mention above. Change `MODEL_NAMESPACE`, `MODEL_NAME`, `MODEL_REVISION` to required values.
    These models will be then automatically loaded on startup.

2. Add the YANG model with the RPC request to the running app
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

In case the device does not report all of its capabilities ,for example, when it sends a list of capabilities without YANG models that ```augment``` other models reported in the capabilities, use the ```force-capability``` parameter, which replaces the list of capabilities received in the gNMI Capabilities response with the custom capabilities defined in this parameter. 

If the ```force-capability``` is specified, each ```GetRequest``` request contains a ```use_models``` field. The ```use_models``` field specifies the list of capabilities that the target should use, when creating a response to the *Get RPC* call. When specified, the target **must** only consider data elements within the defined set of the schema models.

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

### gNMI Southbound Authentication
In this section, we explore the different options of the client's (gNMI Southbound) authentication, with examples on how to use them.

#### TLS Authentication
According to [gNMI-authentication](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-authentication.md) client (lighty.io gNMI Southbound) and server (gNMI device) must create a TLS-secure gRPC channel, before doing any form of gNMI communication.  

lighty.io gNMI Southbound stores each of it's TLS client certificates in an MD-SAL datastore, under an unique id `keystore-id`.  

##### Add Certificate
To add a client certificate, execute RPC `gnmi-certificate-storage:add-keystore-certificate`, for example:
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

##### Remove Certificate
To remove a certificate, execute RPC `gnmi-certificate-storage:remove-keystore-certificate`, for example, to delete a certificate stored under id `keystore-id-1`:

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
To update existing certificates, use the request for registering new certificates with the `keystore-id` you wish to update.

#### Username & Password
If the server (gNMI device) requires also a username and password, [call-credentials](https://grpc.github.io/grpc-java/javadoc/io/grpc/CallCredentials.html), that must be present in metadata of every gRPC request, use field `credentials`. 

An example request are located in [connection-requests](#connecting-gnmi-device).

#### Insecure Connection
If you wish to use an insecure connection (which we **do not recommend**), configure the field `connection-type`. There are two possible values:
1. INSECURE - Skips TLS validation of the certificates. (Equivalent to --skip-verify flag for `gnmic` gNMI client)
2. PLAINTEXT - Indicates that the client wishes to use non secure (non TLS) connection with the target.  
An example request is located in [connection-requests](#connecting-gnmi-device).

### Additional Parameters
If the target requires the gNMI requests, sent by lighty.io gNMI Southbound, to have some specific configuration, we provide the ability to overwrite the default behaviour of constructing these request by exposing some parameters.

Those parameters are:
- `overwrite-data-type` is used to overwrite the type field of gNMI GetRequest (if *not used*, then the type will be filled based on the value passed to read transaction).
- `use-model-name-prefix`is used when device requires a module prefix in first element name of gNMI request path (e.g the interfaces becomes openconfig-interfaces:interfaces, based on the identifier passed to transaction)
- `path-target` is used to specify the context of a particular stream of data and is only set in prefix for a path

### Connecting a gNMI Device
To establish connection and communication with gNMI device via RESTCONF, one needs to add new node to gnmi-topology. This is done by sending the appropriate request (examples below) with a unique node-id.

#### Insecure/Plaintext Connection
To connect to a device with an [INSECURE/PLAINTEXT](#insecure-connection) connection, execute the following RESTCONF request:

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

- As already described, [credentials](#username-and-password) are optional.

#### TLS-Certificate Connection
To connect to a device with TLS, one needs to provide `keystore-id` under which the certificates are stored in datastore.

- See [add-certificate](#tls-authentication) on how to add a client's certificate.

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

- As already described [credentials](#username-and-password) are optional.

### Disconnect gNMI device
To disconnect the device and to delete the mount point, simply DELETE the node from `gnmi-topology`
```
curl --request DELETE 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1'
```

### State of Registered gNMI device
Upon sending the [connection-request](#connecting-gnmi-device), lighty gNMI southbound writes the status of the connection to the node in datastore. To see the status, execute the following RESTCONF command: 

```
curl --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1'
```

If the device's mount point was created successfully, you should see `"node-status":"READY"` and all the capabilities, from which the schema context was created, in the response.  

If something went wrong while creating the mount point, you should see `failure-details` with the reason for the failure.

Upon fixing the issue, [disconnect](#disconnect-gnmi-device) the node and [connect](#connecting-gnmi-device) again.

### Example RESTCONF gNMI Requests
Once the device's mount point is successfully created, one can issue RESTCONF requests, which are translated to gNMI GetRequest and SetRequest, according to the [mapping](#restconf-gnmi-operations-mapping).

For your convenience, we provide a [postman-collection](lighty-rcgnmi-app/lighty-rcgnmi-app.postman_collection.json), which contains some example requests for the described operations.

#### Example: RESTCONF gNMI GetRequest

```
curl --location --request GET 'http://127.0.0.1:8888/restconf/data/network-topology:network-topology/topology=gnmi-topology/node=node-id-1/yang-ext:mount/openconfig-interfaces:interfaces'
```

#### Example: RESTCONF gNMI SetRequest
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

Here are the supported **HTTP methods**:

YANG node type         | HTTP methods
------------------------|-----------------
Configuration data      | POST, PUT, PATCH, DELETE, GET
Non configuration data  | GET
YANG RPC                | POST

For each REST request, lighty.io gNMI Southbound invokes the appropriate gNMI operation GnmiSet/GnmiGet, to process the request.

Below is the mapping of **HTTP to gNMI operations**:

HTTP Method | gNMI operation     | Request data  | Response data
-------------|------------------|---------------|---------------
GET         | GnmiGet     | path          | status, payload
POST        | GnmiSet  | path, payload | status
PATCH       | GnmiSet  | path, payload | status
PUT         | GnmiSet | path, payload | status
DELETE      | GnmiSet  | path          | status

### RESTCONF GET Method Mapping
RESTCONF GET is mapped to [gNMI-GetRequest](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#331-the-getrequest-message) which by default will have the type field set to CONFIG/STATE based on the content requested in RESTCONF request, there are two possible kinds of RESTCONF GET requests which results in different gNMI GetRequests:

1. **RESTCONF GET request without query parameter content** - results in two gNMI GetRequests, one with type set to CONFIG and second with type STATE. Responses of both requests are then merged and returned as payload of RESTCONF response.

2. **RESTCONF GET request with query parameter `content`** - results in one gNMI GetRequst, with type set to CONFIG/STATE based on the RESTCONF `content` query parameter, which might be set to one of config/non-config. To use this option, append `?content=config/non-config` to your RESTCONF GET request.

To override this behaviour to **always** use some predefined value for the `type` field of the resulting gNMI GetRequest, use the `overwrite-data-type` parameter, specified in [additional-parameters](#additional-parameters).

### RESTCONF PUT/POST/PATCH/DELETE Method Mapping
RESTCONF state modifying requests all results in gNMI SetRequest which, based on the operation invokes, contains Update/Replace/Delete fields.

- **PUT/POST** results in gNMI SetRequest with **update** and **replace** fields.
- **PATCH** results in gNMI SetRequest with **update** field.
- **DELETE** results in gNMI SetRequest with **delete** field.

## Build & Start w/ Docker
To build and start the RCgNMI lighty.io application using Docker in a local environment, follow these steps:

1. Build the application using this maven command:
   `mvn clean install -P docker`

2. Start the application using the following Docker command.
   `docker run -it --name lighty-rcgnmi --network host --rm lighty-rcgnmi`

3. To start the application with a custom lighty.io configuration, use _-c_, and custom initial log4j config file, use _-l_, use the command:

  ```
   docker run -it --name lighty-rcgnmi --network host
   -v /absolute_path/to/config-file/configuration.json:/lighty-rcgnmi/configuration.json
   -v /absolute_path/to/log4j-file/log4j2.xml:/lighty-rcgnmi/log4j2.xml
   --rm lighty-rcgnmi -c configuration.json -l log4j2.xml
  ```

If your _configuration.json_ file specifies a path to the initial configuration data to load on start up (for more information, check the [lighty-controller](../../lighty-core/lighty-controller)), you need to mount the JSON/XML file as well:

`-v /absolute/path/to/file/initData.xml:/lighty-rcgnmi/initData.xml`
Then, your path to this file in configuration.json becomes just `./initData.xml`:

   ```
    "initialConfigData": {
          "pathToInitDataFile": "./initData.xml",
          "format": "xml"
    }
   ```

The example configuration files are located [here](lighty-rcgnmi-app/src/main/resources/example-config/).

For additional configurable parameters and their explanation, see previous chapters.

4. If the application was started successfully, then a similar log should appear in the console:

` INFO [main] (RCgNMIApp.java:98) - RCgNMI lighty.io application started in 10.10 s`

5. Test the RCgNMI lighty.io application. The default RESTCONF port is `8888`

## Deployment via helm chart
### Prerequisites
* Kubernetes cluster 1.22.4 (minikube, microk8s, etc.)
* Helm 3.7.1

### Deploy
To easily deploy the lighty.io RcGNMI application to Kubernetes, we provide a custom helm chart located [here](lighty-rcgnmi-app-helm/helm).

To install, make sure that the docker image defined in `values.yaml` is accessible in your kubernetes (for microk8s you can use the [docker-microk8s-script](lighty-rcgnmi-app-helm/helm/microk8s-uploadDocker.sh)), then run the command:

`microk8s helm3 install lighty-rcgnmi-app ./lighty-rcgnmi-app-helm/`

in the `/lighty-rcgnmi-app-helm/helm/` directory.  

Once the deployment is started and all pods and services are ready, [save-logs-script](lighty-rcgnmi-app-helm/helm/microk8s-saveLightyLogs.sh) can be used to save logs from pods.

To ***uninstall** the deployment, run the command:  

`microk8s helm3 uninstall lighty-rcgnmi-app`

### Providing Startup Configuration

By default, the deployed application is started with a custom _configuration.json_ (for more information check [lighty-controller](../../lighty-core/lighty-controller)). We supply this configuration file by passing the Kubernetes configmap (`configmaps.yaml`), which can be modified to your needs.

To use the functionality of loading configuration data on startup, add a new entry to _configmaps.yaml_:

`initData: |
     your initial yang modeled json/xml data
`
Then add:

` "initialConfigData": {
       "pathToInitDataFile": "{{ .Values.lighty.configDirectoryName }}/initData",
       "format": "xml"/"json" depending on format
      }`

entry to controller json node in _lighty-config.json_ in `configmaps.yaml`.

If everything was set up corectly, your data will be loaded to controller on startup and appropriate listeners should be triggered.

For example, if your initial json data contains node in gNMI topology:
 
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
and the gNMI device is running, the connection should be established upon startup. For testing purposes, you can use [lighty-gnmi-device-simulator](https://github.com/PANTHEONtech/lighty-netconf-simulator) as a gNMI device.

## Setup Logging
Default logging configuration may be overwritten by JVM option
```-Dlog4j.configurationFile=path/to/log4j2.xml```

Content of ```log4j2.xml``` is described [here](https://logging.apache.org/log4j/2.x/manual/configuration.html).

## Update logger with JMX
Java Management Extensions is a tool enabled by default, which makes it easy to change the runtime
configuration of the application. Among other options, we use [log4j2](https://logging.apache.org/log4j/2.0/manual/jmx.html)
which has build in option to change logging behaviour during runtime via JMX client which can be connected to running lighty instance.
1. Start the application (see previous sections)
2. Connect the JXM client
   We recommend using `jconsole` because it is part of the standard Java JRE.
   The command for connecting jconsole to JMX server is:  
   `jconsole <ip-of-running-lighty>:<JMX-port>`, the default JMX-port is 1099.

This approach works **only if the application is running locally**.

If you want to connect the JMX client to the application running remotely or containerized (k8s deployment or/and docker),
you need to start the application using the following JAVA_OPTS:
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

If you want to completely disable logger JMX option run application with following JAVA_OPTS
`java -Dlog4j2.disable.jmx=true -jar lighty-rcgnmi-app-<version> ...`

### Connecting JMX client to application running in docker
1. If we want to be able to connect the JMX, we need to start the app with JAVA_OPTS, as described in the
   previous chapter.
   In Docker, the most convenient way to do this is to create env.file and run the docker run with `--env-file env.file` argument
   The env.file must contain the definition of the described JAVA_OPTS environment variable.
   We also need to publish the container JMX_PORT to host, this is done via `-p <JMX_PORT>:<JMX_PORT>` argument.
   So the docker run command becomes:
   `docker run -it --name lighty-rcgnmi --env-file env.file -p <JMX_PORT>:<JMX_PORT> ...`
   The rest of the command stays the same as explained in previous chapters.
2. Connect the JMX client via the command `jconsole <ip-of-container>:<JMX_PORT>`.

### Connecting a JMX client to the application, deployed in kubernetes
Once you have deployed the application via our provided helm chart, in which you enabled jmxRemoting,
you just need to forward the JMX port of the pod, in which the instance of the application you want to debug, is running.
In Kubernetes, this is done via `kubectl port-forward` command.
1. Forward the pod's JMX port, run `kubectl port-forward <name-of-the-pod> <JMX_PORT>`
2. Connect JMX client, run `jconsole <pod-ip>:<JMX-port>`

### Update Logger level in runtime with JMX
After successful connection, JMX client to lighty app is able to update logger information in runtime.
[Log4j2 JMX](https://logging.apache.org/log4j/2.0/manual/jmx.html) provides more configuration but, for this example we show how to change logger level.
1) Open `MBeans` window and chose `org.apache.logging.log4j2`
3) Chose from dropdown  `loggers` than `StatusLogger` and `level`
4) By double-clicking on level value, can be updated to desire [state](https://logging.apache.org/log4j/2.x/manual/customloglevels.html).

For a custom solution or commercial support, [contact us here.](https://pantheon.tech/contact-us)
