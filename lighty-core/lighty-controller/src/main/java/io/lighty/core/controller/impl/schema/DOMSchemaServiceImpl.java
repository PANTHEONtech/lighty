package io.lighty.core.controller.impl.schema;

import com.google.common.collect.ClassToInstanceMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class DOMSchemaServiceImpl implements DOMSchemaService {

    final ModuleInfoBackedContext moduleInfoBackedContext;

    public DOMSchemaServiceImpl(ModuleInfoBackedContext moduleInfoBackedContext) {
        this.moduleInfoBackedContext = moduleInfoBackedContext;
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
        return new SchemaContextListenerRegistration(listener);
    }

    @Override
    public @NonNull ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

}
