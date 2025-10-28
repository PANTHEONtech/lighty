/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import com.typesafe.config.Config;
import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.lighty.server.config.LightyServerConfig;
import io.lighty.server.util.LightyServerConfigUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RncLightyModuleConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RncLightyModuleConfigUtils.class);

    private RncLightyModuleConfigUtils() {
        throw new UnsupportedOperationException();
    }

    public static RncLightyModuleConfiguration loadConfigFromFile(final Path configPath) throws ConfigurationException {
        LOG.info("Loading RNC lighty.io configuration from file {} ...", configPath);
        final ControllerConfiguration controllerConfig;
        final LightyServerConfig lightyServerConfig;
        final RestConfConfiguration restconfConfig;
        final NetconfConfiguration netconfConfig;
        final AAAConfiguration aaaConfig;
        final ModulesConfig moduleConfig;
        try {
            LOG.debug("Loading lighty.io controller module configuration from file...");
            controllerConfig = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
            addDefaultAppModels(controllerConfig);
            final Config pekkoConfig = controllerConfig.getActorSystemConfig().getConfig().resolve();
            controllerConfig.getActorSystemConfig().setConfig(pekkoConfig);
            LOG.debug("lighty.io controller module configuration from file loaded!");

            LOG.debug("Loading lighty.io RESTCONF module configuration from file...");
            restconfConfig = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
            LOG.debug("lighty.io RESTCONF module configuration from file loaded!");

            LOG.debug("Loading lighty.io Jetty server module configuration from file...");
            lightyServerConfig = LightyServerConfigUtils.getServerConfiguration(Files.newInputStream(configPath));
            LOG.debug("lighty.io Jetty server module configuration from file loaded!");

            LOG.debug("Loading lighty.io NETCONF module configuration from file...");
            netconfConfig = NetconfConfigUtils.createNetconfConfiguration(Files.newInputStream(configPath));
            LOG.debug("lighty.io NETCONF module configuration from file loaded!");

            LOG.debug("Loading lighty.io AAA module configuration from file...");
            aaaConfig = AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));
            LOG.debug("lighty.io AAA module configuration from file loaded!");
            LOG.debug("Loading lighty.io app module configuration from file...");
            moduleConfig = ModulesConfig.getModulesConfig(Files.newInputStream(configPath));
            LOG.debug("lighty.io app module configuration from file loaded!");
        } catch (IOException e) {
            throw new ConfigurationException("Exception thrown while loading configuration!", e);
        }
        return new RncLightyModuleConfiguration(controllerConfig, lightyServerConfig, restconfConfig, netconfConfig,
                aaaConfig, moduleConfig);
    }

    public static RncLightyModuleConfiguration loadDefaultConfig() throws ConfigurationException {
        LOG.info("Loading default RNC lighty.io configuration ...");
        final Set<YangModuleInfo> modelPaths = new HashSet<>();
        defaultModels(modelPaths);

        LOG.debug("Loading default lighty.io controller module configuration...");
        final ControllerConfiguration controllerConfig = ControllerConfigUtils
                .getDefaultSingleNodeConfiguration(modelPaths);
        LOG.debug("Default lighty.io controller module configuration!");

        LOG.debug("Loading default lighty.io RESTCONF module configuration...");
        final RestConfConfiguration restConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
        LOG.debug("Default lighty.io RESTCONF module configuration loaded!");

        LOG.debug("Loading default lighty.io Jetty server module configuration...");
        final LightyServerConfig lightyServerConfig = LightyServerConfigUtils.getDefaultLightyServerConfig();
        LOG.debug("Default lighty.io Jetty server module configuration loaded!");

        LOG.debug("Loading default lighty.io NETCONF module configuration...");
        final NetconfConfiguration netconfConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
        LOG.debug("Default lighty.io NETCONF module configuration loaded!");

        LOG.debug("Loading default lighty.io AAA module configuration...");
        final AAAConfiguration aaaConfig = AAAConfigUtils.createDefaultAAAConfiguration();
        LOG.debug("Default lighty.io AAA module configuration loaded!");

        LOG.debug("Loading default lighty.io app module configuration...");
        final ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();
        LOG.debug("Default lighty.io app module configuration loaded!");
        return new RncLightyModuleConfiguration(controllerConfig, lightyServerConfig, restConfConfiguration,
                netconfConfig, aaaConfig, modulesConfig);
    }

    private static void addDefaultAppModels(final ControllerConfiguration controllerConfig) {
        LOG.debug("Adding minimal needed yang models if they are not present...");
        final Set<YangModuleInfo> modelPaths = new HashSet<>(controllerConfig.getSchemaServiceConfig().getModels());
        defaultModels(modelPaths);
        controllerConfig.getSchemaServiceConfig().setModels(Collections.unmodifiableSet(modelPaths));
    }

    private static void defaultModels(final Set<YangModuleInfo> modelPaths) {
        modelPaths.addAll(RestConfConfigUtils.YANG_MODELS);
        modelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
        modelPaths.addAll(NetconfConfigUtils.NETCONF_CALLHOME_MODELS);
        modelPaths.addAll(AAAConfigUtils.YANG_MODELS);
    }
}
