/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AAAConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AAAConfigUtils.class);
    private static final String AAA_ROOT_ELEMENT_NAME = "aaa";

    public static final Set<YangModuleInfo> YANG_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.config.aaa.authn.encrypt.service.config.rev160915
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214
                    .YangModuleInfoImpl.getInstance());

    private AAAConfigUtils() {
        // Hide on purpose
    }

    public static AAAConfiguration getAAAConfiguration(final InputStream jsonConfigInputStream)
            throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(AAA_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", AAA_ROOT_ELEMENT_NAME);
            return new AAAConfiguration();
        }
        final JsonNode aaaNode = configNode.path(AAA_ROOT_ELEMENT_NAME);
        final AAAConfiguration aaaConfiguration;
        try {
            aaaConfiguration = mapper.treeToValue(aaaNode, AAAConfiguration.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                    AAAConfiguration.class), e);
        }

        return aaaConfiguration;
    }

    public static AAAConfiguration createDefaultAAAConfiguration() {
        return new AAAConfiguration();
    }
}
