Lighty NETCONF/RESTCONF Application
==========================
This application provides RESTCONF north-bound interface and utilizes NETCONF south-bound plugin to manage NETCONF devices on the network. 

This application starts:
* Lighty Controller
* Opendaylight RESTCONF plugin
* NETCONF Southbound plugin

Build and Run
-------------
build the project: ```mvn clean install```

### Start this demo example
* build the project using ```mvn clean install```
* got to target directory ```cd lighty-examples/controllers/lighty-community-restconf-netconf-app/target``` 
* unzip example application bundle ```unzip  lighty-community-restconf-netconf-app-9.0.0-SNAPSHOT-bin.zip```
* go to unzipped application directory ```cd lighty-community-restconf-netconf-app-9.0.0-SNAPSHOT```
* start controller example controller application ```java -jar lighty-community-restconf-netconf-app-9.0.0-SNAPSHOT.jar``` 

### Test example application
Once example application has been started using command ```java -jar lighty-community-restconf-netconf-app-9.0.0-SNAPSHOT.jar``` 
RESTCONF web interface is available at URL ```http://localhost:8888/restconf/*```

##### URLs to start with
* __GET__ ```http://localhost:8888/restconf/operations```
* __GET__ ```http://localhost:8888/restconf/data/network-topology:network-topology?content=config```
* __GET__ ```http://localhost:8888/restconf/data/network-topology:network-topology?content=nonconfig```

### Use custom config files
There are two separated config files: for NETCONF SBP single node and for cluster.
`java -jar lighty-community-restconf-netconf-app-9.0.0-SNAPSHOT.jar /path/to/singleNodeConfig.json`

Example configuration for single node is [here](src/main/assembly/resources/sampleConfigSingleNode.json)

Setup Logging
-------------
Default loging configuration may be overwritten by JVM option
```-Dlog4j.configuration=/path/to/log4j.properties```

Content of ```log4j.properties``` is described [here](https://logging.apache.org/log4j/2.x/manual/configuration.html).
