/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import gnmi.Gnmi;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangInstanceIdentifierToPathCodec implements Codec<YangInstanceIdentifier, Gnmi.Path> {
    private static final Logger LOG = LoggerFactory.getLogger(YangInstanceIdentifierToPathCodec.class);

    private final SchemaContextProvider schemaContextProvider;
    /**
     * If the resulting path should contain module identifier in first element.
     * (e.g interfaces -> openconfig-interfaces:interfaces).
     * This approach for example is required in SONIC device.
     */
    private final boolean prefixFirstElement;

    public YangInstanceIdentifierToPathCodec(final SchemaContextProvider schemaContextProvider,
                                             final boolean prefixFirstElement) {
        this.schemaContextProvider = schemaContextProvider;
        this.prefixFirstElement = prefixFirstElement;
    }

    public Gnmi.Path apply(final YangInstanceIdentifier path) {
        final Gnmi.Path.Builder pathBuilder = Gnmi.Path.newBuilder();
        final PeekingIterator<YangInstanceIdentifier.PathArgument> iterator =
                Iterators.peekingIterator(path.getPathArguments().iterator());
        Gnmi.PathElem.Builder previousElemBuilder = Gnmi.PathElem.newBuilder();
        String previousNamespace = "";
        while (iterator.hasNext()) {
            final Gnmi.PathElem.Builder currentElemBuilder = Gnmi.PathElem.newBuilder();
            final YangInstanceIdentifier.PathArgument arg = iterator.next();
            if (doesDefineKeys(arg)) {
                if (arg instanceof YangInstanceIdentifier.NodeWithValue) {
                    addKeysDefinitionNode((YangInstanceIdentifier.NodeWithValue) arg,
                            previousElemBuilder);
                } else {
                    addKeysDefinitionNode((YangInstanceIdentifier.NodeIdentifierWithPredicates) arg,
                            previousElemBuilder);
                }
                previousNamespace = getAndSetNamespaceToPath(previousElemBuilder, previousNamespace, arg);
                pathBuilder.addElem(previousElemBuilder);
            } else {
                addNonKeyDefinitionNode(arg, currentElemBuilder);
                if (!doesDefineAugment(arg) && (!iterator.hasNext() || !doesDefineKeys(iterator.peek()))) {
                    /* Add this PathElem only when current path arg is not augment identifier and
                       next arg does not define key and value pairs */
                    previousNamespace = getAndSetNamespaceToPath(currentElemBuilder, previousNamespace, arg);
                    pathBuilder.addElem(currentElemBuilder);
                }
            }
            previousElemBuilder = currentElemBuilder;
        }
        final Gnmi.Path resultingPath = pathBuilder.build();
        LOG.debug("Resulting gNMI Path of identifier {} is {}", path, resultingPath);
        return resultingPath;
    }

    private String getAndSetNamespaceToPath(final Gnmi.PathElem.Builder elemBuilder,
                                            String previousNamespace,
                                            final PathArgument pathArgument) {
        // Add prefix to first path element, if requested
        if (!prefixFirstElement) {
            return previousNamespace;
        }

        if (pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier) {
            return previousNamespace;
        }

        final QName module = pathArgument.getNodeType();
        final Optional<Module> elemModule = schemaContextProvider.getSchemaContext()
                .findModule(module.getNamespace(), module.getRevision());
        String currentNamespace = elemModule.get().getName();

        if (!previousNamespace.equals(currentNamespace)) {
            elemBuilder.setName(String.format("%s:%s", elemModule.get().getName(), elemBuilder.getName()));
            return currentNamespace;
        } else {
            return previousNamespace;
        }
    }

    private static void addNonKeyDefinitionNode(final YangInstanceIdentifier.PathArgument identifier,
                                                final Gnmi.PathElem.Builder builder) {
        if (identifier instanceof YangInstanceIdentifier.NodeIdentifier) {
            // In case of container or leaf
            builder.setName(identifier.getNodeType().getLocalName());
        } else if (identifier instanceof YangInstanceIdentifier.NodeWithValue) {
            // In case of leaf-list entry
            final YangInstanceIdentifier.NodeWithValue<?> valueNode = (YangInstanceIdentifier.NodeWithValue) identifier;
            builder.setName(valueNode.getValue().toString());
        }
    }

    private static void addKeysDefinitionNode(final YangInstanceIdentifier.NodeIdentifierWithPredicates identifier,
                                              final Gnmi.PathElem.Builder builder) {
        for (Map.Entry<QName, Object> entry : identifier.entrySet()) {
            builder.putKey(entry.getKey().getLocalName(), entry.getValue().toString());
        }

    }

    private static void addKeysDefinitionNode(final YangInstanceIdentifier.NodeWithValue identifier,
                                              final Gnmi.PathElem.Builder builder) {
        builder.putKey(identifier.getNodeType().getLocalName(), identifier.getValue().toString());
    }

    private static boolean doesDefineKeys(final YangInstanceIdentifier.PathArgument pathArgument) {
        return pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates
                || pathArgument instanceof YangInstanceIdentifier.NodeWithValue;
    }

    private static boolean doesDefineAugment(final YangInstanceIdentifier.PathArgument pathArgument) {
        return pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier;
    }
}
