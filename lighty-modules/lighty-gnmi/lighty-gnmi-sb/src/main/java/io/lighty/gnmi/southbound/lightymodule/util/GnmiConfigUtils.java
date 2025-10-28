/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.common.models.ModuleId;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GnmiConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GnmiConfigUtils.class);
    private static final String SCHEMA_SERVICE_ELEMENT_NAME = "initialYangModels";

    public static final String GNMI_CONFIG_JSON_ROOT_ELEMENT = "gnmi";
    public static final Set<YangModuleInfo> YANG_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.topology.rev210316
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.yang.storage.rev210331
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.force.capabilities.rev210702
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.certificate.storage.rev210504
                    .YangModuleInfoImpl.getInstance()
    );

    private GnmiConfigUtils() {
        //Utility class
    }

    public static GnmiConfiguration getDefaultGnmiConfiguration() {
        return new GnmiConfiguration();
    }

    public static GnmiConfiguration getGnmiConfiguration(final InputStream jsonConfigInputStream)
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
            return getDefaultGnmiConfiguration();
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

    private static Optional<Set<YangModuleInfo>> getYangModulesInfoFromConfig(final ObjectMapper mapper,
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
                LOG.error("Expected JSON array at {}", SCHEMA_SERVICE_ELEMENT_NAME);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

}
