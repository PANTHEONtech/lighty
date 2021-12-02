/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

/**
 * The implementation of {@link NodeConverter} which serializes and deserializes binding independent
 * representation into/from JSON representation.
 *
 * @see XmlNodeConverter
 */
public class JsonNodeConverter implements NodeConverter {

    private final JSONCodecFactory jsonCodecFactory;

    /**
     * This constructor will create an instance of {@link JsonNodeConverter} with the given
     * {@link EffectiveModelContext}.
     *
     * <p>
     * The effective model context will be used for proper RPC and Node resolution.
     *
     * <p>
     * The {@code JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02} will be used for JSON
     * serialization/deserialization of data.
     *
     * @param effectiveModelContext initial effective model context
     */
    public JsonNodeConverter(final EffectiveModelContext effectiveModelContext) {
        this(effectiveModelContext, JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
    }

    /**
     * This constructor will create an instance of {@link JsonNodeConverter} with the given
     * {@link EffectiveModelContext} and customizable {@link JSONCodecFactorySupplier}.
     *
     * <p>
     * The effective model context will be used for proper RPC and Node resolution.
     *
     * <p>
     * The {@code JSONCodecFactorySupplier} instance will be used for JSON serialization/deserialization of data.
     *
     * @param effectiveModelContext initial effective model context
     * @param jsonCodecFactorySupplier JSON codec factory supplier
     */
    public JsonNodeConverter(final EffectiveModelContext effectiveModelContext,
            final JSONCodecFactorySupplier jsonCodecFactorySupplier) {
        this.jsonCodecFactory = jsonCodecFactorySupplier.createLazy(effectiveModelContext);
    }

    /**
     * Serializes the given {@link NormalizedNode} into its {@link Writer} representation.
     *
     * @param schemaInferenceStack schema inference stack pointing to normalizedNode's parent
     * @param normalizedNode normalized node to serialize
     * @return {@link Writer}
     * @throws SerializationException if something goes wrong with serialization
     */
    @Override
    @SuppressWarnings({"checkstyle:illegalCatch"})
    public Writer serializeData(final SchemaInferenceStack schemaInferenceStack,
            final NormalizedNode normalizedNode) throws SerializationException {
        final Writer writer = new StringWriter();
        final XMLNamespace initialNamespace = schemaInferenceStack.isEmpty()
                ? normalizedNode.getIdentifier().getNodeType().getNamespace()
                : schemaInferenceStack.currentModule().localQNameModule().getNamespace();
        // nnStreamWriter closes underlying JsonWriter, we don't need too
        final JsonWriter jsonWriter = new JsonWriter(writer);
        // nnWriter closes underlying NormalizedNodeStreamWriter, we don't need too
        final NormalizedNodeStreamWriter nnStreamWriter = normalizedNode instanceof MapEntryNode
                ? JSONNormalizedNodeStreamWriter.createNestedWriter(
                        this.jsonCodecFactory, schemaInferenceStack.toInference(), initialNamespace, jsonWriter)
                : JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                        this.jsonCodecFactory, schemaInferenceStack.toInference(), initialNamespace, jsonWriter);

        try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter)) {
            nnWriter.write(normalizedNode);
            return writer;
        } catch (RuntimeException | IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    @SuppressWarnings({"checkstyle:illegalCatch"})
    public Writer serializeRpc(final SchemaInferenceStack schemaInferenceStack,
            final NormalizedNode normalizedNode) throws SerializationException {
        Preconditions.checkState(normalizedNode instanceof ContainerNode,
                "RPC input/output to serialize is expected to be a ContainerNode");
        final XMLNamespace namespace = schemaInferenceStack.currentModule().localQNameModule().getNamespace();
        // Input/output
        final String localName = schemaInferenceStack.toSchemaNodeIdentifier().lastNodeIdentifier().getLocalName();
        final Writer writer = new StringWriter();
        // nnStreamWriter closes underlying JsonWriter, we don't need too
        final JsonWriter jsonWriter = new JsonWriter(writer);
        // nnWriter closes underlying NormalizedNodeStreamWriter, we don't need too
        final NormalizedNodeStreamWriter nnStreamWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                this.jsonCodecFactory, schemaInferenceStack.toInference(), namespace, jsonWriter);
        try (NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter)) {
            jsonWriter.beginObject().name(localName);
            for (NormalizedNode child : ((ContainerNode) normalizedNode).body()) {
                normalizedNodeWriter.write(child);
            }
            jsonWriter.endObject();
            return writer;
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Deserializes a given JSON input data into {@link NormalizedNode}.
     * @param schemaInferenceStack schema inference stack pointing to a parent of the node to deserialize
     * @param inputData Reader containing input JSON data describing node to deserialize
     * @return deserialized {@link NormalizedNode}
     * @throws DeserializationException is thrown in case of an error during deserialization
     */
    @Override
    @SuppressWarnings({"checkstyle:illegalCatch"})
    public NormalizedNode deserialize(final SchemaInferenceStack schemaInferenceStack,
            final Reader inputData) throws DeserializationException {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        try (JsonReader reader = new JsonReader(inputData);
             NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
             JsonParserStream jsonParser = JsonParserStream.create(streamWriter, this.jsonCodecFactory,
                     schemaInferenceStack.toInference())) {
            jsonParser.parse(reader);
            return result.getResult();
        } catch (RuntimeException | IOException e) {
            throw new DeserializationException(e);
        }
    }

    @Override
    public EffectiveModelContext getModelContext() {
        return jsonCodecFactory.getEffectiveModelContext();
    }

}
