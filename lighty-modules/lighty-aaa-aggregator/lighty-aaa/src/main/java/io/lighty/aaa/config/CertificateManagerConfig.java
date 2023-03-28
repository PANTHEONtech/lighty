/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import io.lighty.aaa.encrypt.service.impl.AAAEncryptionServiceImpl;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.aaa.cert.impl.CertificateManagerService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.AaaCertServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.AaaCertServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.CtlKeystore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.CtlKeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.TrustKeystore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.TrustKeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.ctlkeystore.CipherSuites;

public final class CertificateManagerConfig {
    private CertificateManagerConfig() {
        // no-op
    }

    public static ICertificateManager getDefault(final DataBroker bindingDataBroker,
            final RpcProviderService rpcProviderService) {
        final List<CipherSuites> cipherSuites = new ArrayList<>();
        final CtlKeystore ctlKeystore = new CtlKeystoreBuilder()
                .setName("ctl.jks")
                .setAlias("controller")
                .setDname("CN=ODL, OU=Dev, O=LinuxFoundation, L=QC Montreal, C=CA")
                .setValidity(365)
                .setKeyAlg("RSA")
                .setSignAlg("SHA1WithRSAEncryption")
                .setCipherSuites(cipherSuites)
                .setStorePassword("")
                .setKeysize(1024)
                .build();
        final TrustKeystore trustKeystore = new TrustKeystoreBuilder()
                .setName("truststore.jks")
                .build();
        final AaaCertServiceConfig aaaCertServiceConfig = new AaaCertServiceConfigBuilder()
                .setUseConfig(true)
                .setUseMdsal(true)
                .setBundleName("opendaylight")
                .setCtlKeystore(ctlKeystore)
                .setTrustKeystore(trustKeystore)
                .build();
        return new CertificateManagerService(rpcProviderService, bindingDataBroker, createAAAEncryptionService(),
                aaaCertServiceConfig);
    }

    private static AAAEncryptionService createAAAEncryptionService() {
        final String salt = "TdtWeHbch/7xP52/rp3Usw==";
        final String encryptKey = "V1S1ED4OMeEh";
        final String encryptMethod = "PBKDF2WithHmacSHA1";
        final String encryptType = "AES";
        final int iterationCount = 32768;
        final int encryptKeyLength = 128;
        final String cipherTransforms = "AES/CBC/PKCS5Padding";

        final byte[] encryptionKeySalt = Base64.getDecoder().decode(salt);
        try {
            final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptMethod);
            final KeySpec keySpec = new PBEKeySpec(encryptKey.toCharArray(), encryptionKeySalt,
                iterationCount, encryptKeyLength);
            final SecretKey key = new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(),
                encryptType);
            final IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptionKeySalt);

            final Cipher encryptCipher = Cipher.getInstance(cipherTransforms);
            encryptCipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

            final Cipher decryptCipher = Cipher.getInstance(cipherTransforms);
            decryptCipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

            return new AAAEncryptionServiceImpl(encryptCipher, decryptCipher);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
            | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException("Cannot initialize lighty.io AAAEncryptionService", e);
        }
    }
}
