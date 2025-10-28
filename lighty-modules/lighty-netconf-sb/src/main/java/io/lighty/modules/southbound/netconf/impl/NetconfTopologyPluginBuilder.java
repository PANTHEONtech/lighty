/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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

    public NetconfTopologyPluginBuilder(LightyServices services, NetconfConfiguration config) {
        this.lightyServices = services;
        this.configuration = config;
    }

    /**
     * Create new instance of {@link NetconfTopologyPluginBuilder} from {@link NetconfConfiguration}
     * and {@link LightyServices}.
     * @param config input single-node Netconf configuration.
     * @param services services from {@link LightyController}.
     * @return instance of {@link NetconfTopologyPluginBuilder}.
     */
    public static NetconfTopologyPluginBuilder from(final NetconfConfiguration config,
                                             final LightyServices services) {
        return new NetconfTopologyPluginBuilder(services, config);
    }

    /**
     * Inject executor service to execute futures.
     * @param executor injeted executor service.
     * @return instance of {@link NetconfTopologyPluginBuilder}.
     */
    public NetconfTopologyPluginBuilder withExecutorService(ExecutorService executor) {
        this.executorService = executor;
        return this;
    }

    /**
     * Build new instance of {@link NetconfTopologyPlugin} from {@link NetconfTopologyPluginBuilder}.
     * @return instance of NetconfSouthboundPlugin.
     */
    public NetconfSBPlugin build() {
        if (configuration.isClusterEnabled()) {
            return new NetconfClusteredTopologyPlugin(lightyServices,
                    executorService, configuration.getAaaService());
        } else {
            return new NetconfTopologyPlugin(lightyServices, configuration.getTopologyId(),
                    executorService, configuration.getAaaService());
        }
    }

}
