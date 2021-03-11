/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import com.google.common.io.Closeables;
import io.lighty.codecs.api.ConverterUtils;
import io.lighty.codecs.api.NodeConverter;
import io.lighty.codecs.api.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * The implementation of {@link NodeConverter} which serializes and deserializes binding independent
 * representation into/from XML representation.
 *
 * @deprecated This class is moved to lighty-codecs-util.
 * @see JsonNodeConverter
 */
@Deprecated(forRemoval = true)
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
     * {@link SchemaContext}. This schema context will be used for proper RPC and Node resolution
     *
     * @param effectiveModelContext initial schema context
     */
    public XmlNodeConverter(final EffectiveModelContext effectiveModelContext) {
        this.effectiveModelContext = effectiveModelContext;
    }

    /**
     * This method serializes the given {@link NormalizedNode} into its XML string representation.
     *
     * @see NodeConverter#serializeData(SchemaNode, NormalizedNode)
     *
     * @param schemaNode the parent schema node where the nodes exist
     * @param normalizedNode {@link NormalizedNode} to be serialized
     * @return {@link StringWriter} implementation of {@link Writer} is returned
     * @throws SerializationException if it was not possible to serialize the normalized nodes into XML
     */
    @Override
    public Writer serializeData(final SchemaNode schemaNode, final NormalizedNode<?, ?> normalizedNode)
            throws SerializationException {
        Writer writer = new StringWriter();
        try (NormalizedNodeWriter normalizedNodeWriter =
                createNormalizedNodeWriter(effectiveModelContext, writer, schemaNode.getPath())) {
            normalizedNodeWriter.write(normalizedNode);
            normalizedNodeWriter.flush();
        } catch (IOException ioe) {
            throw new SerializationException(ioe);
        }
        return writer;
    }

    /**
     * This method serializes the input or output of a RPC given as {@link NormalizedNode}
     * representation into XML string representation.
     *
     * <p>
     * To obtain correct {@link SchemaNode} use {@link ConverterUtils#loadRpc(SchemaContext, QName)} method.
     *
     * @param schemaNode input or output {@link SchemaNode}
     * @param normalizedNode {@link NormalizedNode} representation of input or output
     * @return XML string representation of provided BI nodes. It utilizes the {@link StringWriter}
     * @throws SerializationException may be thrown if there was a problem during serialization
     */
    @Override
    public Writer serializeRpc(final SchemaNode schemaNode, final NormalizedNode<?, ?> normalizedNode)
            throws SerializationException {
        Writer writer = new StringWriter();
        XMLStreamWriter xmlStreamWriter = createXmlStreamWriter(writer);
        URI namespace = schemaNode.getQName().getNamespace();
        String localName = schemaNode.getQName().getLocalName();
        try (NormalizedNodeWriter normalizedNodeWriter =
                createNormalizedNodeWriter(effectiveModelContext, xmlStreamWriter, schemaNode.getPath())) {
            // the localName may be "input" or "output" - this may be changed
            xmlStreamWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, localName, namespace.toString());
            xmlStreamWriter.writeDefaultNamespace(namespace.toString());
            for (NormalizedNode<?, ?> child : ((ContainerNode) normalizedNode).getValue()) {
                normalizedNodeWriter.write(child);
            }
            normalizedNodeWriter.flush();
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.flush();
        } catch (IOException | XMLStreamException ioe) {
            throw new SerializationException(ioe);
        }
        return writer;
    }

    /**
     * This method deserializes the provided XML string representation (via {@link Reader}) interface
     * into {@link NormalizedNode}s. During deserialization of RPC input and output a proper
     * {@link SchemaNode} (given for input or output) must be passed. This may be obtained via
     * {@link ConverterUtils#loadRpc(SchemaContext, QName)}.
     *
     * @param schemaNode parent schema node which contains information about the input data
     * @param inputData XML input
     * @return an {@link Optional} representation of {@link NormalizedNode}. If the deserialization
     *         process finished incorrectly an empty value will be present
     * @throws SerializationException if it was not possible to deserialize the input data
     * @throws IllegalArgumentException if a problem occurs during reading the input
     */
    @Override
    public NormalizedNode<?, ?> deserialize(final SchemaNode schemaNode, final Reader inputData)
            throws SerializationException {
        final XMLStreamReader reader;
        try {
            reader = XML_IN_FACTORY.createXMLStreamReader(inputData);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(e);
        }
        final NormalizedNodeResult result = new NormalizedNodeResult();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
                XmlParserStream xmlParser = XmlParserStream.create(streamWriter, this.effectiveModelContext,
                        schemaNode)) {
            xmlParser.parse(reader);
        } catch (XMLStreamException | URISyntaxException | IOException | SAXException e) {
            throw new SerializationException(e);
        } finally {
            closeQuietly(reader);
        }
        return result.getResult();
    }

    /**
     * Utility method to obtain an instance of {@link NormalizedNodeWriter} by using the {@link Writer}.
     */
    private static NormalizedNodeWriter createNormalizedNodeWriter(final SchemaContext schemaContext,
            final Writer backingWriter, final SchemaPath pathToParent) {
        XMLStreamWriter createXMLStreamWriter = createXmlStreamWriter(backingWriter);
        return createNormalizedNodeWriter(schemaContext, createXMLStreamWriter, pathToParent);
    }

    /**
     * Create a new {@link NormalizedNodeWriter}.
     *
     * @see XMLStreamNormalizedNodeStreamWriter#create(XMLStreamWriter, SchemaContext)
     * @see XMLStreamNormalizedNodeStreamWriter#create(XMLStreamWriter, SchemaContext, SchemaPath)
     *
     * @param schemaContext the root schema context
     * @param backingWriter used backing writer
     * @param pathToParent path to parent, may be the same as {@link SchemaContext} param
     * @return a new instance of {@link NormalizedNodeWriter}
     */
    private static NormalizedNodeWriter createNormalizedNodeWriter(final SchemaContext schemaContext,
            final XMLStreamWriter backingWriter, final SchemaPath pathToParent) {
        NormalizedNodeStreamWriter streamWriter;
        if (pathToParent == null) {
            streamWriter = XMLStreamNormalizedNodeStreamWriter.create(backingWriter, schemaContext);
        } else {
            streamWriter = XMLStreamNormalizedNodeStreamWriter.create(backingWriter, schemaContext, pathToParent);
        }
        return NormalizedNodeWriter.forStreamWriter(streamWriter);
    }

    /**
     * Utility method which returns a new instance of {@link XMLStreamWriter} obtained via
     * {@link XmlNodeConverter#XML_OUT_FACTORY}. This factory is namespace aware by default.
     *
     * @param backingWriter backing {@link Writer}
     * @return a fresh instance of {@link XMLStreamWriter}
     * @throws IllegalStateException if it's not possible to obtain the instance
     */
    private static XMLStreamWriter createXmlStreamWriter(final Writer backingWriter) {
        XMLStreamWriter xmlStreamWriter;
        try {
            xmlStreamWriter = XML_OUT_FACTORY.createXMLStreamWriter(backingWriter);
        } catch (XMLStreamException | FactoryConfigurationError e) {
            throw new IllegalStateException(e);
        }
        return xmlStreamWriter;
    }

    /**
     * This method is similar to the {@link Closeables#closeQuietly(Reader)} or other 'closeQuietly
     * methods. It takes the {@link XMLStreamReader} as parameter checks for null and tries to close it
     * while consuming the {@link IOException}. If the {@link IOException} occurs it will be logged.
     *
     * @param xmlStreamReader the given {@link XMLStreamReader} may be null
     */
    public static void closeQuietly(final XMLStreamReader xmlStreamReader) {
        if (xmlStreamReader != null) {
            try {
                xmlStreamReader.close();
            } catch (XMLStreamException e) {
                LOG.warn("Failed to close stream", e);
            }
        }
    }
}
