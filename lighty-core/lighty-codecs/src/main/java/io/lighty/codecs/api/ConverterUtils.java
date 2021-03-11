/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import com.google.common.base.Strings;
import io.lighty.codecs.xml.DocumentedException;
import io.lighty.codecs.xml.XmlElement;
import io.lighty.codecs.xml.XmlUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A utility class which may be helpful while manipulating with binding independent nodes.
 *
 * @deprecated This class is moved to lighty-codecs-util.
 */
@Deprecated(forRemoval = true)
public final class ConverterUtils {
    private ConverterUtils() {
        throw new UnsupportedOperationException("Do not create an instance of utility class");
    }

    /**
     * Returns the {@link RpcDefinition} from the given {@link SchemaContext} and given {@link QName}.
     * The {@link QName} of a rpc can be constructed via
     *
     * <p>
     * {@code
     *  QName.create("http://netconfcentral.org/ns/toaster", "2009-11-20", "make-toast");
     * } , where {@code "make-toast"} is the name of the RPC given in the yang model.
     *
     * <p>
     * If the given RPC was found in the {@link SchemaContext} the {@link RpcDefinition} will be present
     *
     * @see QName
     * @param schemaContext the schema context used for the RPC resolution
     * @param rpcQName {@link QName} of the RPC
     * @return {@link Optional} representation of the {@link RpcDefinition}
     */
    public static Optional<? extends RpcDefinition> loadRpc(final SchemaContext schemaContext, final QName rpcQName) {
        Optional<Module> findModule = findModule(schemaContext, rpcQName);
        if (!findModule.isPresent()) {
            return Optional.empty();
        }
        return findDefinition(rpcQName, findModule.get().getRpcs());
    }

    /**
     * Utility method to extract the {@link SchemaNode} for the given Notification.
     *
     * @param schemaContext to be used
     * @param notificationQname yang RPC name
     * @return {@link Optional} of {@link SchemaNode}
     */
    public static Optional<? extends NotificationDefinition> loadNotification(final SchemaContext schemaContext,
            final QName notificationQname) {
        Optional<Module> findModule = findModule(schemaContext, notificationQname);
        if (!findModule.isPresent()) {
            return Optional.empty();
        }
        return findDefinition(notificationQname, findModule.get().getNotifications());
    }

    /**
     * This method extracts from the given {@link XmlElement} the name and namespace from the first
     * element and creates a {@link QName}.
     *
     * @param xmlElement input data.
     * @return {@link QName} for input data or empty.
     */
    public static Optional<QName> getRpcQName(final XmlElement xmlElement) {
        Optional<String> optionalNamespace = xmlElement.getNamespaceOptionally().toJavaUtil();
        String name = xmlElement.getName();
        if (Strings.isNullOrEmpty(name)) {
            return Optional.empty();
        }
        String revision = null;
        String namespace;
        if (optionalNamespace.isPresent() && !Strings.isNullOrEmpty(optionalNamespace.get())) {
            String[] split = optionalNamespace.get().split("\\?");
            if (split.length > 1 && split[1].contains("revision=")) {
                revision = split[1].replace("revision=", "");

            }
            namespace = split[0];
        } else {
            return Optional.of(QName.create(name));
        }
        if (Strings.isNullOrEmpty(revision)) {
            return Optional.of(QName.create(namespace, name));
        } else {
            return Optional.of(QName.create(namespace, revision, name));
        }
    }

    /**
     * Create RPC QName.
     *
     * @param inputString RPC name
     * @return {@link QName} for RPC name or empty.
     * @throws IllegalArgumentException if there was a problem during parsing the XML document
     * @see ConverterUtils#getRpcQName(XmlElement)
     */
    public static Optional<QName> getRpcQName(final String inputString) {
        try {
            return getRpcQName(XmlElement.fromString(inputString));
        } catch (DocumentedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static XmlElement rpcAsInput(final XmlElement inputXmlElement) {
        return rpcAsInput(inputXmlElement, "");
    }

    /**
     * Removes the first XML tag and replaces it with an {@code <input>} element. This method may be
     * useful when converting the input of a rpc. The provided namespace will be used for the input tag
     * document.
     *
     * @param inputXmlElement input xml data to wrap.
     * @param namespace namespace
     * @return wrapped xml data.
     */
    public static XmlElement rpcAsInput(final XmlElement inputXmlElement, final String namespace) {
        return wrapNodes("input", namespace, inputXmlElement.getChildElements());
    }

    /**
     * Calls the method {@link ConverterUtils#rpcAsOutput(XmlElement, String)} with an empty namespace.
     *
     * @see ConverterUtils#rpcAsOutput(XmlElement, String)
     * @see XmlUtil
     * @param inputXmlElement input rpc element data.
     * @return wrapped xml element.
     */
    public static XmlElement rpcAsOutput(final XmlElement inputXmlElement) {
        return rpcAsOutput(inputXmlElement, "");
    }

    /**
     * Removes the first XML tag and replaces it with an {@code <output>} element. This method may be
     * useful when the output rpc is created. The namespace will be used for the output tag.
     *
     * @see XmlUtil
     * @param inputXmlElement input rpc element data.
     * @param namespace namespace
     * @return wrapped xml element.
     */
    public static XmlElement rpcAsOutput(final XmlElement inputXmlElement, final String namespace) {
        return wrapNodes("output", namespace, inputXmlElement.getChildElements());
    }

    /**
     * Creates an instance of {@link SchemaNode} for the given {@link QName} in the given {@link SchemaContext}.
     *
     * @see ConverterUtils#getSchemaNode(SchemaContext, String, String, String)
     * @param schemaContext the given schema context which contains the {@link QName}
     * @param qname the given {@link QName}
     * @return instance of {@link SchemaNode}
     */
    public static SchemaNode getSchemaNode(final SchemaContext schemaContext, final QName qname) {
        return DataSchemaContextTree.from(schemaContext).getChild(YangInstanceIdentifier.of(qname)).getDataSchemaNode();
    }

    public static SchemaNode getSchemaNode(final SchemaContext schemaContext,
            final YangInstanceIdentifier yangInstanceIdentifier) {
        return DataSchemaContextTree.from(schemaContext).getChild(yangInstanceIdentifier).getDataSchemaNode();
    }

    /**
     * Creates an instance of {@link SchemaNode} for the given namespace, revision and localname. The
     * namespace, revision and localname are used to construct the {@link QName} which must exist in the
     * {@link SchemaContext}.
     *
     * @see ConverterUtils#getSchemaNode(SchemaContext, QName)
     * @param schemaContext given schema context
     * @param namespace {@link QName} namespace
     * @param revision {@link QName} revision
     * @param localName {@link QName} localname
     * @return instance of {@link SchemaNode}
     */
    public static SchemaNode getSchemaNode(final SchemaContext schemaContext, final String namespace,
            final String revision, final String localName) {
        QName qname = QName.create(namespace, revision, localName);
        return DataSchemaContextTree.from(schemaContext).getChild(YangInstanceIdentifier.of(qname)).getDataSchemaNode();
    }

    /**
     * Appends all nodes given as children into a node given by node name with given namespace.
     *
     * @param nodeName the top level node
     * @param namespace provided namespace for the nodename
     * @param children child elements to be appended
     * @return created {@link XmlElement}
     * @see XmlUtil
     */
    private static XmlElement wrapNodes(final String nodeName, final String namespace,
            final Collection<XmlElement> children) {
        StringBuilder sb = new StringBuilder("<").append(nodeName).append(" xmlns=\"").append(namespace).append("\"/>");
        Document document;
        try {
            document = XmlUtil.readXmlToDocument(sb.toString());
        } catch (SAXException | IOException e) {
            // should never happen
            throw new IllegalStateException(e);
        }
        children.forEach(child -> {
            Node importedNode = document.importNode(child.getDomElement(), true);
            document.getChildNodes().item(0).appendChild(importedNode);
        });
        return XmlElement.fromDomDocument(document);
    }

    private static Optional<Module> findModule(final SchemaContext schemaContext, final QName qname) {
        if (qname.getRevision().isPresent()) {
            return schemaContext.findModule(qname.getNamespace(), qname.getRevision());
        }

        Collection<? extends Module> moduleByNamespace = schemaContext.findModules(qname.getNamespace());
        return Optional.ofNullable(moduleByNamespace.isEmpty() || moduleByNamespace.size() > 1 ? null
                : moduleByNamespace.iterator().next());
    }

    private static <T extends SchemaNode> Optional<T> findDefinition(final QName qname, final Collection<T> nodes) {
        List<T> foundNodes = nodes.stream().filter(node -> node.getQName().getLocalName().equals(qname.getLocalName()))
                .collect(Collectors.toList());
        return Optional.ofNullable(foundNodes.isEmpty() || foundNodes.size() > 1 ? null : foundNodes.get(0));
    }
}
