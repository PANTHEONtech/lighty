/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.testng.Assert;

final class TestUtils {

    private static final String NODE_ID = "test-node-id";

    static final String TOPOLOGY_NAME = "test-topo";
    static final Topology TOPOLOGY = new TopologyBuilder()
        .setTopologyId(new TopologyId(TOPOLOGY_NAME))
        .build();
    static final DataObjectIdentifier<Topology> TOPOLOGY_ID = DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, TOPOLOGY.key())
            .build();

    private TestUtils() {

    }

    static YangInstanceIdentifier createNetworkTopologyYIID() {
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                YangInstanceIdentifier.builder();
        return builder.node(NetworkTopology.QNAME).build();
    }

    static YangInstanceIdentifier createTopologyNodeYIID() {
        return YangInstanceIdentifier.builder(createNetworkTopologyYIID())
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "TOPOLOGY-id"), TOPOLOGY_NAME)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, QName.create(Node.QNAME, "node-id"), NODE_ID)
                .build();
    }

    static void writeToTopology(final DataBroker bindingDataBroker,
            final DataObjectIdentifier<Topology> topologyInstanceIdentifier, final Topology topology)
                    throws ExecutionException, InterruptedException {
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier, topology);
        writeTransaction.commit().get();
    }

    static void readFromTopology(final DataBroker bindingDataBroker, final String testTopoId,
            final int expectedCount) throws InterruptedException, ExecutionException, TimeoutException {
        final ReadTransaction readOnlyTransaction = bindingDataBroker.newReadOnlyTransaction();

        final DataObjectIdentifier<NetworkTopology> networkTopologyInstanceIdentifier =
            DataObjectIdentifier.builder(NetworkTopology.class).build();

        final FluentFuture<Optional<NetworkTopology>> upcommingRead = readOnlyTransaction
                .read(LogicalDatastoreType.OPERATIONAL, networkTopologyInstanceIdentifier);
        final Optional<NetworkTopology> networkTopologyOptional = upcommingRead.get(40, TimeUnit.MILLISECONDS);
        final long count;
        if (networkTopologyOptional.isPresent()) {
            count = networkTopologyOptional.get().nonnullTopology().values().stream()
                    .filter(t -> testTopoId.equals(t.getTopologyId().getValue())).count();
        } else {
            count = 0;
        }
        Assert.assertEquals(count, expectedCount);
    }

}
