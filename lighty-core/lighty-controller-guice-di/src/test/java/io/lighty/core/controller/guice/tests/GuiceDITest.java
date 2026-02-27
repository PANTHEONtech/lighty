/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.guice.tests;

import static org.testng.AssertJUnit.assertNotNull;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.guice.LightyControllerModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

class GuiceDITest {
    private static final Logger LOG = LoggerFactory.getLogger(GuiceDITest.class);

    private LightyController lightyController;
    private TestService testService;

    @BeforeClass
    void init() throws ExecutionException, InterruptedException, ConfigurationException {
        ControllerConfiguration defaultSingleNodeConfiguration =
            ControllerConfigUtils.getDefaultSingleNodeConfiguration();
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder
                .from(defaultSingleNodeConfiguration)
                .build();
        lightyController.start().get();

        LightyControllerModule lightyModule = new LightyControllerModule(lightyController.getServices());
        Injector injector = Guice.createInjector(lightyModule);
        testService = injector.getInstance(TestService.class);
    }

    @AfterClass
    @SuppressWarnings("checkstyle:illegalCatch")
    void shutdown() {
        try {
            if (lightyController != null) {
                lightyController.shutdown(60, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOG.error("Shutdown of LightyController failed", e);
        }
    }

    @Test
    void testDIDiagStatusService() {
        assertNotNull(testService.getDiagStatusService());
    }

    @Test
    void testDIActorSystemProvider() {
        assertNotNull(testService.getActorSystemProvider());
    }

    @Test
    void testDISchemaContextProvider() {
        assertNotNull(testService.getSchemaContext());
    }

    @Test
    void testDIDomSchemaService() {
        assertNotNull(testService.getDomSchemaService());
    }

    @Test
    void testDIDomNotificationSubscriptionListenerRegistry() {
        assertNotNull(testService.getDomNotificationSubscriptionListenerRegistry());
    }

    @Test
    void testDIDistributedDataStoreInterfaceConfig() {
        assertNotNull(testService.getDistributedDataStoreInterfaceConfig());
    }

    @Test
    void testDIDistributedDataStoreInterfaceOperational() {
        assertNotNull(testService.getDistributedDataStoreInterfaceOperational());
    }

    @Test
    void testDIBindingNormalizedNodeSerializer() {
        assertNotNull(testService.getBindingNormalizedNodeSerializer());
    }

    @Test
    void testDIBindingCodecTreeFactory() {
        assertNotNull(testService.getBindingCodecTreeFactory());
    }

    @Test
    void testDIDomEntityOwnershipService() {
        assertNotNull(testService.getDomEntityOwnershipService());
    }

    @Test
    void testDIEntityOwnershipService() {
        assertNotNull(testService.getEntityOwnershipService());
    }

    @Test
    void testDIClusterAdminService() {
        assertNotNull(testService.getClusterAdminService());
    }

    @Test
    void testDIClusterSingletonServiceProvider() {
        assertNotNull(testService.getClusterSingletonServiceProvider());
    }

    @Test
    void testDIDomMountPointService() {
        assertNotNull(testService.getDomMountPointService());
    }

    @Test
    void testDIDomNotificationPublishService() {
        assertNotNull(testService.getDomNotificationPublishService());
    }

    @Test
    void testDIDomNotificationService() {
        assertNotNull(testService.getDomNotificationService());
    }

    @Test
    void testDIDomDataBroker() {
        assertNotNull(testService.getDomDataBroker());
    }

    @Test
    void testDIDomRpcService() {
        assertNotNull(testService.getDomRpcService());
    }

    @Test
    void testDIDomRpcProviderService() {
        assertNotNull(testService.getDomRpcProviderService());
    }

    @Test
    void testDIRpcProviderService() {
        assertNotNull(testService.getRpcProviderService());
    }

    @Test
    void testDIMountPointService() {
        assertNotNull(testService.getMountPointService());
    }

    @Test
    void testDINotificationService() {
        assertNotNull(testService.getNotificationService());
    }

    @Test
    void testDINotificationPublishService() {
        assertNotNull(testService.getNotificationPublishService());
    }

    @Test
    void testDILightyServices() {
        assertNotNull(testService.getLightyServices());
    }

    @Test
    void testDILightyModuleRegistryService() {
        assertNotNull(testService.getLightyModuleRegistryService());
    }

    @Test
    void testDIBindingDataBroker() {
        assertNotNull(testService.getBindingDataBroker());
    }

}
