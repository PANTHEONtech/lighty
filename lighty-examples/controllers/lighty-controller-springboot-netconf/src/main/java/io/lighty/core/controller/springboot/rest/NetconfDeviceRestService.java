/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */

package io.lighty.core.controller.springboot.rest;

import io.lighty.core.controller.springboot.rest.dto.NetconfDeviceRequest;
import io.lighty.core.controller.springboot.rest.dto.NetconfDeviceResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPasswordBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/netconf")
public class NetconfDeviceRestService {

    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_IID = InstanceIdentifier
        .create(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId("topology-netconf")));
    private static final long TIMEOUT = 1;
    private static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.create(Toaster.class);

    @Autowired
    @Qualifier("BindingDataBroker")
    private DataBroker dataBroker;

    @Autowired
    private MountPointService mountPointService;

    @GetMapping(path = "/list")
    public ResponseEntity getNetconfDevicesIds() throws InterruptedException, ExecutionException, TimeoutException {
        try (final ReadTransaction tx = dataBroker.newReadOnlyTransaction()) {
            final Optional<Topology> netconfTopoOptional =
                tx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPOLOGY_IID).get(TIMEOUT, TimeUnit.SECONDS);
            final List<NetconfDeviceResponse> response = new ArrayList<>();

            if (netconfTopoOptional.isPresent() && netconfTopoOptional.get().getNode() != null) {
                for (Node node : netconfTopoOptional.get().getNode()) {
                    final NetconfDeviceResponse nodeResponse = NetconfDeviceResponse.from(node);
                    response.add(nodeResponse);

                    final com.google.common.base.Optional<MountPoint> netconfMountPoint =
                        mountPointService.getMountPoint(NETCONF_TOPOLOGY_IID
                            .child(Node.class, new NodeKey(node.getNodeId())));

                    if (netconfMountPoint.isPresent()) {
                        final com.google.common.base.Optional<DataBroker> netconfDataBroker =
                            netconfMountPoint.get().getService(DataBroker.class);
                        if (netconfDataBroker.isPresent()) {
                            final ReadTransaction netconfReadTx =
                                netconfDataBroker.get().newReadOnlyTransaction();

                            final Optional<Toaster> toasterData = netconfReadTx
                                .read(LogicalDatastoreType.OPERATIONAL, TOASTER_IID).get(TIMEOUT, TimeUnit.SECONDS);

                            if (toasterData.isPresent() && toasterData.get().getDarknessFactor() != null) {
                                nodeResponse.setDarknessFactor(toasterData.get().getDarknessFactor());
                            }
                        }
                    }
                }

            }
            return ResponseEntity.ok(response);
        }
    }

    @PutMapping(path = "/id/{netconfDeviceId}")
    public ResponseEntity connectNetconfDevice(@PathVariable final String netconfDeviceId,
                                               @RequestBody final NetconfDeviceRequest deviceInfo)
        throws InterruptedException, ExecutionException, TimeoutException {

        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        final NodeId nodeId = new NodeId(netconfDeviceId);
        final InstanceIdentifier<Node> netconfDeviceIID = NETCONF_TOPOLOGY_IID
            .child(Node.class, new NodeKey(nodeId));

        final Node netconfDeviceData = new NodeBuilder()
            .setNodeId(nodeId)
            .addAugmentation(NetconfNode.class, new NetconfNodeBuilder()
                .setHost(new Host(new IpAddress(new Ipv4Address(deviceInfo.getAddress()))))
                .setPort(new PortNumber(deviceInfo.getPort()))
                .setCredentials(new LoginPasswordBuilder()
                        .setUsername(deviceInfo.getUsername())
                        .setPassword(deviceInfo.getPassword())
                        .build())
                .setTcpOnly(false)
                .build())
            .build();
        tx.put(LogicalDatastoreType.CONFIGURATION, netconfDeviceIID, netconfDeviceData);

        tx.commit().get(TIMEOUT, TimeUnit.SECONDS);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/id/{netconfDeviceId}")
    public ResponseEntity disconnectNetconfDevice(@PathVariable final String netconfDeviceId)
        throws InterruptedException, ExecutionException, TimeoutException {

        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        final NodeId nodeId = new NodeId(netconfDeviceId);
        final InstanceIdentifier<Node> netconfDeviceIID = NETCONF_TOPOLOGY_IID
            .child(Node.class, new NodeKey(nodeId));

        tx.delete(LogicalDatastoreType.CONFIGURATION, netconfDeviceIID);

        tx.commit().get(TIMEOUT, TimeUnit.SECONDS);

        return ResponseEntity.ok().build();
    }
}
