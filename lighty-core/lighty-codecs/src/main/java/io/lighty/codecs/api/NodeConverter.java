/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import io.lighty.codecs.JsonNodeConverter;
import io.lighty.codecs.XmlNodeConverter;
import io.lighty.codecs.xml.XmlElement;
import java.io.Reader;
import java.io.Writer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

/**
 * This interface may be useful when (de)serializing {@link NormalizedNode}s (from)into its XML or
 * JSON representation. Currently there are two implementations {@link XmlNodeConverter} and
 * {@link JsonNodeConverter}.
 *
 * @deprecated This interface is moved to lighty-codecs-util.
 */
@Deprecated(forRemoval = true)
public interface NodeConverter {
    /**
     * This method will serialize the given {@link NormalizedNode} into its string representation.
     *
     * @see ConverterUtils#getSchemaNode(SchemaContext, QName)
     * @param schemaNode parent schema node used during serialization
     * @param normalizedNode normalized nodes to be serialized
     * @return {@link Writer}
     * @throws SerializationException may be throws while serializing data
     */
    Writer serializeData(SchemaNode schemaNode, NormalizedNode<?, ?> normalizedNode) throws SerializationException;

    /**
     * This method will serialize the input {@link NormalizedNode} RPC into its string representation. It
     * is highly recommend to use {@link ConverterUtils#loadRpc(SchemaContext, QName)} and proper
     * input/output definition as the schemaNode parameter.
     *
     * @see ConverterUtils#rpcAsInput(XmlElement)
     * @see ConverterUtils#rpcAsOutput(XmlElement)
     * @see ConverterUtils#getRpcQName(XmlElement)
     * @param schemaNode parent schema node which may be obtained via
     *        {@link ConverterUtils#loadRpc(SchemaContext, QName)} and input/output definition
     * @param normalizedNode normalized nodes to be serialized
     * @return string representation of the given nodes starting with input or output tag
     * @throws SerializationException thrown in case serialization fails.
     */
    Writer serializeRpc(SchemaNode schemaNode, NormalizedNode<?, ?> normalizedNode) throws SerializationException;

    /**
     * This method will deserialize the given input data into {@link NormalizedNode}s. In case of RPC
     * input/output use proper parent schema node obtained via
     * {@link ConverterUtils#loadRpc(SchemaContext, QName)}.
     *
     * @see ConverterUtils#loadRpc(SchemaContext, QName)
     * @see ConverterUtils#rpcAsInput(XmlElement)
     * @see ConverterUtils#rpcAsOutput(XmlElement)
     * @param schemaNode parent schema node
     * @param inputData string representation of input/output RPC or data. In case of RPC the inputData
     *        <b>MUST</b> start with input tag (in case of XML) and object (in case of JSON). The same
     *        goes for RPC output
     * @return deserialized {@link NormalizedNode}s
     * @throws SerializationException is thrown in case of an error during deserialization
     */
    NormalizedNode<?, ?> deserialize(SchemaNode schemaNode, Reader inputData) throws SerializationException;
}
