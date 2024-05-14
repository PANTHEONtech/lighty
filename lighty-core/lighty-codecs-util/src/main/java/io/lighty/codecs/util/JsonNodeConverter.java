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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of {@link NodeConverter} which serializes and deserializes binding independent
 * representation into/from JSON representation.
 *
 * @see XmlNodeConverter
 */
public class JsonNodeConverter implements NodeConverter {
    private static final Logger LOG = LoggerFactory.getLogger(JsonNodeConverter.class);

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
     * @param inference      {@link Inference} pointing to normalizedNode's parent
     * @param normalizedNode normalized node to serialize
     * @return {@link Writer}
     * @throws SerializationException if something goes wrong with serialization
     */
    @Override
    public Writer serializeData(final Inference inference,
            final NormalizedNode normalizedNode) throws SerializationException {
        final Writer writer = new StringWriter();
        final XMLNamespace initialNamespace = normalizedNode.name().getNodeType().getNamespace();
        // nnStreamWriter closes underlying JsonWriter, we don't need too
        final JsonWriter jsonWriter = new JsonWriter(writer);
        // Exclusive nnWriter closes underlying NormalizedNodeStreamWriter, we don't need too
        final boolean useNested = normalizedNode instanceof MapEntryNode;
        final NormalizedNodeStreamWriter nnStreamWriter = useNested
                ? JSONNormalizedNodeStreamWriter.createNestedWriter(
                        this.jsonCodecFactory, inference, initialNamespace, jsonWriter)
                : JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                        this.jsonCodecFactory, inference, initialNamespace, jsonWriter);

        try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter)) {
            nnWriter.write(normalizedNode);
            return writer;
        } catch (IOException e) {
            throw new SerializationException(e);
        } finally {
            if (useNested) {
                try {
                    jsonWriter.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close underlying JsonWriter", e);
                }
            }
        }
    }

    @Override
    public Writer serializeRpc(final Inference inference,
            final NormalizedNode normalizedNode) throws SerializationException {
        Preconditions.checkState(normalizedNode instanceof ContainerNode,
                "RPC input/output to serialize is expected to be a ContainerNode");
        final XMLNamespace namespace = normalizedNode.name().getNodeType().getNamespace();
        // Input/output
        final String localName = normalizedNode.name().getNodeType().getLocalName();
        final Writer writer = new StringWriter();
        // nnStreamWriter closes underlying JsonWriter, we don't need too
        final JsonWriter jsonWriter = new JsonWriter(writer);
        // nnWriter closes underlying NormalizedNodeStreamWriter, we don't need too
        final NormalizedNodeStreamWriter nnStreamWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                this.jsonCodecFactory, inference, namespace, jsonWriter);
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
     *
     * @param inference {@link Inference} pointing to a parent of the node to deserialize
     * @param inputData Reader containing input JSON data describing node to deserialize
     * @return deserialized {@link NormalizedNode}
     * @throws DeserializationException is thrown in case of an error during deserialization
     */
    @Override
    public NormalizedNode deserialize(final Inference inference, final Reader inputData)
            throws DeserializationException {
        if (inference.statementPath().isEmpty()) {
            final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder =
                    ImmutableNodes.newContainerBuilder().withNodeIdentifier(NodeIdentifier.create(SchemaContext.NAME));
            parseToResult(ImmutableNormalizedNodeStreamWriter.from(resultBuilder), inputData, inference);
            return resultBuilder.build();
        } else {
            final var result = new NormalizationResultHolder();
            parseToResult(ImmutableNormalizedNodeStreamWriter.from(result), inputData, inference);
            return result.getResult().data();
        }
    }

    private void parseToResult(final NormalizedNodeStreamWriter writer, final Reader data, final Inference inference)
            throws DeserializationException {
        try (JsonReader reader = new JsonReader(data);
             JsonParserStream jsonParser = JsonParserStream.create(writer, this.jsonCodecFactory, inference)) {

            jsonParser.parse(reader);
        } catch (IOException e) {
            throw new DeserializationException(e);
        }
    }

    @Override
    public EffectiveModelContext getModelContext() {
        return jsonCodecFactory.modelContext();
    }

}
