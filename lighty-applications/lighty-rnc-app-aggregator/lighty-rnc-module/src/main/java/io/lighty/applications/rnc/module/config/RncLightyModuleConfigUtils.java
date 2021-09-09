/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config;

import com.typesafe.config.Config;
import io.lighty.aaa.AAALighty;
import io.lighty.applications.rnc.module.config.util.AAAConfigUtils;
import io.lighty.applications.rnc.module.config.util.RncRestConfConfigUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RncLightyModuleConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RncLightyModuleConfigUtils.class);

    private RncLightyModuleConfigUtils() {
        throw new UnsupportedOperationException();
    }

    public static RncLightyModuleConfiguration loadConfigFromFile(Path configPath) throws ConfigurationException {
        LOG.info("Loading RNC lighty.io configuration from file {} ...", configPath);

        ControllerConfiguration controllerConfig;
        RncRestConfConfiguration restconfConfig;
        NetconfConfiguration netconfConfig;
        RncAAAConfiguration aaaConfig;

        try {
            LOG.debug("Loading lighty.io controller module configuration from file...");
            controllerConfig = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
            addDefaultAppModels(controllerConfig);
            Config akkaConfig = controllerConfig.getActorSystemConfig().getConfig().resolve();
            controllerConfig.getActorSystemConfig().setConfig(akkaConfig);
            LOG.debug("lighty.io controller module configuration from file loaded!");

            LOG.debug("Loading lighty.io RESTCONF module configuration from file...");
            restconfConfig = new RncRestConfConfiguration(RncRestConfConfigUtils
                    .getRestConfConfiguration(Files.newInputStream(configPath)));
            restconfConfig.setSecurityConfig(RncRestConfConfigUtils.createSecurityConfig(restconfConfig));
            LOG.debug("lighty.io RESTCONF module configuration from file loaded!");

            LOG.debug("Loading lighty.io NETCONF module configuration from file...");
            netconfConfig = NetconfConfigUtils.createNetconfConfiguration(Files.newInputStream(configPath));
            LOG.debug("lighty.io NETCONF module configuration from file loaded!");

            LOG.debug("Loading lighty.io AAA module configuration from file...");
            aaaConfig = AAAConfigUtils.getAAAConfiguration(Files.newInputStream(configPath));
            LOG.debug("lighty.io AAA module configuration from file loaded!");
        } catch (IOException e) {
            throw new ConfigurationException("Exception thrown while loading configuration!", e);
        }

        return new RncLightyModuleConfiguration(controllerConfig, restconfConfig, netconfConfig, aaaConfig);
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance"})
    public static RncLightyModuleConfiguration loadDefaultConfig() throws ConfigurationException {
        LOG.info("Loading default RNC lighty.io configuration ...");

        LOG.debug("Loading default lighty.io controller module configuration...");

        Set<YangModuleInfo> modelPaths = new HashSet<>();
        defaultModels(modelPaths);

        ControllerConfiguration controllerConfig = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);
        LOG.debug("Default lighty.io controller module configuration!");

        LOG.debug("Loading default lighty.io RESTCONF module configuration...");
        RncRestConfConfiguration restconfConfig =
                new RncRestConfConfiguration(RncRestConfConfigUtils.getDefaultRestConfConfiguration());
        restconfConfig.setSecurityConfig(RncRestConfConfigUtils.createSecurityConfig(restconfConfig));
        LOG.debug("Default lighty.io RESTCONF module configuration loaded!");

        LOG.debug("Loading default lighty.io NETCONF module configuration...");
        NetconfConfiguration netconfConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
        LOG.debug("Default lighty.io NETCONF module configuration loaded!");

        LOG.debug("Loading default lighty.io AAA module configuration...");
        RncAAAConfiguration aaaConfig = AAAConfigUtils.createDefaultAAAConfiguration();
        LOG.debug("Default lighty.io AAA module configuration loaded!");

        return new RncLightyModuleConfiguration(controllerConfig, restconfConfig, netconfConfig, aaaConfig);
    }

    private static void addDefaultAppModels(ControllerConfiguration controllerConfig) {
        LOG.debug("Adding minimal needed yang models if they are not present...");
        final Set<YangModuleInfo> modelPaths = new HashSet<>(controllerConfig.getSchemaServiceConfig().getModels());
        defaultModels(modelPaths);
        controllerConfig.getSchemaServiceConfig().setModels(Collections.unmodifiableSet(modelPaths));
    }

    private static void defaultModels(Set<YangModuleInfo> modelPaths) {
        modelPaths.addAll(RestConfConfigUtils.YANG_MODELS);
        modelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
        modelPaths.addAll(AAALighty.YANG_MODELS);
    }
}
