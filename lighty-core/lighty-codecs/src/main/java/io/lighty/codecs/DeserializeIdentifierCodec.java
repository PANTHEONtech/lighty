/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Deserialize YangInstanceIdentifier.
 *
 * @deprecated This class can be replaced by the implementation of
 * {@link org.opendaylight.yangtools.yang.data.util.AbstractStringInstanceIdentifierCodec}.
 */
@Deprecated(forRemoval = true)
public class DeserializeIdentifierCodec {

    private final DataSchemaContextTree dataSchemaContextTree;
    private final SchemaContext schemaContext;

    public DeserializeIdentifierCodec(final EffectiveModelContext schemaContext) {
        this.schemaContext = schemaContext;
        this.dataSchemaContextTree = DataSchemaContextTree.from(schemaContext);
    }

    public final String deserialize(final YangInstanceIdentifier identifier) {
        final List<YangInstanceIdentifier.PathArgument> pathArguments = identifier.getPathArguments();
        final QName nodeType = pathArguments.get(0).getNodeType();
        final List<String> elements = new ArrayList<>();
        DataSchemaContextNode<?> current = this.dataSchemaContextTree.getRoot();
        for (final YangInstanceIdentifier.PathArgument arg : pathArguments) {
            current = current.getChild(arg);
            Preconditions.checkArgument(current != null, "Invalid input %s: schema for argument %s not found",
                    identifier, arg);

            if (current.isMixin()) {
                continue;
            }
            if (arg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
                elements.add(buildListArg(arg, current.getDataSchemaNode()));
            } else if (arg instanceof YangInstanceIdentifier.NodeWithValue) {
                elements.add(buildLeafListArg(arg));
            } else {
                elements.add(arg.getNodeType().getLocalName());
            }
        }
        String revision;
        final Optional<Module> module;
        final Optional<Revision> moduleRevision = nodeType.getRevision();
        if (moduleRevision.isPresent()) {
            revision = moduleRevision.get().toString();
            module = this.schemaContext.findModule(nodeType.getNamespace(), moduleRevision.get());
        } else {
            revision = "[not present]";
            module = this.schemaContext.findModule(nodeType.getNamespace());
        }
        if (! module.isPresent()) {
            throw new IllegalStateException("Module with namespace " + nodeType.getNamespace() + " and revision "
                    + revision + " not found");
        }
        return "/" + module.get().getName() + ":" + String.join("/", elements);
    }

    private static String buildLeafListArg(final YangInstanceIdentifier.PathArgument pathArgument) {
        Preconditions.checkState(pathArgument instanceof YangInstanceIdentifier.NodeWithValue<?>);
        final YangInstanceIdentifier.NodeWithValue<?> node = (YangInstanceIdentifier.NodeWithValue<?>) pathArgument;
        return node.getNodeType().getLocalName() + "=" + node.getValue();
    }

    private static String buildListArg(final YangInstanceIdentifier.PathArgument pathArgument,
            final DataSchemaNode schemaNode) {
        final YangInstanceIdentifier.NodeIdentifierWithPredicates listId =
                (YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArgument;
        Preconditions.checkState(schemaNode instanceof ListSchemaNode);
        final ListSchemaNode listSchemaNode = (ListSchemaNode) schemaNode;
        final List<QName> keyDefinition = listSchemaNode.getKeyDefinition();
        final StringBuilder builder = new StringBuilder(listId.getNodeType().getLocalName());
        builder.append("=");
        final List<String> keyValue = new ArrayList<>();
        for (final QName qname : keyDefinition) {
            final Object value = listId.getValue(qname);
            if (value == null) {
                throw new IllegalStateException("all key values must be present");
            }
            keyValue.add(value.toString());
        }
        builder.append(String.join(",", keyValue));
        return builder.toString();
    }
}
