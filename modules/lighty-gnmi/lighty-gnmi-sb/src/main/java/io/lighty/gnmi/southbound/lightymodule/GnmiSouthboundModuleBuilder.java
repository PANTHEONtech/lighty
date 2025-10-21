/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import io.lighty.core.controller.api.LightyServices;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import java.util.concurrent.ExecutorService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

public class GnmiSouthboundModuleBuilder {

    private GnmiConfiguration gnmiConfiguration;
    private LightyServices lightyServices;
    private ExecutorService executorService;
    private CrossSourceStatementReactor yangReactor;
    private AAAEncryptionService aaaEncryptionService;

    public GnmiSouthboundModuleBuilder withConfig(final GnmiConfiguration configuration) {
        this.gnmiConfiguration = configuration;
        return this;
    }

    public GnmiSouthboundModuleBuilder withLightyServices(final LightyServices services) {
        this.lightyServices = services;
        return this;
    }

    public GnmiSouthboundModuleBuilder withExecutorService(final ExecutorService executors) {
        this.executorService = executors;
        return this;
    }

    public GnmiSouthboundModuleBuilder withReactor(final CrossSourceStatementReactor reactor) {
        this.yangReactor = reactor;
        return this;
    }

    public GnmiSouthboundModuleBuilder withEncryptionService(final AAAEncryptionService encryptionService) {
        this.aaaEncryptionService = encryptionService;
        return this;
    }

    public GnmiSouthboundModule build() {
        return new GnmiSouthboundModule(lightyServices, executorService, aaaEncryptionService, gnmiConfiguration,
                yangReactor);
    }
}
