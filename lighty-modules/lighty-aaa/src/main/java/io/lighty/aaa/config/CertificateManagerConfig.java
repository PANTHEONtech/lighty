/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.aaa.cert.impl.CertificateManagerService;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.aaa.encrypt.impl.AAAEncryptionServiceImpl;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.AaaCertServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.AaaCertServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.CtlKeystore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.CtlKeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.TrustKeystore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.TrustKeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.rev151126.aaa.cert.service.config.ctlkeystore.CipherSuites;

public final class CertificateManagerConfig {
    private CertificateManagerConfig() {

    }

    public static ICertificateManager getDefault(final DataBroker bindingDataBroker) {
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
        final AaaEncryptServiceConfig encrySrvConfig = new AaaEncryptServiceConfigBuilder()
                .setEncryptKey("V1S1ED4OMeEh")
                .setPasswordLength(12)
                .setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
                .setEncryptMethod("PBKDF2WithHmacSHA1")
                .setEncryptType("AES")
                .setEncryptIterationCount(32768)
                .setEncryptKeyLength(128)
                .setCipherTransforms("AES/CBC/PKCS5Padding")
                .build();
        final AAAEncryptionService encryptionSrv = new AAAEncryptionServiceImpl(encrySrvConfig, bindingDataBroker);

        return new CertificateManagerService(aaaCertServiceConfig, bindingDataBroker, encryptionSrv);
    }
}
