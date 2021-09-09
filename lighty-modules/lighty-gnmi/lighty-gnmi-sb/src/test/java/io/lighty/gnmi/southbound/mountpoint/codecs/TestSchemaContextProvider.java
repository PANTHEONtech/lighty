/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.SchemaContextHolder;
import io.lighty.gnmi.southbound.schema.TestYangDataStoreService;
import io.lighty.gnmi.southbound.schema.impl.SchemaContextHolderImpl;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.impl.ByPathYangLoaderService;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import java.nio.file.Path;
import java.util.List;
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

    public static TestSchemaContextProvider createFromPath(final Path path)
            throws YangLoadException, SchemaException {
        final TestYangDataStoreService dataStoreService = new TestYangDataStoreService();
        final List<GnmiDeviceCapability> capabilities =
                new ByPathYangLoaderService(path, null)
                .load(dataStoreService);
        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(dataStoreService, null);
        return new TestSchemaContextProvider(schemaContextHolder.getSchemaContext(capabilities));
    }

}
