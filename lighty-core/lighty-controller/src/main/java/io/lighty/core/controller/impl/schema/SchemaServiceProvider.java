package io.lighty.core.controller.impl.schema;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.jdt.annotation.NonNull;
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

    private ModuleInfoBackedContext moduleInfoBackedContext;
    private final YangTextSchemaContextResolver contextResolver = YangTextSchemaContextResolver.create("global-bundle");

    public SchemaServiceProvider(ModuleInfoBackedContext moduleInfoBackedContext) {
        this.moduleInfoBackedContext = moduleInfoBackedContext;
        this.moduleInfoBackedContext.getSchemaContext().getModules().forEach(m->{
            //TODO: populate YangTextSchemaContextResolver with modules from ModuleInfoBackedContext
        });
    }

    @Override
    public SchemaContext getSessionContext() {
        return moduleInfoBackedContext.getSchemaContext();
    }

    @Override
    public SchemaContext getGlobalContext() {
        return moduleInfoBackedContext.getSchemaContext();
    }

    @Override
    public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(SchemaContextListener listener) {
        listener.onGlobalContextUpdated(moduleInfoBackedContext.getSchemaContext());
        SchemaContextListenerRegistration registration = new SchemaContextListenerRegistration(listener);
        return registration;
    }

    @Override
    public @NonNull ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
        return ImmutableClassToInstanceMap.of(DOMYangTextSourceProvider.class, this);
    }

    @Override
    public ListenableFuture<? extends YangTextSchemaSource> getSource(SourceIdentifier sourceIdentifier) {
        return contextResolver.getSource(sourceIdentifier);
    }

}
