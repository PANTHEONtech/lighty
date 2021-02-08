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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureStatement;

public class ConsoleLine extends Line {

    private static final String RW = "rw";
    private static final String RO = "ro";
    private final List<Boolean> isConnected;

    ConsoleLine(final List<Boolean> isConnected, final SchemaNode node, RpcInputOutput inputOutput,
                final SchemaContext context, final List<Integer> removeChoiceQname,
                final Map<URI, String> namespacePrefix, final boolean isKey) {
        super(node, inputOutput, removeChoiceQname, namespacePrefix, context, isKey);
        this.isConnected = isConnected;
    }

    protected void resolveFlag(SchemaNode node, SchemaContext context) {
        if (node instanceof CaseSchemaNode) {
            this.flag = "";
        } else if (node instanceof NotificationDefinition) {
            this.flag = "-n";
        } else if (context.findNotification(node.getPath().getPathFromRoot().iterator().next()).isPresent()) {
            this.flag = RO;
        } else if (this.inputOutput == RpcInputOutput.INPUT) {
            this.flag = "-w";
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
            this.flag = "-x";
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("  ");

        for (boolean connection : isConnected) {
            if (connection) {
                builder.append('|');
            } else {
                builder.append(" ");
            }
            builder.append("  ");
        }
        switch (status) {
            case CURRENT:
                builder.append('+');
                break;
            case OBSOLETE:
                builder.append('o');
                break;
            case DEPRECATED:
                builder.append('x');
                break;
            default:
                break;
        }
        builder.append("--").append(flag);
        builder.append(" ");
        if (isChoice) {
            builder.append('(').append(nodeName).append(')');
        } else if (isCase) {
            builder.append(":(").append(nodeName).append(')');
        } else {
            builder.append(nodeName);
        }
        if (isListOrLeafList) {
            builder.append('*');
            if (!keys.isEmpty()) {
                builder.append(" [");
                final Iterator<String> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    builder.append(iterator.next());
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                }
                builder.append(']');
            }
        } else if (!isMandatory) {
            builder.append('?');
        }
        if (path != null) {
            builder.append("    -> ").append(path);
        } else if (typeName != null) {
            builder.append("       ").append(typeName);
        }
        final Iterator<IfFeatureStatement> ifFeaturesIterator = ifFeatures.iterator();
        if (ifFeaturesIterator.hasNext()) {
            builder.append(" {");
            while (ifFeaturesIterator.hasNext()) {
                builder.append(ifFeaturesIterator.next().rawArgument());
                if (ifFeaturesIterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("}?");
        }
        return builder.toString();
    }
}