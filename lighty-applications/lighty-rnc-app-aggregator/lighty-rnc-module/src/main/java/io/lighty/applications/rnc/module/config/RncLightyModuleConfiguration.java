/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;

public class RncLightyModuleConfiguration {
    private final ControllerConfiguration controllerConfig;
    private final RncRestConfConfiguration restconfConfig;
    private final NetconfConfiguration netconfConfig;
    private final RncAAAConfiguration aaaConfig;

    public RncLightyModuleConfiguration(ControllerConfiguration controllerConfig,
                                        RncRestConfConfiguration restconfConfig,
                                        NetconfConfiguration netconfConfig,
                                        RncAAAConfiguration aaaConfig) {
        this.controllerConfig = controllerConfig;
        this.restconfConfig = restconfConfig;
        this.netconfConfig = netconfConfig;
        this.aaaConfig = aaaConfig;
    }

    public ControllerConfiguration getControllerConfig() {
        return controllerConfig;
    }

    public RncRestConfConfiguration getRestconfConfig() {
        return restconfConfig;
    }

    public NetconfConfiguration getNetconfConfig() {
        return netconfConfig;
    }

    public RncAAAConfiguration getAaaConfig() {
        return aaaConfig;
    }
}
