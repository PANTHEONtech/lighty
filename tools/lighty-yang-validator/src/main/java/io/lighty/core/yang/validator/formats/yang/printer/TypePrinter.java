/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LengthConstraint;
import org.opendaylight.yangtools.yang.model.api.type.PatternConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeRestrictedTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

class TypePrinter {

    private final StatementPrinter printer;
    private final Map<QNameModule, String> moduleToPrefix;

    TypePrinter(final StatementPrinter printer, final Map<QNameModule, String> moduleToPrefix) {
        this.printer = printer;
        this.moduleToPrefix = moduleToPrefix;
    }

    void printTypeDef(final QName name, final TypeDefinition base) {
        printer.openStatement(Statement.TYPEDEF, name.getLocalName());
        printType(base);
        printer.closeStatement();
    }

    void printTypeUsage(final TypeDefinition type) {
        printer.printSimple("type", getTypeName(type));
    }

    void printType(final TypeDefinition type) {
        final String rootName = Util.getRootType(type).getQName().getLocalName();
        if (type instanceof EnumTypeDefinition) {
            printer.openStatement(Statement.TYPE, "enumeration");
            final EnumTypeDefinition enumTypeDefinition = (EnumTypeDefinition) type;
            for (final EnumTypeDefinition.EnumPair enumPair : enumTypeDefinition.getValues()) {
                printEnum(enumPair);
            }
            printer.closeStatement();
        } else if (type instanceof UnionTypeDefinition) {
            final UnionTypeDefinition unionTypeDefinition = (UnionTypeDefinition) type;
            printer.openStatement(Statement.TYPE, "union");
            for (final TypeDefinition<?> unionType : unionTypeDefinition.getTypes()) {
                printUnion(unionType);
            }
            printer.closeStatement();
        } else if (type instanceof DecimalTypeDefinition) {
            printer.openStatement(Statement.TYPE, rootName);
            final DecimalTypeDefinition decimalTypeDefinition = ((DecimalTypeDefinition) type);
            printer.printSimple("fraction-digits", Integer.toString(decimalTypeDefinition.getFractionDigits()));
            final Optional<RangeConstraint<BigDecimal>> rangeConstraint = decimalTypeDefinition.getRangeConstraint();
            if (rangeConstraint.isPresent()) {
                printer.printSimple("range", "\"" + rangeToString(rangeConstraint.get().getAllowedRanges()) + "\"");
            }
            printer.closeStatement();
        } else if (type instanceof StringTypeDefinition) {
            final StringTypeDefinition stringType = (StringTypeDefinition) type;
            if (stringType.getLengthConstraint().isPresent() || !stringType.getPatternConstraints().isEmpty()) {
                printer.openStatement(Statement.TYPE, rootName);
                if (stringType.getLengthConstraint().isPresent()
                        && isRestricted(stringType.getLengthConstraint().get())) {
                    printer.printSimple("length", "\""
                            + rangeToString(stringType.getLengthConstraint().get().getAllowedRanges()) + "\"");
                }
                for (final PatternConstraint patternConstraint : stringType.getPatternConstraints()) {
                    printer.printSimple("pattern", "\"" + patternConstraint.getRegularExpressionString() + "\"");
                }
                printer.closeStatement();
            } else {
                printer.printSimple("type", rootName);
            }
        } else if (type instanceof RangeRestrictedTypeDefinition) {
            printer.openStatement(Statement.TYPE, rootName);
            final RangeRestrictedTypeDefinition rangeRestrictedTypeDefinition = ((RangeRestrictedTypeDefinition) type);
            final Optional<RangeConstraint> rangeConstraint = rangeRestrictedTypeDefinition.getRangeConstraint();
            if (rangeConstraint.isPresent()) {
                printer.printSimple("range", "\""
                        + rangeToString(rangeConstraint.get().getAllowedRanges()) + "\"");
            }
            printer.closeStatement();
        } else {
            printer.printSimple("type", rootName);
        }
    }

    private boolean isRestricted(final LengthConstraint lengthConstraint) {
        final Set<Range<Integer>> asRanges = lengthConstraint.getAllowedRanges().asRanges();
        if (asRanges.size() > 1) {
            return true;
        }
        final Range<Integer> range = asRanges.iterator().next();
        return range.lowerEndpoint() != 0 || range.upperEndpoint() != Integer.MAX_VALUE;
    }

    private String rangeToString(final RangeSet<? extends Number> rangeSet) {
        return rangeSet.asRanges().stream()
                .map(range -> range.lowerEndpoint() + ".." + range.upperEndpoint())
                .collect(Collectors.joining(" | "));
    }


    private void printEnum(final EnumTypeDefinition.EnumPair enumPair) {
        printer.openStatement(Statement.ENUM, enumPair.getName());
        printer.printSimple("value", Integer.toString(enumPair.getValue()));
        final Optional<String> description = enumPair.getDescription();
        if (description.isPresent()) {
            printer.printSimple("description", "\"" + description.get() + "\"");
        }
        printer.closeStatement();
    }

    private void printUnion(final TypeDefinition type) {
        printer.printSimple("type", getTypeName(type));
    }

    private String getTypeName(final TypeDefinition type) {
        final String typeName;
        final String prefix = moduleToPrefix.getOrDefault(type.getQName().getModule(), "");
        if (prefix.isEmpty()) {
            typeName = type.getQName().getLocalName();
        } else {
            typeName = prefix + ":" + type.getQName().getLocalName();
        }
        return typeName;
    }
}

