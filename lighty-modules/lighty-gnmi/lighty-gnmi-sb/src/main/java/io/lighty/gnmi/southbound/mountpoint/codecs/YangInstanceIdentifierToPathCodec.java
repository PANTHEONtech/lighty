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

        while (iterator.hasNext()) {
            final Gnmi.PathElem.Builder currentElemBuilder = Gnmi.PathElem.newBuilder();
            final YangInstanceIdentifier.PathArgument arg = iterator.next();
            if (doesDefineKeys(arg)) {
                addKeysDefinitionNode((YangInstanceIdentifier.NodeIdentifierWithPredicates) arg,
                        previousElemBuilder);
                pathBuilder.addElem(previousElemBuilder.build());
            } else {
                addNonKeyDefinitionNode(arg, currentElemBuilder);
                if ((!iterator.hasNext() || !doesDefineKeys(iterator.peek()))) {
                    /* Add this PathElem only when current path arg is not augment identifier and
                       next arg does not define key and value pairs */
                    pathBuilder.addElem(currentElemBuilder.build());
                }
            }
            previousElemBuilder = currentElemBuilder;
        }

        // Add prefix to first path element, if requested
        if (pathBuilder.getElemCount() > 0 && prefixFirstElement) {
            final Gnmi.PathElem firstElement = pathBuilder.getElem(0);
            final QName nodeType = path.getPathArguments().get(0).getNodeType();

            final Optional<Module> firstElemModule = schemaContextProvider
                    .getSchemaContext().findModule(nodeType.getNamespace(), nodeType.getRevision());
            firstElemModule.ifPresent(module -> pathBuilder.setElem(0, firstElement.toBuilder()
                    .setName(String.format("%s:%s", module.getName(), firstElement.getName()))));
        }
        final Gnmi.Path resultingPath = pathBuilder.build();
        LOG.debug("Resulting gNMI Path of identifier {} is {}", path, resultingPath);
        return resultingPath;
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
            String keyValue;
            final var value = entry.getValue();
            if (value instanceof QName qnameValue) {
                keyValue = qnameValue.getLocalName();
            } else {
                keyValue = value.toString();
            }
            builder.putKey(entry.getKey().getLocalName(), keyValue);
        }
    }

    private static boolean doesDefineKeys(final YangInstanceIdentifier.PathArgument pathArgument) {
        return pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates;
    }

}
