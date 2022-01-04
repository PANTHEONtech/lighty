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
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcInput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcOutput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;

public class JsonNodeConverterTest extends AbstractCodecTest {

    private final NodeConverter bindingSerializer;

    public JsonNodeConverterTest() throws YangParserException {
        bindingSerializer = new JsonNodeConverter(this.effectiveModelContext);
    }

    @Test
    public void testSerializeRpcLeafInput() throws SerializationException {
        final Writer serializedRpc = bindingSerializer.serializeRpc(rpcLeafInputNode,
                LEAF_RPC_QNAME, SimpleInputOutputRpcInput.QNAME);
        assertValidJson(serializedRpc.toString());
    }

    @Test
    public void testSerializeRpcLeafOutput() throws SerializationException {
        final Writer serializedRpc = bindingSerializer.serializeRpc(rpcLeafOutputNode,
                LEAF_RPC_QNAME, SimpleInputOutputRpcOutput.QNAME);
        assertValidJson(serializedRpc.toString());
    }

    @Test
    public void testSerializeToasterData() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(toasterTopLevelContainerNode);
        assertValidJson(serializeData.toString());
    }

    @Test
    public void testSerializeInnerContainer() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(innerContainerNode, TopLevelContainer.QNAME);
        assertValidJson(serializeData.toString());
    }

    @Test
    public void testSerializeList() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(listNode);
        assertValidJson(serializeData.toString());
    }

    @Test
    public void testSerializeListEntry() throws SerializationException {
        final Writer serializedData = bindingSerializer.serializeData(listEntryNode, SampleList.QNAME);
        assertValidJson(serializedData.toString());
    }

    @Test
    public void testDeserializeData() throws DeserializationException {
        final NormalizedNode deserializeData = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("toaster-container.json")));
        assertNotNull(deserializeData);
    }

    @Test
    public void testDeserializeRpcInput() throws DeserializationException {
        final NormalizedNode deserializeRpc = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("rpc-leaf-Input.json")), LEAF_RPC_QNAME);
        assertNotNull(deserializeRpc);
    }

    @Test
    public void testDeserializeRpcOutput() throws DeserializationException {
        final NormalizedNode deserializeRpc = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("rpc-leaf-output.json")), LEAF_RPC_QNAME);
        assertNotNull(deserializeRpc);
    }

    @Test
    public void testDeserializeRpcContainerInput() throws DeserializationException {
        final NormalizedNode deserializeData = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("rpc-container-input.json")), CONTAINER_RPC_QNAME);
        assertNotNull(deserializeData);
    }

    @Test
    public void testDeserializeTopLevelContainer() throws DeserializationException {
        final NormalizedNode deserializeContainer = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("top-level-container.json")));
        assertNotNull(deserializeContainer);
    }

    @Test
    public void testDeserializeInnerContainer() throws DeserializationException {
        final NormalizedNode deserializeContainer = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("inner-container.json")), TopLevelContainer.QNAME);
        assertNotNull(deserializeContainer);
    }

    @Test
    public void testDeserializeInnerLeaf() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("inner-leaf.json")),
                TopLevelContainer.QNAME, SampleContainer.QNAME);
        assertNotNull(result);
    }

    @Test
    public void testDeserializeListSingleEntry() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("single-list-entry.json")));
        assertNotNull(result);
    }

    @Test
    public void testDeserializeListMultipleEntries() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("multiple-list-entries.json")));
        assertNotNull(result);
    }


}
