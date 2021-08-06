/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
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
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonNodeConverterTest extends AbstractCodecTest {

    private static final Logger LOG = LoggerFactory.getLogger(JsonNodeConverterTest.class);

    private final NodeConverter bindingSerializer;

    public JsonNodeConverterTest() throws YangParserException {
        bindingSerializer = new JsonNodeConverter(this.effectiveModelContext);
    }

    @Test
    public void testSerializeRpc_in() throws Exception {
        Optional<? extends RpcDefinition>
                loadedRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        Writer serializedRpc =
                bindingSerializer.serializeRpc(loadedRpc.get().getInput(), testedSimpleRpcInputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializedRpc.toString()));
        LOG.info(serializedRpc.toString());
    }

    @Test
    public void testSerializeRpc_out() throws Exception {
        Optional<? extends RpcDefinition>
                loadedRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        Writer serializedRpc =
                bindingSerializer.serializeRpc(loadedRpc.get().getOutput(), testedSimpleRpcOutputNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializedRpc.toString()));
        LOG.info(serializedRpc.toString());
    }

    @Test
    public void testSerializeData() throws Exception {
        Writer serializeData =
                bindingSerializer.serializeData(this.effectiveModelContext, testedToasterNormalizedNodes);
        Assert.assertFalse(Strings.isNullOrEmpty(serializeData.toString()));
        LOG.info(serializeData.toString());
    }

    @Test
    public void testDeserializeData() throws Exception {
        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(this.effectiveModelContext,
                new StringReader(loadResourceAsString("toaster.json")));
        Assert.assertNotNull(deserializeData);
        LOG.info(deserializeData.toString());
    }

    @Test
    public void testDeserialize_in() throws Exception {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        String loadIoRpcIn = loadResourceAsString("input-output-rpc-in.json");
        NormalizedNode<?, ?> deserializeRpc =
                bindingSerializer.deserialize(loadRpc.get(), new StringReader(loadIoRpcIn));
        Assert.assertNotNull(deserializeRpc);
        LOG.info(deserializeRpc.toString());
    }

    @Test
    public void testDeserialize_out() throws Exception {
        Optional<? extends RpcDefinition>
                loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, SIMPLE_IO_RPC_QNAME);
        String loadIoRpcOut = loadResourceAsString("input-output-rpc-out.json");
        NormalizedNode<?, ?> deserializeRpc =
                bindingSerializer.deserialize(loadRpc.get(), new StringReader(loadIoRpcOut));
        Assert.assertNotNull(deserializeRpc);
        LOG.info(deserializeRpc.toString());
    }

    @Test
    public void testDeserialize_container() throws SerializationException {
        NormalizedNode<?, ?> deserializeContainer = bindingSerializer.deserialize(this.effectiveModelContext,
                new StringReader(loadResourceAsString("top-level-container.json")));
        Assert.assertNotNull(deserializeContainer);
        LOG.info(deserializeContainer.toString());
    }

    @Test
    public void testDeserialize_container_rpc() throws SerializationException {
        Optional<? extends RpcDefinition>
                loadRpcContainer = ConverterUtils.loadRpc(this.effectiveModelContext, CONTAINER_IO_RPC_QNAME);
        NormalizedNode<?, ?> deserializeData = bindingSerializer.deserialize(loadRpcContainer.get(),
                new StringReader(loadResourceAsString("container-io-rpc.json")));
        Assert.assertNotNull(deserializeData);
    }
}
