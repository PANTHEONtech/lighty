/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.server.config.LightyServerConfig;
import io.lighty.server.config.SecurityConfig;
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

public final class LightyServerConfigUtils {

    private static final String SERVER_CONFIG_ROOT_ELEMENT_NAME = "lighty-server";
    private static final Logger LOG = LoggerFactory.getLogger(LightyServerConfigUtils.class);

    private LightyServerConfigUtils() {
        // Hide on purpose
    }

    public static LightyServerConfig getServerConfiguration(final InputStream jsonConfigIS)
            throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigIS);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(SERVER_CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", SERVER_CONFIG_ROOT_ELEMENT_NAME);
            return new LightyServerConfig();
        }
        final JsonNode lightyServerNode = configNode.path(SERVER_CONFIG_ROOT_ELEMENT_NAME);
        final LightyServerConfig lightyServerConfig;
        try {
            lightyServerConfig = mapper.treeToValue(lightyServerNode, LightyServerConfig.class);
            lightyServerConfig.setSecurityConfig(createSecurityConfig(lightyServerConfig));
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                    LightyServerConfig.class), e);
        }

        return lightyServerConfig;
    }

    public static LightyServerConfig getDefaultLightyServerConfig() throws ConfigurationException {
        final LightyServerConfig lightyServerConfig = new LightyServerConfig();
        lightyServerConfig.setSecurityConfig(createSecurityConfig(lightyServerConfig));
        return lightyServerConfig;
    }

    public static SecurityConfig createSecurityConfig(final LightyServerConfig config) throws ConfigurationException {
        try {
            final KeyStore.PasswordProtection passProtection = new KeyStore.PasswordProtection(
                    config.getKeyStorePassword().toCharArray());
            final KeyStore keyStore = KeyStore.Builder.newInstance(
                    config.getKeyStoreType(), null, passProtection).getKeyStore();
            final Optional<InputStream> ksFile = readKeyStoreFile(config.getKeyStoreFilePath());

            if (ksFile.isEmpty()) {
                throw new ConfigurationException("Unable to create KeyStore configuration: KeyStore file was not found"
                        + " on path: " + config.getKeyStoreFilePath());
            }

            final KeyStore.PasswordProtection trustPassProtection = new KeyStore.PasswordProtection(
                    config.getTrustKeyStorePassword().toCharArray());
            final KeyStore trustKeyStore = KeyStore.Builder.newInstance(
                    config.getKeyStoreType(), null, trustPassProtection).getKeyStore();
            final Optional<InputStream> trustKsFile = readKeyStoreFile(config.getTrustKeyStoreFilePath());

            if (trustKsFile.isEmpty()) {
                throw new ConfigurationException("Unable to create TrustKeyStore config: KeyStore file was not found"
                        + " on path: " + config.getTrustKeyStoreFilePath());
            }

            keyStore.load(ksFile.get(), config.getKeyStorePassword().toCharArray());
            trustKeyStore.load(trustKsFile.get(), config.getTrustKeyStorePassword().toCharArray());

            return new SecurityConfig(keyStore, config.getKeyStorePassword(), trustKeyStore,
                    config.getTrustKeyStorePassword(), config.isNeedClientAuth());
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            throw new ConfigurationException("Unable to create KeyStore configuration", e);
        }
    }

    private static Optional<InputStream> readKeyStoreFile(final String keyStoreFilePath) throws IOException {
        InputStream ksFile;
        try {
            LOG.info("Trying to load KeyStore from filesystem from path {}", keyStoreFilePath);
            ksFile = Files.newInputStream(Paths.get(keyStoreFilePath));
            LOG.info("KeyStore found on filesystem on path {}", keyStoreFilePath);
        } catch (NoSuchFileException e) {
            LOG.info("KeyStore not found on filesystem, looking in resources on path {}", keyStoreFilePath);
            ksFile = LightyServerConfigUtils.class.getClassLoader().getResourceAsStream(keyStoreFilePath);
            LOG.info("KeyStore found on classpath on path {}", keyStoreFilePath);
        }
        if (ksFile == null) {
            LOG.error("KeyStore was not found on path {} in filesystem or resources", keyStoreFilePath);
        }
        return Optional.ofNullable(ksFile);
    }
}
