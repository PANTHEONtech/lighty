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
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.gnmi.southbound.yangmodule.util.GnmiConfigUtils;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
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
        return new RcGnmiAppConfiguration(controllerConfig, restconfConfig, null, modulesConfig);
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
        final GnmiConfiguration gnmiConfiguration = getGnmiConfiguration(Files.newInputStream(path));

        LOG.debug("Loading lighty.io app modules configuration...");
        final ModulesConfig modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(path));
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
                Set<QName> moduleIds = new HashSet<>();
                for (JsonNode moduleIdNode : schemaServiceElement) {
                    QName moduleId = mapper.treeToValue(moduleIdNode, QName.class);
                    moduleIds.add(moduleId);
                }
                return Optional.of(getModelsFromClasspath(moduleIds));
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Set<YangModuleInfo> getModelsFromClasspath(final Set<QName> filter) {
        Map<QName, YangModuleInfo> resolvedModules = new HashMap<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (QName moduleId: filter) {
            Set<YangModuleInfo> filteredSet = filterYangModelBindingProviders(moduleId, yangProviderLoader);
            for (YangModuleInfo yangModuleInfo : filteredSet) {
                resolvedModules.put(yangModuleInfo.getName(), yangModuleInfo);

                addDependencies(resolvedModules, yangModuleInfo.getImportedModules());
            }
        }
        return Collections.unmodifiableSet(resolvedModules.values().stream().collect(Collectors.toSet()));
    }


    private static void addDependencies(final Map<QName, YangModuleInfo> resolvedModules,
        final Collection<YangModuleInfo> importedModules) {
        for (YangModuleInfo yangModuleInfo : importedModules) {
            resolvedModules.put(yangModuleInfo.getName(), yangModuleInfo);
            addDependencies(resolvedModules, yangModuleInfo.getImportedModules());
        }
    }

    private static Set<YangModuleInfo> filterYangModelBindingProviders(final QName moduleId,
        final ServiceLoader<YangModelBindingProvider> yangProviderLoader) {
        Set<YangModuleInfo> filteredSet = new HashSet<>();
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            if (moduleId.equals(yangModelBindingProvider.getModuleInfo().getName())) {
                filteredSet.add(yangModelBindingProvider.getModuleInfo());
            }
        }
        return filteredSet;
    }

}
