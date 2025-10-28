/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import static org.mockito.Mockito.when;

import java.util.Optional;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.aaa.cert.impl.KeyStoresDataUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321.KeyStores;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321.KeyStoresBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321.key.stores.SslData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321.key.stores.SslDataBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CertificateManagerConfigTest {

    private static final String BUNDLE_NAME = "opendaylight";

    @Mock
    DataBroker bindingDataBroker;

    @Mock
    ReadTransaction readTransaction;

    @Mock
    RpcProviderService rpcProviderService;

    @BeforeClass
    public void init() {
        MockitoAnnotations.initMocks(this);
        DataObjectIdentifier<AaaEncryptServiceConfig> build = DataObjectIdentifier
            .builder(AaaEncryptServiceConfig.class).build();
        when(bindingDataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        AaaEncryptServiceConfig aaaEncryptServiceConfig = new AaaEncryptServiceConfigBuilder().build();
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, build))
            .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(aaaEncryptServiceConfig)));

        KeyStores keyStores = new KeyStoresBuilder().build();
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, DataObjectIdentifier
            .builder(KeyStores.class).build()))
            .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(keyStores)));

        SslData sslData = new SslDataBuilder()
                .setBundleName(BUNDLE_NAME).build();
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, KeyStoresDataUtils.getSslDataIid(BUNDLE_NAME)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(sslData)));
    }

    @Test
    public void getDefaultTest() {
        ICertificateManager certificateManager = CertificateManagerConfig.getDefault(bindingDataBroker,
                rpcProviderService);

        Assert.assertNotNull(certificateManager);
        Assert.assertNotNull(certificateManager.getServerContext());
        Assert.assertEquals(certificateManager.getServerContext().getProtocol(), "TLS");
        Assert.assertNotNull(certificateManager.getServerContext().getProvider());
        Assert.assertNotNull(certificateManager.getServerContext().getDefaultSSLParameters());
    }
}
