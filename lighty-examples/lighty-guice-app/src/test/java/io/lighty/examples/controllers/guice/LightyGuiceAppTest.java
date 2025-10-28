/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.guice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.examples.controllers.guice.service.DataStoreService;
import io.lighty.examples.controllers.guice.service.DataStoreServiceImpl;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;

public class LightyGuiceAppTest {

    private static final String EXPECTED_TOPOLOGY_ID = "InitialTopology";
    private static final long TIMEOUT_SECONDS = 30;

    private static Main main;
    private static DataStoreService service;

    @BeforeAll
    public static void startUp() {
        final var capturedService = ArgumentCaptor.forClass(DataStoreServiceImpl.class);
        main = Mockito.spy(new Main());
        main.start(new String[]{}, false);
        Mockito.verify(main).writeInitData(capturedService.capture());
        service = capturedService.getValue();
    }

    @AfterAll
    public static void tearDown() {
        main.shutdown();
    }

    @Test
    public void testReadFromDataBroker() throws Exception {
        final var identifier = InstanceIdentifier.create(NetworkTopology.class);
        final var networkTopology = service.readFromDataBroker(identifier).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(networkTopology.isPresent());
        final var topology = networkTopology.get().getTopology();
        assertNotNull(topology);
        assertEquals(1, topology.size());
        assertEquals(EXPECTED_TOPOLOGY_ID, topology.values().iterator().next().getTopologyId().getValue());
    }

    @Test
    public void testReadFromDomDataBroker() throws Exception {
        final var identifier = YangInstanceIdentifier.create(NodeIdentifier.create(NetworkTopology.QNAME));
        final var networkTopology = service.readFromDomDataBroker(identifier).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertTrue(networkTopology.isPresent());
        assertTrue(networkTopology.get() instanceof ContainerNode);
        final var containerNode = (ContainerNode) networkTopology.get();
        final var child = containerNode.body().iterator().next();
        assertTrue(child instanceof MapNode);
        assertEquals(1, ((MapNode) child).body().size());
        final var mapEntryNode = ((MapNode) child).body().iterator().next();
        assertNotNull(mapEntryNode);
        assertEquals(1, mapEntryNode.body().size());
        final var topologyId = mapEntryNode.body().iterator().next();
        assertNotNull(topologyId);
        assertEquals(EXPECTED_TOPOLOGY_ID, topologyId.body());
    }
}
