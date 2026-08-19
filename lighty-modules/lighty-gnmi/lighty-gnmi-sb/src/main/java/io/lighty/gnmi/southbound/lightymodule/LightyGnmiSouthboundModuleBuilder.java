/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import io.lighty.core.controller.api.LightyServices;
import java.util.concurrent.ExecutorService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.gnmi.southbound.yangmodule.config.GnmiConfiguration;

public class LightyGnmiSouthboundModuleBuilder {

    private GnmiConfiguration gnmiConfiguration;
    private LightyServices lightyServices;
    private ExecutorService executorService;
    private AAAEncryptionService aaaEncryptionService;

    public LightyGnmiSouthboundModuleBuilder withConfig(final GnmiConfiguration configuration) {
        this.gnmiConfiguration = configuration;
        return this;
    }

    public LightyGnmiSouthboundModuleBuilder withLightyServices(final LightyServices services) {
        this.lightyServices = services;
        return this;
    }

    public LightyGnmiSouthboundModuleBuilder withExecutorService(final ExecutorService executors) {
        this.executorService = executors;
        return this;
    }

    public LightyGnmiSouthboundModuleBuilder withEncryptionService(final AAAEncryptionService encryptionService) {
        this.aaaEncryptionService = encryptionService;
        return this;
    }

    public LightyGnmiSouthboundModule build() {
        return new LightyGnmiSouthboundModule(lightyServices, executorService, aaaEncryptionService, gnmiConfiguration,
                null, null);
    }
}
