/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import static org.junit.Assert.assertFalse;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.Assert;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeTypes;
import org.opendaylight.mdsal.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotBuilder;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcInput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcOutput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.spi.node.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.spi.node.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.spi.node.impl.ImmutableSystemMapNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
import org.xml.sax.SAXException;

public abstract class AbstractCodecTest {
    protected static final QName MAKE_TOAST_RPC_QNAME = qOfToasterModel("make-toast");
    protected static final QName CONTAINER_RPC_QNAME = qOfTestModel("container-io-rpc");
    protected static final QName LEAF_RPC_QNAME = qOfTestModel("simple-input-output-rpc");
    protected static final QName NOTIFICATION_QNAME = qOfToasterModel("toasterRestocked");

    protected final NormalizedNode toasterTopLevelContainerNode;
    protected final NormalizedNode innerContainerNode;

    protected final NormalizedNode rpcLeafInputNode;
    protected final NormalizedNode rpcLeafOutputNode;

    protected final NormalizedNode notificationNode;

    protected final NormalizedNode listEntryNode;
    protected final NormalizedNode listNode;

    protected final BindingCodecContext bindingCodecContext;
    protected final EffectiveModelContext effectiveModelContext;

    public AbstractCodecTest() throws YangParserException {
        this.bindingCodecContext = createCodecContext(loadModuleInfos());
        this.effectiveModelContext = bindingCodecContext.getRuntimeContext().modelContext();

        this.toasterTopLevelContainerNode = topLevelContainerNode();
        this.rpcLeafInputNode = rpcLeafInputNode();
        this.rpcLeafOutputNode = rpcLeafOutputNode();
        this.notificationNode = notificationContainer();
        this.listEntryNode = listEntryNode();
        this.listNode = listNode();
        this.innerContainerNode = innerContainerNode();
    }

    private static BindingCodecContext createCodecContext(final List<YangModuleInfo> moduleInfos)
            throws YangParserException {
        final YangXPathParserFactory xpathFactory = new AntlrXPathParserFactory();
        final DefaultYangParserFactory defaultYangParserFactory = new DefaultYangParserFactory(xpathFactory);
        final DefaultBindingRuntimeGenerator bindingRuntimeGenerator = new DefaultBindingRuntimeGenerator();
        final ModuleInfoSnapshotBuilder moduleInfoSnapshotBuilder = new ModuleInfoSnapshotBuilder(
                defaultYangParserFactory);
        moduleInfoSnapshotBuilder.add(moduleInfos);
        final BindingRuntimeTypes bindingRuntimeTypes = bindingRuntimeGenerator
                .generateTypeMapping(moduleInfoSnapshotBuilder.build().modelContext());

        final DefaultBindingRuntimeContext defaultBindingRuntimeContext = new DefaultBindingRuntimeContext(
                bindingRuntimeTypes, moduleInfoSnapshotBuilder.build());
        return new BindingCodecContext(defaultBindingRuntimeContext);
    }

    protected static String loadResourceAsString(final String fileName) {
        URL resource = Resources.getResource(fileName);
        try {
            return Resources.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load toaster xml file", e);
        }
    }

    /**
     * Helper method for loading {@link YangModuleInfo}s from the classpath.
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

    private static NormalizedNode topLevelContainerNode() {
        return new ImmutableContainerNodeBuilder().withNodeIdentifier(NodeIdentifier.create(Toaster.QNAME))
                .withValue(List.of(
                        ImmutableNodes.leafNode(
                                NodeIdentifier.create(qOfToasterModel("toasterManufacturer")), "manufacturer"),
                        ImmutableNodes.leafNode(
                                NodeIdentifier.create(qOfToasterModel("toasterStatus")), ToasterStatus.Up.getName()),
                        ImmutableNodes.leafNode(
                                NodeIdentifier.create(qOfToasterModel("darknessFactor")), 50)))
                .build();
    }

    private static NormalizedNode rpcLeafInputNode() {
        return new ImmutableContainerNodeBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SimpleInputOutputRpcInput.QNAME))
                .withChild(ImmutableNodes.leafNode(
                        NodeIdentifier.create(qOfTestModel("input-obj")), "testValue"))
                .build();
    }

    private static NormalizedNode rpcLeafOutputNode() {
        return new ImmutableContainerNodeBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SimpleInputOutputRpcOutput.QNAME))
                .withChild(ImmutableNodes.leafNode(
                        NodeIdentifier.create(qOfTestModel("output-obj")), "testValue"))
                .build();
    }

    private static NormalizedNode notificationContainer() {
        return new ImmutableContainerNodeBuilder()
                .withNodeIdentifier(NodeIdentifier.create(ToasterRestocked.QNAME))
                .withChild(ImmutableNodes.leafNode(
                        NodeIdentifier.create(qOfToasterModel("amountOfBread")), 1)).build();
    }

    private static NormalizedNode listEntryNode() {
        final QName key = qOfTestModel("name");
        return new ImmutableMapEntryNodeBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(
                        SampleList.QNAME, key, "nameValue"))
                .withValue(List.of(
                        ImmutableNodes.leafNode(NodeIdentifier.create(key), "nameValue"),
                        ImmutableNodes.leafNode(NodeIdentifier
                                .create(qOfTestModel("value")), 1)))
                .build();
    }

    private static NormalizedNode listNode() {
        return new ImmutableSystemMapNodeBuilder().withNodeIdentifier(new NodeIdentifier(SampleList.QNAME))
                .withChild((MapEntryNode) listEntryNode()).build();
    }

    private static NormalizedNode innerContainerNode() {
        return new ImmutableContainerNodeBuilder().withNodeIdentifier(NodeIdentifier.create(SampleContainer.QNAME))
                .withValue(List.of(
                    org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes.leafNode(
                                NodeIdentifier.create(qOfTestModel("name")), "name")))
                .build();
    }

    protected static QName qOfTestModel(final String localName) {
        return org.opendaylight.yang.svc.v1.http.pantheon.tech.ns.test.models.rev180119.YangModuleInfoImpl
                .qnameOf(localName);
    }

    protected static QName qOfToasterModel(final String localName) {
        return org.opendaylight.yang.svc.v1.http.netconfcentral.org.ns.toaster.rev091120.YangModuleInfoImpl
                .qnameOf(localName);
    }

    protected static void assertValidJson(final String json) {
        assertFalse(Strings.isNullOrEmpty(json));
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            Assert.fail(String.format("XML %s is not valid, reason: %s", json, e));
        }
    }

    protected static void assertValidXML(final String xml) {
        assertFalse(Strings.isNullOrEmpty(xml));
        try {
            XmlUtil.readXmlToDocument(xml);
        } catch (SAXException | IOException e) {
            Assert.fail(String.format("XML %s is not valid, reason: %s", xml, e));
        }
    }

}
