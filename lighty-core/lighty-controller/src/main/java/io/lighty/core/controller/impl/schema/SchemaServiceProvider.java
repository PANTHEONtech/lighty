/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.schema;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.repo.YangTextSchemaContextResolver;

public class SchemaServiceProvider implements DOMSchemaService, DOMYangTextSourceProvider {

    private final ModuleInfoBackedContext moduleInfoBackedContext;

    public SchemaServiceProvider(final ModuleInfoBackedContext moduleInfoBackedContext) {
        this.moduleInfoBackedContext = moduleInfoBackedContext;
    }

    @Override
    public SchemaContext getSessionContext() {
        return this.moduleInfoBackedContext.getSchemaContext();
    }

    @Override
    public SchemaContext getGlobalContext() {
        return this.moduleInfoBackedContext.getSchemaContext();
    }

    @Override
    public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(final SchemaContextListener listener) {
        listener.onGlobalContextUpdated(this.moduleInfoBackedContext.getSchemaContext());
        final SchemaContextListenerRegistration registration = new SchemaContextListenerRegistration(listener);
        return registration;
    }

    @Override
    public ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
        return ImmutableClassToInstanceMap.of(DOMYangTextSourceProvider.class, this);
    }

    @Override
    public ListenableFuture<? extends YangTextSchemaSource> getSource(final SourceIdentifier sourceIdentifier) {
        return moduleInfoBackedContext.getSource(sourceIdentifier);
    }

}
