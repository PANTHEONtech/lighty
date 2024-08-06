# Lighty BGP + RESTCONF Application

Application starts the following components:
* [Lighty Controller](../../lighty-core/lighty-controller/README.md)
* [Lighty RESTCONF northbound module](../../lighty-modules/lighty-restconf-nb-community/README.md)
* [Lighty BGP southbound module](../../lighty-modules/lighty-bgp/README.md)

## Prerequisites

In order to build and start the application locally, you need:
* Java 21 or later 
* Maven 3.8.5 or later

## Build

To build the application issue the following:
`mvn clean install`

## Start 

To start the application, follow these steps:
1. Build the application
2. Move to `target` directory
3. Unzip application distribution `unzip lighty-bgp-community-restconf-app-<VERSION>-bin.zip`
4. Move to unzipped application directory `lighty-bgp-community-restconf-app-<VERSION>`
5. Start the application `java -jar lighty-bgp-community-restconf-app-<VERSION>.jar`

## Postman collection

REST commands which are referenced in this guide are present in [postman collection](LIGHTY-BGP.postman_collection.json).

### Start with lighty.io JSON configuration

To start the application with custom JSON configuration, use argument `-c path` or `--config-path path`
where path is the file path of lighty JSON configuration.  
 See [example config](src/main/resources/lightyConfiguration.json).

## How to use
In this section, we explain the initial BGP configuration and present a simplistic guide on how to connect a BGP peer
and obtain a topology view of network.  
Note that this guide does not explain various BGP concepts and configuration and user can learn about them in official
[OpendayLight BGPCEP user guide](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/index.html). 

### Initial BGP configuration

The application starts the [BGP lighty module](../../lighty-modules/lighty-bgp/README.md), which means that Opendaylight's BGPCEP
default routing policies are loaded on startup.  
To verify this, issue the **Get routing policies** [postman](#Postman collection) command which should result in response containing two routing policies:
`default-odl-export-policy` and `default-odl-import-policy`.
These policies are imported as a part of initial configuration only for convenience and user is free to change them according
to his needs, see [RIB policy configuration](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/bgp-user-guide-rib-config-policies.html).
In this guide we will use these default policies as they make configuring BGP easier.

### Creating BGP protocol instance

The first thing to create is BGP protocol instance which configures the local BGP speaker.  
To do this, modify the **Create BGP protocol instance** [postman](#Postman collection) command and run it.  
Options and descriptions of individual fields can be found at [Protocol configuration](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/bgp-user-guide-protocol-configuration.html).  
After creating the BGP protocol instance, RIB with corresponding identifier is also created, to retrieve it, issue the
**Get created RIB** [postman](#Postman collection) command.

### Connecting BGP peer

Once the local BGP speaker has been configured, we can proceed to connecting BGP peers.  
For convenience and for simpler peer configuration, first create peer groups.  
Peer group is a piece of configuration which can be reused when connecting peers. [Postman](#Postman collection) command **Create peer group** is used to
create one peer group which describes an external BGP peer with AS 64496. More information about various options can be found at
[peer configuration docs](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/bgp-user-guide-bgp-peering.html).  
  
  
To connect a BGP peer, configure and issue the **Connect peer** [postman](#Postman collection) command, which references
previously created peer group.  
Once the peer is created and connected, message:
`BGP Session with peer [id: ...] established successfully.` should be logged, then BGP tables synchronization takes place i.e
for each enabled afi/safi, message `BGP Synchronization finished for table TablesKey{afi=..., safi=...}` is logged and RIB is filled,
to verify this issue **Get created RIB** [postman](#Postman collection) command.  
State of the session with BGP peer can be obtained via **Get peer state** [postman](#Postman collection) command.

### Topology providers

Providers are building topology view of BGP routes stored in local BGP speaker’s Loc-RIB.  
Output topologies are rendered in a form of standardised IETF network topology model. More information can be found at
[topology provider guide](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/bgp-user-guide-topology-provider.html).  
To create a new instance of topology provider, the **Create topology provider** [postman](#Postman collection) command
can be used. Note that the **topology-types** is used to determine what kind of topology should the topology provider create.
To obtain the created topology view, **Get topology view** [postman](#Postman collection) command can be used.
