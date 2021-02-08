/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import io.lighty.core.yang.validator.GroupArguments;
import io.lighty.core.yang.validator.config.Configuration;
import io.lighty.core.yang.validator.simplify.SchemaTree;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FormatPlugin {

    final Logger log;
    SchemaContext schemaContext;
    List<RevisionSourceIdentifier> sources;
    SchemaTree schemaTree;
    Path output;
    Configuration configuration;

    FormatPlugin(final Class<?> clazz) {
        log = LoggerFactory.getLogger(clazz);
    }

    void init(final SchemaContext context, final List<RevisionSourceIdentifier> testFilesSchemaSources,
              final SchemaTree tree, final Configuration config) {
        this.schemaContext = context;
        this.sources = testFilesSchemaSources;
        this.schemaTree = tree;
        this.configuration = config;
        final String out = config.getOutput();
        if (out == null) {
            this.output = null;
        } else {
            this.output = Paths.get(out);
        }
    }

    /**
     * Logic of the plugin. Use logger to print
     */
    abstract void emitFormat();

    /**
     * This serves to generate help about current plugin,
     * in case that user will use --help option with lyv.sh.
     *
     * @return instance of Help object that will contain name of the format with its description
     */
    abstract Help getHelp();

    /**
     * This serves to resolve configurations based on specific format.
     */
    public abstract Optional<GroupArguments> getGroupArguments();
}
