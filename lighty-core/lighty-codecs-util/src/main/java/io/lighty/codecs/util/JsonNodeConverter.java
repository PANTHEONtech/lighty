/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
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
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

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
        this.jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .createLazy(effectiveModelContext);
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
     * This method serializes the provided {@link NormalizedNode} into its JSON representation.
     *
     * @param schemaNode {@link SchemaNode} may be obtained via
     *        {@link ConverterUtils#getSchemaNode(EffectiveModelContext, QName)} or
     *        {@link ConverterUtils#getSchemaNode(EffectiveModelContext, String, String, String)}
     * @param normalizedNode {@link NormalizedNode} to be serialized
     * @return string representation of JSON serialized data is returned via {@link StringWriter}
     * @throws SerializationException if there was a problem during writing JSON data
     */
    @Override
    public Writer serializeData(final SchemaNode schemaNode, final NormalizedNode<?, ?> normalizedNode)
            throws SerializationException {
        Writer writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        URI namespace = schemaNode.getQName().getNamespace();
        NormalizedNodeStreamWriter create = JSONNormalizedNodeStreamWriter.createExclusiveWriter(this.jsonCodecFactory,
                schemaNode.getPath(), namespace, jsonWriter);
        try (NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(create)) {
            normalizedNodeWriter.write(normalizedNode);
            jsonWriter.flush();
        } catch (IOException ioe) {
            throw new SerializationException(ioe);
        }
        return writer;
    }

    /**
     * This method serializes the {@link NormalizedNode} which represents the input or output of an RPC.
     *
     * @param schemaNode the input or output {@link SchemaNode} of the RPC
     * @param normalizedNode serialized binding independent data
     * @return JSON string representation of the given {@link NormalizedNode}
     * @throws SerializationException if an {@link IOException} occurs during serialization
     */
    @Override
    public Writer serializeRpc(final SchemaNode schemaNode, final NormalizedNode<?, ?> normalizedNode)
            throws SerializationException {
        Writer writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        String localName = schemaNode.getQName().getLocalName();
        URI namespace = schemaNode.getQName().getNamespace();
        NormalizedNodeStreamWriter create = JSONNormalizedNodeStreamWriter.createExclusiveWriter(this.jsonCodecFactory,
                schemaNode.getPath(), namespace, jsonWriter);
        try (NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(create)) {
            jsonWriter.beginObject().name(localName);
            for (NormalizedNode<?, ?> child : ((ContainerNode) normalizedNode).getValue()) {
                normalizedNodeWriter.write(child);
            }
            // XXX dirty check for end of object. When serializing RPCs with input/output which is not a
            // container
            // the object is not closed.
            if (!writer.toString().endsWith("}")) {
                jsonWriter.endObject();
            }
        } catch (IOException ioe) {
            throw new SerializationException(ioe);
        }
        return writer;
    }

    /**
     * Deserializes the given JSON representation into {@link NormalizedNode}s.
     *
     * @param schemaNode a correct {@link SchemaNode} may be obtained via
     *        {@link ConverterUtils#getSchemaNode(EffectiveModelContext, QName)} or
     *        {@link ConverterUtils#getSchemaNode(EffectiveModelContext, String, String, String)} or
     *        {@link ConverterUtils#loadRpc(EffectiveModelContext, QName)} depending on the input/output
     * @param inputData reader containing input data.
     * @return {@link NormalizedNode} representation of input data
     * @throws SerializationException if there was a problem during deserialization or reading the input
     *         data
     */
    @Override
    public NormalizedNode<?, ?> deserialize(final SchemaNode schemaNode, final Reader inputData)
            throws SerializationException {
        NormalizedNodeResult result = new NormalizedNodeResult();
        try (JsonReader reader = new JsonReader(inputData);
                NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
                JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                        this.jsonCodecFactory, schemaNode)) {
            jsonParser.parse(reader);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
        return result.getResult();
    }
}
