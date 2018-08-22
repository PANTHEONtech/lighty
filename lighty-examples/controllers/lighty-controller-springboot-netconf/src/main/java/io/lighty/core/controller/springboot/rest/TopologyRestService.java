/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */

package io.lighty.core.controller.springboot.rest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/topology")
public class TopologyRestService {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyRestService.class);
    private final static long TIMEOUT = 1L;

    @Autowired
    @Qualifier("BindingDataBroker")
    private DataBroker databroker;

    @PutMapping("/id/{topologyId}")
    public ResponseEntity putTopologyOperational(@PathVariable final String topologyId) {

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
            return ResponseEntity.ok().build();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            LOG.error("Could not store topology to datastore: {}", topology, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity getAllTopologyIdsOperational()
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
                return ResponseEntity.ok(topology);
            } else {
                return ResponseEntity.ok(Collections.emptyList());
            }
        }
    }

    @DeleteMapping("/id/{topologyId}")
    public ResponseEntity deleteTopologyOperational(@PathVariable final String topologyId) {

        final WriteTransaction tx = databroker.newWriteOnlyTransaction();

        final InstanceIdentifier<Topology> iid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)));

        tx.delete(LogicalDatastoreType.OPERATIONAL, iid);

        try {
            tx.commit().get(TIMEOUT, TimeUnit.SECONDS);
            LOG.info("Topology {} was deleted from datastore", topologyId);
            return ResponseEntity.ok().build();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            LOG.error("Could not delete topology {} from datastore", topologyId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
