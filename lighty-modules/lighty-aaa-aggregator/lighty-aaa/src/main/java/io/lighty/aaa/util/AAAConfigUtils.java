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
import java.io.Reader;
import java.security.KeyPair;
import java.security.Provider;
import java.security.Security;
import java.util.Set;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AAAConfigUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AAAConfigUtils.class);
    private static final String AAA_ROOT_ELEMENT_NAME = "aaa";

    public static final Set<YangModuleInfo> YANG_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.config.aaa.authn.encrypt.service.config.rev240202
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214
                    .YangModuleInfoImpl.getInstance());

    private static final Provider BCPROV;

    static {
        final var prov = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        BCPROV = prov != null ? prov : new BouncyCastleProvider();
    }

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

    public static KeyPair decodePrivateKey(final Reader reader, final String passphrase) throws IOException {
        try (PEMParser keyReader = new PEMParser(reader)) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().setProvider(BCPROV)
                    .build(passphrase.toCharArray());

            Object privateKey = keyReader.readObject();
            KeyPair keyPair;
            if (privateKey instanceof PEMEncryptedKeyPair pemPrivateKey) {
                keyPair = converter.getKeyPair(pemPrivateKey.decryptKeyPair(decryptionProv));
            } else {
                keyPair = converter.getKeyPair((PEMKeyPair) privateKey);
            }
            return keyPair;
        }
    }

    public static AAAConfiguration createDefaultAAAConfiguration() {
        return new AAAConfiguration();
    }
}
