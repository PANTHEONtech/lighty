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
