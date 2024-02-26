/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.NETCONF_RUNNING_NODEID;
import static org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil.toId;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.netconf.api.EffectiveOperation;
import org.opendaylight.netconf.api.NamespaceURN;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastores.rev180214.Running;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.nmda.rev190107.edit.data.input.EditContent;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnydataNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedAnydata;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedMetadata;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.YangInstanceIdentifierWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedMetadata;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.data.spi.node.impl.ImmutableAnydataNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

public class NetconfNmdaBaseServiceImpl extends NetconfBaseServiceImpl implements NetconfNmdaBaseService {

    public static final QName NETCONF_NMDA_EXTENSION_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:ietf-netconf-nmda", "2019-01-07", "ietf-netconf-nmda").intern();
    public static final QName NETCONF_GET_DATA_QNAME =
            QName.create(NETCONF_NMDA_EXTENSION_QNAME, "get-data").intern();
    public static final QName NETCONF_EDIT_DATA_QNAME =
            QName.create(NETCONF_NMDA_EXTENSION_QNAME, "edit-data").intern();
    private static final NodeIdentifier NETCONF_FILTER_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "subtree-filter").intern());
    private static final NodeIdentifier NETCONF_FILTER_CHOICE_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "filter-spec").intern());
    private static final NodeIdentifier NETCONF_ORIGIN_FILTERS_CHOICE_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "origin-filters").intern());
    private static final NodeIdentifier NETCONF_DATASTORE_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "datastore").intern());
    private static final NodeIdentifier NETCONF_CONFIG_FILTER_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "config-filter").intern());
    private static final NodeIdentifier NETCONF_MAX_DEPTH_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "max-depth").intern());
    private static final NodeIdentifier NETCONF_ORIGIN_FILTER_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "origin-filter").intern());
    private static final NodeIdentifier NETCONF_NEGATED_ORIGIN_FILTER_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "negated-origin-filter").intern());
    private static final NodeIdentifier NETCONF_WITH_ORIGIN_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_GET_DATA_QNAME, "with-origin").intern());

    private static final QName NETCONF_OPERATION_QNAME_LEGACY = QName.create(NamespaceURN.BASE,
            XmlNetconfConstants.OPERATION_ATTR_KEY).intern();
    private static final NodeIdentifier NETCONF_EDIT_DATA_CONFIG_NODEID =
            NodeIdentifier.create(QName.create(NETCONF_EDIT_DATA_QNAME, "config").intern());

    public NetconfNmdaBaseServiceImpl(NodeId nodeId, DOMRpcService domRpcService,
            EffectiveModelContext effectiveModelContext) {
        super(nodeId, domRpcService, effectiveModelContext);
    }

    @Override
    public ListenableFuture<? extends DOMRpcResult> getData(QName sourceDatastore,
                                                            Optional<YangInstanceIdentifier> filterYII,
                                                            Optional<Boolean> configFilter,
                                                            Optional<Integer> maxDepth,
                                                            Optional<Set<QName>> originFilter,
                                                            Optional<Boolean> negateOriginFilter,
                                                            Optional<Boolean> withOrigin) {
        final List<DataContainerChild> getDataChildren = new ArrayList<>();
        getDataChildren.add(getDatastoreNode(requireNonNull(sourceDatastore)));
        filterYII.ifPresent(yangIID -> getDataChildren.add(getFilterSpecChoiceNode(yangIID)));
        configFilter.ifPresent(configFilterValue -> getDataChildren.add(getConfigFilterNode(configFilterValue)));

        originFilter.ifPresent(originFilterQNames -> getDataChildren.add(getOriginFilterSpecChoiceNode(
                originFilterQNames, negateOriginFilter.isPresent() && negateOriginFilter.get())));

        maxDepth.ifPresent(maxDepthValue -> getDataChildren.add(getMaxDepthNode(maxDepthValue)));
        if (withOrigin.isPresent() && withOrigin.get()) {
            getDataChildren.add(getWithOriginNode());
        }

        DataContainerChild[] getDataChildrenArray = new DataContainerChild[getDataChildren.size()];
        getDataChildrenArray = getDataChildren.toArray(getDataChildrenArray);

        return getDOMRpcService().invokeRpc(NETCONF_GET_DATA_QNAME,
                NetconfMessageTransformUtil.wrap(NodeIdentifier.create(NETCONF_GET_DATA_QNAME), getDataChildrenArray));
    }

    @Override
    public ListenableFuture<? extends DOMRpcResult> editData(QName targetDatastore,
                                                             Optional<NormalizedNode> data,
                                                             YangInstanceIdentifier dataPath,
                                                             Optional<EffectiveOperation> dataEffectiveOperation,
                                                             Optional<EffectiveOperation> defaultEffectiveOperation) {

        final var parentPath = dataPath.isEmpty() ? dataPath : dataPath.coerceParent();
        final var result = new NormalizationResultHolder();
        try (var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result)) {
            try (var iidWriter = YangInstanceIdentifierWriter.open(streamWriter,
                    getEffectiveModelContext(), parentPath);
                 var nnWriter = NormalizedNodeWriter.forStreamWriter(streamWriter)) {
                nnWriter.write(data.orElseThrow(() -> new NoSuchElementException("Data is missing")));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to convert " + dataPath, e);
        }
        final NormalizedNode editNNContent = result.getResult().data();

        final NormalizedMetadata metadata = dataEffectiveOperation
                .map(oper -> leafMetadata(dataPath, oper))
                .orElse(null);
        final SchemaInferenceStack stack = SchemaInferenceStack.of(getEffectiveModelContext());
        stack.enterSchemaTree(editNNContent.name().getNodeType());
        final AnydataNode<NormalizedAnydata> editContent = new ImmutableAnydataNodeBuilder<>(NormalizedAnydata.class)
                .withNodeIdentifier(NETCONF_EDIT_DATA_CONFIG_NODEID)
                .withValue(NormalizedAnydata.of(stack.toInference(), editNNContent, metadata))
                .build();

        ChoiceNode editStructure = Builders.choiceBuilder().withNodeIdentifier(toId(EditContent.QNAME))
                .withChild(editContent).build();
        Preconditions.checkNotNull(editStructure);
        return getDOMRpcService().invokeRpc(NETCONF_EDIT_DATA_QNAME,
                NetconfMessageTransformUtil.wrap(NETCONF_EDIT_DATA_QNAME,
                        getDatastoreNode(requireNonNull(targetDatastore)),
                        getDefaultOperationNode(defaultEffectiveOperation.orElseThrow(() ->
                                new NoSuchElementException("Default Modify Action is missing"))), editStructure));
    }

    @Override
    public ListenableFuture<? extends DOMRpcResult> deleteConfig(QName targetDatastore) {
        if (Running.QNAME.equals(targetDatastore)) {
            targetDatastore = NETCONF_RUNNING_NODEID.getNodeType();
        }
        return super.deleteConfig(targetDatastore);
    }

    private DataContainerChild getDatastoreNode(QName datastore) {
        return Builders.leafBuilder().withNodeIdentifier(NETCONF_DATASTORE_NODEID)
                .withValue(datastore).build();
    }

    private DataContainerChild getConfigFilterNode(Boolean configFilter) {
        return Builders.leafBuilder().withNodeIdentifier(NETCONF_CONFIG_FILTER_NODEID)
                .withValue(configFilter).build();
    }

    private DataContainerChild getMaxDepthNode(Integer maxDepth) {
        return Builders.leafBuilder().withNodeIdentifier(NETCONF_MAX_DEPTH_NODEID)
                .withValue(maxDepth).build();
    }

    private DataContainerChild getOriginFilterNode(Set<QName> originFilter) {
        List<LeafSetEntryNode<Object>> leafSetEntryNodes = new ArrayList<>();
        originFilter.forEach(originFilterEntry -> {
            leafSetEntryNodes.add((LeafSetEntryNode<Object>) Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new NodeWithValue(NETCONF_ORIGIN_FILTER_NODEID.getNodeType(),
                            originFilterEntry))
                    .withValue(originFilterEntry)
                    .build());
        });
        return Builders.leafSetBuilder().withNodeIdentifier(NETCONF_ORIGIN_FILTER_NODEID)
                .withValue(leafSetEntryNodes).build();
    }

    private DataContainerChild getNegatedOriginFilterNode(Set<QName> negatedOriginFilter) {
        List<LeafSetEntryNode<Object>> leafSetEntryNodes = new ArrayList<>();
        negatedOriginFilter.forEach(negatedOriginFilterEntry -> {
            leafSetEntryNodes.add((LeafSetEntryNode<Object>) Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new NodeWithValue(NETCONF_NEGATED_ORIGIN_FILTER_NODEID.getNodeType(),
                            negatedOriginFilterEntry))
                    .withValue(negatedOriginFilterEntry)
                    .build());
        });
        return Builders.leafSetBuilder().withNodeIdentifier(NETCONF_NEGATED_ORIGIN_FILTER_NODEID)
                .withValue(leafSetEntryNodes).build();
    }

    private DataContainerChild getWithOriginNode() {
        return Builders.leafBuilder().withNodeIdentifier(NETCONF_WITH_ORIGIN_NODEID)
                .withValue(Empty.value())
                .build();
    }

    private DataContainerChild getDefaultOperationNode(EffectiveOperation defaultEffectiveOperation) {
        final String opString = defaultEffectiveOperation.name().toLowerCase(Locale.US);
        return Builders.leafBuilder().withNodeIdentifier(NodeIdentifier.create(
                        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.nmda.rev190107
                                .YangModuleInfoImpl.qnameOf("default-operation")))
                .withValue(opString).build();
    }

    private NormalizedMetadata leafMetadata(YangInstanceIdentifier path, final EffectiveOperation oper) {
        final List<PathArgument> args = path.getPathArguments();
        final Deque<ImmutableNormalizedMetadata.Builder> builders = new ArrayDeque<>(args.size());

        // Step one: open builders
        builders.push(ImmutableNormalizedMetadata.builder());

        // Step two: set the top builder's metadata
        Optional.ofNullable(builders.peek())
                .ifPresent(builder -> builder.withAnnotation(
                        NETCONF_OPERATION_QNAME_LEGACY,
                        oper.toString().toLowerCase(Locale.US)));

        // Step three: build the tree
        while (true) {
            final ImmutableNormalizedMetadata currentMeta = builders.pop().build();
            final ImmutableNormalizedMetadata.Builder parent = builders.peek();
            if (parent != null) {
                for (PathArgument arg : args) {
                    parent.withChild(arg,parent.build());
                }
            } else {
                return currentMeta;
            }
        }
    }

    private ChoiceNode getOriginFilterSpecChoiceNode(final Set<QName> originFilter, final boolean negateOriginFilter) {
        final DataContainerChild originFilterChild;
        if (negateOriginFilter) {
            originFilterChild = getNegatedOriginFilterNode(originFilter);
        } else {
            originFilterChild = getOriginFilterNode(originFilter);
        }
        return Builders.choiceBuilder()
                .withNodeIdentifier(NETCONF_ORIGIN_FILTERS_CHOICE_NODEID)
                .withChild(originFilterChild)
                .build();
    }

    private ChoiceNode getFilterSpecChoiceNode(final YangInstanceIdentifier filterYII) {
        final NormalizedNode filterNN = ImmutableNodes.fromInstanceId(getEffectiveModelContext(), filterYII);
        final SchemaInferenceStack stack = SchemaInferenceStack.of(getEffectiveModelContext());
        stack.enterSchemaTree(filterNN.name().getNodeType());
        final AnydataNode<NormalizedAnydata> subtreeFilter =
                (AnydataNode<NormalizedAnydata>) new ImmutableAnydataNodeBuilder(NormalizedAnydata.class)
                        .withNodeIdentifier(NETCONF_FILTER_NODEID)
                        .withValue(NormalizedAnydata.of(stack.toInference(), filterNN))
                        .build();
        return Builders.choiceBuilder()
                .withNodeIdentifier(NETCONF_FILTER_CHOICE_NODEID)
                .withChild(subtreeFilter)
                .build();
    }
}
