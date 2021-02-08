/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

abstract class Indenting {

    abstract void println(int level, String name, String text, boolean separately);

    abstract void println(int level, String text);

    abstract void println(String text);

    static String indent(final int level, String name, final String text, boolean separately) {
        final String[] textLines = text.split("\n");
        final StringBuilder builder = new StringBuilder();

        if (textLines.length > 1) {
            separately = true;
        }
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
        if (!separately) {
            if (!name.isEmpty()) {
                name += " ";
            }
        }
        builder.append(name);
        if (separately) {
            builder.append("\n");
        }
        boolean firstLine = true;
        for (String line : textLines) {
            if (separately && !line.isEmpty()) {
                for (int i = 0; i <= level; i++) {
                    builder.append("    ");
                }
            }
            builder.append(line);
            if (textLines.length > 1) {
                if (firstLine) {
                    builder.setLength(builder.length() - 1);
                    firstLine = false;
                }
                builder.append("\n");
            }
        }
        if (textLines.length > 1) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }
}

