/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import static org.mockito.Mockito.when;
import static org.testng.Assert.expectThrows;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.server.LightyServerBuilder;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.PasswordCredentials;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321.key.stores.SslDataKey;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ShiroInitializationTest {
    private static final AAAConfiguration AAA_CONFIGURATION = AAAConfigUtils.createDefaultAAAConfiguration();
    private static final String BUNDLE_NAME = "opendaylight";
    @Mock
    private LightyServerBuilder server;
    @Mock
    private DataBroker bindingDataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private CredentialAuth<PasswordCredentials> credentialAuth;
    @Mock
    private RpcProviderService rpcProviderService;
    private AAALighty aaaLighty;

    @BeforeClass
    public void init() {
        // Initialize the mock objects
        MockitoAnnotations.initMocks(this);

        // Set up mocks for some datastore reads
        final InstanceIdentifier<AaaEncryptServiceConfig> aaaEncryptInstanceIdentifier = InstanceIdentifier.builder(
                AaaEncryptServiceConfig.class).build();
        when(bindingDataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        final AaaEncryptServiceConfig aaaEncryptServiceConfig = new AaaEncryptServiceConfigBuilder().build();
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, aaaEncryptInstanceIdentifier))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(aaaEncryptServiceConfig)));

        final KeyStores keyStores = new KeyStoresBuilder().build();
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(KeyStores.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(keyStores)));

        final SslData sslData = new SslDataBuilder().setBundleName(BUNDLE_NAME).build();
        final KeyedInstanceIdentifier<SslData, SslDataKey> keyStoresInstanceIdentifier = InstanceIdentifier.create(
                KeyStores.class).child(SslData.class, new SslDataKey(BUNDLE_NAME));
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, keyStoresInstanceIdentifier))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(sslData)));
    }

    @AfterMethod
    public void tearDown() {
        if (aaaLighty != null) {
            // Stop the object and ensure that stopping was successful
            assertTrue(aaaLighty.stopProcedure());
            assertTrue(aaaLighty.shutdown(60, TimeUnit.SECONDS));
        }
    }

    // Test that the AAALighty object can be stopped after failing to initialize
    @Test
    public void testStopProcedureWithFailedInitialization() {
        // Create an AAALighty object with mocked dependencies
        this.aaaLighty = new AAALighty(bindingDataBroker, credentialAuth, server, AAA_CONFIGURATION);
        // Ensure that the object was created successfully
        assertNotNull(aaaLighty);
        // Expect an Exception to be thrown when trying to initialize the object
        expectThrows(Exception.class, () -> aaaLighty.initProcedure());
    }

    // Test that the AAALighty object can be successfully initialized and stopped
    @Test
    public void testSuccessfulInitialization() throws InterruptedException {
        // set CertificateManager
        AAA_CONFIGURATION.setCertificateManager(
                CertificateManagerConfig.getDefault(bindingDataBroker, rpcProviderService));

        // Create a LightyServerBuilder object
        final LightyServerBuilder serverBuilder = new LightyServerBuilder(
                new InetSocketAddress("localhost/127.0.0.1", 8182));

        // Create an AAALighty object
        this.aaaLighty = new AAALighty(bindingDataBroker, null, serverBuilder, AAA_CONFIGURATION);
        // Ensure that the object was created successfully
        assertNotNull(aaaLighty);

        // Initialize the object and ensure that initialization was successful
        final boolean initProcedure = aaaLighty.initProcedure();
        assertTrue(initProcedure);
    }
}
