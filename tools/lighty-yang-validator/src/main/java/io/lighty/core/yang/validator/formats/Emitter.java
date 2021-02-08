/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import io.lighty.core.yang.validator.config.Configuration;
import io.lighty.core.yang.validator.simplify.SchemaTree;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;

public interface Emitter {

    /**
     * Initialize option that emits some output.
     *
     * @param config                 all the configuration chosen by user
     * @param context                yang schema context
     * @param testFilesSchemaSources yang modules that are tested
     * @param schemaTree             Tree representation of schema nodes
     */
    void init(Configuration config, SchemaContext context, List<RevisionSourceIdentifier> testFilesSchemaSources,
              SchemaTree schemaTree);

    /**
     * Create logic and emit output.
     */
    void emit();
}
