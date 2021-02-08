/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import io.lighty.core.yang.validator.GroupArguments;
import io.lighty.core.yang.validator.config.DependConfiguration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;

public class Depends extends FormatPlugin {

    private static final String HELP_NAME = "depend";
    private static final String HELP_DESCRIPTION = "return all module`s dependencies";
    private static final String MODULE = "module ";
    private static final String AT = "@";
    private static final String DEPENDS_TEXT = " depends on following modules: ";
    private static final String SPACE = " ";
    private static final String NON_RECURSIVE = "(not recursively)";
    private static final String ONLY_MODULES = "(Imports only)";
    private static final String ONLY_SUBMODULES = "(Submodules only)";

    private final Set<String> modules = new HashSet<>();

    public Depends() {
        super(Depends.class);
    }

    @Override
    public void emitFormat() {
        final DependConfiguration dependConfiguration = this.configuration.getDependConfiguration();
        for (final RevisionSourceIdentifier source : this.sources) {
            final Module module = this.schemaContext.findModule(source.getName(), source.getRevision()).get();
            final StringBuilder dependantsBuilder = new StringBuilder(MODULE);
            dependantsBuilder.append(module.getName())
                    .append(AT);
            if (module.getRevision().isPresent()) {
                dependantsBuilder.append(module.getRevision().get());
            }
            dependantsBuilder.append(DEPENDS_TEXT);
            if (!dependConfiguration.isModuleImportsOnly()) {
                resolveSubmodules(module, dependConfiguration);
            }
            if (!dependConfiguration.isModuleIncludesOnly()) {
                resolveImports(module, dependConfiguration);
            }
            for (final String name : modules) {
                dependantsBuilder.append(name)
                        .append(SPACE);
            }
            if (dependConfiguration.isModuleImportsOnly()) {
                dependantsBuilder.append(ONLY_MODULES);
            } else if (dependConfiguration.isModuleIncludesOnly()) {
                dependantsBuilder.append(ONLY_SUBMODULES);
            }
            if (dependConfiguration.isModuleDependentsOnly()) {
                dependantsBuilder.append(NON_RECURSIVE);
            }
            final String dependandsText = dependantsBuilder.toString();
            log.info(dependandsText);
        }
    }

    private void resolveImports(final Module module, final DependConfiguration dependConfiguration) {
        for (ModuleImport m : module.getImports()) {
            final String moduleName = m.getModuleName();
            if (dependConfiguration.getExcludedModuleNames().contains(moduleName)) {
                continue;
            }
            for (Module contextModule : this.schemaContext.getModules()) {
                if (moduleName.equals(contextModule.getName())) {
                    final Optional<Revision> importedModuleRevision = m.getRevision();
                    final Optional<Revision> contextModuleRevision = contextModule.getRevision();
                    if (importedModuleRevision.isPresent() && contextModuleRevision.isPresent()) {
                        if (!contextModuleRevision.get().toString().equals(importedModuleRevision.get().toString())) {
                            continue;
                        }
                    }
                    modules.add(contextModule.getName());
                    if (!dependConfiguration.isModuleDependentsOnly()) {
                        if (!dependConfiguration.isModuleImportsOnly()) {
                            resolveSubmodules(contextModule, dependConfiguration);
                        }
                        resolveImports(contextModule, dependConfiguration);
                    }
                    break;
                }
            }
        }
    }

    private void resolveSubmodules(final Module module, final DependConfiguration dependConfiguration) {
        final StringBuilder dependantsBuilder = new StringBuilder();
        for (Module m : module.getSubmodules()) {
            final String moduleName = m.getName();
            if (dependConfiguration.getExcludedModuleNames().contains(moduleName)) {
                continue;
            }
            dependantsBuilder.append(moduleName);
            final Optional<Revision> revision = m.getRevision();
            if (revision.isPresent()) {
                dependantsBuilder
                        .append(AT)
                        .append(revision.get().toString());
            }
            final String moduleWithRevision = dependantsBuilder.toString();
            if (dependConfiguration.getExcludedModuleNames().contains(moduleWithRevision)) {
                continue;
            }
            modules.add(moduleWithRevision);
            if (!dependConfiguration.isModuleDependentsOnly()) {
                if (!dependConfiguration.isModuleIncludesOnly()) {
                    resolveImports(m, dependConfiguration);
                }
                resolveSubmodules(m, dependConfiguration);
            }
        }
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        final GroupArguments groupArguments = new GroupArguments(HELP_NAME,
                "Depend format based arguments: ");
        groupArguments.addOption("List dependencies of the module only (do not look"
                        + "recursively in other imported, included modules).",
                Collections.singletonList("--module-depends-only"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("List module dependencies of the module"
                        + " (only imports, no submodules will be included).",
                Collections.singletonList("--modules-only"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("List submodule dependencies of the module"
                        + " (only includes, no imported modules will be included).",
                Collections.singletonList("--submodules-only"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("List dependencies of the module"
                        + " but exclude any module listed in this option. Warning -"
                        + " This will exclude all the (sub)modules in the excluded module"
                        + " as well.",
                Collections.singletonList("--exclude-module-name"), false, "*", Collections.emptyList(),
                new CollectionArgumentChoice<>(Collections.emptyList()), List.class);
        return Optional.of(groupArguments);
    }
}
