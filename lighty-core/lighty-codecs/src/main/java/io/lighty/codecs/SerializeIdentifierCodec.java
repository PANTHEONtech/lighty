/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import com.google.common.base.Preconditions;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeIdentifierCodec {
    private static final Logger LOG = LoggerFactory.getLogger(SerializeIdentifierCodec.class);

    private final SchemaContext schemaContext;
    private final DataSchemaContextTree dataSchemaContextTree;

    public SerializeIdentifierCodec(final SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
        this.dataSchemaContextTree = DataSchemaContextTree.from(schemaContext);
    }

    /**
     * Returns an unique {@link YangInstanceIdentifier} based on the provided String identifier.
     * @param identifier URI like String identifier of a node in the schema tree formatted like:
     *                   <br>
     *                   {@code MODULE_NAME@REVISION:path_to_node/..}
     *                   <br>
     *                   If the specified revision is not present (or is wrongly formatted) and there are
     *                   multiple revisions of the MODULE_NAME loaded, we perform the serialization on
     *                   the latest revision.
     *
     * @return YangInstanceIdentifier of a node
     */
    public YangInstanceIdentifier serialize(final String identifier) {
        String data = identifier;
        if (data.startsWith("/")) {
            data = data.substring(1);
        }
        if (data.endsWith("/")) {
            data = data.substring(0, data.length() - 1);
        }
        final List<String> pathArgs = Arrays.asList(data.split("/"));
        final String[] nameRevision_pathStart = pathArgs.get(0).split(":");
        final String[] name_Revision = nameRevision_pathStart[0].split("@");
        final String moduleName = name_Revision[0];
        String revisionString = null;
        if (name_Revision.length > 1) {
            revisionString = name_Revision[1];
        }
        Optional<Revision> requestedRevision = Optional.empty();
        try {
            requestedRevision = Revision.ofNullable(revisionString);
        } catch (DateTimeException e) {
            LOG.warn("Wrongly formatted revision detected {} ", revisionString);
        }
        Collection<? extends Module> modules = this.schemaContext.findModules(moduleName);

        if (modules.isEmpty()) {
            throw new IllegalStateException("Module with name " + moduleName + " not found");
        }

        Optional<? extends Module> foundModule = Optional.empty();

        if (requestedRevision.isPresent()) {
            Optional<Revision> finalRequestedRevision = requestedRevision;
            foundModule = modules.stream()
                    .filter(m -> m.getRevision().isPresent())
                    .filter(m -> m.getRevision().get().equals(finalRequestedRevision.get()))
                    .findFirst();
        }

        if (modules.size() > 1 && foundModule.isEmpty()) {
            LOG.debug("Multiple revisions of module {} found: ", moduleName);
            modules = modules.stream()
                    .filter(m -> m.getRevision().isPresent())
                    .peek(m -> LOG.debug("\t Revision: {} ", m.getRevision().get().toString()))
                    .sorted((m1, m2) -> m2.getRevision().get().compareTo(m1.getRevision().get()))
                    .collect(Collectors.toList());
            LOG.debug("Using latest revision");
        }
        Module module;
        if (foundModule.isEmpty()) {
            module = modules.stream().findFirst().get();
        } else {
            module = foundModule.get();
        }
        LOG.debug("Using module {} with revision: {} ", module.getName(),
                module.getRevision().isPresent() ? module.getRevision().get() : "none");

        final QNameModule qNameModule = module.getQNameModule();
        pathArgs.set(0, nameRevision_pathStart[1]);
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        DataSchemaContextNode<?> schemaNode = this.dataSchemaContextTree.getRoot();
        for (final String args : pathArgs) {
            final QName qName = getQname(qNameModule, args);
            DataSchemaContextNode<?> foundChild = schemaNode.getChild(qName);
            if (foundChild != null && foundChild.isMixin()) {
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
