/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
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
import com.google.common.collect.ImmutableSet;
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
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ControllerConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerConfigUtils.class);

    /**
     * This list of models comes from odl-mdsal-models feature
     * {@code mvn:org.opendaylight.mdsal.model/features-mdsal-model} and various controller artifacts containing core
     * YANG files. This is also recommended default model set for majority of Lighty controller applications.
     */
    public static final Set<YangModuleInfo> YANG_MODELS = ImmutableSet.of(
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.$YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.$YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.$YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.$YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.$YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.yang.extension.yang.ext.rev130709.$YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore
            .provider.rev140612.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners
            .rev150804.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.prefix.shard
            .configuration.rev170110.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013
            .$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsal.core.general.entity.rev150930
                .$YangModuleInfoImpl.getInstance()
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
            LOG.warn("Json config does not contain {} element. Using defaults.", jsonPath.toString());
            return getDefaultSingleNodeConfiguration();
        }
        JsonNode controllerNode = configNode.path(CONTROLLER_CONFIG_ROOT_ELEMENT_NAME);


        ControllerConfiguration controllerConfiguration;
        try {
            controllerConfiguration = mapper.treeToValue(controllerNode,
                    ControllerConfiguration.class);
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
                    throw new ConfigurationException(String.format("JSON controller config file is missing %s element!",
                            jsonPath));
                }
            } else {
                throw new ConfigurationException(String.format("JSON controller config file is missing %s element!",
                        jsonPath));
            }
        } catch (JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                    ControllerConfiguration.class), e);
        }

        injectActorSystemConfigToControllerConfig(controllerConfiguration);

        LOG.info("Controller configuration: Restore dir path: {}\n"
                + "Module Shards config path: {}\n"
                + "Modules config path: {}\n"
                + "Akka-default config path: {}\n"
                + "Factory-akka-default config path: {}",
                controllerConfiguration.getRestoreDirectoryPath(),
                controllerConfiguration.getModuleShardsConfig(),
                controllerConfiguration.getModulesConfig(),
                controllerConfiguration.getActorSystemConfig().getAkkaConfigPath(),
                controllerConfiguration.getActorSystemConfig().getFactoryAkkaConfigPath());

        return controllerConfiguration;
    }

    /**
     * Get typical single node configuration with default model set.
     * @return Instance of LightyController configuration data.
     * @throws ConfigurationException if unable to find akka config files
     */
    public static ControllerConfiguration getDefaultSingleNodeConfiguration()
            throws ConfigurationException {
        return getDefaultSingleNodeConfiguration(ImmutableSet.of());
    }

    /**
     * Get typical single node configuration with custom model set.
     * @param additionalModels List of models which is used in addition to default model set.
     * @return Instance of LightyController configuration data.
     * @throws ConfigurationException if unable to find akka config files
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
        Config akkaConfig = getAkkaConfigFromPath(controllerConfiguration.getActorSystemConfig().getAkkaConfigPath());
        Config factoryAkkaConfig = getAkkaConfigFromPath(
                controllerConfiguration.getActorSystemConfig().getFactoryAkkaConfigPath());
        Config finalConfig = akkaConfig.withFallback(factoryAkkaConfig);

        controllerConfiguration.getActorSystemConfig().setClassLoader(
                ControllerConfigUtils.class.getClass().getClassLoader());
        controllerConfiguration.getActorSystemConfig().setConfig(finalConfig);
    }

    public static Config getAkkaConfigFromPath(final String pathToConfig) throws ConfigurationException {
        Config akkaConfig = null;
        try {
            akkaConfig = ConfigFactory.parseFile(new File(pathToConfig));
        } catch (ConfigException e) {
            LOG.debug("Cannot read Akka config from filesystem: {}", pathToConfig, e);
        }
        if (akkaConfig == null || akkaConfig.isEmpty()) {
            // read from JAR resources on classpath
            akkaConfig = ConfigFactory.parseResources(
                    ControllerConfigUtils.class.getClass().getClassLoader(),
                    pathToConfig);
            if (akkaConfig.isEmpty()) {
                throw new ConfigurationException("Cannot find Akka config on classpath: " + pathToConfig);
            }

            LOG.info("Used Akka config file from classpath: {}", pathToConfig);
            return akkaConfig;
        }
        LOG.info("Used Akka config file from filesystem: {}", pathToConfig);
        return akkaConfig;
    }
}
