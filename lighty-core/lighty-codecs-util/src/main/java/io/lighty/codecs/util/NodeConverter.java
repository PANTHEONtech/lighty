/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import java.io.Reader;
import java.io.Writer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;

/**
 * This interface may be useful when (de)serializing {@link NormalizedNode}s (from)into its XML or
 * JSON representation. Currently there are two implementations {@link XmlNodeConverter} and
 * {@link JsonNodeConverter}.
 */
public interface NodeConverter {

    default Writer serializeData(Absolute schemaNodeIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        final Inference inference = SchemaInferenceStack.of(getModelContext(), schemaNodeIdentifier).toInference();
        return serializeData(inference, normalizedNode);
    }

    default Writer serializeData(NormalizedNode normalizedNode) throws SerializationException {
        return serializeData(SchemaInferenceStack.of(getModelContext()).toInference(), normalizedNode);
    }

    default Writer serializeData(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        final Inference inference = ConverterUtils.toInference(yangInstanceIdentifier, getModelContext());
        return serializeData(inference, normalizedNode);
    }

    Writer serializeData(Inference inference, NormalizedNode normalizedNode)
            throws SerializationException;

    default Writer serializeRpc(Absolute schemaNodeIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        final Inference inference = SchemaInferenceStack.of(getModelContext(), schemaNodeIdentifier).toInference();
        return serializeRpc(inference, normalizedNode);
    }

    default Writer serializeRpc(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode normalizedNode)
            throws SerializationException {
        final Inference inference = ConverterUtils.toInference(yangInstanceIdentifier, getModelContext());
        return serializeRpc(inference, normalizedNode);
    }

    /**
     * Serializes the input/output {@link NormalizedNode} of a RPC into its string representation.
     *
     * @param inference      {@link Inference} of input/output container of the RPC
     * @param normalizedNode normalized nodes to be serialized
     * @return string representation of the given nodes starting with input or output tag
     * @throws SerializationException thrown in case serialization fails.
     */
    Writer serializeRpc(Inference inference, NormalizedNode normalizedNode)
            throws SerializationException;

    default NormalizedNode deserialize(Absolute schemaNodeIdentifier, Reader inputData)
            throws DeserializationException {
        return deserialize(SchemaInferenceStack.of(getModelContext(), schemaNodeIdentifier).toInference(), inputData);
    }

    default NormalizedNode deserialize(Reader inputData) throws DeserializationException {
        return deserialize(SchemaInferenceStack.of(getModelContext()).toInference(), inputData);
    }

    default NormalizedNode deserialize(YangInstanceIdentifier yangInstanceIdentifier, Reader inputData)
            throws DeserializationException {
        final Inference inference = ConverterUtils.toInference(yangInstanceIdentifier, getModelContext());
        return deserialize(inference, inputData);
    }

    NormalizedNode deserialize(Inference inference, Reader inputData)
            throws DeserializationException;

    EffectiveModelContext getModelContext();
}
