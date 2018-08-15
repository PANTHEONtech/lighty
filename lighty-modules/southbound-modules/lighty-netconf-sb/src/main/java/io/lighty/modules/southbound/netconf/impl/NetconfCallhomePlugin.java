/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import java.util.concurrent.ExecutorService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.netconf.callhome.mount.CallHomeMountDispatcher;
import org.opendaylight.netconf.callhome.mount.IetfZeroTouchCallHomeServerProvider;
import org.opendaylight.netconf.callhome.mount.SchemaRepositoryProviderImpl;
import org.opendaylight.netconf.topology.api.SchemaRepositoryProvider;
import org.slf4j.LoggerFactory;

public class NetconfCallhomePlugin extends AbstractLightyModule {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(NetconfCallhomePlugin.class);

    private final IetfZeroTouchCallHomeServerProvider provider;

    public NetconfCallhomePlugin(final LightyServices lightyServices, final String topologyId,
            final ExecutorService executorService, final AAAEncryptionService encryptionService) {
        super(executorService);
        final SchemaRepositoryProvider schemaRepositoryProvider =
                new SchemaRepositoryProviderImpl("shared-schema-repository-impl");
        final CallHomeMountDispatcher dispatcher = new CallHomeMountDispatcher(topologyId,
                lightyServices.getEventExecutor(), lightyServices.getScheduledThreaPool(), lightyServices.getThreadPool(),
                schemaRepositoryProvider, lightyServices.getBindingDataBrokerOld(), lightyServices
                        .getDOMMountPointServiceOld(), encryptionService);
        this.provider = new IetfZeroTouchCallHomeServerProvider(lightyServices.getBindingDataBrokerOld(), dispatcher);
    }

    @Override
    protected boolean initProcedure() {
        this.provider.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        try {
            this.provider.close();
        } catch (final Exception e) {
            LOG.error("{} failed to close!", this.provider.getClass(), e);
            return false;
        }
        return true;
    }

}
