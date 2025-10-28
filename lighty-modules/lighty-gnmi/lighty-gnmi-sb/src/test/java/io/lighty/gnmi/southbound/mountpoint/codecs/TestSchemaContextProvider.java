/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.SchemaContextHolder;
import io.lighty.gnmi.southbound.schema.TestYangDataStoreService;
import io.lighty.gnmi.southbound.schema.impl.SchemaContextHolderImpl;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.impl.ByClassPathYangLoaderService;
import io.lighty.gnmi.southbound.schema.loader.impl.ByPathYangLoaderService;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class TestSchemaContextProvider implements SchemaContextProvider {

    private final EffectiveModelContext schemaContext;

    public TestSchemaContextProvider(final EffectiveModelContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    @Override
    public EffectiveModelContext getSchemaContext() {
        return schemaContext;

    }

    public static TestSchemaContextProvider createInstance(final Path path, final Set<YangModuleInfo> moduleInfoSet)
            throws YangLoadException, SchemaException {
        final TestYangDataStoreService dataStoreService = new TestYangDataStoreService();
        final List<GnmiDeviceCapability> capabilities = new ByPathYangLoaderService(path).load(dataStoreService);
        capabilities.addAll(new ByClassPathYangLoaderService(moduleInfoSet).load(dataStoreService));

        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(dataStoreService, null);
        return new TestSchemaContextProvider(schemaContextHolder.getSchemaContext(capabilities));
    }

}
