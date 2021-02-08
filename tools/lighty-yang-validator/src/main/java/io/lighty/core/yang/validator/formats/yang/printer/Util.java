/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

final class Util {

    private Util() {
        // NO-OP
    }

    static TypeDefinition getRootType(final TypeDefinition typeDefinition) {
        TypeDefinition typeDef = typeDefinition;
        while (typeDef.getBaseType() != null) {
            typeDef = typeDef.getBaseType();
        }
        return typeDef;
    }

    static boolean isBaseType(final TypeDefinition<? extends TypeDefinition<?>> type) {
        return type.getQName().getNamespace().toString().equals("urn:ietf:params:xml:ns:yang:1");
    }
}

