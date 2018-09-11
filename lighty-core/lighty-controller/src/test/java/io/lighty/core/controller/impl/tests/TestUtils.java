/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.tests;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
                    .child(Topology.class, TOPOLOGY.getKey()).build();

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
        WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier, topology);
        writeTransaction.submit().get();
    }

    static void readFromTopology(final DataBroker bindingDataBroker, final String testTopoId,
            int expectedCount) throws InterruptedException, ExecutionException, TimeoutException {
        ReadOnlyTransaction readOnlyTransaction = bindingDataBroker.newReadOnlyTransaction();

        InstanceIdentifier<NetworkTopology> networkTopologyInstanceIdentifier =
                InstanceIdentifier.builder(NetworkTopology.class).build();

        ListenableFuture<Optional<NetworkTopology>> upcommingRead = readOnlyTransaction
                .read(LogicalDatastoreType.OPERATIONAL, networkTopologyInstanceIdentifier);
        Optional<NetworkTopology> networkTopologyOptional =
                upcommingRead.get(40, TimeUnit.MILLISECONDS);
        NetworkTopology networkTopology = networkTopologyOptional.get();

        long count = networkTopology.getTopology().stream()
                .filter(t -> testTopoId.equals(t.getTopologyId().getValue())).count();
        Assert.assertEquals(count, expectedCount);
    }

}
