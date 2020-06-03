/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import io.lighty.codecs.api.Codec;
import io.lighty.codecs.api.NodeConverter;
import io.lighty.codecs.xml.XmlElement;
import io.lighty.codecs.xml.XmlUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.restconf.common.errors.RestconfDocumentedException;
import org.opendaylight.yangtools.rfc8040.model.api.YangDataSchemaNode;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class DataCodec<T extends DataObject> implements Codec<T> {

    private static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY = XMLOutputFactory.newInstance();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    private final BindingToNormalizedNodeCodec codec;
    private final DeserializeIdentifierCodec deserializeIdentifierCodec;
    private final SerializeIdentifierCodec serializeIdentifierCodec;
    private final JsonNodeConverter jsonNodeConverter;
    private final XmlNodeConverter xmlNodeConverter;
    private final SchemaContext schemaContext;

    public DataCodec(final SchemaContext schemaContext) {
        this.schemaContext = requireNonNull(schemaContext);
        final BindingNormalizedNodeCodecRegistry registry = new BindingNormalizedNodeCodecRegistry();
        this.codec = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
            registry);
        this.codec.onGlobalContextUpdated(schemaContext);

        this.deserializeIdentifierCodec = new DeserializeIdentifierCodec(schemaContext);
        this.serializeIdentifierCodec = new SerializeIdentifierCodec(schemaContext);
        this.xmlNodeConverter = new XmlNodeConverter(this.schemaContext);
        this.jsonNodeConverter = new JsonNodeConverter(this.schemaContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public T convertToBindingAwareData(final YangInstanceIdentifier identifier, final NormalizedNode<?, ?> data) {
        final Entry<InstanceIdentifier<?>, DataObject> dataObjectEntry =
                this.codec.fromNormalizedNode(identifier, data);
        if (dataObjectEntry != null) {
            return (T) dataObjectEntry.getValue();
        }
        return null;
    }

    @Override
    public Collection<T> convertBindingAwareList(final YangInstanceIdentifier identifier, final MapNode mapNode) {
        Collection<MapEntryNode> children = mapNode.getValue();
        final Builder<T> listBuilder = ImmutableList.builderWithExpectedSize(children.size());
        for (MapEntryNode entry : children) {
            listBuilder.add(verifyNotNull(convertToBindingAwareData(identifier, entry), identifier));
        }
        return listBuilder.build();
    }

    private T verifyNotNull(T dataObject, YangInstanceIdentifier identifier) {
        if (dataObject == null) {
            throw new VerifyException("Unexpected null value for list IID " + identifier);
        }
        return dataObject;
    }

    @Override
    public YangInstanceIdentifier deserializeIdentifier(final InstanceIdentifier<T> identifier) {
        return this.codec.toNormalized(identifier);
    }

    @Override
    public String deserializeIdentifier(final YangInstanceIdentifier identifier) {
        return this.deserializeIdentifierCodec.deserialize(identifier);
    }

    @Override
    public YangInstanceIdentifier convertIdentifier(final String identifier) {
        return this.serializeIdentifierCodec.serialize(identifier);
    }

    @Override
    public Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> convertToNormalizedNode(
            final InstanceIdentifier<T> identifier, final T data) {
        return this.codec.toNormalizedNode(identifier, data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convertToBindingAwareRpc(final SchemaPath schemaPath, final ContainerNode rpcData) {
        return (T) this.codec.fromNormalizedNodeRpcData(schemaPath, rpcData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convertToBindingAwareNotification(final SchemaPath schemaPath, final ContainerNode norificationData) {
        return (T) this.codec.fromNormalizedNodeNotification(schemaPath, norificationData);
    }

    @Override
    public ContainerNode convertToBindingIndependentRpc(final DataContainer rpcData) {
        return this.codec.toNormalizedNodeRpcData(rpcData);
    }

    @Override
    public ContainerNode convertToBindingIndependentNotification(final Notification notificationData) {
        return this.codec.toNormalizedNodeNotification(notificationData);
    }

    @Override
    public NormalizedNode<?, ?> serializeXMLError(final String body) {
        final Optional<Revision> restconfRevision = Revision.ofNullable("2017-01-26");
        final Optional<Module> optModule = this.schemaContext.findModule("ietf-restconf", restconfRevision);
        if (!optModule.isPresent()) {
            throw new IllegalStateException("ietf-restconf module was not found in schema context.");
        }
        final Module restconfModule = optModule.get();
        final List<UnknownSchemaNode> unknownSchemaNodes = restconfModule.getUnknownSchemaNodes();
        final QNameModule qNameRestconfModule = QNameModule
                .create(URI.create("urn:ietf:params:xml:ns:yang:ietf-restconf"), restconfRevision);
        final QName yangDataYangErrors = QName.create(qNameRestconfModule, "yang-errors");
        YangDataSchemaNode yangDataNode =
                (YangDataSchemaNode) unknownSchemaNodes.stream()
                        .filter(unknownSchemaNode -> yangDataYangErrors.equals(unknownSchemaNode.getQName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("node yang-error wasn't found in ietf-restconf "));

        final DataSchemaNode schemaNode = yangDataNode.getContainerSchemaNode();
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);

        try (XmlParserStream xmlParser = XmlParserStream.create(writer, this.schemaContext, schemaNode)) {
            final Document doc = XmlUtil.readXmlToDocument(body);
            final XmlElement element = XmlElement.fromDomDocument(doc);
            final Element domElement = element.getDomElement();
            xmlParser.traverse(new DOMSource(domElement));
            return resultHolder.getResult();
        } catch (SAXException | IOException | XMLStreamException | URISyntaxException e) {
            throw new RestconfDocumentedException(e.getMessage(), e);
        }
    }

    @Override
    public BindingToNormalizedNodeCodec getCodec() {
        return this.codec;
    }

    @Override
    public SchemaContext getSchemaContext() {
        return this.schemaContext;
    }

    @Override
    public NodeConverter withJson() {
        return this.jsonNodeConverter;
    }

    @Override
    public NodeConverter withXml() {
        return this.xmlNodeConverter;
    }
}
