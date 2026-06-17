/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.applications.rcgnmi.module;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.gnmi.southbound.yangmodule.config.GnmiConfiguration;
import org.opendaylight.gnmi.southbound.yangmodule.util.GnmiConfigUtils;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RcGnmiAppModuleConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RcGnmiAppModuleConfigUtils.class);
    public static final String GNMI_CONFIG_JSON_ROOT_ELEMENT = "gnmi";
    public static final String SCHEMA_SERVICE_ELEMENT_NAME = "schemaServiceConfig";


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
        LOG.debug("Loading default lighty.io app modules configuration...");
        final ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();
        return new RcGnmiAppConfiguration(controllerConfig, restconfConfig, new GnmiConfiguration(), modulesConfig);
    }

    public static RcGnmiAppConfiguration loadConfiguration(final Path path) throws ConfigurationException, IOException {
        LOG.debug("Loading lighty.io controller module configuration...");
        final ControllerConfiguration controllerConfig;
        try (InputStream is = Files.newInputStream(path)) {
            controllerConfig = ControllerConfigUtils.getConfiguration(is);
        }

        final Config pekkoConfig = controllerConfig.getActorSystemConfig().getConfig().resolve();
        controllerConfig.getActorSystemConfig().setConfig(pekkoConfig);

        LOG.debug("Loading lighty.io RESTCONF module configuration...");
        final RestConfConfiguration restconfConfig;
        try (InputStream is = Files.newInputStream(path)) {
            restconfConfig = RestConfConfigUtils.getRestConfConfiguration(is);
        }

        final RestConfConfiguration defaultRestconfConfig = RestConfConfigUtils.getDefaultRestConfConfiguration();
        if (restconfConfig.getInetAddress().equals(defaultRestconfConfig.getInetAddress())) {
            // by default listen on any IP address (0.0.0.0) not only on loopback
            restconfConfig.setInetAddress(new InetSocketAddress(restconfConfig.getHttpPort()).getAddress());
        }

        LOG.debug("Loading lighty.io gNMI module configuration...");
        GnmiConfiguration gnmiConfiguration;
        try (InputStream is = Files.newInputStream(path)) {
            gnmiConfiguration = getGnmiConfiguration(is);
        }
        if (gnmiConfiguration == null) {
            gnmiConfiguration = new GnmiConfiguration();
        }
        if (gnmiConfiguration.getYangModulesInfo() == null || gnmiConfiguration.getYangModulesInfo().isEmpty()) {
            gnmiConfiguration.setYangModulesInfo(controllerConfig.getSchemaServiceConfig().getModels());
        }
        LOG.debug("Loading lighty.io app modules configuration...");
        final ModulesConfig modulesConfig;
        try (InputStream is = Files.newInputStream(path)) {
            modulesConfig = ModulesConfig.getModulesConfig(is);
        }
        return new RcGnmiAppConfiguration(controllerConfig, restconfConfig, gnmiConfiguration, modulesConfig);
    }

    private static void defaultModels(final Set<YangModuleInfo> modelPaths) {
        modelPaths.addAll(RestConfConfigUtils.YANG_MODELS);
        modelPaths.addAll(GnmiConfigUtils.YANG_MODELS);
    }

    private static GnmiConfiguration getGnmiConfiguration(final InputStream jsonConfigInputStream)
        throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(GNMI_CONFIG_JSON_ROOT_ELEMENT)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", GNMI_CONFIG_JSON_ROOT_ELEMENT);
            return null;
        }
        final GnmiConfiguration gnmiConfiguration;
        try {
            JsonNode gnmiConfigJsonNode = configNode.path(GNMI_CONFIG_JSON_ROOT_ELEMENT);
            gnmiConfiguration
                = mapper.treeToValue(gnmiConfigJsonNode, GnmiConfiguration.class);
            final Optional<Set<YangModuleInfo>> yangModulesInfo
                = getYangModulesInfoFromConfig(mapper, gnmiConfigJsonNode);
            yangModulesInfo.ifPresent(gnmiConfiguration::setYangModulesInfo);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                GnmiConfiguration.class), e);
        }
        return gnmiConfiguration;
    }


    public static Optional<Set<YangModuleInfo>> getYangModulesInfoFromConfig(final ObjectMapper mapper,
        final JsonNode gnmiConfigJsonNode) throws JsonProcessingException {
        if (gnmiConfigJsonNode.has(SCHEMA_SERVICE_ELEMENT_NAME)) {
            final JsonNode schemaServiceElement = gnmiConfigJsonNode.path(SCHEMA_SERVICE_ELEMENT_NAME);
            if (schemaServiceElement.isArray()) {
                Set<ModuleId> moduleIds = new HashSet<>();
                for (JsonNode moduleIdNode : schemaServiceElement) {
                    ModuleId moduleId = mapper.treeToValue(moduleIdNode, ModuleId.class);
                    moduleIds.add(moduleId);
                }
                return Optional.of(YangModuleUtils.getModelsFromClasspath(moduleIds));
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
