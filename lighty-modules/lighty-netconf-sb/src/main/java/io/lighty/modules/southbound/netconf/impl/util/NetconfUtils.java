/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl.util;

import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DATA_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DEFAULT_OPERATION_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_LOCK_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_SOURCE_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_TARGET_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_UNLOCK_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_VALIDATE_NODEID;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.ROLLBACK_ON_ERROR_OPTION;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Locale;
import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.netconf.api.EffectiveOperation;
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.netconf.topology.spi.NetconfNodeUtils;
import org.opendaylight.netconf.util.NetconfUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.copy.config.input.target.ConfigTarget;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.edit.config.input.EditContent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.get.config.input.source.ConfigSource;
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
    public static final QName NETCONF_DELETE_CONFIG_QNAME =
            QName.create(NetconfUtil.NETCONF_QNAME, "delete-config").intern();
    public static final NodeIdentifier NETCONF_DELETE_CONFIG_NODEID =
            NodeIdentifier.create(NETCONF_DELETE_CONFIG_QNAME);
    private static final NodeIdentifier CONFIG_SOURCE_NODEID = NodeIdentifier.create(ConfigSource.QNAME);
    private static final NodeIdentifier CONFIG_TARGET_NODEID = NodeIdentifier.create(ConfigTarget.QNAME);
    private static final NodeIdentifier EDIT_CONTENT_NODEID = NodeIdentifier.create(EditContent.QNAME);
    private static final QName TOPOLOGY_ID_QNAME = QName.create(Topology.QNAME, "topology-id").intern();
    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "node-id").intern();
    private static final String TOPOLOGY_NETCONF = "topology-netconf";

    private NetconfUtils() {
    }

    public static InstanceIdentifier<Node> createNetConfNodeMountPointII(NodeId nodeId) {
        KeyedInstanceIdentifier<Topology, TopologyKey> instanceIdentifier =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                        new TopologyKey(new TopologyId(NetconfNodeUtils.DEFAULT_TOPOLOGY_NAME)));
        InstanceIdentifier<Node> netconfNodeIID =
                instanceIdentifier.child(Node.class, new NodeKey(nodeId));
        return netconfNodeIID;
    }

    public static YangInstanceIdentifier createNetConfNodeMountPointYII(NodeId nodeId) {
        var yangInstanceIdentifier = YangInstanceIdentifier.builder()
                .node(NetworkTopology.QNAME)
                .node(Topology.QNAME)
                .nodeWithKey(Topology.QNAME, TOPOLOGY_ID_QNAME, TOPOLOGY_NETCONF)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME, NODE_ID_QNAME, nodeId.getValue())
                .build();
        return yangInstanceIdentifier;
    }

    public static ListenableFuture<Optional<NormalizedNode>> extractDataFromRpcResult(
            Optional<YangInstanceIdentifier> path, ListenableFuture<DOMRpcResult> rpcFuture) {
        return Futures.transform(rpcFuture, result -> {
            Preconditions.checkArgument(
                    result.getErrors().isEmpty(), "Unable to read data: %s, errors: %s", path, result.getErrors());
            DataContainerChild dataNode =
                    result.getResult().getChildByArg(NETCONF_DATA_NODEID);
            return NormalizedNodes.findNode(dataNode, path.get().getPathArguments());
        }, MoreExecutors.directExecutor());
    }

    public static DataContainerChild createEditConfigStructure(EffectiveModelContext effectiveModelContext,
            Optional<NormalizedNode> lastChild,
            Optional<EffectiveOperation> operation,
            YangInstanceIdentifier dataPath) {
        AnyxmlNode<?> configContent = NetconfMessageTransformUtil
                .createEditConfigAnyxml(effectiveModelContext, dataPath, operation, lastChild);
        return Builders.choiceBuilder().withNodeIdentifier(EDIT_CONTENT_NODEID).withChild(configContent).build();
    }

    public static ContainerNode getEditConfigContent(
            QName targetDatastore, DataContainerChild editStructure,
            Optional<EffectiveOperation> defaultOperation, boolean rollback) {
        DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> editBuilder =
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
                    .withValue(ROLLBACK_ON_ERROR_OPTION).build());
        }

        // Edit content
        editBuilder.withChild(editStructure);
        return editBuilder.build();
    }

    public static DataContainerChild getSourceNode(QName sourceDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_SOURCE_NODEID)
                .withChild(Builders.choiceBuilder().withNodeIdentifier(CONFIG_SOURCE_NODEID).withChild(
                                Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(sourceDatastore))
                                        .withValue(Empty.value()).build())
                        .build()).build();
    }

    public static ContainerNode getLockContent(QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_LOCK_NODEID)
                .withChild(getTargetNode(targetDatastore)).build();
    }

    public static DataContainerChild getTargetNode(QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_TARGET_NODEID)
                .withChild(Builders.choiceBuilder().withNodeIdentifier(CONFIG_TARGET_NODEID).withChild(
                                Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(targetDatastore))
                                        .withValue(Empty.value()).build())
                        .build()).build();
    }

    public static ContainerNode getCopyConfigContent(QName sourceDatastore, QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_COPY_CONFIG_NODEID)
                .withChild(getTargetNode(targetDatastore)).withChild(getSourceNode(sourceDatastore)).build();
    }

    public static ContainerNode getDeleteConfigContent(QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_DELETE_CONFIG_NODEID)
                .withChild(getTargetNode(targetDatastore)).build();
    }

    public static ContainerNode getValidateContent(QName sourceDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_VALIDATE_NODEID)
                .withChild(getSourceNode(sourceDatastore)).build();
    }

    public static ContainerNode getUnLockContent(QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(NETCONF_UNLOCK_NODEID)
                .withChild(getTargetNode(targetDatastore)).build();
    }

}
