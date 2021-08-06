/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import com.google.common.base.Strings;
import java.io.StringReader;
import java.io.Writer;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;

public class XmlNodeConverterTest extends AbstractCodecTest {

    private final NodeConverter bindingSerializer;

    public XmlNodeConverterTest() throws YangParserException {
        bindingSerializer = new XmlNodeConverter(this.effectiveModelContext);
    }

    @Test
    public void testSerializeRpc_in() throws Exception {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        Writer serializeRpc =
                bindingSerializer.serializeRpc(loadRpc.orElseThrow().getInput(), testedSimpleRpcInputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeRpc.toString()));
    }

    @Test
    public void testSerializeRpc_out() throws Exception {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        Writer serializeRpc =
                bindingSerializer.serializeRpc(loadRpc.orElseThrow().getOutput(), testedSimpleRpcOutputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeRpc.toString()));
    }

    @Test
    public void testSerializeData() throws Exception {
        Writer serializeData =
                bindingSerializer.serializeData(this.effectiveModelContext, testedToasterNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeData.toString()));
    }

    @Test
    public void testDeserializeData() throws Exception {
        final DataSchemaNode schemaNode = DataSchemaContextTree.from(this.effectiveModelContext)
                .findChild(YangInstanceIdentifier.of(Toaster.QNAME)).orElseThrow().getDataSchemaNode();

        NormalizedNode<?, ?> deserializeData =
                bindingSerializer.deserialize(schemaNode, new StringReader(loadToasterXml()));
        Assert.assertNotNull(deserializeData);
    }

    @Test
    public void testDeserializeRpc_in() throws Exception {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        String loadMakeToasterInputXml = loadResourceAsString("input-output-rpc-in.xml");
        NormalizedNode<?, ?> deserializeRpc = bindingSerializer
                .deserialize(loadRpc.orElseThrow().getInput(), new StringReader(loadMakeToasterInputXml));
        Assert.assertNotNull(deserializeRpc);
    }

    @Test
    public void testDeserializeRpc_out() throws Exception {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        String loadMakeToasterInputXml = loadResourceAsString("input-output-rpc-out.xml");
        NormalizedNode<?, ?> deserializeRpc = bindingSerializer
                .deserialize(loadRpc.orElseThrow().getOutput(), new StringReader(loadMakeToasterInputXml));
        Assert.assertNotNull(deserializeRpc);
    }

    @Test
    public void testLoadNotification() {
        Optional<? extends NotificationDefinition> loadNotification =
                ConverterUtils.loadNotification(this.effectiveModelContext,
                        QName.create(TOASTER_NAMESPACE, TOASTER_REVISION, "toasterRestocked"));
        Assert.assertNotNull(loadNotification);
        Assert.assertTrue(loadNotification.isPresent());
    }

    @Test
    @Ignore
    public void testSerializeNotification() throws Exception {
        String notification = loadResourceAsString("notification.xml");
        Optional<? extends NotificationDefinition> loadNotification =
                ConverterUtils.loadNotification(this.effectiveModelContext,
                        QName.create(TOASTER_NAMESPACE, TOASTER_REVISION, "toasterRestocked"));
        NormalizedNode<?, ?> nodes =
                bindingSerializer.deserialize(loadNotification.orElseThrow(), new StringReader(notification));
        Assert.assertNotNull(nodes);
    }

    @Test
    public void testSerializeData_container() throws SerializationException {
        final DataSchemaNode schemaNode = DataSchemaContextTree.from(this.effectiveModelContext)
                .findChild(YangInstanceIdentifier.of(TopLevelContainer.QNAME)).orElseThrow().getDataSchemaNode();

        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(schemaNode,
                new StringReader(loadResourceAsString("top-level-container.xml")));
        Assert.assertNotNull(deserializeData);
    }

    @Test
    public void testSerializeData_container_rpc() throws SerializationException {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, CONTAINER_IO_RPC_QNAME);
        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(loadRpc.orElseThrow().getInput(),
                new StringReader(loadResourceAsString("container-io-rpc.xml")));
        Assert.assertNotNull(deserializeData);
    }

    @Test
    public void testSerializeData_list() throws SerializationException {
        SchemaNode schemaNode = ConverterUtils.getSchemaNode(this.effectiveModelContext, SAMPLES_NAMESPACE,
            SAMPLES_REVISION, "sample-list").orElseThrow().getDataSchemaNode();
        Writer serializedData = bindingSerializer.serializeData(schemaNode, testedSampleListNormalizedNodes);
        Assert.assertNotNull(serializedData.toString());
    }

    @Test
    public void testDeserializeData_list_single() throws SerializationException {
        SchemaNode schemaNode = ConverterUtils.getSchemaNode(this.effectiveModelContext, SAMPLES_NAMESPACE,
            SAMPLES_REVISION, "sample-list").orElseThrow().getDataSchemaNode();
        NormalizedNode<?, ?> serializedData = bindingSerializer
            .deserialize(schemaNode, new StringReader(loadResourceAsString("sample-list.xml")));
        Assert.assertNotNull(serializedData.toString());
    }

    @Test
    public void testDeserializeData_list_multiple() throws SerializationException {
        NormalizedNode<?, ?> serializedData = bindingSerializer.deserialize(this.effectiveModelContext,
                new StringReader(loadResourceAsString("sample-list-multiple.xml")));
        Assert.assertNotNull(serializedData.toString());
    }
}
