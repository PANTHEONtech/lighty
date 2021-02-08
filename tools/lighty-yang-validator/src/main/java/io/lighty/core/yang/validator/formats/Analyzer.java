/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import io.lighty.core.yang.validator.GroupArguments;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.EffectiveSchemaContext;

public class Analyzer extends FormatPlugin {

    private static final String HELP_NAME = "analyze";
    private static final String HELP_DESCRIPTION = "return count of each keyword";
    private final Map<String, Integer> counter = new HashMap<>();

    public Analyzer() {
        super(Analyzer.class);
    }

    @Override
    void emitFormat() {
        for (Object subStatement : ((EffectiveSchemaContext) this.schemaContext).getRootDeclaredStatements()) {
            analyzeSubstatement((DeclaredStatement) subStatement);
        }

        printOut();
    }

    private void printOut() {
        for (Map.Entry<String, Integer> entry : new TreeMap<>(this.counter).entrySet()) {
            log.info(String.format("%s: %d", entry.getKey(), entry.getValue()));
        }
    }

    private void analyzeSubstatement(final DeclaredStatement subStatement) {
        String name = subStatement.statementDefinition().getStatementName().getLocalName();
        counter.compute(name, (key, val) -> (val == null) ? 1 : val + 1);
        final Collection substatements = subStatement.declaredSubstatements();
        for (final Object nextSubstatement : substatements) {
            analyzeSubstatement((DeclaredStatement)nextSubstatement);
        }
    }

    @Override
    Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        // TODO make option to iterate through multiple modules and give output only from those modules not overlaping
        // TODO make option print as html table
        // TODO make option to sort output alphabetically or by number of occurrences
        // TODO make option to ignore some specific keywords
        // TODO make option to search only for some specific keywords
        return Optional.empty();
    }
}
