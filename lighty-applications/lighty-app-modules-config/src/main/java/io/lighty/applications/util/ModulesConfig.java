/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModulesConfig {
    private static final Logger LOG = LoggerFactory.getLogger(ModulesConfig.class);
    private static final String MODULES_ELEMENT_NAME = "modules";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private long moduleTimeoutSeconds = 60;

    /**
     * Load ModulesConfig configuration from InputStream containing JSON data.
     *
     * @param jsonConfigInputStream InputStream containing ModulesConfig configuration data in JSON format.
     * @return Object representation of configuration data.
     * @throws ConfigurationException In case InputStream does not contain valid JSON data,
     *     or cannot bind Json tree to type.
     */
    public static ModulesConfig getModulesConfig(final InputStream jsonConfigInputStream)
            throws ConfigurationException {
        final JsonNode configNode;
        try {
            configNode = MAPPER.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(MODULES_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", MODULES_ELEMENT_NAME);
            return ModulesConfig.getDefaultModulesConfig();
        }
        final var modulesNode = configNode.path(MODULES_ELEMENT_NAME);
        try {
            return MAPPER.treeToValue(modulesNode, ModulesConfig.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(
                    String.format("Cannot bind Json tree to type: %s", ModulesConfig.class), e);
        }
    }

    /**
     * Get default ModulesConfig configuration.
     *
     * @return Object representation of configuration data.
     */
    public static ModulesConfig getDefaultModulesConfig() {
        return new ModulesConfig();
    }

    public long getModuleTimeoutSeconds() {
        return moduleTimeoutSeconds;
    }

    public void setModuleTimeoutSeconds(final long moduleTimeoutSeconds) {
        this.moduleTimeoutSeconds = moduleTimeoutSeconds;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModulesConfig)) {
            return false;
        }
        final ModulesConfig that = (ModulesConfig) obj;
        return moduleTimeoutSeconds == that.moduleTimeoutSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleTimeoutSeconds);
    }
}
