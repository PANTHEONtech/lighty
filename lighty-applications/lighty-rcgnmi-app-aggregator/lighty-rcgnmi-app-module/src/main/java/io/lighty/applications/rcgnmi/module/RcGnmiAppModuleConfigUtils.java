/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import com.typesafe.config.Config;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RcGnmiAppModuleConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RcGnmiAppModuleConfigUtils.class);

    private RcGnmiAppModuleConfigUtils() {
        throw new UnsupportedOperationException();
    }

    public static RcGnmiAppConfiguration loadDefaultConfig() throws ConfigurationException {
        LOG.debug("Loading default lighty.io controller module configuration...");
        final Set<YangModuleInfo> modelPaths = new HashSet<>();
        defaultModels(modelPaths);

        final ControllerConfiguration controllerConfig = ControllerConfigUtils
                .getDefaultSingleNodeConfiguration(modelPaths);
        LOG.debug("Loading default lighty.io RESTCONF module configuration...");
        final RestConfConfiguration restconfConfig = RestConfConfigUtils.getDefaultRestConfConfiguration();
        // by default listen on any IP address (0.0.0.0) not only on loopback
        restconfConfig.setInetAddress(new InetSocketAddress(restconfConfig.getHttpPort()).getAddress());
        LOG.debug("Loading default lighty.io gNMI module configuration...");
        final GnmiConfiguration gnmiConfiguration = GnmiConfigUtils.getDefaultGnmiConfiguration();
        LOG.debug("Loading default lighty.io app modules configuration...");
        final ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();
        return new RcGnmiAppConfiguration(controllerConfig, restconfConfig, gnmiConfiguration, modulesConfig);
    }

    public static RcGnmiAppConfiguration loadConfiguration(final Path path) throws ConfigurationException, IOException {
        LOG.debug("Loading lighty.io controller module configuration...");
        final ControllerConfiguration controllerConfig = ControllerConfigUtils
                .getConfiguration(Files.newInputStream(path));
        final Config pekkoConfig = controllerConfig.getActorSystemConfig().getConfig().resolve();
        controllerConfig.getActorSystemConfig().setConfig(pekkoConfig);
        LOG.debug("Loading lighty.io RESTCONF module configuration...");
        final RestConfConfiguration restconfConfig = RestConfConfigUtils
                .getRestConfConfiguration(Files.newInputStream(path));
        final RestConfConfiguration defaultRestconfConfig = RestConfConfigUtils.getDefaultRestConfConfiguration();
        if (restconfConfig.getInetAddress().equals(defaultRestconfConfig.getInetAddress())) {
            // by default listen on any IP address (0.0.0.0) not only on loopback
            restconfConfig.setInetAddress(new InetSocketAddress(restconfConfig.getHttpPort()).getAddress());
        }

        LOG.debug("Loading lighty.io gNMI module configuration...");
        final GnmiConfiguration gnmiConfiguration = GnmiConfigUtils.getGnmiConfiguration(Files.newInputStream(path));

        LOG.debug("Loading lighty.io app modules configuration...");
        final ModulesConfig modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(path));
        return new RcGnmiAppConfiguration(controllerConfig, restconfConfig, gnmiConfiguration, modulesConfig);
    }

    private static void defaultModels(final Set<YangModuleInfo> modelPaths) {
        modelPaths.addAll(RestConfConfigUtils.YANG_MODELS);
        modelPaths.addAll(GnmiConfigUtils.YANG_MODELS);
    }
}
