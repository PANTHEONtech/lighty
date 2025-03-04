/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ControllerConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerConfigUtils.class);

    /**
     * This list of models comes from odl-mdsal-models feature
     * {@code mvn:org.opendaylight.mdsal.model/features-mdsal-model} and various controller artifacts containing core
     * YANG files. This is also recommended default model set for majority of Lighty controller applications.
     */
    public static final Set<YangModuleInfo> YANG_MODELS = Set.of(
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev230126
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.l2.types.rev130827
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.yang.extension.yang.ext.rev130709
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore
                .provider.rev250130
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.mdsal.core.general.entity.rev150930
                .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.rpc.rev151215
                .YangModuleInfoImpl.getInstance()
    );

    public static final String CONTROLLER_CONFIG_ROOT_ELEMENT_NAME = "controller";
    public static final String SCHEMA_SERVICE_CONFIG_ELEMENT_NAME = "schemaServiceConfig";
    public static final String TOP_LEVEL_MODELS_ELEMENT_NAME = "topLevelModels";
    private static final String JSON_PATH_DELIMITER = "/";

    private ControllerConfigUtils() {

    }

    /**
     * Read configuration from InputStream representing JSON configuration data.
     * @param jsonConfigInputStream InputStream representing JSON configuration.
     * @return Instance of LightyController configuration data.
     * @throws ConfigurationException Thrown in case that JSON configuration is not readable or incorrect, or yang
     *                                model resources cannot be loaded.
     */
    public static ControllerConfiguration getConfiguration(final InputStream jsonConfigInputStream)
            throws ConfigurationException {
        Preconditions.checkNotNull(jsonConfigInputStream);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;

        //read configuration from JSON stream
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }

        if (configNode == null || configNode.isMissingNode()) {
            throw new ConfigurationException("Configuration was not loaded, empty or missing configuration.");
        }

        StringBuilder jsonPath = new StringBuilder().append(CONTROLLER_CONFIG_ROOT_ELEMENT_NAME);
        if (!configNode.has(CONTROLLER_CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", CONTROLLER_CONFIG_ROOT_ELEMENT_NAME);
            return getDefaultSingleNodeConfiguration();
        }
        JsonNode controllerNode = configNode.path(CONTROLLER_CONFIG_ROOT_ELEMENT_NAME);

        ControllerConfiguration controllerConfiguration = parseControllerConfig(mapper, jsonPath, controllerNode);

        injectActorSystemConfigToControllerConfig(controllerConfiguration);

        LOG.info("Controller configuration: Restore dir path: {}\n"
                + "Module Shards config path: {}\n"
                + "Modules config path: {}\n"
                + "Pekko-default config path: {}\n"
                + "Factory-pekko-default config path: {}",
                controllerConfiguration.getRestoreDirectoryPath(),
                controllerConfiguration.getModuleShardsConfig(),
                controllerConfiguration.getModulesConfig(),
                controllerConfiguration.getActorSystemConfig().getPekkoConfigPath(),
                controllerConfiguration.getActorSystemConfig().getFactoryPekkoConfigPath());

        return controllerConfiguration;
    }

    private static ControllerConfiguration parseControllerConfig(final ObjectMapper mapper,
            final StringBuilder jsonPath, final JsonNode controllerNode) throws ConfigurationException {
        final ControllerConfiguration controllerConfiguration;
        try {
            controllerConfiguration = mapper.treeToValue(controllerNode, ControllerConfiguration.class);
            if (controllerNode.has(DatastoreConfigurationUtils.DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME)) {
                JsonNode configDatastoreCtxNode = controllerNode.path(
                    DatastoreConfigurationUtils.DATASTORECTX_CONFIG_ROOT_ELEMENT_NAME);
                controllerConfiguration.setConfigDatastoreContext(DatastoreConfigurationUtils.createDatastoreContext(
                    configDatastoreCtxNode, LogicalDatastoreType.CONFIGURATION));
            } else {
                LOG.warn("JSON configuration for Config DataStore context is missing, using default one.");
                controllerConfiguration.setConfigDatastoreContext(
                    DatastoreConfigurationUtils.createDefaultConfigDatastoreContext());
            }
            if (controllerNode.has(DatastoreConfigurationUtils.DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME)) {
                JsonNode operDatastoreCtxNode = controllerNode.path(
                    DatastoreConfigurationUtils.DATASTORECTX_OPERATIONAL_ROOT_ELEMENT_NAME);
                controllerConfiguration.setOperDatastoreContext(DatastoreConfigurationUtils.createDatastoreContext(
                    operDatastoreCtxNode, LogicalDatastoreType.OPERATIONAL));
            } else {
                LOG.warn("JSON configuration for Operational DataStore context is missing, using default one.");
                controllerConfiguration.setOperDatastoreContext(
                    DatastoreConfigurationUtils.createDefaultOperationalDatastoreContext());
            }
            if (controllerNode.has(SCHEMA_SERVICE_CONFIG_ELEMENT_NAME)) {
                setModelsToControllerConfiguration(mapper, jsonPath, controllerNode, controllerConfiguration);
            } else {
                LOG.warn("JSON controller config is missing {} element, make sure to inject required models manually",
                        SCHEMA_SERVICE_CONFIG_ELEMENT_NAME);
            }
        } catch (JsonProcessingException e) {
            throw new ConfigurationException(
                    String.format("Cannot bind Json tree to type: %s", ControllerConfiguration.class), e);
        }
        return controllerConfiguration;
    }

    private static void setModelsToControllerConfiguration(final ObjectMapper mapper, final StringBuilder jsonPath,
            final JsonNode controllerNode, final ControllerConfiguration controllerConfiguration)
            throws JsonProcessingException, ConfigurationException {
        jsonPath.append(JSON_PATH_DELIMITER).append(SCHEMA_SERVICE_CONFIG_ELEMENT_NAME);
        JsonNode schemaServiceNode = controllerNode.path(SCHEMA_SERVICE_CONFIG_ELEMENT_NAME);
        if (schemaServiceNode.has(TOP_LEVEL_MODELS_ELEMENT_NAME)) {
            jsonPath.append(JSON_PATH_DELIMITER).append(TOP_LEVEL_MODELS_ELEMENT_NAME);
            JsonNode topLevelModelsNode = schemaServiceNode.path(TOP_LEVEL_MODELS_ELEMENT_NAME);
            if (topLevelModelsNode.isArray()) {
                Set<ModuleId> moduleIds = new HashSet<>();
                for (JsonNode moduleIdNode: topLevelModelsNode) {
                    ModuleId moduleId = mapper.treeToValue(moduleIdNode, ModuleId.class);
                    moduleIds.add(moduleId);
                }
                Set<YangModuleInfo> modelsFromClasspath = YangModuleUtils.getModelsFromClasspath(moduleIds);
                controllerConfiguration.getSchemaServiceConfig().setModels(modelsFromClasspath);
            } else {
                throw new ConfigurationException("Expected JSON array at " + jsonPath);
            }
        } else {
            throw new ConfigurationException(
                    String.format("JSON controller config file is missing %s element!", jsonPath));
        }
    }

    /**
     * Get typical single node configuration with default model set.
     * @return Instance of LightyController configuration data.
     * @throws ConfigurationException if unable to find pekko config files
     */
    public static ControllerConfiguration getDefaultSingleNodeConfiguration()
            throws ConfigurationException {
        return getDefaultSingleNodeConfiguration(Set.of());
    }

    /**
     * Get typical single node configuration with custom model set.
     * @param additionalModels List of models which is used in addition to default model set.
     * @return Instance of LightyController configuration data.
     * @throws ConfigurationException if unable to find pekko config files
     */
    public static ControllerConfiguration getDefaultSingleNodeConfiguration(final Set<YangModuleInfo> additionalModels)
            throws ConfigurationException {
        ControllerConfiguration controllerConfiguration = new ControllerConfiguration();
        injectActorSystemConfigToControllerConfig(controllerConfiguration);

        Set<YangModuleInfo> allModels = Stream.concat(YANG_MODELS.stream(), additionalModels.stream())
                .collect(Collectors.toSet());
        controllerConfiguration.getSchemaServiceConfig().setModels(allModels);

        controllerConfiguration.setConfigDatastoreContext(
            DatastoreConfigurationUtils.createDefaultConfigDatastoreContext());
        controllerConfiguration.setOperDatastoreContext(
            DatastoreConfigurationUtils.createDefaultOperationalDatastoreContext());

        return controllerConfiguration;
    }

    private static void injectActorSystemConfigToControllerConfig(final ControllerConfiguration controllerConfiguration)
            throws ConfigurationException {
        Config pekkoConfig = getPekkoConfigFromPath(
            controllerConfiguration.getActorSystemConfig().getPekkoConfigPath());
        Config factoryPekkoConfig = getPekkoConfigFromPath(
                controllerConfiguration.getActorSystemConfig().getFactoryPekkoConfigPath());
        Config finalConfig = pekkoConfig.withFallback(factoryPekkoConfig);

        controllerConfiguration.getActorSystemConfig().setClassLoader(
                ControllerConfigUtils.class.getClass().getClassLoader());
        controllerConfiguration.getActorSystemConfig().setConfig(finalConfig);
    }

    public static Config getPekkoConfigFromPath(final String pathToConfig) throws ConfigurationException {
        Config pekkoConfig = null;
        try {
            pekkoConfig = ConfigFactory.parseFile(new File(pathToConfig));
        } catch (ConfigException e) {
            LOG.debug("Cannot read Pekko config from filesystem: {}", pathToConfig, e);
        }
        if (pekkoConfig == null || pekkoConfig.isEmpty()) {
            // read from JAR resources on classpath
            pekkoConfig = ConfigFactory.parseResources(
                    ControllerConfigUtils.class.getClass().getClassLoader(),
                    pathToConfig);
            if (pekkoConfig.isEmpty()) {
                throw new ConfigurationException("Cannot find Pekko config on classpath: " + pathToConfig);
            }

            LOG.info("Used Pekko config file from classpath: {}", pathToConfig);
            return pekkoConfig;
        }
        LOG.info("Used Pekko config file from filesystem: {}", pathToConfig);
        return pekkoConfig;
    }
}
