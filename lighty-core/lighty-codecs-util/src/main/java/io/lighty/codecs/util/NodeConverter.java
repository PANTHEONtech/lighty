/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import java.io.Reader;
import java.io.Writer;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

/**
 * This interface may be useful when (de)serializing {@link NormalizedNode}s (from)into its XML or
 * JSON representation. Currently there are two implementations {@link XmlNodeConverter} and
 * {@link JsonNodeConverter}.
 */
public interface NodeConverter {

    @Deprecated(forRemoval = true)
    default Writer serializeData(SchemaNode schemaNode, NormalizedNode normalizedNode) throws SerializationException {
        return serializeData(SchemaInferenceStack.ofSchemaPath(getModelContext(), schemaNode.getPath()),
                normalizedNode);
    }

    default Writer serializeData(SchemaNodeIdentifier.Absolute schemaNodeIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        return serializeData(SchemaInferenceStack.of(getModelContext(), schemaNodeIdentifier), normalizedNode);
    }

    default Writer serializeData(NormalizedNode normalizedNode, QName... schemaIdentifierPath)
            throws SerializationException {
        return serializeData(SchemaNodeIdentifier.Absolute.of(schemaIdentifierPath), normalizedNode);
    }

    default Writer serializeData(NormalizedNode normalizedNode) throws SerializationException {
        return serializeData(SchemaInferenceStack.of(getModelContext()), normalizedNode);
    }

    default Writer serializeData(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        final Optional<SchemaNodeIdentifier.Absolute> schemaNodeIdentifier = ConverterUtils.toSchemaNodeIdentifier(
                yangInstanceIdentifier);
        return schemaNodeIdentifier.isPresent()
                ? serializeData(schemaNodeIdentifier.get(), normalizedNode)
                : serializeData(normalizedNode);
    }

    Writer serializeData(SchemaInferenceStack schemaInferenceStack, NormalizedNode normalizedNode)
            throws SerializationException;

    @Deprecated(forRemoval = true)
    default Writer serializeRpc(SchemaNode schemaNode, NormalizedNode normalizedNode) throws SerializationException {
        return serializeRpc(SchemaInferenceStack.ofSchemaPath(getModelContext(), schemaNode.getPath()), normalizedNode);
    }

    default Writer serializeRpc(NormalizedNode normalizedNode, QName... schemaIdentifierPath)
            throws SerializationException {
        return serializeRpc(SchemaNodeIdentifier.Absolute.of(schemaIdentifierPath), normalizedNode);
    }

    default Writer serializeRpc(SchemaNodeIdentifier.Absolute schemaNodeIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        return serializeRpc(SchemaInferenceStack.of(getModelContext(), schemaNodeIdentifier), normalizedNode);
    }

    default Writer serializeRpc(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        final Optional<SchemaNodeIdentifier.Absolute> schemaNodeIdentifier = ConverterUtils.toSchemaNodeIdentifier(
                yangInstanceIdentifier);
        return schemaNodeIdentifier.isPresent()
                ? serializeRpc(schemaNodeIdentifier.get(), normalizedNode)
                : serializeRpc(normalizedNode);
    }

    /**
     * Serializes the input/output {@link NormalizedNode} of a RPC into its string representation.
     *
     * @param schemaInferenceStack inference stack of input/output container of the RPC
     * @param normalizedNode normalized nodes to be serialized
     * @return string representation of the given nodes starting with input or output tag
     * @throws SerializationException thrown in case serialization fails.
     */
    Writer serializeRpc(SchemaInferenceStack schemaInferenceStack, NormalizedNode normalizedNode)
            throws SerializationException;

    @Deprecated(forRemoval = true)
    default NormalizedNode deserialize(SchemaNode schemaNode, Reader inputData)
            throws DeserializationException {
        return deserialize(SchemaInferenceStack.ofSchemaPath(getModelContext(), schemaNode.getPath()), inputData);
    }

    default NormalizedNode deserialize(Reader inputData, QName... schemaIdentifierPath)
            throws DeserializationException {
        return deserialize(SchemaNodeIdentifier.Absolute.of(schemaIdentifierPath), inputData);
    }

    default NormalizedNode deserialize(SchemaNodeIdentifier.Absolute schemaNodeIdentifier, Reader inputData)
            throws DeserializationException {
        return deserialize(SchemaInferenceStack.of(getModelContext(), schemaNodeIdentifier), inputData);
    }

    default NormalizedNode deserialize(Reader inputData) throws DeserializationException {
        return deserialize(SchemaInferenceStack.of(getModelContext()), inputData);
    }

    default NormalizedNode deserialize(YangInstanceIdentifier yangInstanceIdentifier, Reader inputData)
            throws DeserializationException {
        final Optional<SchemaNodeIdentifier.Absolute> schemaNodeIdentifier = ConverterUtils.toSchemaNodeIdentifier(
                yangInstanceIdentifier);
        return schemaNodeIdentifier.isPresent()
                ? deserialize(schemaNodeIdentifier.get(), inputData)
                : deserialize(inputData);
    }

    NormalizedNode deserialize(SchemaInferenceStack schemaInferenceStack, Reader inputData)
            throws DeserializationException;

    EffectiveModelContext getModelContext();
}
