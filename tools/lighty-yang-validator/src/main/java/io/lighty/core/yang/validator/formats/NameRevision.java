/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import io.lighty.core.yang.validator.GroupArguments;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;

public class NameRevision extends FormatPlugin {

    private static final String HELP_NAME = "name-revision";
    private static final String HELP_DESCRIPTION = "return file name in a <name>@<revision> format";
    private static final String ET = "@";

    public NameRevision() {
        super(NameRevision.class);
    }

    @Override
    public void emitFormat() {
        for (final RevisionSourceIdentifier source : this.sources) {
            final Module module = this.schemaContext.findModule(source.getName(), source.getRevision()).get();
            final Optional<Revision> revision = module.getRevision();
            String nameRevision = module.getName();
            if (revision.isPresent()) {
                nameRevision += ET + revision.get().toString();
            }
            log.info(nameRevision);
        }
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        return Optional.empty();
    }
}
