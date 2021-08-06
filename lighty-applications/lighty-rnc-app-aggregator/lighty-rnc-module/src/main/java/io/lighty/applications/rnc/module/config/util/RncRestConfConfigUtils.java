/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.module.config.util;

import static io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils.RESTCONF_CONFIG_ROOT_ELEMENT_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.applications.rnc.module.config.RncLightyModuleConfigUtils;
import io.lighty.applications.rnc.module.config.RncRestConfConfiguration;
import io.lighty.applications.rnc.module.config.SecurityConfig;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RncRestConfConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RncRestConfConfigUtils.class);

    private RncRestConfConfigUtils() {
        throw new UnsupportedOperationException();
    }

    public static RncRestConfConfiguration getRestConfConfiguration(final InputStream jsonConfigInputStream)
        throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(RESTCONF_CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", RESTCONF_CONFIG_ROOT_ELEMENT_NAME);
            return new RncRestConfConfiguration();
        }
        final JsonNode restconfNode = configNode.path(RESTCONF_CONFIG_ROOT_ELEMENT_NAME);
        RncRestConfConfiguration restconfConfigurationrnc;
        try {
            restconfConfigurationrnc = mapper.treeToValue(restconfNode, RncRestConfConfiguration.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration.class), e);
        }

        return restconfConfigurationrnc;
    }

    public static RncRestConfConfiguration getRestConfConfiguration(
            final RncRestConfConfiguration rncRestConfConfiguration, final LightyServices lightyServices) {
        final RncRestConfConfiguration config = new RncRestConfConfiguration(rncRestConfConfiguration);
        config.setDomDataBroker(lightyServices.getClusteredDOMDataBroker());
        config.setSchemaService(lightyServices.getDOMSchemaService());
        config.setDomRpcService(lightyServices.getDOMRpcService());
        config.setDomActionService(lightyServices.getDOMActionService());
        config.setDomNotificationService(lightyServices.getDOMNotificationService());
        config.setDomMountPointService(lightyServices.getDOMMountPointService());
        config.setDomSchemaService(lightyServices.getDOMSchemaService());
        return config;
    }

    public static RncRestConfConfiguration getDefaultRestConfConfiguration() {
        return new RncRestConfConfiguration();
    }

    public static SecurityConfig createSecurityConfig(RncRestConfConfiguration config) throws ConfigurationException {
        try {
            final KeyStore.PasswordProtection passProtection =
                    new KeyStore.PasswordProtection(config.getKeyStorePassword().toCharArray());
            final KeyStore keyStore =
                    KeyStore.Builder.newInstance(config.getKeyStoreType(), null, passProtection).getKeyStore();
            final Optional<InputStream> ksFile = readKeyStoreFile(config.getKeyStoreFilePath());

            if (ksFile.isEmpty()) {
                throw new ConfigurationException("Unable to create KeyStore configuration: KeyStore file was not found"
                        + " on path: " + config.getKeyStoreFilePath());
            }

            keyStore.load(ksFile.get(), config.getKeyStorePassword().toCharArray());
            return new SecurityConfig(keyStore, config.getKeyStorePassword());
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            throw new ConfigurationException("Unable to create KeyStore configuration", e);
        }
    }

    private static Optional<InputStream> readKeyStoreFile(String keyStoreFilePath) throws IOException {
        InputStream ksFile;
        try {
            LOG.info("Trying to load KeyStore from filesystem from path {}", keyStoreFilePath);
            ksFile = Files.newInputStream(Paths.get(keyStoreFilePath));
            LOG.info("KeyStore found on filesystem on path {}", keyStoreFilePath);
        } catch (NoSuchFileException e) {
            LOG.info("KeyStore not found on filesystem, looking in resources on path {}", keyStoreFilePath);
            ksFile = RncLightyModuleConfigUtils.class.getClassLoader().getResourceAsStream(keyStoreFilePath);
            LOG.info("KeyStore found on classpath on path {}", keyStoreFilePath);
        }
        if (ksFile == null) {
            LOG.error("KeyStore was not found on path {} in filesystem or resources", keyStoreFilePath);
        }
        return Optional.ofNullable(ksFile);
    }
}
