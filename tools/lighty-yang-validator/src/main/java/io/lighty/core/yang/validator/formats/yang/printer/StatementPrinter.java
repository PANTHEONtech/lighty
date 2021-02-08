/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

class StatementPrinter {

    private final Indenting printer;
    private int indentation;

    StatementPrinter(final Indenting printer) {
        this.printer = printer;
    }

    void openStatement(final Statement type, final String name) {
        printer.println(indentation, type.getText() + " " + name + " {");
        indentation++;
    }

    void openStatement(final String name) {
        printer.println(indentation, name + " {");
        indentation++;
    }

    void closeStatement() {
        indentation--;
        printer.println(indentation, "}");
    }

    void printSimple(final String name, final String text) {
        printer.println(indentation, name, text + ";", false);
    }

    void printSimpleSeparately(final String name, final String text) {
        printer.println(indentation, name, text + ";", true);
    }

    void printEmptyLine() {
        printer.println("");
    }

    void printConfig(final boolean config) {
        printSimple("config", Boolean.toString(config));
    }
}

