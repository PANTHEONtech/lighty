/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl.util;

import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_DATA_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_DEFAULT_OPERATION_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_LOCK_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_SOURCE_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_TARGET_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_UNLOCK_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_VALIDATE_NODEID;
import static org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.YangModuleInfoImpl.qnameOf;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Locale;
import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.netconf.api.EffectiveOperation;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.copy.config.input.target.ConfigTarget;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.edit.config.input.EditContent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.get.config.input.source.ConfigSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev231121.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyxmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public final class NetconfUtils {
    public static final NodeIdentifier NETCONF_DELETE_CONFIG_QNAME =
        NodeIdentifier.create(qnameOf("delete-config"));
    public static final NodeIdentifier NETCONF_DELETE_CONFIG_NODEID =
            NodeIdentifier.create(NETCONF_DELETE_CONFIG_QNAME.getNodeType());
    private static final NodeIdentifier CONFIG_SOURCE_NODEID = NodeIdentifier.create(ConfigSource.QNAME);
    private static final NodeIdentifier CONFIG_TARGET_NODEID = NodeIdentifier.create(ConfigTarget.QNAME);
    private static final NodeIdentifier EDIT_CONTENT_NODEID = NodeIdentifier.create(EditContent.QNAME);
    private static final QName TOPOLOGY_ID_QNAME = QName.create(Topology.QNAME, "topology-id").intern();
    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "node-id").intern();
    private static final String TOPOLOGY_NETCONF = "topology-netconf";

    private NetconfUtils() {
        // hidden on purpose
    }

    public static InstanceIdentifier<Node> createNetConfNodeMountPointII(final NodeId nodeId) {
        KeyedInstanceIdentifier<Topology, TopologyKey> instanceIdentifier =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                        new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
        InstanceIdentifier<Node> netconfNodeIID =
                instanceIdentifier.child(Node.class, new NodeKey(nodeId));
        return netconfNodeIID;
    }

    public static YangInstanceIdentifier createNetConfNodeMountPointYII(final NodeId nodeId) {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder()
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TOPOLOGY_ID_QNAME, TOPOLOGY_NETCONF)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, NODE_ID_QNAME, nodeId.getValue())
                .build();
        return yangInstanceIdentifier;
    }

    public static ListenableFuture<Optional<NormalizedNode>> extractDataFromRpcResult(
            final Optional<YangInstanceIdentifier> path, final ListenableFuture<DOMRpcResult> rpcFuture) {
        return Futures.transform(rpcFuture, result -> {
            Preconditions.checkArgument(
                    result.errors().isEmpty(), "Unable to read data: %s, errors: %s", path, result.errors());
            final DataContainerChild dataNode =
                    result.value().getChildByArg(NETCONF_DATA_NODEID);
            return NormalizedNodes.findNode(dataNode, path.get().getPathArguments());
        }, MoreExecutors.directExecutor());
    }

    public static DataContainerChild createEditConfigStructure(final EffectiveModelContext effectiveModelContext,
                                                             final Optional<NormalizedNode> lastChild,
                                                             final Optional<EffectiveOperation> operation,
                                                             final YangInstanceIdentifier dataPath) {
        final AnyxmlNode<?> configContent = NetconfMessageTransformUtil
                .createEditConfigAnyxml(effectiveModelContext, dataPath, operation, lastChild);
        return Builders.choiceBuilder().withNodeIdentifier(EDIT_CONTENT_NODEID).withChild(configContent).build();
    }

    public static ContainerNode getEditConfigContent(
            final QName targetDatastore, final DataContainerChild editStructure,
            final Optional<EffectiveOperation> defaultOperation, final boolean rollback) {
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> editBuilder =
                Builders.containerBuilder().withNodeIdentifier(NETCONF_EDIT_CONFIG_NODEID);

        // Target
        editBuilder.withChild(getTargetNode(targetDatastore));

        // Default operation
        if (defaultOperation.isPresent()) {
            editBuilder.withChild(Builders.leafBuilder().withNodeIdentifier(NETCONF_DEFAULT_OPERATION_NODEID)
                    .withValue(defaultOperation.get().name().toLowerCase(Locale.US)).build());
        }

        // Error option
        if (rollback) {
            editBuilder.withChild(Builders.leafBuilder().withNodeIdentifier(NETCONF_ERROR_OPTION_NODEID)
                    .withValue("rollback-on-error").build());
        }

        // Edit content
        editBuilder.withChild(editStructure);
        return editBuilder.build();
    }

    public static DataContainerChild getSourceNode(final QName sourceDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_SOURCE_NODEID)
                .withChild(Builders.choiceBuilder().withNodeIdentifier(CONFIG_SOURCE_NODEID).withChild(
                        Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(sourceDatastore))
                                .withValue(Empty.value()).build())
                        .build()).build();
    }

    public static ContainerNode getLockContent(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_LOCK_NODEID)
                .withChild(getTargetNode(targetDatastore)).build();
    }

    public static DataContainerChild getTargetNode(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_TARGET_NODEID)
                .withChild(Builders.choiceBuilder().withNodeIdentifier(CONFIG_TARGET_NODEID).withChild(
                        Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(targetDatastore))
                                .withValue(Empty.value()).build())
                        .build()).build();
    }

    public static ContainerNode getCopyConfigContent(final QName sourceDatastore, final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_COPY_CONFIG_NODEID)
                .withChild(getTargetNode(targetDatastore)).withChild(getSourceNode(sourceDatastore)).build();
    }

    public static ContainerNode getDeleteConfigContent(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_DELETE_CONFIG_NODEID)
                .withChild(getTargetNode(targetDatastore)).build();
    }

    public static ContainerNode getValidateContent(final QName sourceDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_VALIDATE_NODEID)
                .withChild(getSourceNode(sourceDatastore)).build();
    }

    public static ContainerNode getUnLockContent(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_UNLOCK_NODEID)
                .withChild(getTargetNode(targetDatastore)).build();
    }
}
