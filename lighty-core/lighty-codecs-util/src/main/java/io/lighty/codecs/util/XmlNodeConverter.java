/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import com.google.common.base.Preconditions;
import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.codecs.util.exception.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of {@link NodeConverter} which serializes and deserializes binding independent
 * representation into/from XML representation.
 *
 * @see JsonNodeConverter
 */
public class XmlNodeConverter implements NodeConverter {

    private static final Logger LOG = LoggerFactory.getLogger(XmlNodeConverter.class);

    private static final XMLInputFactory XML_IN_FACTORY;
    private static final XMLOutputFactory XML_OUT_FACTORY;

    static {
        XML_IN_FACTORY = XMLInputFactory.newInstance();
        XML_OUT_FACTORY = XMLOutputFactory.newFactory();
        XML_OUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    private final EffectiveModelContext effectiveModelContext;

    /**
     * The only constructor will create an instance of {@link XmlNodeConverter} with the given
     * {@link EffectiveModelContext}. This effective model context will be used for proper RPC and Node resolution
     *
     * @param effectiveModelContext initial effective model context
     */
    public XmlNodeConverter(final EffectiveModelContext effectiveModelContext) {
        this.effectiveModelContext = effectiveModelContext;
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
        XMLStreamWriter xmlStreamWriter;
        try {
            xmlStreamWriter = XML_OUT_FACTORY.createXMLStreamWriter(writer);
        } catch (XMLStreamException | FactoryConfigurationError e) {
            throw new SerializationException(e);
        }
        final NormalizedNodeStreamWriter nnStreamWriter = XMLStreamNormalizedNodeStreamWriter
                .create(xmlStreamWriter, inference);
        try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter)) {
            nnWriter.write(normalizedNode);
            return writer;
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public Writer serializeRpc(final Inference inference, final NormalizedNode normalizedNode)
            throws SerializationException {
        Preconditions.checkState(normalizedNode instanceof ContainerNode,
                "RPC input/output to serialize is expected to be a ContainerNode");
        final Writer writer = new StringWriter();
        XMLStreamWriter xmlStreamWriter;
        try {
            xmlStreamWriter = XML_OUT_FACTORY.createXMLStreamWriter(writer);
        } catch (XMLStreamException | FactoryConfigurationError e) {
            throw new SerializationException(e);
        }
        final XMLNamespace namespace = normalizedNode.name().getNodeType().getNamespace();
        // Input/output
        final String localName = normalizedNode.name().getNodeType().getLocalName();
        final NormalizedNodeStreamWriter nnStreamWriter = XMLStreamNormalizedNodeStreamWriter
                .create(xmlStreamWriter, inference);
        try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter)) {
            xmlStreamWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, localName, namespace.toString());
            xmlStreamWriter.writeDefaultNamespace(namespace.toString());
            for (NormalizedNode child : ((ContainerNode) normalizedNode).body()) {
                nnWriter.write(child);
            }
            xmlStreamWriter.writeEndElement();
            return writer;
        } catch (XMLStreamException | IOException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Deserializes a given XML input data into {@link NormalizedNode}.
     *
     * <p>
     * In the case of deserializing multiple top level list entries, entries are expected to be wrapped in
     * {@code <data xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">}.
     *
     * @param inference {@link Inference} pointing to a node we are trying to deserialize
     * @param inputData Reader containing input JSON data describing node to deserialize
     * @return deserialized {@link NormalizedNode}
     * @throws DeserializationException is thrown in case of an error during deserialization
     */
    @Override
    public NormalizedNode deserialize(final Inference inference, final Reader inputData)
            throws DeserializationException {
        final XMLStreamReader reader;
        try {
            reader = XML_IN_FACTORY.createXMLStreamReader(inputData);
        } catch (XMLStreamException e) {
            throw new DeserializationException(e);
        }

        final NormalizationResultHolder result = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, inference)) {
            xmlParser.parse(reader);
            return result.getResult().data();
        } catch (XMLStreamException | IOException e) {
            throw new DeserializationException(e);
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                LOG.warn("Failed to close XML stream", e);
            }
        }

    }

    @Override
    public EffectiveModelContext getModelContext() {
        return effectiveModelContext;
    }

}
