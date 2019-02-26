/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the LIGHTY.IO LICENSE,
 * version 1.1. If a copy of the license was not distributed with this file,
 * You can obtain one at https://lighty.io/license/1.1/
 */
package io.lighty.modules.southbound.ovsdb;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbSouthboundPlugin extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbSouthboundPlugin.class);

    private final LightyServices lightyServices;
    private SouthboundProvider southboundProvider;

    public OvsdbSouthboundPlugin(final LightyServices lightyServices) {
        this.lightyServices = lightyServices;
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Initializing ovsdb southbound plugin");

        this.southboundProvider =
                new SouthboundProvider(
                        this.lightyServices.getControllerBindingDataBroker(), this.lightyServices
                        .getEntityOwnershipService(),
                        new OvsdbConnectionService(),
                        this.lightyServices.getDOMSchemaService(),
                        this.lightyServices.getBindingNormalizedNodeSerializer(),
                        this.lightyServices.getSystemReadyMonitor(),
                        this.lightyServices.getDiagStatusService());
        this.southboundProvider.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        try {
            if (this.southboundProvider != null) {
                this.southboundProvider.close();
            }
            return true;
        } catch (final Exception e) {
            LOG.error("Stop procedure failed!", e);
            return false;
        }
    }
}
