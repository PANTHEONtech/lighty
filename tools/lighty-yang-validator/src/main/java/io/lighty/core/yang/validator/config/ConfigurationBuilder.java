/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.config;

import io.lighty.core.yang.validator.LyvParameters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opendaylight.yangtools.yang.common.QName;

public class ConfigurationBuilder {

    private final Configuration configuration;

    public ConfigurationBuilder() {
        this.configuration = new Configuration();
    }

    public ConfigurationBuilder setTreeConfiguration(final int treeDepth, final int lineLength,
                                                     final boolean help, final boolean modulePrefix,
                                                     final boolean treePrefixMainModule) {
        final TreeConfiguration treeConfiguration = new TreeConfiguration(treeDepth, lineLength, help,
                modulePrefix, treePrefixMainModule);
        this.configuration.setTreeConfiguration(treeConfiguration);
        return this;
    }

    public ConfigurationBuilder setDependConfiguration(final boolean moduleDependentsOnly,
                                                       final boolean moduleImportsOnly,
                                                       final boolean moduleIncludesOnly,
                                                       final Set<String> excludedModuleNames) {
        final DependConfiguration dependConfiguration = new DependConfiguration(moduleDependentsOnly,
                moduleImportsOnly, moduleIncludesOnly, excludedModuleNames);
        this.configuration.setDependConfiguration(dependConfiguration);
        return this;
    }

    public ConfigurationBuilder setUpdateFrom(final String checkUpdateFrom) {
        this.configuration.setUpdateFrom(checkUpdateFrom);
        return this;
    }

    public ConfigurationBuilder setCheckUpdateFromConfiguration(final int rfcVersion,
                                                                final List<String> checkUpdateFromPath) {
        final CheckUpdateFromConfiguration checkUpdateFromConfiguration =
                new CheckUpdateFromConfiguration(rfcVersion, checkUpdateFromPath);
        this.configuration.setCheckUpdateFromConfiguration(checkUpdateFromConfiguration);
        return this;
    }

    public ConfigurationBuilder setSupportedFeatures(final List<Object> features) {
        this.configuration.setSupportedFeatures(resolveSupportedFeatures(features));
        return this;
    }

    public ConfigurationBuilder setModuleNames(final List<String> moduleNames) {
        this.configuration.setModuleNames(moduleNames);
        return this;
    }

    public ConfigurationBuilder setOutput(final String output) {
        this.configuration.setOutput(output);
        return this;
    }

    public ConfigurationBuilder setDebug(final boolean debug) {
        this.configuration.setDebug(debug);
        return this;
    }

    public ConfigurationBuilder setQuiet(final boolean quiet) {
        this.configuration.setQuiet(quiet);
        return this;
    }

    public ConfigurationBuilder setPath(final List<String> path) {
        this.configuration.setPath(path);
        return this;
    }

    public ConfigurationBuilder setYangModules(final List<String> yang) {
        this.configuration.setYangModules(yang);
        return this;
    }

    public ConfigurationBuilder setRecursive(final boolean recursive) {
        this.configuration.setRecursive(recursive);
        return this;
    }

    public ConfigurationBuilder setFormat(final String format) {
        this.configuration.setFormat(format);
        return this;
    }

    public ConfigurationBuilder setSimplify(final String simplify) {
        this.configuration.setSimplify(simplify);
        return this;
    }

    public ConfigurationBuilder setParseAll(final List<String> parseAll) {
        this.configuration.setParseAll(parseAll);
        return this;
    }

    public ConfigurationBuilder from(final LyvParameters params) {
        final Namespace namespace = params.parseArguments();
        this.configuration.setSupportedFeatures(resolveSupportedFeatures(namespace.getList("features")));
        this.configuration.setModuleNames(namespace.getList("module_name"));
        this.configuration.setOutput(namespace.getString("output"));
        this.configuration.setDebug(namespace.getBoolean("debug"));
        this.configuration.setQuiet(namespace.getBoolean("quiet"));
        this.configuration.setPath(namespace.getList("path"));
        this.configuration.setYangModules(namespace.getList("yang"));
        this.configuration.setRecursive(namespace.getBoolean("recursive"));
        this.configuration.setFormat(namespace.getString("format"));
        this.configuration.setSimplify(namespace.getString("simplify"));
        this.configuration.setParseAll(namespace.getList("parse_all"));
        final boolean singleModuledependentsOnly = namespace.getBoolean("module_depends_only");
        final boolean modulesOnly = namespace.getBoolean("modules_only");
        final boolean submodulesOnly = namespace.getBoolean("submodules_only");
        final Set<String> excludedModuleNames = new HashSet<>(namespace.getList("exclude_module_name"));
        final DependConfiguration dependConfiguration = new DependConfiguration(singleModuledependentsOnly,
                modulesOnly, submodulesOnly, excludedModuleNames);
        final int treeDepth = namespace.getInt("tree_depth");
        final int lineLength = namespace.getInt("tree_line_length");
        final boolean treeHelp = namespace.getBoolean("tree_help");
        final boolean treeModulePrefix = namespace.getBoolean("tree_prefix_module");
        final boolean treePrefixMainModule = namespace.getBoolean("tree_prefix_main_module");
        final TreeConfiguration treeConfiguration = new TreeConfiguration(treeDepth, lineLength, treeHelp,
                treeModulePrefix, treePrefixMainModule);
        this.configuration.setTreeConfiguration(treeConfiguration);
        this.configuration.setDependConfiguration(dependConfiguration);
        this.configuration.setUpdateFrom(namespace.getString("check_update_from"));
        final CheckUpdateFromConfiguration checkUpdateFromConfiguration = new CheckUpdateFromConfiguration(
                namespace.getInt("rfc_version"), namespace.getList("check_update_from_path"));
        this.configuration.setCheckUpdateFromConfiguration(checkUpdateFromConfiguration);
        return this;
    }

    private Set<QName> resolveSupportedFeatures(List<Object> features) {
        Set<QName> supportedFeatures = null;
        if (features != null) {
            supportedFeatures = new HashSet<>();
            for (final Object featureStr : features) {
                supportedFeatures.add(QName.create((String) featureStr));
            }
        }
        return supportedFeatures;
    }

    public Configuration build() {
        return configuration;
    }
}
