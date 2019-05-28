/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl.util;

import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DEFAULT_OPERATION_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_LOCK_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_SOURCE_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_TARGET_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_VALIDATE_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.ROLLBACK_ON_ERROR_OPTION;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.netconf.api.ModifyAction;
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.copy.config.input.target.ConfigTarget;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.edit.config.input.EditContent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.get.config.input.source.ConfigSource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
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
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public final class NetconfUtils {

    private static final String NETCONF_TOPOLOGY_NAMESPACE = "urn:TBD:params:xml:ns:yang:network-topology";
    private static final String NETCONF_TOPOLOGY_VERSION = "2013-10-21";
    private static final String TOPOLOGY_ID = "topology-id";
    private static final String TOPOLOGY_NETCONF = "topology-netconf";
    private static final String NODE_ID = "node-id";

    public static final QName NETCONF_DELETE_CONFIG_QNAME =
            QName.create(NetconfMessageTransformUtil.NETCONF_QNAME, "delete-config").intern();

    private NetconfUtils() {
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
                .nodeWithKey(Topology.QNAME,
                        QName.create(NETCONF_TOPOLOGY_NAMESPACE, NETCONF_TOPOLOGY_VERSION, TOPOLOGY_ID),
                        TOPOLOGY_NETCONF)
                .node(Node.QNAME)
                .nodeWithKey(Node.QNAME,
                        QName.create(NETCONF_TOPOLOGY_NAMESPACE, NETCONF_TOPOLOGY_VERSION, NODE_ID), nodeId.getValue())
                .build();
        return yangInstanceIdentifier;
    }

    public static ListenableFuture<Optional<NormalizedNode<?, ?>>> extractDataFromRpcResult(
            final Optional<YangInstanceIdentifier> path, final ListenableFuture<DOMRpcResult> rpcFuture) {
        return Futures.transform(rpcFuture, result -> {
            Preconditions.checkArgument(
                    result.getErrors().isEmpty(), "Unable to read data: %s, errors: %s", path, result.getErrors());
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataNode =
                    ((ContainerNode) result.getResult()).getChild(
                            NetconfMessageTransformUtil.toId(NetconfMessageTransformUtil.NETCONF_DATA_QNAME)).get();
            return NormalizedNodes.findNode(dataNode, path.get().getPathArguments());
        }, MoreExecutors.directExecutor());
    }

    public static DataContainerChild<?, ?> createEditConfigStructure(final SchemaContext schemaContext,
                                                             final Optional<NormalizedNode<?, ?>> lastChild,
                                                             final Optional<ModifyAction> operation,
                                                             final YangInstanceIdentifier dataPath) {
        final AnyXmlNode configContent = NetconfMessageTransformUtil
                .createEditConfigAnyxml(schemaContext, dataPath, operation, lastChild);
        return Builders.choiceBuilder().withNodeIdentifier(toId(EditContent.QNAME)).withChild(configContent).build();
    }

    public static ContainerNode getEditConfigContent(
            final QName targetDatastore, final DataContainerChild<?, ?> editStructure,
            final Optional<ModifyAction> defaultOperation, final boolean rollback) {
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> editBuilder =
                Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_EDIT_CONFIG_QNAME));

        // Target
        editBuilder.withChild(getTargetNode(targetDatastore));

        // Default operation
        if (defaultOperation.isPresent()) {
            final String opString = defaultOperation.get().name().toLowerCase();
            editBuilder.withChild(Builders.leafBuilder().withNodeIdentifier(toId(NETCONF_DEFAULT_OPERATION_QNAME))
                    .withValue(opString).build());
        }

        // Error option
        if (rollback) {
            editBuilder.withChild(Builders.leafBuilder().withNodeIdentifier(toId(NETCONF_ERROR_OPTION_QNAME))
                    .withValue(ROLLBACK_ON_ERROR_OPTION).build());
        }

        // Edit content
        editBuilder.withChild(editStructure);
        return editBuilder.build();
    }

    public static DataContainerChild<?, ?> getSourceNode(final QName sourceDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_SOURCE_QNAME))
                .withChild(Builders.choiceBuilder().withNodeIdentifier(toId(ConfigSource.QNAME)).withChild(
                        Builders.leafBuilder().withNodeIdentifier(toId(sourceDatastore))
                                .withValue(Empty.getInstance()).build())
                        .build()).build();
    }

    public static ContainerNode getLockContent(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_LOCK_QNAME))
                .withChild(getTargetNode(targetDatastore)).build();
    }

    public static DataContainerChild<?, ?> getTargetNode(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_TARGET_QNAME))
                .withChild(Builders.choiceBuilder().withNodeIdentifier(toId(ConfigTarget.QNAME)).withChild(
                        Builders.leafBuilder().withNodeIdentifier(toId(targetDatastore))
                                .withValue(Empty.getInstance()).build())
                        .build()).build();
    }

    public static NormalizedNode<?, ?> getCopyConfigContent(final QName sourceDatastore, final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_COPY_CONFIG_QNAME))
                .withChild(getTargetNode(targetDatastore)).withChild(getSourceNode(sourceDatastore)).build();
    }

    public static NormalizedNode<?, ?> getDeleteConfigContent(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_DELETE_CONFIG_QNAME))
                .withChild(getTargetNode(targetDatastore)).build();
    }

    public static NormalizedNode<?, ?> getValidateContent(final QName sourceDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_VALIDATE_QNAME))
                .withChild(getSourceNode(sourceDatastore)).build();
    }

    public static NormalizedNode<?, ?> getUnLockContent(final QName targetDatastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_UNLOCK_QNAME))
                .withChild(getTargetNode(targetDatastore)).build();
    }

}
