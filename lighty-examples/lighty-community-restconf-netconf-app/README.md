# Lighty NETCONF/RESTCONF Application
This application provides RESTCONF north-bound interface and utilizes NETCONF south-bound plugin to manage NETCONF devices on the network. 
Application works as standalone SDN controller. It is capable to connect to NETCONF devices and expose connected devices over RESTCONF north-bound APIs.

This application starts:
* Lighty Controller
* OpenDaylight RESTCONF plugin
* OpenDaylight Swagger servlet
* NETCONF south-bound plugin

![architecture](docs/restconf-netconf-controller-architecture.svg)

This roughly translates to OpenDaylight feature set installed by karaf command:
```
feature:install odl-netconf-all
```

## Build and Run
build the project: ```mvn clean install```

### Start this demo example
* build the project using ```mvn clean install```
* go to target directory ```cd lighty-examples/lighty-community-restconf-netconf-app/target``` 
* unzip example application bundle ```unzip  lighty-community-restconf-netconf-app-12.3.1-SNAPSHOT-bin.zip```
* go to unzipped application directory ```cd lighty-community-restconf-netconf-app-12.3.1-SNAPSHOT```
* start controller example controller application ```java -jar lighty-community-restconf-netconf-app-12.3.1-SNAPSHOT.jar``` 

### Test example application
Once example application has been started using command ```java -jar lighty-community-restconf-netconf-app-12.3.1-SNAPSHOT.jar``` 
RESTCONF web interface is available at URL ```http://localhost:8888/restconf/*```

##### URLs to start with
* __GET__ ```http://localhost:8888/restconf/operations```
* __GET__ ```http://localhost:8888/restconf/data/network-topology:network-topology?content=config```
* __GET__ ```http://localhost:8888/restconf/data/network-topology:network-topology?content=nonconfig```

##### Swagger UI
This application example has active [Swagger](https://swagger.io/) UI for RESTCONF.

URLs for Swagger RESTCONF [draft18](https://tools.ietf.org/html/draft-ietf-netconf-restconf-18) implementation (enabled by default):
* __Swagger APIs__ ``http://localhost:8888/apidoc/18/apis`` 
* __Swagger UI__ ``http://localhost:8888/apidoc/18/explorer/index.html`` 

URLs for Swagger RESTCONF [draft02](https://tools.ietf.org/html/draft-bierman-netconf-restconf-02) implementation:
* __Swagger APIs__ ``http://localhost:8888/apidoc/apis`` 
* __Swagger UI__ ``http://localhost:8888/apidoc/explorer/index.html`` 

### Use custom config files
There are two separated config files: for NETCONF SBP single node and for cluster.
`java -jar lighty-community-restconf-netconf-app-12.3.1-SNAPSHOT.jar /path/to/singleNodeConfig.json`

Example configuration for single node is [here](src/main/assembly/resources/sampleConfigSingleNode.json)

## Setup Logging
Default loging configuration may be overwritten by JVM option
```-Dlog4j.configuration=/path/to/log4j.properties```

Content of ```log4j.properties``` is described [here](https://logging.apache.org/log4j/2.x/manual/configuration.html).
