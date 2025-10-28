/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.applications.util.ModulesConfig;
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
    private final ModulesConfig moduleConfig;

    public RncLightyModuleConfiguration(final ControllerConfiguration controllerConfig,
                                        final LightyServerConfig serverConfig,
                                        final RestConfConfiguration restConfConfiguration,
                                        final NetconfConfiguration netconfConfig,
                                        final AAAConfiguration aaaConfig,
                                        final ModulesConfig moduleConfig) {
        this.controllerConfig = controllerConfig;
        this.serverConfig = serverConfig;
        this.restConfConfiguration = restConfConfiguration;
        this.netconfConfig = netconfConfig;
        this.aaaConfig = aaaConfig;
        this.moduleConfig = moduleConfig;
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

    public ModulesConfig getModuleConfig() {
        return moduleConfig;
    }
}
