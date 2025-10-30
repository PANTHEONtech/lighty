/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.session.security;

import io.lighty.gnmi.southbound.schema.certstore.service.CertificationStorageService;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import io.lighty.modules.gnmi.connector.configuration.SecurityFactory;
import io.lighty.modules.gnmi.connector.security.Security;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.Keystore;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.SecurityChoice;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnly;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreGnmiSecurityProvider implements GnmiSecurityProvider {

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreGnmiSecurityProvider.class);
    private final CertificationStorageService certService;

    public KeystoreGnmiSecurityProvider(final CertificationStorageService certificationStorageService) {
        this.certService = certificationStorageService;
    }

    @Override
    public Security getSecurity(final GnmiNode gnmiNode) throws SessionSecurityException {
        final SecurityChoice securityChoice = gnmiNode.getConnectionParameters().getSecurityChoice();
        if (securityChoice instanceof Secure) {
            final String keystoreId = ((Secure) securityChoice).getKeystoreId();
            return getSecurityFromKeystoreId(keystoreId);
        } else if (securityChoice instanceof InsecureDebugOnly) {
            LOG.debug("Creating Security with insecure connection");
            return SecurityFactory.createInsecureGnmiSecurity();
        } else {
            throw new SessionSecurityException(
                    "Missing security configuration. Add keystoreId or connection-type parameter");
        }
    }

    private Security getSecurityFromKeystoreId(final String keystoreId)
            throws SessionSecurityException {
        final Optional<Keystore> optionalKeystore;
        try {
            optionalKeystore = this.certService.readCertificate(keystoreId)
                    .get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new SessionSecurityException(
                    String.format("Unable to read keystore [%s] certificates from operational datastore",
                            keystoreId), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SessionSecurityException(
                    String.format("Interrupted while reading keystore [%s] certificates from operational datastore",
                            keystoreId), e);
        }
        if (optionalKeystore.isPresent()) {
            LOG.debug("Creating Security from keystore [{}]", keystoreId);
            return getSecurityWithCertificates(optionalKeystore.get());
        }
        throw new SessionSecurityException(
                String.format("Certificate with id [%s] is not found in datastore ", keystoreId));
    }

    private Security getSecurityWithCertificates(final Keystore keystore) throws SessionSecurityException {
        final KeyPair keyPair;
        if (keystore.getPassphrase() != null) {
            keyPair = getKeyPair(keystore.getClientKey(), keystore.getPassphrase());
        } else {
            keyPair = getKeyPair(keystore.getClientKey(), "");
        }
        return createSecurityFromKeystore(keyPair, keystore);
    }

    private Security createSecurityFromKeystore(final KeyPair keyPair, final Keystore keystore)
            throws SessionSecurityException {
        try {
            return SecurityFactory.createGnmiSecurity(keystore.getCaCertificate(), keystore.getClientCert(),
                    keyPair.getPrivate());
        } catch (CertificateException e) {
            throw new SessionSecurityException("Error while creating security with certificates", e);
        }
    }

    private KeyPair getKeyPair(final String clientKey, final String passphrase) throws SessionSecurityException {
        try {
            return decodePrivateKey(
                    new StringReader(this.certService
                            .decrypt(clientKey)
                            .replace("\\\\n", "\n")),
                    this.certService.decrypt(passphrase));
        } catch (IOException e) {
            throw new SessionSecurityException("Error while creating KeyPair from private key and passphrase", e);
        } catch (GeneralSecurityException e) {
            LOG.error("Failed do decrypt input {}", clientKey);
            throw new RuntimeException(e);
        }
    }

    public static KeyPair decodePrivateKey(final Reader reader, final String passphrase) throws IOException {
        try (PEMParser keyReader = new PEMParser(reader)) {
            final Provider bcprov;
            final var prov = java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
            bcprov = prov != null ? prov : new BouncyCastleProvider();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().setProvider(bcprov)
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
}
