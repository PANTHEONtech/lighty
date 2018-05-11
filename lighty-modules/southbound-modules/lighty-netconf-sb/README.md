# NETCONF plugins
This module contains 3 plugins that provide following Opendaylight features functionality:

 - odl-netconf-topology - NetconfTopologyPluginImpl
 - odl-netconf-clustered-topology - NetconfClusteredTopologyPluginImpl
 - odl-netconf-callhome - NetconfCallhomePluginImpl

Detailed information about usage can be found in
[Netconf User Guide](http://docs.opendaylight.org/en/stable-carbon/user-guide/netconf-user-guide.html).

## NETCONF topology plugin
Plugin listens on changes in config data store on path network-topology/topology/topology-netconf/.
When new NETCONF node is created, it initiates a connection to the NETCONF device.
It registers mount point which provides following services:

 - DOMRpcService
 - DOMDataBroker
 - DOMNotificationService

## Netconf clustered topology plugin
Clustered topology plugin provides the same functionality as the topology plugin, but it is cluster aware.
When a new node is written to data store, only one cluster member is elected as a master and only master
member creates a ssh connection to device. Other cluster members use master for communication with the device.
Slaves route all their requests to the device via master member.

Only one topology plugin must be used at the same time. Running both topology plugins is not supported.

## Netconf call home plugin
In most cases the client initiates a connection to a NETCONF server, running on the device. NETCONF call home
plugin allows the server(device) side to initiate a connection to a client(Lighty). Lighty must be configured
to accept device connection. It is done in the odl-netconf-callhome-server.yang module. Device public key is
added there together with credentials, which the plugin uses to authenticate the device. Call home server runs
on a port 6666, by default.

This plugin can be used together with one of the topology plugins.
