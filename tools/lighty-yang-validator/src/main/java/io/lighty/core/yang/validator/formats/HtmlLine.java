/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import com.google.common.collect.Lists;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.CaseEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.InputEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.OutputEffectiveStatement;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.AbstractUndeclaredEffectiveStatement;

public class HtmlLine extends Line {

    private static final String RW = "config";
    private static final String RO = "no config";

    private final String description;
    private final List<Integer> ids;
    private final String schema;

    HtmlLine(final List<Integer> ids, final SchemaNode node, final RpcInputOutput inputOutput,
             final SchemaContext context, final List<Integer> removeChoiceQname, final Map<URI, String> namespacePrefix,
             final Optional<AugmentationSchemaNode> augment, final boolean isKey) {
        super(node, inputOutput, removeChoiceQname, namespacePrefix, context, isKey);
        this.ids = ids;
        if (augment.isPresent()) {
            description = augment.get().getDescription().orElse("");
        } else {
            description = node.getDescription().orElse("");
        }

        if (augment.isPresent()) {
            schema = "augment";
        } else if (node instanceof EffectiveStatement) {
            if (node instanceof AbstractUndeclaredEffectiveStatement) {
                if (node instanceof CaseEffectiveStatement) {
                    schema = "case";
                } else if (node instanceof InputEffectiveStatement) {
                    schema = "input";
                } else if (node instanceof OutputEffectiveStatement) {
                    schema = "output";
                } else {
                    schema = "";
                }
            } else {
                schema = ((EffectiveStatement) node).getDeclared().statementDefinition().getStatementName()
                        .getLocalName();
            }
        } else {
            schema = "";
        }

        Iterable<QName> pathFromRoot;
        if (augment.isPresent()) {
            pathFromRoot = augment.get().getTargetPath().getNodeIdentifiers();
            nodeName = augment.get().getTargetPath().asSchemaPath().getLastComponent().getLocalName();
            status = augment.get().getStatus();
            flag = "";
        } else {
            pathFromRoot = node.getPath().getPathFromRoot();
        }
        final StringBuilder pathBuilder = new StringBuilder();
        for (QName path : pathFromRoot) {
            final String prefix = namespacePrefix.getOrDefault(path.getNamespace(),
                    context.findModule(path.getModule()).get().getPrefix());

            pathBuilder.append('/')
                    .append(prefix)
                    .append(':')
                    .append(path.getLocalName());
        }
        path = pathBuilder.toString();

    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        final String id = ids.stream().map(String::valueOf).collect(Collectors.joining("."));
        String pid = "";
        if (ids.size() > 1) {
            pid = ids.subList(0, ids.size() - 1)
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("."));
        }
        if (typeName == null) {
            typeName = "";
        }
        String key = "";
        if (!keys.isEmpty()) {
            key = "[" + String.join(",", keys) + "]";
        }
        builder.append("<tr data-node-id=\"")
                .append(id)
                .append("\" data-node-pid=\"")
                .append(pid)
                .append("\">")
                .append("<td title=\"")
                .append(description)
                .append("\">")
                .append(nodeName)
                .append(key);
        if ("container".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-folder-open\"></i></span> </td>");
        } else if ("list".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-list\"></i></span> </td>");
        } else if ("leaf-list".equals(schema)) {
            builder.append(" <span><i class=\"fab fa-pagelines\"></i></span> </td>");
        } else if ("augment".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-external-link-alt\"></i></span> </td>");
        } else if ("rpc".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-envelope\"></i></span> </td>");
        } else if ("notification".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-bell\"></i></span> </td>");
        } else if ("choice".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-tasks\"></i></span> </td>");
        } else if ("case".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-check\"></i></span> </td>");
        } else if ("input".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-share\"></i></span> </td>");
        } else if ("output".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-reply\"></i></span> </td>");
        } else if ("action".equals(schema)) {
            builder.append(" <span><i class=\"fas fa-play\"></i></span> </td>");
        }
        else {
            builder.append(" <span><i class=\"fas fa-leaf\"></i></span> </td>");
        }
        final String enclosingTd = "</td>";
        builder.append("<td>")
                .append(schema)
                .append(enclosingTd)
                .append("<td>")
                .append(typeName)
                .append(enclosingTd)
                .append("<td>")
                .append(flag)
                .append(enclosingTd)
                .append("<td>");
        switch (status) {
            case CURRENT:
                builder.append("current");
                break;
            case OBSOLETE:
                builder.append("obsolete");
                break;
            case DEPRECATED:
                builder.append("deprecated");
                break;
            default:
                break;
        }
        builder.append(enclosingTd)
                .append("<td>")
                .append(path)
                .append(enclosingTd)
                .append("</tr>");

        return builder.toString();
    }

    @Override
    protected void resolveFlag(SchemaNode node, SchemaContext context) {
        if (node instanceof CaseSchemaNode || node instanceof RpcDefinition || node instanceof NotificationDefinition
                || node instanceof ActionDefinition) {
            // do not emit the "config/no config" for rpc/action/notification/case SchemaNode
            this.flag = "";
        } else if (context.findNotification(node.getPath().getPathFromRoot().iterator().next()).isPresent()) {
            this.flag = RO;
        } else if (this.inputOutput == RpcInputOutput.INPUT) {
            this.flag = RW;
        } else if (this.inputOutput == RpcInputOutput.OUTPUT) {
            this.flag = RO;
        } else if (node instanceof DataSchemaNode) {
            final ArrayList<QName> qNames = Lists.newArrayList(node.getPath().getPathFromRoot().iterator());
            final ListIterator<Integer> integerListIterator =
                    this.removeChoiceQname.listIterator(this.removeChoiceQname.size());
            while (integerListIterator.hasPrevious()) {
                qNames.remove(integerListIterator.previous().intValue());
            }
            if (node instanceof ChoiceSchemaNode) {
                qNames.remove(qNames.size() - 1);
                if (context.findDataTreeChild(qNames).get().isConfiguration()
                        && ((ChoiceSchemaNode) node).isConfiguration()) {
                    this.flag = RW;
                } else {
                    this.flag = RO;
                }
            } else if (context.findDataTreeChild(qNames).get().isConfiguration()) {
                this.flag = RW;
            } else {
                this.flag = RO;
            }
        } else {
            this.flag = RW;
        }
    }
}
