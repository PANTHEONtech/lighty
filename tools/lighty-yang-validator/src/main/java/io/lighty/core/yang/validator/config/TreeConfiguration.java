/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.config;

public class TreeConfiguration {

    private final int treeDepth;
    private final int lineLength;
    private final boolean help;
    private final boolean modulePrefix;
    private final boolean prefixMainModule;

    TreeConfiguration(final int treeDepth, final int lineLength, final boolean help,
                      final boolean modulePrefix, final boolean prefixMainModule) {
        this.treeDepth = treeDepth;
        this.lineLength = lineLength;
        this.help = help;
        this.modulePrefix = modulePrefix;
        this.prefixMainModule = prefixMainModule;
    }

    public boolean isPrefixMainModule() {
        return prefixMainModule;
    }

    public boolean isModulePrefix() {
        return modulePrefix;
    }

    public boolean isHelp() {
        return help;
    }

    public int getTreeDepth() {
        return treeDepth;
    }

    public int getLineLength() {
        return lineLength;
    }
}
