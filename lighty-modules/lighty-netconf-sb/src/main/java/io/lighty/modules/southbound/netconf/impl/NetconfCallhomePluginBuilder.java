/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import java.util.concurrent.ExecutorService;

/**
 * Builder for {@link NetconfCallhomePlugin}.
 */
public class NetconfCallhomePluginBuilder {

    private LightyServices lightyServices;
    private NetconfConfiguration configuration;
    private ExecutorService executorService = null;

    public NetconfCallhomePluginBuilder(LightyServices services, NetconfConfiguration config) {
        this.lightyServices = services;
        this.configuration = config;
    }

    /**
     * Create new instance of {@link NetconfCallhomePluginBuilder} from {@link NetconfConfiguration} and
     * {@link LightyServices}.
     * @param config input Netconf configuration.
     * @param services services from {@link LightyController}
     * @return instance of {@link NetconfCallhomePluginBuilder} class.
     */
    public static NetconfCallhomePluginBuilder from(final NetconfConfiguration config,
                                             final LightyServices services) {
        return new NetconfCallhomePluginBuilder(services, config);
    }

    /**
     * Inject executor service to execute futures.
     * @param executor injected executor service.
     * @return instance of {@link NetconfCallhomePluginBuilder}.
     */
    public NetconfCallhomePluginBuilder withExecutorService(ExecutorService executor) {
        this.executorService = executor;
        return this;
    }

    /**
     * Build new instance of {@link NetconfCallhomePlugin} from {@link NetconfCallhomePluginBuilder}.
     * @return instance of NetconfSouthboundPlugin.
     */
    public NetconfCallhomePlugin build() {
        return new NetconfCallhomePlugin(lightyServices, configuration.getTopologyId(), executorService,
                configuration.getAaaService());
    }
}
