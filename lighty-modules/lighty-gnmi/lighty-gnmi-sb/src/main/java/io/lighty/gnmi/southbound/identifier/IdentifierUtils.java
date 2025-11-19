/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.identifier;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.binding.DataObjectStep;
import org.opendaylight.yangtools.binding.KeyStep;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class IdentifierUtils {

    private IdentifierUtils() {
        // Utility class
    }

    public static final String GNMI_TOPOLOGY_ID = "gnmi-topology";
    public static final DataObjectIdentifier<Topology> GNMI_TOPO_ID = DataObjectIdentifier
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(GNMI_TOPOLOGY_ID)))
            .build();
    public static final DataObjectReference<Node> GNMI_NODE_DTI = GNMI_TOPO_ID.toBuilder()
            .toReferenceBuilder()
            .child(Node.class)
            .build();

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

    public static DataObjectIdentifier<Node> gnmiNodeID(final NodeId nodeId) {
        return GNMI_TOPO_ID.toBuilder().child(Node.class, new NodeKey(nodeId)).build();
    }
}
