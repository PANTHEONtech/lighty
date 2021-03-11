/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Serialize YangInstanceIdentifier.
 *
 * @deprecated This class can be replaced by the implementation of
 * {@link org.opendaylight.yangtools.yang.data.util.AbstractStringInstanceIdentifierCodec}.
 */
@Deprecated(forRemoval = true)
public class SerializeIdentifierCodec {

    private final SchemaContext schemaContext;
    private final DataSchemaContextTree dataSchemaContextTree;

    public SerializeIdentifierCodec(final SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
        this.dataSchemaContextTree = DataSchemaContextTree.from(schemaContext);
    }

    public YangInstanceIdentifier serialize(final String identifier) {
        String data = identifier;
        if (data.startsWith("/")) {
            data = data.substring(1);
        }
        if (data.endsWith("/")) {
            data = data.substring(0, data.length() - 1);
        }
        final List<String> pathArgs = Arrays.asList(data.split("/"));
        final String[] first = pathArgs.get(0).split(":");
        final String moduleName = first[0];
        final Optional<? extends Module> module = this.schemaContext.findModule(moduleName);
        if (! module.isPresent()) {
            throw new IllegalStateException("Module with name" + moduleName + " not found");
        }
        final QNameModule qNameModule = module.get().getQNameModule();
        pathArgs.set(0, first[1]);
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        DataSchemaContextNode<?> schemaNode = this.dataSchemaContextTree.getRoot();
        for (final String args : pathArgs) {
            final QName qName = getQname(qNameModule, args);
            if (schemaNode != null && schemaNode.getChild(qName) != null && schemaNode.getChild(qName).isMixin()) {
                schemaNode = schemaNode.getChild(qName);
                final DataSchemaNode dataSchemaNode = schemaNode.getDataSchemaNode();
                if (dataSchemaNode instanceof ListSchemaNode) {
                    builder.node(qName);
                    final ListSchemaNode listSchemaNode = (ListSchemaNode) dataSchemaNode;
                    builder.node(buildNodeWithKey(listSchemaNode, args, qName));
                    schemaNode = schemaNode.getChild(qName);
                } else if (dataSchemaNode instanceof ChoiceSchemaNode) {
                    builder.node(dataSchemaNode.getQName());
                    builder.node(qName);
                } else if (dataSchemaNode instanceof LeafListSchemaNode) {
                    builder.node(qName);
                    final LeafListSchemaNode leafListSchemaNode = (LeafListSchemaNode) dataSchemaNode;
                    builder.node(buildNodeWithValue(leafListSchemaNode, args));
                }
            } else {
                builder.node(qName);
            }
        }
        return builder.build();
    }

    private static YangInstanceIdentifier.NodeWithValue<?> buildNodeWithValue(
            final LeafListSchemaNode leafListSchemaNode, final String arg) {
        final String[] split = arg.split("=");
        Preconditions.checkArgument(split.length == 2);
        return new YangInstanceIdentifier.NodeWithValue<>(leafListSchemaNode.getQName(), split[1]);
    }

    private static YangInstanceIdentifier.NodeIdentifierWithPredicates buildNodeWithKey(
            final ListSchemaNode listSchemaNode, final String pathArg, final QName qname) {
        Preconditions.checkArgument(pathArg.contains("="), "pathArg does not containg list with keys");
        final String[] listWithKeys = pathArg.split("=");
        final String keys = listWithKeys[1];
        final Map<QName, Object> mapKeys = new HashMap<>();
        final String[] keyValues = keys.split(",");
        for (int i = 0; i < listSchemaNode.getKeyDefinition().size(); i++) {
            Preconditions.checkArgument(keyValues.length > i, "all key values must be present");
            mapKeys.put(listSchemaNode.getKeyDefinition().get(i), keyValues[i]);
        }
        return YangInstanceIdentifier.NodeIdentifierWithPredicates.of(qname, mapKeys);
    }

    private static QName getQname(final QNameModule qnameModule, final String args) {
        if (args.contains("=")) {
            final String[] listWithKeys = args.split("=");
            return QName.create(qnameModule, listWithKeys[0]);
        }
        return QName.create(qnameModule, args);
    }

}
