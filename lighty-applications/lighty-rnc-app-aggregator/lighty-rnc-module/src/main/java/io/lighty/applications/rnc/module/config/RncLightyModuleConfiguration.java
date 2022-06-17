/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.server.config.LightyServerConfig;

public class RncLightyModuleConfiguration {
    private final ControllerConfiguration controllerConfig;
    private final LightyServerConfig serverConfig;
    private final RestConfConfiguration restConfConfiguration;
    private final NetconfConfiguration netconfConfig;
    private final AAAConfiguration aaaConfig;

    public RncLightyModuleConfiguration(final ControllerConfiguration controllerConfig,
                                        final LightyServerConfig serverConfig,
                                        final RestConfConfiguration restConfConfiguration,
                                        final NetconfConfiguration netconfConfig,
                                        final AAAConfiguration aaaConfig) {
        this.controllerConfig = controllerConfig;
        this.serverConfig = serverConfig;
        this.restConfConfiguration = restConfConfiguration;
        this.netconfConfig = netconfConfig;
        this.aaaConfig = aaaConfig;
    }

    public ControllerConfiguration getControllerConfig() {
        return controllerConfig;
    }

    public LightyServerConfig getServerConfig() {
        return serverConfig;
    }

    public RestConfConfiguration getRestconfConfig() {
        return restConfConfiguration;
    }

    public NetconfConfiguration getNetconfConfig() {
        return netconfConfig;
    }

    public AAAConfiguration getAaaConfig() {
        return aaaConfig;
    }
}
