# Lighty NETCONF Application using Quarkus & GraalVM
This application utilizes NETCONF south-bound plugin to manage NETCONF devices on the network. 
Application works as standalone SDN controller. It is capable to connect to NETCONF devices 
and expose connected devices over custom REST north-bound APIs.
This demo shows how to run [lighty.io](https://lighty.io/) in [quarkus.io](https://quarkus.io/) framework.

Application initializes OpenDaylight core components (MD-SAL, yangtools and controller) and NetConf Southbound plugin inside quarkus.io environment.

![architecture](docs/architecture.svg)

## Install environment
Once you have [downloaded GraalVM](https://github.com/oracle/graal/releases), 
extract the archive and set the environment variables:
```
export PATH={YOUR_PATH}/graalvm-ce-19.0.0/bin:$PATH
export GRAALVM_HOME={YOUR_PATH}/graalvm-ce-19.0.0/
export JAVA_HOME={YOUR_PATH}/graalvm-ce-19.0.0/
```

## Build & Run
build and run the project: ```mvn compile quarkus:dev```

### Using REST APIs
This [postman collection](docs/lighty.io-quarkus.io-demo.postman_collection.json) 
contains implemented REST API examples.
