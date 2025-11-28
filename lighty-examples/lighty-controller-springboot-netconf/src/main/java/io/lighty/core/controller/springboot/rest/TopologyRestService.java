/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.rest;

import io.lighty.core.controller.springboot.utils.Utils;
import org.apache.commons.text.StringEscapeUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/services/data/topology")
public class TopologyRestService {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRestService.class);
    private final static long TIMEOUT = 1L;

    @Autowired
    @Qualifier("BindingDataBroker")
    private DataBroker databroker;

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/list")
    public ResponseEntity getAllTopologyIdsOperational(Authentication authentication)
            throws InterruptedException, ExecutionException, TimeoutException {
        Utils.logUserData(LOG, authentication);

        try (final ReadTransaction tx = databroker.newReadOnlyTransaction()) {
            final var objectIdentifier = DataObjectIdentifier.builder(NetworkTopology.class).build();
            final Optional<NetworkTopology> readData =
                    tx.read(LogicalDatastoreType.OPERATIONAL, objectIdentifier).get(TIMEOUT, TimeUnit.SECONDS);

            if (readData.isPresent() && readData.get().getTopology() != null) {
                final List<String> topology = readData.get().nonnullTopology().values().stream()
                        .map(topology1 -> topology1.getTopologyId().getValue())
                        .collect(Collectors.toList());
                return ResponseEntity.ok(topology);
            } else {
                return ResponseEntity.ok(Collections.emptyList());
            }
        }
    }

    @Secured({"ROLE_ADMIN"})
    @PutMapping("/id/{topologyId}")
    public ResponseEntity putTopologyOperational(@PathVariable final String topologyId, Authentication authentication)
            throws InterruptedException {
        Utils.logUserData(LOG, authentication);

        final WriteTransaction tx = databroker.newWriteOnlyTransaction();

        final Topology topology = new TopologyBuilder()
            .setTopologyId(new TopologyId(topologyId))
            .build();
        final DataObjectIdentifier<Topology> objectIdentifier = DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
            .build();

        tx.put(LogicalDatastoreType.OPERATIONAL, objectIdentifier, topology);

        try {
            tx.commit().get(TIMEOUT, TimeUnit.SECONDS);
            LOG.info("Topology was stored to datastore: {}", topology);
            return ResponseEntity.ok().build();
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Could not store topology to datastore: {}", topology, e);
            return ResponseEntity.status(500).build();
        }
    }

    @Secured({"ROLE_ADMIN"})
    @DeleteMapping("/id/{topologyId}")
    public ResponseEntity deleteTopologyOperational(@PathVariable final String topologyId, Authentication authentication)
            throws InterruptedException {
        Utils.logUserData(LOG, authentication);

        final WriteTransaction tx = databroker.newWriteOnlyTransaction();

        final DataObjectIdentifier<Topology> objectIdentifier = DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
            .build();

        tx.delete(LogicalDatastoreType.OPERATIONAL, objectIdentifier);

        try {
            tx.commit().get(TIMEOUT, TimeUnit.SECONDS);
            LOG.info("Topology {} was deleted from datastore", StringEscapeUtils.escapeJava(topologyId));
            return ResponseEntity.ok().build();
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Could not delete topology {} from datastore", StringEscapeUtils.escapeJava(topologyId), e);
            return ResponseEntity.status(500).build();
        }
    }

}
