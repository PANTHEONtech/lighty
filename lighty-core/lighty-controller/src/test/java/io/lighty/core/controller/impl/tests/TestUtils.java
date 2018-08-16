/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.testng.Assert;

/**
 * author: vincent on 18.8.2017.
 */
class TestUtils {

    private static final String NODE_ID = "test-node-id";

    static final String TOPOLOGY_ID = "test-topo";
    static final Topology TOPOLOGY =
            new TopologyBuilder().setTopologyId(new TopologyId(TOPOLOGY_ID)).build();
    static final InstanceIdentifier<Topology> TOPOLOGY_IID =
            InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, TOPOLOGY.key()).build();

    static YangInstanceIdentifier createNetworkTopologyYIID() {
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                YangInstanceIdentifier.builder();
        return builder.node(NetworkTopology.QNAME).build();
    }

    static YangInstanceIdentifier createTopologyNodeYIID() {
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                YangInstanceIdentifier.builder(createNetworkTopologyYIID());
        builder.node(Topology.QNAME)
        .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "TOPOLOGY-id"),
                TOPOLOGY_ID).node(Node.QNAME)
        .nodeWithKey(Node.QNAME, QName.create(Node.QNAME, "node-id"), NODE_ID);
        return builder.build();
    }

    static void writeToTopology(final DataBroker bindingDataBroker,
            final InstanceIdentifier<Topology> topologyInstanceIdentifier, final Topology topology)
                    throws ExecutionException, InterruptedException {
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier, topology);
        writeTransaction.commit().get();
    }

    static void writeToTopology(final org.opendaylight.controller.md.sal.binding.api.DataBroker bindingDataBroker,
            final InstanceIdentifier<Topology> topologyInstanceIdentifier, final Topology topology)
                    throws ExecutionException, InterruptedException {
        final org.opendaylight.controller.md.sal.binding.api.WriteTransaction writeTransaction = bindingDataBroker
                .newWriteOnlyTransaction();
        writeTransaction.put(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL,
                topologyInstanceIdentifier, topology);
        writeTransaction.submit().get();
    }

    static void readFromTopology(final DataBroker bindingDataBroker, final String testTopoId,
            final int expectedCount) throws InterruptedException, ExecutionException, TimeoutException {
        final ReadTransaction readOnlyTransaction = bindingDataBroker.newReadOnlyTransaction();

        final InstanceIdentifier<NetworkTopology> networkTopologyInstanceIdentifier =
                InstanceIdentifier.builder(NetworkTopology.class).build();

        final FluentFuture<Optional<NetworkTopology>> upcommingRead = readOnlyTransaction
                .read(LogicalDatastoreType.OPERATIONAL, networkTopologyInstanceIdentifier);
        final Optional<NetworkTopology> networkTopologyOptional =
                upcommingRead.get(40, TimeUnit.MILLISECONDS);
        final NetworkTopology networkTopology = networkTopologyOptional.get();

        final long count = networkTopology.getTopology().stream()
                .filter(t -> testTopoId.equals(t.getTopologyId().getValue())).count();
        Assert.assertEquals(count, expectedCount);
    }

    static void readFromTopology(final org.opendaylight.controller.md.sal.binding.api.DataBroker bindingDataBroker,
            final String testTopoId, final int expectedCount) throws InterruptedException, ExecutionException,
    TimeoutException {
        final ReadOnlyTransaction readOnlyTransaction = bindingDataBroker.newReadOnlyTransaction();

        final InstanceIdentifier<NetworkTopology> networkTopologyInstanceIdentifier = InstanceIdentifier.builder(
                NetworkTopology.class).build();

        final CheckedFuture<com.google.common.base.Optional<NetworkTopology>, ReadFailedException> upcommingRead =
                readOnlyTransaction.read(
                        org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL,
                        networkTopologyInstanceIdentifier);
        final com.google.common.base.Optional<NetworkTopology> networkTopologyOptional = upcommingRead.get(40,
                TimeUnit.MILLISECONDS);
        final NetworkTopology networkTopology = networkTopologyOptional.get();

        final long count = networkTopology.getTopology().stream().filter(t -> testTopoId.equals(t.getTopologyId()
                .getValue())).count();
        Assert.assertEquals(count, expectedCount);
    }
}
