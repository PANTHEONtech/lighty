/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.config;

import java.util.Set;

public class DependConfiguration {

    private final boolean moduleIncludesOnly;
    private final boolean moduleImportsOnly;
    private final boolean moduleDependentsOnly;
    private final Set<String> excludedModuleNames;

    DependConfiguration(final boolean moduleDependentsOnly, final boolean moduleImportsOnly,
                        final boolean moduleIncludesOnly, final Set<String> excludedModuleNames) {
        this.moduleIncludesOnly = moduleIncludesOnly;
        this.moduleDependentsOnly = moduleDependentsOnly;
        this.moduleImportsOnly = moduleImportsOnly;
        this.excludedModuleNames = excludedModuleNames;
    }

    public boolean isModuleIncludesOnly() {
        return moduleIncludesOnly;
    }

    public boolean isModuleImportsOnly() {
        return moduleImportsOnly;
    }

    public boolean isModuleDependentsOnly() {
        return moduleDependentsOnly;
    }

    public Set<String> getExcludedModuleNames() {
        return excludedModuleNames;
    }
}
