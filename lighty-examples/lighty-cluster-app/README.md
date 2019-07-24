# Lighty Clustered NETCONF/RESTCONF Application

This demo application is based on [NETCONF/RESTCONF Application](../lighty-community-restconf-netconf-app/README.md), but 
demonstrates [Akka cluster](https://doc.akka.io/docs/akka/current/cluster-usage.html) capabilities of [lighty.io](https://github.com/PantheonTechnologies/lighty-core) and [OpenDaylight](https://www.opendaylight.org/). 

This application starts:
* Lighty Controller
* OpenDaylight RESTCONF plugin
* OpenDaylight Swagger servlet
* NETCONF south-bound plugin
* OpenDaylight Akka 3-node cluster

## Application architecture
![Application Architecture](docs/app-architecture.svg)

## Build and Run
build the project: ```mvn clean install```

### Start this demo example
* build the project using ```mvn clean install```
* go to target directory ```cd lighty-examples/lighty-cluster-app/target``` 
* unzip example application bundle ```unzip  lighty-cluster-app-10.1.1-SNAPSHOT-bin.zip```
* make 3 separate instances of this application 
```
cp -a lighty-cluster-app-10.1.1-SNAPSHOT lighty-cluster-app-10.1.1-SNAPSHOT-01
cp -a lighty-cluster-app-10.1.1-SNAPSHOT lighty-cluster-app-10.1.1-SNAPSHOT-02
cp -a lighty-cluster-app-10.1.1-SNAPSHOT lighty-cluster-app-10.1.1-SNAPSHOT-03
```
* start all 3 cluster nodes in separate terminals 
```
cd lighty-cluster-app-10.1.1-SNAPSHOT-01
./start-controller-node-01.sh

cd lighty-cluster-app-10.1.1-SNAPSHOT-02
./start-controller-node-02.sh

cd lighty-cluster-app-10.1.1-SNAPSHOT-03
./start-controller-node-03.sh
```
* all 3 cluster nodes are running on localhost ``127.0.0.1``

### Cluster ports on 127.0.0.1
| node instance      | port | service type                     |
|--------------------|------|----------------------------------|
| controller-node-01 | 8186 | restconf websocket notifications |
|                    | 8889 | restconf http                    |
|                    | 2550 | akka netty tcp                   |
|                    | 8558 | akka http management port        |
| controller-node-02 | 8187 | restconf websocket notifications |
|                    | 8890 | restconf http                    |
|                    | 2551 | akka netty tcp                   |
|                    | 8559 | akka http management port        |
| controller-node-02 | 8189 | restconf websocket notifications |
|                    | 8891 | restconf http                    |
|                    | 2552 | akka netty tcp                   |
|                    | 8560 | akka http management port        |

### Akka clustering
This demo utilizes [Akka clustering](https://doc.akka.io/docs/akka/current/cluster-usage.html)
and [Akka cluster http management](https://doc.akka.io/docs/akka-management/current/cluster-http-management.html) extensions.
To get info about cluster state, use following REST call.

__GET__ ``http://127.0.0.1:{akka_http_management_port}/management/cluster/members/``
