/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.quarkus.service;

import io.lighty.examples.controllers.quarkus.service.dto.NetconfDeviceRequest;
import io.lighty.examples.controllers.quarkus.service.dto.NetconfDeviceResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
@Path("/services/data/netconf")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NetconfDeviceRestService {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceRestService.class);

    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_IID = InstanceIdentifier
        .create(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId("topology-netconf")));
    private static final long TIMEOUT = 1;
    private static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.create(Toaster.class);

    @Inject
    private DataBroker dataBroker;

    @Inject
    private MountPointService mountPointService;

    @GET
    @Path("/list")
    public Response getNetconfDevicesIds() throws InterruptedException, ExecutionException, TimeoutException {
        try (final ReadTransaction tx = dataBroker.newReadOnlyTransaction()) {
            final Optional<Topology> netconfTopoOptional =
                tx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPOLOGY_IID).get(TIMEOUT, TimeUnit.SECONDS);
            final List<NetconfDeviceResponse> response = new ArrayList<>();

            if (netconfTopoOptional.isPresent() && netconfTopoOptional.get().getNode() != null) {
                for (Node node : netconfTopoOptional.get().getNode()) {
                    NetconfDeviceResponse nodeResponse = NetconfDeviceResponse.from(node);

                    final Optional<MountPoint> netconfMountPoint =
                        mountPointService.getMountPoint(NETCONF_TOPOLOGY_IID
                            .child(Node.class, new NodeKey(node.getNodeId())));

                    if (netconfMountPoint.isPresent()) {
                        final Optional<DataBroker> netconfDataBroker =
                            netconfMountPoint.get().getService(DataBroker.class);
                        if (netconfDataBroker.isPresent()) {
                            final ReadTransaction netconfReadTx =
                                netconfDataBroker.get().newReadOnlyTransaction();

                            final Optional<Toaster> toasterData = netconfReadTx
                                .read(LogicalDatastoreType.OPERATIONAL, TOASTER_IID).get(TIMEOUT, TimeUnit.SECONDS);

                            if (toasterData.isPresent() && toasterData.get().getDarknessFactor() != null) {
                                nodeResponse = NetconfDeviceResponse.from(node, toasterData.get().getDarknessFactor());
                            }
                        }
                    }
                    response.add(nodeResponse);
                }
            }
            return Response.ok(response).build();
        }
    }

    @POST
    @Path("/id/{netconfDeviceId}")
    public Response connectNetconfDevice(@PathParam("netconfDeviceId") final String netconfDeviceId,
                                         final NetconfDeviceRequest deviceInfo)
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
        return Response.ok().build();
    }

    @DELETE
    @Path("/id/{netconfDeviceId}")
    public Response disconnectNetconfDevice(@PathParam("netconfDeviceId") final String netconfDeviceId)
        throws InterruptedException, ExecutionException, TimeoutException {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        final NodeId nodeId = new NodeId(netconfDeviceId);
        final InstanceIdentifier<Node> netconfDeviceIID = NETCONF_TOPOLOGY_IID
            .child(Node.class, new NodeKey(nodeId));

        tx.delete(LogicalDatastoreType.CONFIGURATION, netconfDeviceIID);
        tx.commit().get(TIMEOUT, TimeUnit.SECONDS);
        return Response.ok().build();
    }

}
