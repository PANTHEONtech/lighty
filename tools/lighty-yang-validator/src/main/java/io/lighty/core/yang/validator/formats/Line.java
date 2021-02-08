/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnydataEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.AnyxmlEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureAwareDeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.IfFeatureStatement;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.BaseTypes;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.AbstractDeclaredEffectiveStatement;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.DeclaredEffectiveStatementBase;

abstract class Line {

    private static final String BOOLEAN = "boolean";
    private static final String IDENTITYREF = "identityref";
    private static final String ANYXML = "<anyxml>";
    private static final String ANYDATA = "<anydata>";

    final RpcInputOutput inputOutput;
    final List<Integer> removeChoiceQname;
    private final Map<URI, String> namespacePrefix;

    final List<IfFeatureStatement> ifFeatures = new ArrayList<>();
    final List<String> keys = new ArrayList<>();
    final boolean isMandatory;
    final boolean isListOrLeafList;
    final boolean isChoice;
    final boolean isCase;
    Status status;
    String nodeName;
    String flag;
    String path;
    String typeName;

    Line(final SchemaNode node, final RpcInputOutput inputOutput,
         final List<Integer> removeChoiceQname, final Map<URI, String> namespacePrefix, final SchemaContext context,
         final boolean isKey) {
        this.status = node.getStatus();
        this.isMandatory = (node instanceof MandatoryAware && ((MandatoryAware) node).isMandatory())
                || node instanceof ContainerSchemaNode || node instanceof CaseSchemaNode
                || node instanceof NotificationDefinition || node instanceof ActionDefinition
                || node instanceof RpcDefinition || isKey;
        this.isListOrLeafList = node instanceof LeafListSchemaNode || node instanceof ListSchemaNode;
        this.isChoice = node instanceof ChoiceSchemaNode;
        this.isCase = node instanceof CaseSchemaNode;
        this.nodeName = node.getQName().getLocalName();
        this.inputOutput = inputOutput;
        this.removeChoiceQname = removeChoiceQname;
        this.namespacePrefix = namespacePrefix;
        resolveFlag(node, context);
        resolvePathAndType(node);
        resolveKeys(node);
        resolveIfFeatures(node);
    }

    protected abstract void resolveFlag(SchemaNode node, SchemaContext context);

    private void resolveIfFeatures(SchemaNode node) {
        final DeclaredStatement declared = getDeclared(node);
        if (declared instanceof IfFeatureAwareDeclaredStatement) {
            final Collection ifFeature = ((IfFeatureAwareDeclaredStatement) declared).getIfFeatures();
            this.ifFeatures.addAll(ifFeature);
        }
    }

    private DeclaredStatement getDeclared(final SchemaNode node) {
        if (node instanceof DeclaredEffectiveStatementBase) {
            return ((DeclaredEffectiveStatementBase) node).getDeclared();
        } else if (node instanceof AbstractDeclaredEffectiveStatement) {
            return ((AbstractDeclaredEffectiveStatement) node).getDeclared();
        }
        return null;
    }

    private void resolveKeys(SchemaNode node) {
        if (node instanceof ListSchemaNode) {
            for (QName qName : ((ListSchemaNode) node).getKeyDefinition()) {
                keys.add(qName.getLocalName());
            }
        }
    }

    private void resolvePathAndType(SchemaNode node) {
        if (node instanceof TypedDataSchemaNode) {
            TypeDefinition<? extends TypeDefinition<?>> type = ((TypedDataSchemaNode) node).getType();
            if (type instanceof IdentityrefTypeDefinition) {
                typeName = IDENTITYREF;
            } else if (type instanceof BooleanTypeDefinition) {
                typeName = BOOLEAN;
            } else if (type.getBaseType() == null) {
                typeName = type.getQName().getLocalName();
            } else {
                if (nodeName.equals(type.getQName().getLocalName())) {
                    type = type.getBaseType();
                }
                String prefix = namespacePrefix.get(type.getQName().getNamespace());
                if (prefix == null
                        || BaseTypes.isYangBuildInType(type.getPath().getLastComponent().getLocalName())) {
                    typeName = type.getQName().getLocalName();
                } else {
                    typeName = prefix + ":" + type.getQName().getLocalName();
                }
            }
            if (type instanceof LeafrefTypeDefinition) {
                path = ((LeafrefTypeDefinition) type).getPathStatement().toString();
            } else {
                path = null;
            }
        } else if (node instanceof AnydataEffectiveStatement) {
            typeName = ANYDATA;
            path = null;
        } else if (node instanceof AnyxmlEffectiveStatement) {
            typeName = ANYXML;
            path = null;
        } else {
            typeName = null;
            path = null;
        }
    }
}
