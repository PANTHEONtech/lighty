/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.type.BaseTypes;

class SmartTypePrintingStrategy {

    private final Map<QNameModule, String> moduleToPrefix;
    private final Module module;
    private final Set<TypeDefinition> typedefs = new TreeSet<>(Comparator.comparing(SchemaNode::getQName));

    SmartTypePrintingStrategy(final Module module, final Map<QNameModule, String> moduleToPrefix) {

        this.module = module;
        this.moduleToPrefix = moduleToPrefix;
    }

    void printTypedefs(final StatementPrinter printer, final Set<TypeDefinition> usedTypes) {
        final TypePrinter typePrinter = new TypePrinter(printer, moduleToPrefix);
        for (final TypeDefinition<?> typeDefinition : module.getTypeDefinitions()) {
            if (usedTypes.contains(typeDefinition)) {
                typedefs.add(typeDefinition);
            }
        }
        for (final TypeDefinition typedef : typedefs) {

            typePrinter.printTypeDef(typedef.getQName(), typedef);
        }
    }

    void printType(final StatementPrinter printer, final TypedDataSchemaNode typed) {
        final TypePrinter typePrinter = new TypePrinter(printer, moduleToPrefix);
        final TypeDefinition<? extends TypeDefinition<?>> type = typed.getType();
        TypeDefinition<? extends TypeDefinition<?>> maybeTypedef = type;
        while (maybeTypedef.getBaseType() != null) {
            if (typedefs.contains(maybeTypedef) || !type.getQName().getModule().equals(module.getQNameModule())) {
                typePrinter.printTypeUsage(maybeTypedef);
                return;
            }
            maybeTypedef = maybeTypedef.getBaseType();
        }
        if (typed.getType() instanceof BooleanTypeDefinition
                && !"boolean".equals(typed.getType().getQName().getLocalName())) {
            typePrinter.printType(BaseTypes.booleanType());
        } else {
            typePrinter.printType(type);
        }
    }
}

