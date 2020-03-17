/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import com.google.common.base.Strings;
import io.lighty.codecs.api.ConverterUtils;
import io.lighty.codecs.api.NodeConverter;
import io.lighty.codecs.api.SerializationException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URISyntaxException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlNodeConverterTest extends AbstractCodecTest {
    private static final Logger LOG = LoggerFactory.getLogger(XmlNodeConverterTest.class);

    private final NodeConverter bindingSerializer;

    public XmlNodeConverterTest() {
        bindingSerializer = new XmlNodeConverter(schemaContext);
    }

    @Test
    public void testSerializeRpc_in() throws Exception {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        Writer serializeRpc =
                bindingSerializer.serializeRpc(loadRpc.get().getInput(), testedSimpleRpcInputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeRpc.toString()));
        LOG.info(serializeRpc.toString());
    }

    @Test
    public void testSerializeRpc_out() throws Exception {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        Writer serializeRpc =
                bindingSerializer.serializeRpc(loadRpc.get().getOutput(), testedSimpleRpcOutputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeRpc.toString()));
        LOG.info(serializeRpc.toString());
    }

    @Test
    public void testSerializeData() throws Exception {
        Writer serializeData = bindingSerializer.serializeData(schemaContext, testedToasterNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeData.toString()));
        LOG.info(serializeData.toString());
    }

    @Test
    public void testDeserializeData() throws Exception {
        final DataSchemaNode schemaNode = DataSchemaContextTree.from(this.schemaContext)
                .getChild(YangInstanceIdentifier.of(Toaster.QNAME)).getDataSchemaNode();

        NormalizedNode<?, ?> deserializeData =
                bindingSerializer.deserialize(schemaNode, new StringReader(loadToasterXml()));
        Assert.assertNotNull(deserializeData);
        LOG.info(deserializeData.toString());
    }

    @Test
    public void testDeserializeRpc_in() throws Exception {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        String loadMakeToasterInputXml = loadResourceAsString("input-output-rpc-in.xml");
        NormalizedNode<?, ?> deserializeRpc =
                bindingSerializer.deserialize(loadRpc.get().getInput(), new StringReader(loadMakeToasterInputXml));
        Assert.assertNotNull(deserializeRpc);
        LOG.info(deserializeRpc.toString());
    }

    @Test
    public void testDeserializeRpc_out() throws Exception {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        String loadMakeToasterInputXml = loadResourceAsString("input-output-rpc-out.xml");
        NormalizedNode<?, ?> deserializeRpc =
                bindingSerializer.deserialize(loadRpc.get().getOutput(), new StringReader(loadMakeToasterInputXml));
        Assert.assertNotNull(deserializeRpc);
        LOG.info(deserializeRpc.toString());
    }

    @Test
    public void testLoadNotification() throws Exception {
        Optional<NotificationDefinition> loadNotification =
                ConverterUtils.loadNotification(schemaContext, QName.create(TOASTER_NAMESPACE, TOASTER_REVISION,
                        "toasterRestocked"));
        Assert.assertNotNull(loadNotification);
        Assert.assertTrue(loadNotification.isPresent());
    }

    @Test
    @Ignore
    public void testSerializeNotification() throws Exception {
        String notification = loadResourceAsString("notification.xml");
        Optional<NotificationDefinition> loadNotification =
                ConverterUtils.loadNotification(schemaContext, QName.create(TOASTER_NAMESPACE, TOASTER_REVISION,
                        "toasterRestocked"));
        NormalizedNode<?, ?> nodes =
                bindingSerializer.deserialize(loadNotification.get(), new StringReader(notification));
        Assert.assertNotNull(nodes);
    }

    @Test
    public void testSerializeData_container() throws SerializationException {
        final DataSchemaNode schemaNode = DataSchemaContextTree.from(this.schemaContext)
                .getChild(YangInstanceIdentifier.of(TopLevelContainer.QNAME)).getDataSchemaNode();

        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(schemaNode,
                new StringReader(loadResourceAsString("top-level-container.xml")));
        Assert.assertNotNull(deserializeData);
        LOG.info(deserializeData.toString());
    }

    @Test
    public void testSerializeData_container_rpc() throws SerializationException {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, CONTAINER_IO_RPC_QNAME);
        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(loadRpc.get().getInput(),
                new StringReader(loadResourceAsString("container-io-rpc.xml")));
        Assert.assertNotNull(deserializeData);
    }

    @Test
    public void testSerializeData_list() throws SerializationException {
        SchemaNode schemaNode =
                ConverterUtils.getSchemaNode(schemaContext, SAMPLES_NAMESPACE, SAMPLES_REVISION, "sample-list");
        Writer serializedData = bindingSerializer.serializeData(schemaNode, testedSampleListNormalizedNodes);
        Assert.assertNotNull(serializedData.toString());
    }

    @Test
    public void testDeserializeData_list_single() throws SerializationException {
        SchemaNode schemaNode =
                ConverterUtils.getSchemaNode(schemaContext, SAMPLES_NAMESPACE, SAMPLES_REVISION, "sample-list");
        NormalizedNode<?, ?> serializedData =
                bindingSerializer.deserialize(schemaNode, new StringReader(loadResourceAsString("sample-list.xml")));
        Assert.assertNotNull(serializedData.toString());
    }

    @Test
    public void testDeserializeData_list_multiple() throws SerializationException, URISyntaxException {
        NormalizedNode<?, ?> serializedData = bindingSerializer.deserialize(schemaContext,
                new StringReader(loadResourceAsString("sample-list-multiple.xml")));
        Assert.assertNotNull(serializedData.toString());
    }
}
