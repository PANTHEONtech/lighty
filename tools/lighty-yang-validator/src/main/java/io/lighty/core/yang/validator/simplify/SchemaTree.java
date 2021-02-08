/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.simplify;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class SchemaTree implements Comparable<SchemaTree> {
    private final QName qname;
    private final DataSchemaNode schemaNode;
    private final boolean isRootNode;
    private final boolean isAugmenting;
    private final ActionDefinition actionNode;

    private final Map<SchemaPath, SchemaTree> children = new LinkedHashMap<>();

    SchemaTree(final QName qname, final DataSchemaNode schemaNode,
               final boolean isRootNode, final boolean isAugmenting,
               final ActionDefinition actionNode) {
        this.qname = qname;
        this.schemaNode = schemaNode;
        this.isRootNode = isRootNode;
        this.isAugmenting = isAugmenting;
        this.actionNode = actionNode;
    }

    private QName getQname() {
        return qname;
    }

    public boolean isRootNode() {
        return this.isRootNode;
    }

    public boolean isAugmenting() {
        return this.isAugmenting;
    }

    public DataSchemaNode getSchemaNode() {
        return schemaNode;
    }

    public ActionDefinition getActionNode() {
        return actionNode;
    }

    public void addChild(final SchemaTree tree) {
        children.putIfAbsent(tree.getSchemaNode().getPath(), tree);
    }

    public SchemaTree addChild(final DataSchemaNode schemaNodeInput, final boolean isRootNodeInput,
                               final boolean isAugmentingInput) {
        final SchemaTree tree = new SchemaTree(schemaNodeInput.getQName(), schemaNodeInput,
                isRootNodeInput, isAugmentingInput, null);
        final SchemaTree prev = children.putIfAbsent(tree.getSchemaNode().getPath(), tree);
        return prev == null ? tree : prev;
    }

    SchemaTree addChild(final ActionDefinition schemaNodeInput, final boolean isRootNodeInput,
                        final boolean augmentation) {
        final SchemaTree tree = new SchemaTree(schemaNodeInput.getQName(), null,
                isRootNodeInput, augmentation, schemaNodeInput);
        final SchemaTree prev = children.putIfAbsent(tree.getActionNode().getPath(), tree);
        return prev == null ? tree : prev;
    }

    public Map<SchemaPath, SchemaTree> getChildren() {
        return children;
    }

    public Map<SchemaPath, SchemaTree> getDataSchemaNodeChildren() {
        Map<SchemaPath, SchemaTree> ret = new LinkedHashMap<>();
        for (Map.Entry<SchemaPath, SchemaTree> child : children.entrySet()) {
            if (child.getValue().getSchemaNode() != null) {
                ret.put(child.getKey(), child.getValue());
            }
        }
        return ret;
    }

    public Map<SchemaPath, SchemaTree> getActionDefinitionChildren() {
        Map<SchemaPath, SchemaTree> ret = new LinkedHashMap<>();
        for (Map.Entry<SchemaPath, SchemaTree> child : children.entrySet()) {
            if (child.getValue().getActionNode() != null) {
                ret.put(child.getKey(), child.getValue());
            }
        }
        return ret;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SchemaTree that = (SchemaTree) obj;
        return Objects.equals(qname, that.qname)
                && Objects.equals(schemaNode, that.schemaNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qname, schemaNode);
    }

    @Override
    public int compareTo(final SchemaTree originalTree) {
        return this.getQname().compareTo(originalTree.getQname());
    }
}

