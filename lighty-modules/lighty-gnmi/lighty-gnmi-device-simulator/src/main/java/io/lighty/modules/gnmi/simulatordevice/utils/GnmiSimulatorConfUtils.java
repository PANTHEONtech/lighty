/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GnmiSimulatorConfUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSimulatorConfUtils.class);

    private static final String CONFIG_ROOT_ELEMENT_NAME = "gnmi_simulator";
    public static final String SCHEMA_SERVICE_ELEMENT_NAME = "schemaServiceConfig";
    public static final String TOP_LEVEL_MODELS_ELEMENT_NAME = "topLevelModels";

    private GnmiSimulatorConfUtils() {
        //Utility class
    }

    public static GnmiSimulatorConfiguration loadGnmiSimulatorConfiguration(final InputStream jsonConfigInputStream) {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new RuntimeException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", CONFIG_ROOT_ELEMENT_NAME);
            return new GnmiSimulatorConfiguration();
        }
        final JsonNode simulatorConfNode = configNode.path(CONFIG_ROOT_ELEMENT_NAME);
        GnmiSimulatorConfiguration gnmiSimulatorConfiguration;
        try {
            gnmiSimulatorConfiguration = mapper.treeToValue(simulatorConfNode, GnmiSimulatorConfiguration.class);
            if (simulatorConfNode.has(SCHEMA_SERVICE_ELEMENT_NAME)) {
                final JsonNode schemaServiceElement = simulatorConfNode.path(SCHEMA_SERVICE_ELEMENT_NAME);
                gnmiSimulatorConfiguration = setModelsToSimulatorConfig(mapper, schemaServiceElement,
                        gnmiSimulatorConfiguration);
            }

        } catch (final JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot bind Json tree to type: %s",
                io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration.class), e);
        }
        return gnmiSimulatorConfiguration;
    }

    private static GnmiSimulatorConfiguration setModelsToSimulatorConfig(final ObjectMapper mapper,
            final JsonNode schemaServiceElement, final GnmiSimulatorConfiguration gnmiSimulatorConfiguration)
            throws JsonProcessingException {
        if (schemaServiceElement.has(TOP_LEVEL_MODELS_ELEMENT_NAME)) {
            JsonNode yangModels = schemaServiceElement.path(TOP_LEVEL_MODELS_ELEMENT_NAME);
            if (yangModels.isArray()) {
                Set<ModuleId> moduleIds = new HashSet<>();
                for (JsonNode moduleIdNode : yangModels) {
                    ModuleId moduleId = mapper.treeToValue(moduleIdNode, ModuleId.class);
                    moduleIds.add(moduleId);
                }
                Set<YangModuleInfo> modelsFromClasspath = YangModuleUtils.getModelsFromClasspath(moduleIds);
                gnmiSimulatorConfiguration.setYangModulesInfo(modelsFromClasspath);
            } else {
                LOG.error("Expected JSON array at {}", TOP_LEVEL_MODELS_ELEMENT_NAME);
            }
        } else {
            LOG.info("Missing [{}] inside element [{}] in gNMI configuration", TOP_LEVEL_MODELS_ELEMENT_NAME,
                    SCHEMA_SERVICE_ELEMENT_NAME);
        }
        return gnmiSimulatorConfiguration;
    }

    public static GnmiSimulatorConfiguration loadDefaultGnmiSimulatorConfiguration() {
        GnmiSimulatorConfiguration gnmiSimulatorConfiguration = new GnmiSimulatorConfiguration();
        gnmiSimulatorConfiguration.setYangsPath(GnmiSimulatorConfUtils.class.getResource("/yangs").getPath());
        return gnmiSimulatorConfiguration;
    }

}
