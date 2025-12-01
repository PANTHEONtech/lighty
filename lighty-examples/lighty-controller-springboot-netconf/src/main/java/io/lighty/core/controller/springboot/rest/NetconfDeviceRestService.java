/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.rest;

import io.lighty.core.controller.springboot.rest.dto.NetconfDeviceRequest;
import io.lighty.core.controller.springboot.rest.dto.NetconfDeviceResponse;
import io.lighty.core.controller.springboot.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev251028.credentials.credentials.LoginPwUnencryptedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev251028.credentials.credentials.login.pw.unencrypted.LoginPasswordUnencryptedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.NetconfNodeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.netconf.node.augment.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/services/data/netconf")
public class NetconfDeviceRestService {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceRestService.class);

    private static final DataObjectIdentifier<Topology> NETCONF_TOPOLOGY_ID = DataObjectIdentifier
        .builder(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId("topology-netconf")))
        .build();
    private static final long TIMEOUT = 1;
    private static final DataObjectIdentifier<Toaster> TOASTER_ID = DataObjectIdentifier.builder(Toaster.class)
        .build();

    @Autowired
    @Qualifier("BindingDataBroker")
    private DataBroker dataBroker;

    @Autowired
    private MountPointService mountPointService;

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping(path = "/list")
    public ResponseEntity getNetconfDevicesIds(Authentication authentication) throws InterruptedException,
            ExecutionException, TimeoutException {
        Utils.logUserData(LOG, authentication);
        final Optional<Topology> netconfTopoOptional;
        try (final ReadTransaction tx = dataBroker.newReadOnlyTransaction()) {
            netconfTopoOptional = tx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPOLOGY_ID)
                    .get(TIMEOUT, TimeUnit.SECONDS);
        }

        if (netconfTopoOptional.isPresent()) {
            return ResponseEntity.ok(getNetconfDevices(netconfTopoOptional.get()));
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    private List<NetconfDeviceResponse> getNetconfDevices(final Topology netconfTopology)
            throws InterruptedException, TimeoutException, ExecutionException {
        final List<NetconfDeviceResponse> devices = new ArrayList<>();
        for (Node node : netconfTopology.nonnullNode().values()) {
            NetconfDeviceResponse nodeResponse = NetconfDeviceResponse.from(node);
            final Optional<MountPoint> netconfMountPoint = mountPointService.findMountPoint(NETCONF_TOPOLOGY_ID
                .toBuilder()
                .child(Node.class, new NodeKey(node.getNodeId()))
                .build());
            if (netconfMountPoint.isPresent()) {
                final Optional<DataBroker> netconfDataBroker = netconfMountPoint.get().getService(DataBroker.class);

                if (netconfDataBroker.isPresent()) {
                    final Optional<Toaster> toasterData;
                    try (final ReadTransaction netconfReadTx = netconfDataBroker.get().newReadOnlyTransaction()) {
                        toasterData = netconfReadTx.read(LogicalDatastoreType.OPERATIONAL, TOASTER_ID)
                                .get(TIMEOUT, TimeUnit.SECONDS);
                    }
                    if (toasterData.isPresent() && toasterData.get().getDarknessFactor() != null) {
                        nodeResponse = NetconfDeviceResponse.from(node, toasterData.get()
                                .getDarknessFactor().toJava());
                    }
                }
            }
            devices.add(nodeResponse);
        }
        return devices;
    }

    @Secured({"ROLE_ADMIN"})
    @PutMapping(path = "/id/{netconfDeviceId}")
    public ResponseEntity connectNetconfDevice(@PathVariable final String netconfDeviceId,
                                               @RequestBody final NetconfDeviceRequest deviceInfo, Authentication authentication)
        throws InterruptedException, ExecutionException, TimeoutException {
        Utils.logUserData(LOG, authentication);
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        final NodeId nodeId = new NodeId(netconfDeviceId);
        final DataObjectIdentifier<Node> netconfDeviceID = NETCONF_TOPOLOGY_ID
            .toBuilder()
            .child(Node.class, new NodeKey(nodeId))
            .build();

        final Node netconfDeviceData = new NodeBuilder()
            .setNodeId(nodeId)
            .addAugmentation(new NetconfNodeAugmentBuilder().setNetconfNode(
                new NetconfNodeBuilder().setHost(new Host(new IpAddress(new Ipv4Address(deviceInfo.getAddress()))))
                .setPort(new PortNumber(Uint16.valueOf(deviceInfo.getPort())))
                .setCredentials(new LoginPwUnencryptedBuilder().setLoginPasswordUnencrypted(
                    new LoginPasswordUnencryptedBuilder()
                        .setUsername(deviceInfo.getUsername())
                        .setPassword(deviceInfo.getPassword())
                        .build()).build())
                .setTcpOnly(false)
                .build()).build())
            .build();
        tx.put(LogicalDatastoreType.CONFIGURATION, netconfDeviceID, netconfDeviceData);

        tx.commit().get(TIMEOUT, TimeUnit.SECONDS);

        return ResponseEntity.ok().build();
    }

    @Secured({"ROLE_ADMIN"})
    @DeleteMapping(path = "/id/{netconfDeviceId}")
    public ResponseEntity disconnectNetconfDevice(@PathVariable final String netconfDeviceId, Authentication authentication)
        throws InterruptedException, ExecutionException, TimeoutException {
        Utils.logUserData(LOG, authentication);
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        final NodeId nodeId = new NodeId(netconfDeviceId);
        final DataObjectIdentifier<Node> netconfDeviceID = NETCONF_TOPOLOGY_ID
            .toBuilder()
            .child(Node.class, new NodeKey(nodeId))
            .build();

        tx.delete(LogicalDatastoreType.CONFIGURATION, netconfDeviceID);

        tx.commit().get(TIMEOUT, TimeUnit.SECONDS);

        return ResponseEntity.ok().build();
    }

}
