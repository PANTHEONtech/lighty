/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
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
 * Builder for {@link NetconfTopologyPlugin}.
 */
public class NetconfTopologyPluginBuilder {

    private LightyServices lightyServices;
    private NetconfConfiguration configuration;
    private ExecutorService executorService = null;

    /**
     * Create new instance of {@link NetconfTopologyPluginBuilder} from {@link NetconfConfiguration}
     * and {@link LightyServices}.
     * @param configuration input single-node Netconf configuration.
     * @param lightyServices services from {@link LightyController}.
     * @return instance of {@link NetconfTopologyPluginBuilder}.
     */
    public NetconfTopologyPluginBuilder from(final NetconfConfiguration configuration,
                                             final LightyServices lightyServices) {
        this.configuration = configuration;
        this.lightyServices = lightyServices;
        return this;
    }

    /**
     * Inject executor service to execute futures
     * @param executorService
     * @return instance of {@link NetconfTopologyPluginBuilder}.
     */
    public NetconfTopologyPluginBuilder withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Build new instance of {@link NetconfTopologyPlugin} from {@link NetconfTopologyPluginBuilder}.
     * @return instance of NetconfSouthboundPlugin.
     */
    public NetconfSBPlugin build() {
        if (configuration.isClusterEnabled()) {
            return new NetconfClusteredTopologyPlugin(lightyServices, configuration.getTopologyId(),
                    configuration.getClientDispatcher(), configuration.getWriteTxTimeout(), executorService,
                    configuration.getAaaService());
        } else {
            return new NetconfTopologyPlugin(lightyServices, configuration.getTopologyId(),
                    configuration.getClientDispatcher(), executorService, configuration.getAaaService());
        }
    }

}
