# lighty.io SpringBoot Keycloak integration example

This is simple demo application which uses Lighty with NETCONF Southbound plugin inside Spring Boot.

Application initializes [OpenDaylight](https://www.opendaylight.org/) core components (MD-SAL, yangtools and controller) and NetConf Southbound plugin
inside spring framework environment.

There is initialized fully functional md-sal inside the spring. The DataBroker is provided for SpringBoot dependency
injection subsystem services and used in exposed REST endpoints. The REST endpoints provides very simple functionality
for network-topology model inside global datastore.

Alongside the basic data broker, there is also integrated NETCONF Southbound plugin and some very basic NetConf
functionality exposed through REST endpoints. The "lighty-toaster-device" was used as a NetConf device which uses
toaster model from [ODL repository](https://github.com/YangModels/yang/blob/19fea483099dbf2864b3c3186a789d12d919f4db/experimental/odp/toaster.yang). 

![architecture](docs/architecture.svg)

## Security
This demo utilizes IBM [Keycloak](https://www.keycloak.org/) identity and access management for authentication and authorization.
Client first requests an access token, which is then used to authorize access to resources.

### Users available
This demo requires username / password access, following users are available
* userName="bob", password="secret", roles={ ROLE_USER, ROLE_ADMIN } 
* userName="alice", password="secret", roles={ ROLE_USER } 

## Build
```
mvn clean install
```


## Start
Start the keycloak docker image with preconfigured realm and users
```
sudo docker-compose -f keycloak/docker-compose.yml up
```

It is necessary to copy toaster@2009-11-20.yang file to $WORKING_DIR/cache/schema/toaster@2009-11-20.yang, to be 
possible to read NETCONF data from testing device (lighty-toaster-device).
```
mvn spring-boot:run
```

or

```
java -jar target/lighty-controller-springboot-9.2.1.jar
```

or in any IDE, run main in 

```
io.lighty.core.controller.springboot.MainApp
```


## Using REST APIs
Login with username password, see __Users available__ section.
This request must be first one in order to set the access token
for consequent requests. Token is valid for 5 minutes (configurable
in keycloak admin).
```
export TOKEN=`curl -ss \
                   --data 'client_id=lighty-app&username=alice&password=secret&grant_type=password' \
                   'http://localhost:8080/auth/realms/LightyRealm/protocol/openid-connect/token' \
              | sed -e 's/\(^.*access_token":"\)\(.*\)\(","expires_in.*$\)/\2/'`
```
##### GET /services/data/topology/list
list all topology IDs stored in datastore
```
curl -i -H "Authorization: Bearer ${TOKEN}"  \
-X GET "http://localhost:8888/services/data/topology/list"
```
##### PUT /services/data/topology/id/{topologyId}
create new topology with topology id "test-topology-id"
```
curl -i -H "Authorization: Bearer ${TOKEN}" \
-X PUT "http://localhost:8888/services/data/topology/id/test-topology-id"
```
##### DELETE /services/data/topology/id/{topologyId}
delete existing topology with topology id "test-topology-id"
```
curl -i -H "Authorization: Bearer ${TOKEN}" \
-X DELETE "http://localhost:8888/services/data/topology/id/test-topology-id"
```
##### GET /services/data/netconf/list
list all NETCONF devices with its connection status and "darknessFactor" data loaded from device
(darknessFactor is contained in toaster model from ODL)
```
curl -i -H "Authorization: Bearer ${TOKEN}" \
-X GET "http://localhost:8888/services/data/netconf/list"
```
##### PUT /services/data/netconf/id/{netconfDeviceId}
attempt to connect to device "test-device" with specific credentials and address:port
```
curl -i -X PUT \
-H "Authorization: Bearer ${TOKEN}" \
-H "Content-Type:application/json" \
--data \
'{
    "username": "admin",
    "password": "admin",
    "address": "127.0.0.1",
    "port": "17830"
}' \
"http://localhost:8888/services/data/netconf/id/test-device"
```
##### DELETE /services/data/netconf/id/{netconfDeviceId}
disconnect NETCONF device "test-device"
```
curl -i -H "Authorization: Bearer ${TOKEN}"  \
-X DELETE "http://localhost:8888/services/data/netconf/id/test-device"
```

### Notable Classes

#### MainApp
- main Spring boot initializer class

#### SecurityRestController
- REST API for http session login / logout

#### LightyConfiguration
- Lighty.io services initialization and beans definition for SpringBoot dependency injection system

#### TopologyRestService
- REST endpoints definition
- uses beans defined in class LightyConfiguration for modifying topologies in ODL md-sal

#### NetconfDeviceRestService
- REST endpoints definition for ODL NETCONF
- uses beans defined in class LightyConfiguration for connecting, disconnecting and listing NetConf devices

