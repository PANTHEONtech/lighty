/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.schema;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

import javax.annotation.Nonnull;

public class SchemaContextListenerRegistration implements ListenerRegistration<SchemaContextListener> {

    private SchemaContextListener listener;

    public SchemaContextListenerRegistration(SchemaContextListener listener) {
        this.listener = listener;
    }

    @Nonnull
    @Override
    public SchemaContextListener getInstance() {
        return listener;
    }

    @Override
    public void close() {

    }

}
