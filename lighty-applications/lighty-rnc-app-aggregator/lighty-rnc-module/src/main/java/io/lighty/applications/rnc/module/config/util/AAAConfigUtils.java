/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.applications.rnc.module.config.RncAAAConfiguration;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AAAConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AAAConfigUtils.class);
    public static final String AAA_ROOT_ELEMENT_NAME = "aaa";


    private AAAConfigUtils() {
        throw new UnsupportedOperationException();
    }

    public static RncAAAConfiguration getAAAConfiguration(final InputStream jsonConfigInputStream)
            throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(AAA_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", AAA_ROOT_ELEMENT_NAME);
            return new RncAAAConfiguration();
        }
        final JsonNode aaaNode = configNode.path(AAA_ROOT_ELEMENT_NAME);
        RncAAAConfiguration rncAaaConfiguration;
        try {
            rncAaaConfiguration = mapper.treeToValue(aaaNode, RncAAAConfiguration.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                    RestConfConfiguration.class), e);
        }

        return rncAaaConfiguration;
    }

    public static RncAAAConfiguration createDefaultAAAConfiguration() {
        return new RncAAAConfiguration();
    }
}
