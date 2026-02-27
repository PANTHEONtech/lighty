/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import java.io.StringReader;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.ContainerIoRpcInput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.ContainerWithList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcInput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SimpleInputOutputRpcOutput;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.with.list.TestList;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;

class JsonNodeConverterTest extends AbstractCodecTest {

    private final NodeConverter bindingSerializer;

    JsonNodeConverterTest() throws YangParserException {
        bindingSerializer = new JsonNodeConverter(this.effectiveModelContext);
    }

    @Test
    void testSerializeRpcLeafInput() throws SerializationException {
        final Writer serializedRpc = bindingSerializer.serializeRpc(Absolute.of(LEAF_RPC_QNAME,
                SimpleInputOutputRpcInput.QNAME), rpcLeafInputNode);
        assertValidJson(serializedRpc.toString());
    }

    @Test
    void testSerializeRpcLeafOutput() throws SerializationException {
        final Writer serializedRpc = bindingSerializer.serializeRpc(Absolute.of(LEAF_RPC_QNAME,
                SimpleInputOutputRpcOutput.QNAME), rpcLeafOutputNode);
        assertValidJson(serializedRpc.toString());
    }

    @Test
    void testSerializeToasterData() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(toasterTopLevelContainerNode);
        assertValidJson(serializeData.toString());
    }

    @Test
    void testSerializeInnerContainer() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(Absolute.of(TopLevelContainer.QNAME),
                innerContainerNode);
        assertValidJson(serializeData.toString());
    }

    @Test
    void testSerializeList() throws SerializationException {
        final Writer serializeData = bindingSerializer.serializeData(listNode);
        assertValidJson(serializeData.toString());
    }

    @Test
    void testSerializeListEntry() throws SerializationException {
        final Writer serializedData = bindingSerializer.serializeData(Absolute.of(SampleList.QNAME), listEntryNode);
        assertValidJson(serializedData.toString());
    }

    @Test
    void testDeserializeContainerData() throws DeserializationException {
        final NormalizedNode deserializeData = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("toaster-container.json")));
        assertNotNull(deserializeData);
        assertEquals(expectedToasterContainerNN(), deserializeData);
    }

    @Test
    void testDeserializeRpcInput() throws DeserializationException {
        final NormalizedNode deserializeRpc = bindingSerializer.deserialize(Absolute.of(LEAF_RPC_QNAME),
                new StringReader(loadResourceAsString("rpc-leaf-Input.json")));
        assertNotNull(deserializeRpc);
        assertEquals(expectedRpcInputNN(), deserializeRpc);
    }

    @Test
    void testDeserializeRpcOutput() throws DeserializationException {
        final NormalizedNode deserializeRpc = bindingSerializer.deserialize(Absolute.of(LEAF_RPC_QNAME),
                new StringReader(loadResourceAsString("rpc-leaf-output.json")));
        assertNotNull(deserializeRpc);
        assertEquals(expectedRpcOutputNN(), deserializeRpc);
    }

    @Test
    void testDeserializeRpcContainerInput() throws DeserializationException {
        final NormalizedNode deserializeData = bindingSerializer.deserialize(Absolute.of(CONTAINER_RPC_QNAME),
                new StringReader(loadResourceAsString("rpc-container-input.json")));
        assertNotNull(deserializeData);
        assertEquals(expectedRpcContainerInputNN(), deserializeData);
    }

    @Test
    void testDeserializeTopLevelContainer() throws DeserializationException {
        final NormalizedNode deserializeContainer = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("top-level-container.json")));
        assertNotNull(deserializeContainer);
        assertEquals(expectedTopLevelContainerNN(), deserializeContainer);
    }

    @Test
    void testDeserializeInnerContainer() throws DeserializationException {
        final NormalizedNode deserializeContainer = bindingSerializer.deserialize(Absolute.of(TopLevelContainer.QNAME),
                new StringReader(loadResourceAsString("inner-container.json")));
        assertNotNull(deserializeContainer);
        assertEquals(expectedInnerContainerNN(), deserializeContainer);
    }

    @Test
    void testDeserializeInnerLeaf() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(Absolute.of(TopLevelContainer.QNAME,
                SampleContainer.QNAME), new StringReader(loadResourceAsString("inner-leaf.json")));
        assertNotNull(result);
        assertEquals(expectedInnerLeafNN(), result);
    }

    @Test
    void testDeserializeListSingleEntry() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("single-list-entry.json")));
        assertNotNull(result);
        assertEquals(expectedListSingleEntryNN(), result);
    }

    @Test
    void testDeserializeTestList() throws DeserializationException {
        final YangInstanceIdentifier testData = YangInstanceIdentifier.builder()
                .node(ContainerWithList.QNAME)
                .build();
        final StringReader stringReader = new StringReader(loadResourceAsString("test-list.json"));
        final NormalizedNode result = bindingSerializer.deserialize(testData, stringReader);
        assertNotNull(result);
        assertEquals(expectedTestListNN(), result);
    }

    @Test
    void testDeserializeListMultipleEntries() throws DeserializationException {
        final NormalizedNode result = bindingSerializer.deserialize(
                new StringReader(loadResourceAsString("multiple-list-entries.json")));
        assertNotNull(result);
        assertEquals(expectedListMultipleEntriesNN(), result);
    }

    private static NormalizedNode expectedToasterContainerNN() {
        return wrapWithBaseContainer(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(Toaster.QNAME))
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(qOfToasterModel("toasterManufacturer")))
                        .withValue("manufacturer")
                        .build())
                .withChild((ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(qOfToasterModel("toasterStatus")))
                        .withValue("up")
                        .build()))
                .withChild((ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(qOfToasterModel("darknessFactor")))
                        .withValue(Uint32.valueOf(201392110))
                        .build()))
                .build());
    }

    private static NormalizedNode expectedRpcInputNN() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SimpleInputOutputRpcInput.QNAME))
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("input-obj")))
                        .withValue("a")
                        .build())
                .build();
    }

    private static NormalizedNode expectedRpcOutputNN() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SimpleInputOutputRpcOutput.QNAME))
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("output-obj")))
                        .withValue("a")
                        .build())
                .build();
    }

    private static NormalizedNode expectedRpcContainerInputNN() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(ContainerIoRpcInput.QNAME))
                .withChild(expectedInnerContainerNN())
                .build();
    }

    private static NormalizedNode expectedTopLevelContainerNN() {
        return wrapWithBaseContainer(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(TopLevelContainer.QNAME))
                .withChild(expectedInnerContainerNN())
                .build());
    }

    private static DataContainerChild expectedInnerContainerNN() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SampleContainer.QNAME))
                .withChild(expectedInnerLeafNN())
                .withChild((ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("value")))
                        .withValue(Uint32.valueOf(1))
                        .build()))
                .build();
    }

    private static DataContainerChild expectedInnerLeafNN() {
        return ImmutableNodes.newLeafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("name")))
                .withValue("name")
                .build();
    }

    private static NormalizedNode expectedListSingleEntryNN() {
        return wrapWithBaseContainer(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SampleList.QNAME))
                .withChild(ImmutableNodes.newMapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(SampleList.QNAME,
                                qOfTestModel("name"), "test"))
                        .withChild(ImmutableNodes.newLeafBuilder()
                                .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("name")))
                                .withValue("test")
                                .build())
                        .build())
                .build());
    }

    private static NormalizedNode expectedTestListNN() {
        return ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(NodeIdentifier.create(TestList.QNAME))
                .withChild(ImmutableNodes.newMapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(TestList.QNAME,
                                qOfTestModel("test-name"), "test"))
                        .withChild(ImmutableNodes.newLeafBuilder()
                                .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("test-name")))
                                .withValue("test")
                                .build())
                        .build())
                .build();
    }

    private static NormalizedNode expectedListMultipleEntriesNN() {
        return wrapWithBaseContainer(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SampleList.QNAME))
                .withChild(ImmutableNodes.newMapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(SampleList.QNAME,
                                qOfTestModel("name"), "test"))
                        .withChild(ImmutableNodes.newLeafBuilder()
                                .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("name")))
                                .withValue("test")
                                .build())
                        .build())
                .withChild(ImmutableNodes.newMapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(SampleList.QNAME,
                                qOfTestModel("name"), "test2"))
                        .withChild(ImmutableNodes.newLeafBuilder()
                                .withNodeIdentifier(NodeIdentifier.create(qOfTestModel("name")))
                                .withValue("test2")
                                .build())
                        .build())
                .build());
    }

    private static NormalizedNode wrapWithBaseContainer(final DataContainerChild child) {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SchemaContext.NAME))
                .withChild(child)
                .build();
    }
}
