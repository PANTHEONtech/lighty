/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.examples.controllers.quarkus.service;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


@ApplicationScoped
@Path("/services/data/topology")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TopologyRestService {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRestService.class);
    private final static long TIMEOUT = 1L;

    @Inject
    private DataBroker databroker;

    @GET
    @Path("/list")
    public Response getAllTopologyIdsOperational()
            throws InterruptedException, ExecutionException, TimeoutException {

        try (final ReadTransaction tx = databroker.newReadOnlyTransaction()) {
            final InstanceIdentifier<NetworkTopology> iid =
                    InstanceIdentifier.create(NetworkTopology.class);
            final Optional<NetworkTopology> readData =
                    tx.read(LogicalDatastoreType.OPERATIONAL, iid).get(TIMEOUT, TimeUnit.SECONDS);

            if (readData.isPresent()) {
                final List<String> topology = readData.get().getTopology().stream()
                        .map(topology1 -> topology1.getTopologyId().getValue())
                        .collect(Collectors.toList());
                return Response.ok(topology).build();
            } else {
                return Response.ok(Collections.emptyList()).build();
            }
        }
    }

    @PUT
    @Path("/id/{topologyId}")
    public Response putTopologyOperational(@PathParam("topologyId") final String topologyId) {

        final WriteTransaction tx = databroker.newWriteOnlyTransaction();

        final Topology topology = new TopologyBuilder()
            .setTopologyId(new TopologyId(topologyId))
            .build();
        final InstanceIdentifier<Topology> iid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)));

        tx.put(LogicalDatastoreType.OPERATIONAL, iid, topology);

        try {
            tx.commit().get(TIMEOUT, TimeUnit.SECONDS);
            LOG.info("Topology was stored to datastore: {}", topology);
            return Response.ok().build();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            LOG.error("Could not store topology to datastore: {}", topology, e);
            return Response.status(500).build();
        }
    }

    @DELETE
    @Path("/id/{topologyId}")
    public Response deleteTopologyOperational(@PathParam("topologyId") final String topologyId) {

        final WriteTransaction tx = databroker.newWriteOnlyTransaction();

        final InstanceIdentifier<Topology> iid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)));

        tx.delete(LogicalDatastoreType.OPERATIONAL, iid);

        try {
            tx.commit().get(TIMEOUT, TimeUnit.SECONDS);
            LOG.info("Topology {} was deleted from datastore", topologyId);
            return Response.ok().build();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            LOG.error("Could not delete topology {} from datastore", topologyId, e);
            return Response.status(500).build();
        }
    }

}
