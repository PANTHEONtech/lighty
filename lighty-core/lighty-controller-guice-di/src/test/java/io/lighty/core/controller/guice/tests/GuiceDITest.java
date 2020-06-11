/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.guice.tests;

import static org.testng.Assert.fail;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GuiceDITest {

    private LightyController lightyController;
    private TestService testService;

    @BeforeClass
    public void init() throws ExecutionException, InterruptedException, ConfigurationException {
        ControllerConfiguration defaultSingleNodeConfiguration =
                ControllerConfigUtils.getDefaultSingleNodeConfiguration();
        lightyController = LightyControllerBuilder
                .from(defaultSingleNodeConfiguration)
                .build();
        lightyController.start().get();

        LightyControllerModule lightyModule = new LightyControllerModule(lightyController.getServices());
        Injector injector = Guice.createInjector(lightyModule);
        testService = injector.getInstance(TestService.class);
    }

    @AfterClass
    @SuppressWarnings("checkstyle:illegalCatch")
    public void shutdown() {
        try {
            if (lightyController != null) {
                lightyController.shutdown();
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testDiDiagStatusService() {
        assertNotNull(testService.getDiagStatusService());
    }

    @Test
    public void testDiActorSystemProvider() {
        assertNotNull(testService.getActorSystemProvider());
    }

    @Test
    public void testDiSchemaContextProvider() {
        assertNotNull(testService.getSchemaContextProvider());
    }

    @Test
    public void testDiDomSchemaService() {
        assertNotNull(testService.getDomSchemaService());
    }

    @Test
    public void testDiDomYangTextSourceProvider() {
        assertNotNull(testService.getDomYangTextSourceProvider());
    }

    @Test
    public void testDiDOMNotificationSubscriptionListenerRegistry() {
        assertNotNull(testService.getDomNotificationSubscriptionListenerRegistry());
    }

    @Test
    public void testDiDistributedDataStoreInterfaceConfig() {
        assertNotNull(testService.getDistributedDataStoreInterfaceConfig());
    }

    @Test
    public void testDiDistributedDataStoreInterfaceOperational() {
        assertNotNull(testService.getDistributedDataStoreInterfaceOperational());
    }

    @Test
    public void testDiDomDataTreeShardingService() {
        assertNotNull(testService.getDomDataTreeShardingService());
    }

    @Test
    public void testDiDOMDataTreeService() {
        assertNotNull(testService.getDomDataTreeService());
    }

    @Test
    public void testDiDistributedShardFactory() {
        assertNotNull(testService.getDistributedShardFactory());
    }

    @Test
    public void testDiBindingNormalizedNodeSerializer() {
        assertNotNull(testService.getBindingNormalizedNodeSerializer());
    }

    @Test
    public void testDiBindingCodecTreeFactory() {
        assertNotNull(testService.getBindingCodecTreeFactory());
    }

    @Test
    public void testDiDOMEntityOwnershipService() {
        assertNotNull(testService.getDomEntityOwnershipService());
    }

    @Test
    public void testDiEntityOwnershipService() {
        assertNotNull(testService.getEntityOwnershipService());
    }

    @Test
    public void testDiClusterAdminService() {
        assertNotNull(testService.getClusterAdminService());
    }

    @Test
    public void testDiClusterSingletonServiceProvider() {
        assertNotNull(testService.getClusterSingletonServiceProvider());
    }

    @Test
    public void testDiEventExecutor() {
        assertNotNull(testService.getEventExecutor());
    }

    @Test
    public void testDiEventLoopGroupBoss() {
        assertNotNull(testService.getEventLoopGroupBoss());
    }

    @Test
    public void testDiEventLoopGroupWorker() {
        assertNotNull(testService.getEventLoopGroupWorker());
    }

    @Test
    public void testDiThreadPool() {
        assertNotNull(testService.getThreadPool());
    }

    @Test
    public void testDiScheduledThreadPool() {
        assertNotNull(testService.getScheduledThreadPool());
    }

    @Test
    public void testDiTimer() {
        assertNotNull(testService.getTimer());
    }

    @Test
    public void testDiDOMMountPointService() {
        assertNotNull(testService.getDomMountPointService());
    }

    @Test
    public void testDiDOMNotificationPublishService() {
        assertNotNull(testService.getDomNotificationPublishService());
    }

    @Test
    public void testDiDOMNotificationService() {
        assertNotNull(testService.getDomNotificationService());
    }

    @Test
    public void testDiDOMDataBroker() {
        assertNotNull(testService.getDomDataBroker());
    }

    @Test
    public void testDiDOMRpcService() {
        assertNotNull(testService.getDomRpcService());
    }

    @Test
    public void testDiDOMRpcProviderService() {
        assertNotNull(testService.getDomRpcProviderService());
    }

    @Test
    public void testDiRpcProviderService() {
        assertNotNull(testService.getRpcProviderService());
    }

    @Test
    public void testDiMountPointService() {
        assertNotNull(testService.getMountPointService());
    }

    @Test
    public void testDiNotificationService() {
        assertNotNull(testService.getNotificationService());
    }

    @Test
    public void testDiNotificationPublishService() {
        assertNotNull(testService.getNotificationPublishService());
    }

    @Test
    public void testDiLightyServices() {
        assertNotNull(testService.getLightyServices());
    }

    @Test
    public void testDiLightyModuleRegistryService() {
        assertNotNull(testService.getLightyModuleRegistryService());
    }

    @Test
    public void testDiBindingDataBroker() {
        assertNotNull(testService.getBindingDataBroker());
    }

}
