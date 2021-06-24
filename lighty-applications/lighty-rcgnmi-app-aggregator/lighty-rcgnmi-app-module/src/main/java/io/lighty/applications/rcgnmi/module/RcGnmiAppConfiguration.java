/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;

public class RcGnmiAppConfiguration {
    private final ControllerConfiguration controllerConfig;
    private final RestConfConfiguration restconfConfig;
    private final GnmiConfiguration gnmiConfiguration;

    public RcGnmiAppConfiguration(final ControllerConfiguration controllerConfig,
                                  final RestConfConfiguration restconfConfig,
                                  final GnmiConfiguration gnmiConfiguration) {
        this.controllerConfig = controllerConfig;
        this.restconfConfig = restconfConfig;
        this.gnmiConfiguration = gnmiConfiguration;
    }

    public ControllerConfiguration getControllerConfig() {
        return controllerConfig;
    }

    public RestConfConfiguration getRestconfConfig() {
        return restconfConfig;
    }

    public GnmiConfiguration getGnmiConfiguration() {
        return gnmiConfiguration;
    }

}
