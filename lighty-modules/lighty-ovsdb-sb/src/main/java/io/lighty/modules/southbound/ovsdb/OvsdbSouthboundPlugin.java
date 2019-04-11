/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
