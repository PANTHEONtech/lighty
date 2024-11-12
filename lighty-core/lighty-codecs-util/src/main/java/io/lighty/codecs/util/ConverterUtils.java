/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContext;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A utility class which may be helpful while manipulating with binding independent nodes.
 */
public final class ConverterUtils {
    private ConverterUtils() {
        throw new UnsupportedOperationException("Do not create an instance of utility class");
    }

    /**
     * Returns the {@link RpcDefinition} from the given {@link EffectiveModelContext} and given {@link QName}.
     * The {@link QName} of a rpc can be constructed via
     *
     * <p>{@code
     * QName.create("http://netconfcentral.org/ns/toaster", "2009-11-20", "make-toast");
     * } , where {@code "make-toast"} is the name of the RPC given in the yang model.
     *
     * <p>If the given RPC was found in the {@link EffectiveModelContext} the {@link RpcDefinition} will be returned
     *
     * @param effectiveModelContext the effective model context used for the RPC resolution
     * @param rpcQName              {@link QName} of the RPC
     * @return {@link Optional} representation of the {@link RpcDefinition}
     * @see QName
     */
    public static Optional<? extends RpcDefinition> loadRpc(final EffectiveModelContext effectiveModelContext,
            final QName rpcQName) {
        Optional<Module> findModule = findModule(effectiveModelContext, rpcQName);
        if (findModule.isEmpty()) {
            return Optional.empty();
        }
        return findDefinition(rpcQName, findModule.get().getRpcs());
    }

    /**
     * Utility method to extract the {@link SchemaNode} for the given Notification.
     *
     * @param effectiveModelContext to be used
     * @param notificationQname     yang RPC name
     * @return {@link Optional} of {@link SchemaNode}
     */
    public static Optional<? extends NotificationDefinition> loadNotification(
            final EffectiveModelContext effectiveModelContext, final QName notificationQname) {
        Optional<Module> findModule = findModule(effectiveModelContext, notificationQname);
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
        String nxmlNamespace = xmlElement.namespace();
        String name = xmlElement.getName();
        if (Strings.isNullOrEmpty(name)) {
            return Optional.empty();
        }
        String revision = null;
        String namespace;
        if (nxmlNamespace != null) {
            String[] split = nxmlNamespace.split("\\?");
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
     * @param namespace       namespace
     * @return wrapped xml data.
     */
    public static XmlElement rpcAsInput(final XmlElement inputXmlElement, final String namespace) {
        return wrapNodes("input", namespace, inputXmlElement.getChildElements());
    }

    /**
     * Calls the method {@link ConverterUtils#rpcAsOutput(XmlElement, String)} with an empty namespace.
     *
     * @param inputXmlElement input rpc element data.
     * @return wrapped xml element.
     * @see ConverterUtils#rpcAsOutput(XmlElement, String)
     * @see XmlUtil
     */
    public static XmlElement rpcAsOutput(final XmlElement inputXmlElement) {
        return rpcAsOutput(inputXmlElement, "");
    }

    /**
     * Removes the first XML tag and replaces it with an {@code <output>} element. This method may be
     * useful when the output rpc is created. The namespace will be used for the output tag.
     *
     * @param inputXmlElement input rpc element data.
     * @param namespace       namespace
     * @return wrapped xml element.
     * @see XmlUtil
     */
    public static XmlElement rpcAsOutput(final XmlElement inputXmlElement, final String namespace) {
        return wrapNodes("output", namespace, inputXmlElement.getChildElements());
    }

    /**
     * Finds the {@link DataSchemaContext} for the given {@link QName} in {@link EffectiveModelContext}.
     *
     * <p>Search is performed only on first level nodes of the modules, for recursive search,
     * the {@link YangInstanceIdentifier} is needed, thus consider using
     * {@link ConverterUtils#getSchemaNode(EffectiveModelContext, YangInstanceIdentifier)}.
     *
     * @param effectiveModelContext model context to search
     * @param qname                 {@link QName} of node to search for
     * @return optional found {@link DataSchemaContext}
     */
    public static Optional<@NonNull DataSchemaContext> getSchemaNode(
            final EffectiveModelContext effectiveModelContext, final QName qname) {
        return getSchemaNode(effectiveModelContext, YangInstanceIdentifier.of(qname));
    }

    /**
     * Finds the {@link DataSchemaContext} for the given {@link YangInstanceIdentifier}
     * in {@link EffectiveModelContext}.
     *
     * @param effectiveModelContext  model context to search
     * @param yangInstanceIdentifier {@link YangInstanceIdentifier} of the node to search for
     * @return optional found {@link DataSchemaContext}
     */
    public static Optional<@NonNull DataSchemaContext> getSchemaNode(
            final EffectiveModelContext effectiveModelContext, final YangInstanceIdentifier yangInstanceIdentifier) {
        return DataSchemaContextTree.from(effectiveModelContext).findChild(yangInstanceIdentifier);
    }

    /**
     * Finds the {@link DataSchemaContext} for the given namespace, revision and local name
     * in {@link EffectiveModelContext}.
     *
     * <p>Search is performed only on first level nodes of the modules, for recursive search,
     * the {@link YangInstanceIdentifier} is needed, thus consider using
     * {@link ConverterUtils#getSchemaNode(EffectiveModelContext, YangInstanceIdentifier)}.
     *
     * @param effectiveModelContext model context to search
     * @param namespace             {@link QName} module namespace of the node to search for
     * @param revision              {@link QName} module revision of the node to search for
     * @param localName             {@link QName} local name of the node to search for
     * @return optional found {@link DataSchemaContext}
     */
    public static Optional<@NonNull DataSchemaContext> getSchemaNode(
            final EffectiveModelContext effectiveModelContext, final String namespace, final String revision,
            final String localName) {
        return getSchemaNode(effectiveModelContext, QName.create(namespace, revision, localName));
    }

    /**
     * Converts provided {@link YangInstanceIdentifier} to {@link Inference}.
     *
     * @param yangInstanceIdentifier yang instance identifier
     * @param effectiveModelContext current model context
     * @return {@link Inference}
     */
    public static Inference toInference(final YangInstanceIdentifier yangInstanceIdentifier,
            final EffectiveModelContext effectiveModelContext) {
        return DataSchemaContextTree.from(effectiveModelContext)
                .enterPath(Objects.requireNonNull(yangInstanceIdentifier))
                .orElseThrow()
                .stack()
                .toInference();
    }

    /**
     * Appends all nodes given as children into a node given by node name with given namespace.
     *
     * @param nodeName  the top level node
     * @param namespace provided namespace for the nodename
     * @param children  child elements to be appended
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

    private static Optional<Module> findModule(final EffectiveModelContext effectiveModelContext, final QName qname) {
        if (qname.getRevision().isPresent()) {
            return effectiveModelContext.findModule(qname.getNamespace(), qname.getRevision());
        }

        Collection<? extends Module> moduleByNamespace = effectiveModelContext.findModules(qname.getNamespace());
        return Optional.ofNullable(moduleByNamespace.isEmpty() || moduleByNamespace.size() > 1 ? null
                : moduleByNamespace.iterator().next());
    }

    private static <T extends SchemaNode> Optional<T> findDefinition(final QName qname, final Collection<T> nodes) {
        List<T> foundNodes = nodes.stream().filter(node -> node.getQName().getLocalName().equals(qname.getLocalName()))
                .collect(Collectors.toList());
        return Optional.ofNullable(foundNodes.size() != 1 ? null : foundNodes.get(0));
    }
}
