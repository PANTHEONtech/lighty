/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

import java.io.PrintStream;

class IndentingPrinter extends Indenting {
    private final PrintStream printStream;

    IndentingPrinter(final PrintStream printStream) {
        this.printStream = printStream;
    }

    void println(int level, final String name, final String text, final boolean separately) {
        printStream.println(indent(level, name, text, separately));
    }

    void println(final int level, final String text) {
        printStream.println(indent(level, "", text, false));
    }

    void println(final String text) {
        printStream.println(text);
    }
}


