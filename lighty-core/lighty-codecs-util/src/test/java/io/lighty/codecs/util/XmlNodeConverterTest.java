/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import static org.junit.Assert.assertNotNull;

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import java.io.StringReader;
import java.io.Writer;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.ContainerIoRpcInput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcInput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcOutput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;

public class XmlNodeConverterTest extends AbstractCodecTest {

    private final NodeConverter bindingSerializer;

    public XmlNodeConverterTest() throws YangParserException {
        bindingSerializer = new XmlNodeConverter(this.effectiveModelContext);
    }

    @Test
    public void testSerializeRpcLeafInput() throws SerializationException {
        final Writer serializedRpc = bindingSerializer.serializeRpc(rpcLeafInputNode,
                LEAF_RPC_QNAME, SimpleInputOutputRpcInput.QNAME);
        assertValidXML(serializedRpc.toString());
    }

    @Test
    public void testSerializeRpcLeafOutput() throws SerializationException {
        final Writer serializedRpc = bindingSerializer.serializeRpc(rpcLeafOutputNode,
                LEAF_RPC_QNAME, SimpleInputOutputRpcOutput.QNAME);
        assertValidXML(serializedRpc.toString());
    }

    @Test
    public void testSerializeToasterData() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(toasterTopLevelContainerNode);
        assertValidXML(serializeData.toString());
    }

    @Test
    public void testSerializeInnerContainer() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(innerContainerNode, TopLevelContainer.QNAME);
        assertValidXML(serializeData.toString());
    }

    @Test
    public void testSerializeList() throws SerializationException {
        final Writer serializedData = bindingSerializer.serializeData(listNode);
        assertNotNull(serializedData.toString());
    }

    @Test
    public void testSerializeListEntry() throws SerializationException {
        final Writer serializedData = bindingSerializer.serializeData(
                listEntryNode, SampleList.QNAME);
        assertNotNull(serializedData.toString());
    }

    @Test
    public void testDeserializeData() throws DeserializationException {
        NormalizedNode deserializeData = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("toaster-container.xml")), Toaster.QNAME);
        assertNotNull(deserializeData);
    }

    @Test
    public void testDeserializeRpcInput() throws DeserializationException {
        final NormalizedNode deserializeRpc = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("rpc-leaf-input.xml")), LEAF_RPC_QNAME,
                SimpleInputOutputRpcInput.QNAME);
        assertNotNull(deserializeRpc);
    }

    @Test
    public void testDeserializeRpcOutput() throws DeserializationException {
        final NormalizedNode deserializeRpc = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("rpc-leaf-output.xml")), LEAF_RPC_QNAME,
                SimpleInputOutputRpcOutput.QNAME);
        assertNotNull(deserializeRpc);
    }

    @Test
    @Ignore("Not yet possible for ODL reasons")
    public void testDeserializeNotification() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("notification.xml")));
        assertNotNull(result);
    }

    @Test
    public void testDeserializeContainerRpcInput() throws DeserializationException {
        final NormalizedNode deserializeData = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("rpc-container-input.xml")),
                CONTAINER_RPC_QNAME, ContainerIoRpcInput.QNAME);
        assertNotNull(deserializeData);
    }

    @Test
    public void testDeserializeTopLevelContainer() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("top-level-container.xml")), TopLevelContainer.QNAME);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeInnerContainer() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("inner-container.xml")),
                TopLevelContainer.QNAME, SampleContainer.QNAME);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeInnerLeaf() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("inner-leaf.xml")),
                TopLevelContainer.QNAME, SampleContainer.QNAME, $YangModuleInfoImpl.qnameOf("name"));
        assertNotNull(result);
    }

    @Test
    public void testDeserializeListSingleEntry() throws DeserializationException {
        NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("single-list-entry.xml")), SampleList.QNAME);
        assertNotNull(result.toString());
    }

    @Test
    public void testDeserializeListMultipleEntries() throws DeserializationException {
        NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("multiple-list-entries.xml")));
        assertNotNull(result.toString());
    }

}
