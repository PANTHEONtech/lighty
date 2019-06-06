/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_OPERATION_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.netconf.api.ModifyAction;
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.nmda.rev190107.edit.data.input.EditContent;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.rfc7952.data.api.NormalizedMetadata;
import org.opendaylight.yangtools.rfc7952.data.util.ImmutableMetadataNormalizedAnydata;
import org.opendaylight.yangtools.rfc7952.data.util.ImmutableNormalizedMetadata;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnydataNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedAnydata;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAnydataNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.ImmutableNormalizedAnydata;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NetconfNmdaBaseServiceImpl extends NetconfBaseServiceImpl implements NetconfNmdaBaseService {

    public static final QName NETCONF_NMDA_EXTENSION_QNAME = QName.create("urn:ietf:params:xml:ns:yang:ietf-netconf-nmda", "2019-01-07", "ietf-netconf-nmda").intern();
    public static final QName NETCONF_GET_DATA_QNAME = QName.create(NETCONF_NMDA_EXTENSION_QNAME, "get-data");
    public static final QName NETCONF_EDIT_DATA_QNAME = QName.create(NETCONF_NMDA_EXTENSION_QNAME, "edit-data").intern();
    private static final YangInstanceIdentifier.NodeIdentifier NETCONF_FILTER_NODEID =
            YangInstanceIdentifier.NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "subtree-filter"));
    private static final YangInstanceIdentifier.NodeIdentifier NETCONF_FILTER_CHOICE_NODEID =
            YangInstanceIdentifier.NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "filter-spec"));
    private static final YangInstanceIdentifier.NodeIdentifier NETCONF_ORIGIN_FILTERS_CHOICE_NODEID =
            YangInstanceIdentifier.NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "origin-filters"));
    private static final QName NETCONF_OPERATION_QNAME_LEGACY = NETCONF_OPERATION_QNAME.withoutRevision().intern();
    private static final YangInstanceIdentifier.NodeIdentifier NETCONF_EDIT_DATA_CONFIG_NODEID =
            YangInstanceIdentifier.NodeIdentifier.create(QName.create(NETCONF_EDIT_DATA_QNAME, "config"));

    private final DOMRpcService domRpcService;
    private final SchemaContext schemaContext;

    public NetconfNmdaBaseServiceImpl(NodeId nodeId, DOMRpcService domRpcService, SchemaContext schemaContext) {
        super(nodeId, domRpcService, schemaContext);
        this.domRpcService = domRpcService;
        this.schemaContext = schemaContext;
    }

    @Override
    public ListenableFuture<? extends DOMRpcResult> getData(QName sourceDatastore,
                                                            Optional<YangInstanceIdentifier> filterYII,
                                                            Optional<Boolean> configFilter,
                                                            Optional<Integer> maxDepth,
                                                            Optional<Set<QName>> originFilter,
                                                            Optional<Boolean> negateOriginFilter,
                                                            Optional<Boolean> withOrigin) {
        List<DataContainerChild<?, ?>> getDataChildren = new ArrayList<>();

        Preconditions.checkNotNull(sourceDatastore);
        getDataChildren.add(getDatastoreNode(sourceDatastore));

        if (filterYII.isPresent()) {
            NormalizedNode<?, ?> filterNN = ImmutableNodes.fromInstanceId(schemaContext, filterYII.get());
            QName nodeType = filterNN.getNodeType();
            Optional<DataSchemaNode> dataTreeChild = schemaContext.findDataTreeChild(nodeType);

            final AnydataNode<NormalizedAnydata> subtreeFilter =
                    ImmutableAnydataNodeBuilder.create(NormalizedAnydata.class)
                            .withNodeIdentifier(NETCONF_FILTER_NODEID)
                            .withValue(new ImmutableNormalizedAnydata(schemaContext, dataTreeChild.get(), filterNN))
                            .build();
            final ChoiceNode filterSpecChoice =
                    Builders.choiceBuilder()
                            .withNodeIdentifier(NETCONF_FILTER_CHOICE_NODEID)
                            .withChild(subtreeFilter)
                            .build();

            getDataChildren.add(filterSpecChoice);
        }

        configFilter.ifPresent(configFilterValue -> getDataChildren.add(getConfigFilterNode(configFilterValue)));

        if (originFilter.isPresent()) {
            DataContainerChild<?, ?> originFilterChild;
            if (negateOriginFilter.isPresent() && negateOriginFilter.get()) {
                originFilterChild = getNegatedOriginFilterNode(originFilter.get());
            } else {
                originFilterChild = getOriginFilterNode(originFilter.get());
            }
            final ChoiceNode originFilterSpecChoice =
                    Builders.choiceBuilder()
                            .withNodeIdentifier(NETCONF_ORIGIN_FILTERS_CHOICE_NODEID)
                            .withChild(originFilterChild)
                            .build();
            getDataChildren.add(originFilterSpecChoice);
        }

        maxDepth.ifPresent(maxDepthValue -> getDataChildren.add(getMaxDepthNode(maxDepthValue)));

        if (withOrigin.isPresent() && withOrigin.get()) {
            getDataChildren.add(getWithOriginNode());
        }

        DataContainerChild<?, ?>[] getDataChildrenArray = new DataContainerChild<?, ?>[getDataChildren.size()];
        getDataChildrenArray = getDataChildren.toArray(getDataChildrenArray);

        return domRpcService.invokeRpc(toPath(NETCONF_GET_DATA_QNAME),
                NetconfMessageTransformUtil.wrap(NETCONF_GET_DATA_QNAME, getDataChildrenArray));
    }

    @Override
    public ListenableFuture<? extends DOMRpcResult> editData(QName targetDatastore,
                                                             Optional<NormalizedNode<?, ?>> data,
                                                             YangInstanceIdentifier dataPath,
                                                             Optional<ModifyAction> dataModifyActionAttribute,
                                                             Optional<ModifyAction> defaultModifyAction) {
        Preconditions.checkNotNull(targetDatastore);

        NormalizedNode<?, ?> editNNContent = ImmutableNodes.fromInstanceId(schemaContext, dataPath, data.get());
        QName nodeType = editNNContent.getNodeType();
        Optional<DataSchemaNode> dataTreeChild = schemaContext.findDataTreeChild(nodeType);

        final NormalizedMetadata metadata = dataModifyActionAttribute.map(oper -> leafMetadata(dataPath, oper)).orElse(null);

        final AnydataNode<NormalizedAnydata> editContent = ImmutableAnydataNodeBuilder.create(NormalizedAnydata.class)
                .withNodeIdentifier(NETCONF_EDIT_DATA_CONFIG_NODEID)
                .withValue(new ImmutableMetadataNormalizedAnydata(schemaContext, dataTreeChild.get(), editNNContent, metadata)).build();

        ChoiceNode editStructure = Builders.choiceBuilder().withNodeIdentifier(toId(EditContent.QNAME))
                .withChild(editContent).build();

        Preconditions.checkNotNull(editStructure);

        return domRpcService.invokeRpc(toPath(NETCONF_EDIT_DATA_QNAME),
                NetconfMessageTransformUtil.wrap(NETCONF_EDIT_DATA_QNAME,
                        getDatastoreNode(targetDatastore), getDefaultOperationNode(dataModifyActionAttribute.get()), editStructure));
    }

    private DataContainerChild<?, ?> getDatastoreNode(QName datastore) {
        return Builders.leafBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "datastore")))
                .withValue(datastore).build();
    }

    private DataContainerChild<?, ?> getConfigFilterNode(Boolean configFilter) {
        return Builders.leafBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "config-filter")))
                .withValue(configFilter).build();
    }

    private DataContainerChild<?, ?> getMaxDepthNode(Integer maxDepth) {
        return Builders.leafBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "max-depth")))
                .withValue(maxDepth).build();
    }

    private DataContainerChild<?, ?> getOriginFilterNode(Set<QName> originFilter) {
        List<LeafSetEntryNode<Object>> leafSetEntryNodes = new ArrayList<>();
        originFilter.forEach(originFilterEntry -> {
            leafSetEntryNodes.add(Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new YangInstanceIdentifier
                            .NodeWithValue(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "origin-filter"), originFilterEntry))
                    .withValue(originFilterEntry)
                    .build());
        });
        return Builders.leafSetBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "origin-filter")))
                .withValue(leafSetEntryNodes).build();
    }

    private DataContainerChild<?, ?> getNegatedOriginFilterNode(Set<QName> negatedOriginFilter) {
        List<LeafSetEntryNode<Object>> leafSetEntryNodes = new ArrayList<>();
        negatedOriginFilter.forEach(negatedOriginFilterEntry -> {
            leafSetEntryNodes.add(Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new YangInstanceIdentifier
                            .NodeWithValue(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "negated-origin-filter"), negatedOriginFilterEntry))
                    .withValue(negatedOriginFilterEntry)
                    .build());
        });
        return Builders.leafSetBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "negated-origin-filter")))
                .withValue(leafSetEntryNodes).build();
    }

    private DataContainerChild<?, ?> getWithOriginNode() {
        return Builders.leafBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "with-origin")))
                .withValue(Empty.getInstance())
                .build();
    }

    private DataContainerChild<?, ?> getDefaultOperationNode(ModifyAction defaultModifyAction) {
        final String opString = defaultModifyAction.name().toLowerCase();
        return Builders.leafBuilder().withNodeIdentifier(toId(QName.create(NETCONF_NMDA_EXTENSION_QNAME, "default-operation")))
                .withValue(opString).build();
    }

    private NormalizedMetadata leafMetadata(YangInstanceIdentifier path, final ModifyAction oper) {
        final List<YangInstanceIdentifier.PathArgument> args = path.getPathArguments();
        final Deque<ImmutableNormalizedMetadata.Builder> builders = new ArrayDeque<>(args.size());

        // Step one: open builders
        for (YangInstanceIdentifier.PathArgument arg : args) {
            builders.push(ImmutableNormalizedMetadata.builder().withIdentifier(arg));
        }

        // Step two: set the top builder's metadata
        builders.peek().withAnnotation(NETCONF_OPERATION_QNAME_LEGACY, oper.toString().toLowerCase(Locale.US));

        // Step three: build the tree
        while (true) {
            final ImmutableNormalizedMetadata currentMeta = builders.pop().build();
            final ImmutableNormalizedMetadata.Builder parent = builders.peek();
            if (parent != null) {
                parent.withChild(currentMeta);
            } else {
                return currentMeta;
            }
        }
    }
}
