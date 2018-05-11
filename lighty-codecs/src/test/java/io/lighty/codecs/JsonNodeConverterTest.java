/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.codecs;

import io.lighty.codecs.api.ConverterUtils;
import io.lighty.codecs.api.NodeConverter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import io.lighty.codecs.api.SerializationException;

public class JsonNodeConverterTest extends AbstractCodecTest {

    private static final Logger LOG = LoggerFactory.getLogger(JsonNodeConverterTest.class);
    
    private final NodeConverter bindingSerializer;

    public JsonNodeConverterTest() {
        bindingSerializer = new JsonNodeConverter(schemaContext);
    }

    @Test
    public void testSerializeRpc_in() throws Exception {
        Optional<RpcDefinition> loadedRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        Writer serializedRpc =
                bindingSerializer.serializeRpc(loadedRpc.get().getInput(), testedSimpleRpcInputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializedRpc.toString()));
        LOG.info(serializedRpc.toString());
    }

    @Test
    public void testSerializeRpc_out() throws Exception {
        Optional<RpcDefinition> loadedRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        Writer serializedRpc =
                bindingSerializer.serializeRpc(loadedRpc.get().getOutput(), testedSimpleRpcOutputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializedRpc.toString()));
        LOG.info(serializedRpc.toString());
    }

    @Test
    public void testSerializeData() throws Exception {
        Writer serializeData = bindingSerializer.serializeData(schemaContext, testedToasterNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeData.toString()));
        LOG.info(serializeData.toString());
    }

    @Test
    public void testDeserializeData() throws Exception {
        NormalizedNode<?, ?> deserializeData =
                bindingSerializer.deserialize(schemaContext, new StringReader(loadResourceAsString("toaster.json")));
        Assert.assertNotNull(deserializeData);
        LOG.info(deserializeData.toString());
    }

    @Test
    public void testDeserialize_in() throws Exception {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        String loadIoRpcIn = loadResourceAsString("input-output-rpc-in.json");
        NormalizedNode<?, ?> deserializeRpc =
                bindingSerializer.deserialize(loadRpc.get(), new StringReader(loadIoRpcIn));
        Assert.assertNotNull(deserializeRpc);
        LOG.info(deserializeRpc.toString());
    }

    @Test
    public void testDeserialize_out() throws Exception {
        Optional<RpcDefinition> loadRpc = ConverterUtils.loadRpc(schemaContext, SIMPLE_IO_RPC_QNAME);
        String loadIoRpcOut = loadResourceAsString("input-output-rpc-out.json");
        NormalizedNode<?, ?> deserializeRpc =
                bindingSerializer.deserialize(loadRpc.get(), new StringReader(loadIoRpcOut));
        Assert.assertNotNull(deserializeRpc);
        LOG.info(deserializeRpc.toString());
    }

    @Test
    public void testDeserialize_container() throws SerializationException {
        NormalizedNode<?, ?> deserializeContainer = bindingSerializer.deserialize(this.schemaContext,
                new StringReader(loadResourceAsString("top-level-container.json")));
        Assert.assertNotNull(deserializeContainer);
        LOG.info(deserializeContainer.toString());
    }

    @Test
    public void testDeserialize_container_rpc() throws SerializationException {
        Optional<RpcDefinition> loadRpcContainer = ConverterUtils.loadRpc(schemaContext, CONTAINER_IO_RPC_QNAME);
        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(loadRpcContainer.get(),
                new StringReader(loadResourceAsString("container-io-rpc.json")));
        Assert.assertNotNull(deserializeData);
    }
}
