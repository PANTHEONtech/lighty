/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

public enum Statement {

    MODULE("module"),
    AUGMENT("augment"),
    TYPEDEF("typedef"),
    TYPE("type"),
    ENUM("enum"),
    CONTAINER("container"),
    LIST("list"),
    LEAF("leaf"),
    LEAF_LIST("leaf-list"),
    CHOICE("choice"),
    CASE("case"),
    GROUPING("grouping"),
    IMPORT("import"),
    REVISION("revision");


    private final String text;

    Statement(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

