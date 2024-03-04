/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.identifier;

import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObjectStep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyStep;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class IdentifierUtils {

    private IdentifierUtils() {
        // Utility class
    }

    public static final String GNMI_TOPOLOGY_ID = "gnmi-topology";
    public static final InstanceIdentifier<Topology> GNMI_TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(GNMI_TOPOLOGY_ID)))
            .build();

    public static final DataTreeIdentifier<Node> GNMI_NODE_DTI = DataTreeIdentifier
            .create(LogicalDatastoreType.CONFIGURATION, GNMI_TOPO_IID.child(Node.class));

    public static YangInstanceIdentifier nodeidToYii(final NodeId nodeId) {
        return YangInstanceIdentifier.builder()
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), GNMI_TOPOLOGY_ID)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, QName.create(Node.QNAME, "node-id"), nodeId.getValue())
                .build();
    }

    public static NodeId nodeIdOfPathArgument(final DataObjectStep pathArgument)
            throws IllegalStateException {
        if (pathArgument instanceof KeyStep<?, ?> identifiableItem) {
            final var key = identifiableItem.key();
            if (key instanceof NodeKey nodeKey) {
                return nodeKey.getNodeId();
            }
        }
        throw new IllegalStateException("Unable to create NodeId from: " + pathArgument);
    }

    public static InstanceIdentifier<Node> gnmiNodeIID(final NodeId nodeId) {
        return GNMI_TOPO_IID.child(Node.class, new NodeKey(nodeId));
    }
}
