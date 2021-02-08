/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2019 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.simplify.stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.lighty.core.yang.validator.simplify.SchemaTree;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import org.opendaylight.yangtools.odlext.model.api.YangModeledAnyxmlSchemaNode;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.util.AbstractNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.AnyXmlNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.CompositeNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.ContainerNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.LeafListEntryNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.LeafListNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.LeafNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.ListEntryNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.ListNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.ParserStreamUtils;
import org.opendaylight.yangtools.yang.data.util.SimpleNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.YangModeledAnyXmlNodeDataWithSchema;
import org.opendaylight.yangtools.yang.data.util.codec.TypeAwareCodec;
import org.opendaylight.yangtools.yang.model.api.AnyxmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.w3c.dom.Document;


/**
 * This class provides functionality for parsing an XML source containing YANG-modeled data. It disallows multiple
 * instances of the same element except for leaf-list and list entries. It also expects that the YANG-modeled data in
 * the XML source are wrapped in a root element.
 * This class is copied from ODL XmlParserStream and adjusted to fill in our SchemaTree class in read function
 */
@Beta
public final class TrackingXmlParserStream implements Closeable, Flushable {
    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        final TransformerFactory fa = TransformerFactory.newInstance();
        if (!fa.getFeature(StAXResult.FEATURE)) {
            if (!fa.getFeature(StAXResult.FEATURE)) {
                throw new TransformerFactoryConfigurationError("No TransformerFactory supporting StAXResult found.");
            }
        }

        TRANSFORMER_FACTORY = fa;
    }

    private static final String XML_STANDARD_VERSION = "1.0";
    private final NormalizedNodeStreamWriter writer;
    private final XmlCodecFactory codecs;
    private final DataSchemaNode parentNode;
    private final boolean strictParsing;

    private final SchemaTree tree;

    public TrackingXmlParserStream(final NormalizedNodeStreamWriter writer, final XmlCodecFactory codecs,
                                   final DataSchemaNode parentNode, final boolean strictParsing,
                                   final SchemaTree tree) {
        this.writer = requireNonNull(writer);
        this.codecs = requireNonNull(codecs);
        this.parentNode = parentNode;
        this.strictParsing = strictParsing;
        this.tree = tree;
    }

    /**
     * This method parses the XML source and emits node events into a NormalizedNodeStreamWriter based on the
     * YANG-modeled data contained in the XML source.
     *
     * @param reader StAX reader which is to used to walk through the XML source
     * @return instance of XmlParserStream
     * @throws XMLStreamException           if a well-formedness error or an unexpected processing condition occurs
     *                                      while parsing the XML
     * @throws URISyntaxException           if the namespace URI of an XML element contains a syntax error
     * @throws IOException                  if an error occurs while parsing the value of an anyxml node
     */
    public TrackingXmlParserStream parse(final XMLStreamReader reader) throws XMLStreamException, URISyntaxException,
            IOException {
        if (reader.hasNext()) {
            reader.nextTag();
            final AbstractNodeDataWithSchema nodeDataWithSchema;
            if (parentNode instanceof ContainerSchemaNode) {
                nodeDataWithSchema = new ContainerNodeDataWithSchema((ContainerSchemaNode) parentNode);
            } else if (parentNode instanceof ListSchemaNode) {
                nodeDataWithSchema = new ListNodeDataWithSchema((ListSchemaNode) parentNode);
            } else if (parentNode instanceof YangModeledAnyxmlSchemaNode) {
                nodeDataWithSchema = new YangModeledAnyXmlNodeDataWithSchema((YangModeledAnyxmlSchemaNode) parentNode);
            } else if (parentNode instanceof AnyxmlSchemaNode) {
                nodeDataWithSchema = new AnyXmlNodeDataWithSchema((AnyxmlSchemaNode) parentNode);
            } else if (parentNode instanceof LeafSchemaNode) {
                nodeDataWithSchema = new LeafNodeDataWithSchema((LeafSchemaNode) parentNode);
            } else if (parentNode instanceof LeafListSchemaNode) {
                nodeDataWithSchema = new LeafListNodeDataWithSchema((LeafListSchemaNode) parentNode);
            } else {
                throw new IllegalStateException("Unsupported schema node type " + parentNode.getClass() + ".");
            }


            read(reader, nodeDataWithSchema, reader.getLocalName(), tree);
            nodeDataWithSchema.write(writer);
        }

        return this;
    }

    /**
     * This method traverses a {@link DOMSource} and emits node events into a NormalizedNodeStreamWriter based on the
     * YANG-modeled data contained in the source.
     *
     * @param src {@link DOMSource} to be traversed
     * @return instance of XmlParserStream
     * @throws XMLStreamException           if a well-formedness error or an unexpected processing condition occurs
     *                                      while parsing the XML
     * @throws URISyntaxException           if the namespace URI of an XML element contains a syntax error
     * @throws IOException                  if an error occurs while parsing the value of an anyxml node
     */
    @Beta
    public TrackingXmlParserStream traverse(final DOMSource src) throws XMLStreamException, URISyntaxException,
            IOException {
        return parse(new DOMSourceXMLStreamReader(src));
    }

    private static ImmutableMap<QName, String> getElementAttributes(final XMLStreamReader in) {
        checkState(in.isStartElement(), "Attributes can be extracted only from START_ELEMENT.");
        final Map<QName, String> attributes = new LinkedHashMap<>();

        for (int attrIndex = 0; attrIndex < in.getAttributeCount(); attrIndex++) {
            String attributeNS = in.getAttributeNamespace(attrIndex);

            if (attributeNS == null) {
                attributeNS = "";
            }

            // Skip namespace definitions
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attributeNS)) {
                continue;
            }

            final QName qName = QName.create(URI.create(attributeNS), in.getAttributeLocalName(attrIndex));
            attributes.put(qName, in.getAttributeValue(attrIndex));
        }

        return ImmutableMap.copyOf(attributes);
    }

    private static Document readAnyXmlValue(final XMLStreamReader in) throws XMLStreamException {
        // Underlying reader might return null when asked for version, however when such reader is plugged into
        // Stax -> DOM transformer, it fails with NPE due to null version. Use default xml version in such case.
        final XMLStreamReader inWrapper;
        if (in.getVersion() == null) {
            inWrapper = new StreamReaderDelegate(in) {
                @Override
                public String getVersion() {
                    final String ver = super.getVersion();
                    return ver != null ? ver : XML_STANDARD_VERSION;
                }
            };
        } else {
            inWrapper = in;
        }

        final DOMResult result = new DOMResult();
        try {
            TRANSFORMER_FACTORY.newTransformer().transform(new StAXSource(inWrapper), result);
        } catch (final TransformerException e) {
            throw new XMLStreamException("Unable to read anyxml value", e);
        }
        return (Document) result.getNode();
    }

    /**
     * Recursive method which constructs the {@code parent} tree and sets the values of
     * it's {@link SimpleNodeDataWithSchema} (leaf-list entry, leaf, anyxml) based on the parsed xml.
     * <br>
     * Method also populates the {@link SchemaTree} {@code  tree} which we then use
     * for formatting the outputs of lighty-yang-validator.
     *
     * @param in            StAX based XML reader
     * @param parent        The data node parent of the schema tree of the current nest level
     * @param rootElement   root xml element
     * @param schemaTree          SchemaTree which we are constructing as we parse the xml
     * @throws XMLStreamException   if a well-formedness error or an unexpected processing condition occurs
     *                              while parsing the XML
     * @throws URISyntaxException   if the namespace URI of an XML element contains a syntax error
     */

    private void read(final XMLStreamReader in, final AbstractNodeDataWithSchema parent,
                      final String rootElement, SchemaTree schemaTree) throws XMLStreamException, URISyntaxException {
        if (!in.hasNext()) {
            return;
        }

        if (parent instanceof LeafNodeDataWithSchema || parent instanceof LeafListEntryNodeDataWithSchema) {
            parent.setAttributes(getElementAttributes(in));
            setValue(parent, in.getElementText().trim(), in.getNamespaceContext());
            if (isNextEndDocument(in)) {
                return;
            }

            if (!isAtElement(in)) {
                in.nextTag();
            }
            return;
        }

        if (parent instanceof ListEntryNodeDataWithSchema || parent instanceof ContainerNodeDataWithSchema) {
            parent.setAttributes(getElementAttributes(in));
        }

        if (parent instanceof LeafListNodeDataWithSchema || parent instanceof ListNodeDataWithSchema) {
            String xmlElementName = in.getLocalName();
            while (xmlElementName.equals(parent.getSchema().getQName().getLocalName())) {
                read(in, newEntryNode(parent), rootElement, schemaTree);
                if (in.getEventType() == XMLStreamConstants.END_DOCUMENT
                        || in.getEventType() == XMLStreamConstants.END_ELEMENT) {
                    break;
                }
                xmlElementName = in.getLocalName();
            }

            return;
        }

        if (parent instanceof AnyXmlNodeDataWithSchema) {
            setValue(parent, readAnyXmlValue(in), in.getNamespaceContext());
            if (isNextEndDocument(in)) {
                return;
            }

            if (!isAtElement(in)) {
                in.nextTag();
            }

            return;
        }

        if (parent instanceof YangModeledAnyxmlSchemaNode) {
            parent.setAttributes(getElementAttributes(in));
        }

        switch (in.nextTag()) {
            case XMLStreamConstants.START_ELEMENT:
                /*
                 This Set keeps track of defined elements so we do not introduce multiple values for the same node.
                 For example :
                 ...
                 <interfaces xmlns="urn:ietf:params:xml:ns:yang:ietf-interfaces">
                    <interface>
                        <name>eth0</name>
                        <name>eth1</name>
                 ...
                 Is not valid
                 */
                // FIXME: Upstream yangtools from which this code is copied has this FIXME, so wait for solution
                // The task is there to keep track of that
                // https://jira.pantheon.sk/browse/PTODL-516
                final Set<Entry<String, String>> namesakes = new HashSet<>();
                while (in.hasNext()) {
                    final String xmlElementName = in.getLocalName();
                    DataSchemaNode parentSchema = parent.getSchema();

                    final String parentSchemaName = parentSchema.getQName().getLocalName();
                    if (parentSchemaName.equals(xmlElementName)
                            && in.getEventType() == XMLStreamConstants.END_ELEMENT) {
                        if (isNextEndDocument(in)) {
                            break;
                        }

                        if (!isAtElement(in)) {
                            in.nextTag();
                        }
                        break;
                    }

                    if (in.isEndElement() && rootElement.equals(xmlElementName)) {
                        break;
                    }

                    if (parentSchema instanceof YangModeledAnyxmlSchemaNode) {
                        parentSchema = ((YangModeledAnyxmlSchemaNode) parentSchema).getSchemaOfAnyXmlData();
                    }
                    /*
                     Check if xml node is already added to the Set
                     if yes, we have multiple values for the same node, which is not valid.
                     */
                    final String xmlElementNamespace = in.getNamespaceURI();
                    if (!namesakes.add(new SimpleImmutableEntry<>(xmlElementNamespace, xmlElementName))) {
                        final Location loc = in.getLocation();
                        throw new IllegalStateException(String.format(
                                "Duplicate namespace \"%s\" element \"%s\" in XML input at: line %s column %s",
                                xmlElementNamespace, xmlElementName, loc.getLineNumber(), loc.getColumnNumber()));
                    }
                    /*
                     Finds the actual nodes from the provided schema based on the xml element name and namespace
                     defined in the xmlns="[namespace]" metadata.
                     */
                    final Deque<DataSchemaNode> childDataSchemaNodes =
                            ParserStreamUtils.findSchemaNodeByNameAndNamespace(parentSchema, xmlElementName,
                                    new URI(xmlElementNamespace));

                    if (childDataSchemaNodes.isEmpty()) {
                        checkState(!strictParsing, "Schema for node with name %s and namespace %s does not exist at %s",
                                xmlElementName, xmlElementNamespace, parentSchema.getPath());
                        skipUnknownNode(in);
                        continue;
                    }

                    final SchemaTree parentTree = schemaTree;
                    for (final DataSchemaNode less : childDataSchemaNodes) {
                        /*
                         Check if SchemaNode found based on the xmlElementName is direct child of the root node.
                         If yes, the node is not from another module.
                         */
                        if (Iterables.size(less.getPath().getPathFromRoot()) == 1) {
                            schemaTree = schemaTree.addChild(less, true, false);
                        /*
                         If not, the node can be augmented, we need to check if the modules
                         of it's parent and grand parent. If they are not the same, the node is from another module,
                         therefore we treat it as augment.
                         */
                        } else {
                            final Iterator<QName> iterator = less.getPath().getPathTowardsRoot().iterator();

                            final QName first = iterator.next();
                            final QName second = iterator.next();
                            if (second.getModule().equals(first.getModule())) {
                                schemaTree = schemaTree.addChild(less, false, false);
                            } else {
                                schemaTree = schemaTree.addChild(less, true, true);
                            }
                        }
                    }
                    read(in, ((CompositeNodeDataWithSchema) parent)
                            .addChild(childDataSchemaNodes), rootElement, schemaTree);
                    schemaTree = parentTree;
                }
                break;
            case XMLStreamConstants.END_ELEMENT:
                if (isNextEndDocument(in)) {
                    break;
                }

                if (!isAtElement(in)) {
                    in.nextTag();
                }
                break;
            default:
                break;
        }
    }

    private static boolean isNextEndDocument(final XMLStreamReader in) throws XMLStreamException {
        return !in.hasNext() || in.next() == XMLStreamConstants.END_DOCUMENT;
    }

    private static boolean isAtElement(final XMLStreamReader in) {
        return in.getEventType() == XMLStreamConstants.START_ELEMENT
                || in.getEventType() == XMLStreamConstants.END_ELEMENT;
    }

    private static void skipUnknownNode(final XMLStreamReader in) throws XMLStreamException {
        // in case when the unknown node and at least one of its descendant nodes have the same name
        // we cannot properly reach the end just by checking if the current node is an end element and has the same name
        // as the root unknown element. therefore we ignore the names completely and just track the level of nesting
        int levelOfNesting = 0;
        while (in.hasNext()) {
            // in case there are text characters in an element, we cannot skip them by calling nextTag()
            // therefore we skip them by calling next(), and then proceed to next element
            in.next();
            if (!isAtElement(in)) {
                in.nextTag();
            }
            if (in.isStartElement()) {
                levelOfNesting++;
            }

            if (in.isEndElement()) {
                if (levelOfNesting == 0) {
                    break;
                }

                levelOfNesting--;
            }
        }

        in.nextTag();
    }

    /**
     * Sets the {@code SimpleNodeDataWithSchema} value based on the provided value from xml.
     *
     * @param parent node for which we are setting the value.
     * @param value provided value
     * @param nsContext namespace context of the xml node which contains the {@code value}
     */

    private void setValue(final AbstractNodeDataWithSchema parent, final Object value,
                          final NamespaceContext nsContext) {
        checkArgument(parent instanceof SimpleNodeDataWithSchema, "Node %s is not a simple type",
                parent.getSchema().getQName());
        final SimpleNodeDataWithSchema parentSimpleNode = (SimpleNodeDataWithSchema) parent;
        checkArgument(parentSimpleNode.getValue() == null, "Node '%s' has already set its value to '%s'",
                parentSimpleNode.getSchema().getQName(), parentSimpleNode.getValue());

        parentSimpleNode.setValue(translateValueByType(value, parentSimpleNode.getSchema(), nsContext));
    }

    private Object translateValueByType(final Object value, final DataSchemaNode node,
                                        final NamespaceContext namespaceCtx) {
        if (node instanceof AnyxmlSchemaNode) {

            checkArgument(value instanceof Document);
            /*
             *  FIXME: Figure out some YANG extension dispatch, which will
             *  reuse JSON parsing or XML parsing - anyxml is not well-defined in
             *  JSON.
             *  Upstream yangtools from which this code is copied has this FIXME, so wait for solution
             *  The task is there to keep track of that
             *  https://jira.pantheon.sk/browse/PTDL-1181
             */
            return new DOMSource(((Document) value).getDocumentElement());
        }

        checkArgument(node instanceof TypedDataSchemaNode);
        checkArgument(value instanceof String);
        final TypeAwareCodec xmlCodec = codecs.codecFor((TypedDataSchemaNode) node);
        return xmlCodec.parseValue(namespaceCtx, (String) value);
    }

    private static AbstractNodeDataWithSchema newEntryNode(final AbstractNodeDataWithSchema parent) {
        final AbstractNodeDataWithSchema newChild;
        if (parent instanceof ListNodeDataWithSchema) {
            newChild = ListEntryNodeDataWithSchema.forSchema((ListSchemaNode) parent.getSchema());
        } else {
            newChild = new LeafListEntryNodeDataWithSchema((LeafListSchemaNode) parent.getSchema());
        }
        ((CompositeNodeDataWithSchema) parent).addChild(newChild);
        return newChild;
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }
}

