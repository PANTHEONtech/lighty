/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterBuilder;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

public abstract class AbstractCodecTest {

    protected static final long EXPECTED_ONE = 1L;
    protected static final long COFFEE_VALUE = 0xC00FFEEL;
    protected static final String TOASTER_NAMESPACE = "http://netconfcentral.org/ns/toaster";
    protected static final String TOASTER_REVISION = "2009-11-20";

    protected static final String SAMPLES_NAMESPACE = "http://pantheon.tech/ns/test-models";
    protected static final String SAMPLES_REVISION = "2018-01-19";

    protected static final InstanceIdentifier<Toaster> TOASTER_INSTANCE_IDENTIFIER =
            InstanceIdentifier.create(Toaster.class);
    protected static final YangInstanceIdentifier TOASTER_YANG_INSTANCE_IDENTIFIER =
            YangInstanceIdentifier.of(Toaster.QNAME);

    protected static final QName SIMPLE_IO_RPC_QNAME =
            QName.create(SAMPLES_NAMESPACE, SAMPLES_REVISION, "simple-input-output-rpc");
    protected static final QName MAKE_TOAST_RPC_QNAME = QName.create(TOASTER_NAMESPACE, TOASTER_REVISION, "make-toast");
    protected static final QName CONTAINER_IO_RPC_QNAME =
            QName.create(SAMPLES_NAMESPACE, SAMPLES_REVISION, "container-io-rpc");

    // tested DataObject
    protected final Toaster testedToaster;
    // tested DataObject for RPC
    protected final MakeToastInput testedMakeToasterInput;
    // BI representation of testedToaster
    protected final NormalizedNode<?, ?> testedToasterNormalizedNodes;
    // BI representation of testedMakeToasterInput
    protected final NormalizedNode<?, ?> testedMakeToasterNormalizedNodes;

    protected final NormalizedNode<?, ?> testedSimpleRpcInputNormalizedNodes;
    protected final NormalizedNode<?, ?> testedSimpleRpcOutputNormalizedNodes;
    protected final NormalizedNode<?, ?> testedNotificationNormalizedNodes;
    protected final NormalizedNode<?, ?> testedSampleListNormalizedNodes;
    protected final NormalizedNode<?, ?> testedSampleMapNodeNormalizedNodes;

    // schema context loaded from classpath entries
    protected final SchemaContext schemaContext;

    private final List<YangModuleInfo> moduleInfos;
    private final ModuleInfoBackedContext moduleInfoBackedCntxt;

    public AbstractCodecTest() {
        this.moduleInfos = loadModuleInfos();
        this.moduleInfoBackedCntxt = ModuleInfoBackedContext.create();
        this.schemaContext = getSchemaContext(moduleInfos);

        this.testedToaster = new ToasterBuilder().setDarknessFactor(COFFEE_VALUE)
                .setToasterManufacturer(new DisplayString("manufacturer")).setToasterStatus(ToasterStatus.Up).build();
        this.testedMakeToasterInput = new MakeToastInputBuilder().setToasterDoneness(EXPECTED_ONE).build();

        this.testedMakeToasterNormalizedNodes = createMakeToasterInput();
        this.testedToasterNormalizedNodes = createToasterNormalizedNodes();

        this.testedSimpleRpcInputNormalizedNodes = simpleRpcInputNormalizedNodes_in();
        this.testedSimpleRpcOutputNormalizedNodes = simpleRpcInputNormalizedNodes_out();
        this.testedNotificationNormalizedNodes = toasterNotificationNormalizedNodes();
        this.testedSampleListNormalizedNodes = sampleListNormalizedNodes();
        this.testedSampleMapNodeNormalizedNodes = sampleMapNode();
    }

    /**
     * Utility method to create the {@link NodeIdentifier} for a node within the toaster module. The
     * namespace and version are given by this module.
     * 
     * @param nodeName of the node
     * @return created {@link NodeIdentifier}
     */
    protected static NodeIdentifier getNodeIdentifier(String namespace, String revision, String nodeName) {
        return new NodeIdentifier(getQName(namespace, revision, nodeName));
    }

    /**
     * Utility method to create the {@link QName} for a node within the toaster module
     * 
     * @param nodeName
     * @return created {@link QName}
     */
    protected static QName getQName(String namespace, String revision, String nodeName) {
        return QName.create(namespace, revision, nodeName);
    }

    protected static NodeIdentifier getToasterNodeIdentifier(String nodeName) {
        return getNodeIdentifier(TOASTER_NAMESPACE, TOASTER_REVISION, nodeName);
    }


    /**
     * Loads the XML file containing a sample {@link Toaster} object
     * 
     * <pre>
     * {@code
     * <toaster xmlns="http://netconfcentral.org/ns/toaster">
     *   <toasterManufacturer>manufacturer</toasterManufacturer>
     *   <toasterStatus>up</toasterStatus>
     *   <darknessFactor>201392110</darknessFactor>
     * </toaster>
     * }
     * </pre>
     * 
     * @return
     */
    protected static String loadToasterXml() {
        return loadResourceAsString("toaster.xml");
    }

    protected static String loadResourceAsString(String fileName) {
        URL resource = Resources.getResource(fileName);
        String loadedFileContent;
        try {
            loadedFileContent = Resources.asCharSource(resource, StandardCharsets.UTF_8).read();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load toaster xml file");
        }
        return loadedFileContent;
    }


    /**
     * Helper method for loading {@link YangModuleInfo}s from the classpath
     * 
     * @return {@link List} of loaded {@link YangModuleInfo}
     */
    private static List<YangModuleInfo> loadModuleInfos() {
        List<YangModuleInfo> moduleInfos = new LinkedList<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
        }
        return moduleInfos;
    }

    /**
     * Build the {@link SchemaContext} based on the loaded {@link YangModuleInfo}s
     * 
     * @param moduleInfos {@link List} of {@link YangModuleInfo}s to be used while creating
     *        {@link SchemaContext}
     * @return prepared {@link SchemaContext}
     */
    private SchemaContext getSchemaContext(List<YangModuleInfo> moduleInfos) {
        moduleInfoBackedCntxt.addModuleInfos(moduleInfos);
        Optional<SchemaContext> tryToCreateSchemaContext = moduleInfoBackedCntxt.tryToCreateSchemaContext();
        if (!tryToCreateSchemaContext.isPresent()) {
            throw new IllegalStateException();
        }
        return tryToCreateSchemaContext.get();
    }

    private static NormalizedNode<?, ?> createToasterNormalizedNodes() {
        NodeIdentifier toasterNodeIdentifier = new NodeIdentifier(Toaster.QNAME);
        LeafNode<String> manufacturer = new ImmutableLeafNodeBuilder<String>().withValue("manufacturer")
                .withNodeIdentifier(getToasterNodeIdentifier("toasterManufacturer")).build();
        LeafNode<String> toasterStatus = new ImmutableLeafNodeBuilder<String>().withValue(ToasterStatus.Up.getName())
                .withNodeIdentifier(getToasterNodeIdentifier("toasterStatus")).build();
        LeafNode<Long> darknessFactor = new ImmutableLeafNodeBuilder<Long>().withValue(COFFEE_VALUE)
                .withNodeIdentifier(getToasterNodeIdentifier("darknessFactor")).build();
        ContainerNode containerNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(toasterNodeIdentifier)
                .withValue(ImmutableList.of(manufacturer, darknessFactor, toasterStatus)).build();
        return containerNode;
    }

    private static NormalizedNode<?, ?> simpleRpcInputNormalizedNodes_in() {
        NodeIdentifier toasterNodeIdentifier =
                new NodeIdentifier(QName.create(SAMPLES_NAMESPACE, "2018-01-19", "input"));
        LeafNode<String> input = new ImmutableLeafNodeBuilder<String>()
                .withNodeIdentifier(new NodeIdentifier(QName.create(SAMPLES_NAMESPACE, "2018-01-19", "input-obj")))
                .withValue("a").build();
        ContainerNode containerNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(toasterNodeIdentifier)
                .withValue(ImmutableList.of(input)).build();
        return containerNode;
    }

    private static NormalizedNode<?, ?> simpleRpcInputNormalizedNodes_out() {
        NodeIdentifier toasterNodeIdentifier =
                new NodeIdentifier(QName.create(SAMPLES_NAMESPACE, "2018-01-19", "output"));
        LeafNode<String> input = new ImmutableLeafNodeBuilder<String>()
                .withNodeIdentifier(new NodeIdentifier(QName.create(SAMPLES_NAMESPACE, "2018-01-19", "output-obj")))
                .withValue("a").build();
        ContainerNode containerNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(toasterNodeIdentifier)
                .withValue(ImmutableList.of(input)).build();
        return containerNode;
    }
    
    private static NormalizedNode<?, ?> toasterNotificationNormalizedNodes() {
        NodeIdentifier toasterNodeIdentifier =
                new NodeIdentifier(QName.create(TOASTER_NAMESPACE, TOASTER_REVISION, "toasterRestocked"));
        LeafNode<Long> value = new ImmutableLeafNodeBuilder<Long>()
                .withNodeIdentifier(
                        new NodeIdentifier(QName.create(TOASTER_NAMESPACE, TOASTER_REVISION, "amountOfBread")))
                .withValue(1L).build();
        ContainerNode containerNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(toasterNodeIdentifier)
                .withValue(ImmutableList.of(value)).build();
        return containerNode;
    }

    /**
     * Builds the {@link NormalizedNode} representation of {@link DataCodecTest#testedMakeToasterInput}
     * 
     * @return {@link NormalizedNode} representation
     */
    private static NormalizedNode<?, ?> createMakeToasterInput() {
        NodeIdentifier toasterNodeIdentifier = new NodeIdentifier(MakeToastInput.QNAME);
        LeafNode<Long> doneness = new ImmutableLeafNodeBuilder<Long>().withValue(EXPECTED_ONE)
                .withNodeIdentifier(getToasterNodeIdentifier("toasterDoneness")).build();
        ContainerNode containerNode = ImmutableContainerNodeBuilder.create().withNodeIdentifier(toasterNodeIdentifier)
                .addChild(doneness).build();
        return containerNode;
    }

    private static NormalizedNode<?, ?> sampleListNormalizedNodes() {
        DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> create =
                ImmutableMapEntryNodeBuilder.create();
        QName keyQname = QName.create(SAMPLES_NAMESPACE, SAMPLES_REVISION, "name");
        QName valueQname = QName.create(SAMPLES_NAMESPACE, SAMPLES_REVISION, "value");
        NodeIdentifierWithPredicates nodeIdentifier = new NodeIdentifierWithPredicates(
                QName.create(SAMPLES_NAMESPACE, SAMPLES_REVISION, "sample-list"), ImmutableMap.of(keyQname, "name"));
        create.withNodeIdentifier(nodeIdentifier);
        LeafNode<String> name = new ImmutableLeafNodeBuilder<String>().withNodeIdentifier(new NodeIdentifier(keyQname))
                .withValue("name").build();
        LeafNode<Short> value = new ImmutableLeafNodeBuilder<Short>().withNodeIdentifier(new NodeIdentifier(valueQname))
                .withValue((short) 1).build();
        create.withValue(ImmutableList.of(name, value));
        return create.build();
    }

    private static NormalizedNode<?, ?> sampleMapNode() {
        return ImmutableMapNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(SampleList.QNAME))
                .withChild((MapEntryNode) sampleListNormalizedNodes()).build();
    }
}
