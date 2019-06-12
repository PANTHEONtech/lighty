package io.lighty.core.controller.guice.tests;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.guice.LightyControllerModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;

public class GuiceDITest {

    private static LightyController lightyController;
    private static TestService testService;

    @BeforeClass
    public static void init() throws ExecutionException, InterruptedException, ConfigurationException {
        ControllerConfiguration defaultSingleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration();
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        lightyController = lightyControllerBuilder
                .from(defaultSingleNodeConfiguration)
                .build();
        lightyController.start().get();

        LightyControllerModule lightyModule = new LightyControllerModule(lightyController.getServices());
        Injector injector = Guice.createInjector(lightyModule);
        testService = injector.getInstance(TestService.class);
    }

    @Test
    public void testDIDiagStatusService() {
        Assert.assertNotNull(testService.getDiagStatusService());
    }

    @Test
    public void testDIActorSystemProvider() {
        Assert.assertNotNull(testService.getActorSystemProvider());
    }

    @Test
    public void testDISchemaContextProvider() {
        Assert.assertNotNull(testService.getSchemaContextProvider());
    }

    @Test
    public void testDIDomSchemaService() {
        Assert.assertNotNull(testService.getDomSchemaService());
    }

    @Test
    public void testDIDomYangTextSourceProvider() {
        Assert.assertNotNull(testService.getDomYangTextSourceProvider());
    }

    @Test
    public void testDIDOMNotificationSubscriptionListenerRegistry() {
        Assert.assertNotNull(testService.getDomNotificationSubscriptionListenerRegistry());
    }

    @Test
    public void testDIDistributedDataStoreInterfaceConfig() {
        Assert.assertNotNull(testService.getDistributedDataStoreInterfaceConfig());
    }

    @Test
    public void testDIDistributedDataStoreInterfaceOperational() {
        Assert.assertNotNull(testService.getDistributedDataStoreInterfaceOperational());
    }

    @Test
    public void testDIDomDataTreeShardingService() {
        Assert.assertNotNull(testService.getDomDataTreeShardingService());
    }

    @Test
    public void testDIDOMDataTreeService() {
        Assert.assertNotNull(testService.getDomDataTreeService());
    }

    @Test
    public void testDIDistributedShardFactory() {
        Assert.assertNotNull(testService.getDistributedShardFactory());
    }

    @Test
    public void testDIBindingNormalizedNodeSerializer() {
        Assert.assertNotNull(testService.getBindingNormalizedNodeSerializer());
    }

    @Test
    public void testDIBindingCodecTreeFactory() {
        Assert.assertNotNull(testService.getBindingCodecTreeFactory());
    }

    @Test
    public void testDIDOMEntityOwnershipService() {
        Assert.assertNotNull(testService.getDomEntityOwnershipService());
    }

    @Test
    public void testDIEntityOwnershipService() {
        Assert.assertNotNull(testService.getEntityOwnershipService());
    }

    @Test
    public void testDIClusterAdminService() {
        Assert.assertNotNull(testService.getClusterAdminService());
    }

    @Test
    public void testDIClusterSingletonServiceProvider() {
        Assert.assertNotNull(testService.getClusterSingletonServiceProvider());
    }

    @Test
    public void testDIEventExecutor() {
        Assert.assertNotNull(testService.getEventExecutor());
    }

    @Test
    public void testDIEventLoopGroupBoss() {
        Assert.assertNotNull(testService.getEventLoopGroupBoss());
    }

    @Test
    public void testDIEventLoopGroupWorker() {
        Assert.assertNotNull(testService.getEventLoopGroupWorker());
    }

    @Test
    public void testDIThreadPool() {
        Assert.assertNotNull(testService.getThreadPool());
    }

    @Test
    public void testDIScheduledThreadPool() {
        Assert.assertNotNull(testService.getScheduledThreadPool());
    }

    @Test
    public void testDITimer() {
        Assert.assertNotNull(testService.getTimer());
    }

    @Test
    public void testDIDOMMountPointService() {
        Assert.assertNotNull(testService.getDomMountPointService());
    }

    @Test
    public void testDIDOMNotificationPublishService() {
        Assert.assertNotNull(testService.getDomNotificationPublishService());
    }

    @Test
    public void testDIDOMNotificationService() {
        Assert.assertNotNull(testService.getDomNotificationService());
    }

    @Test
    public void testDIDOMDataBroker() {
        Assert.assertNotNull(testService.getDomDataBroker());
    }

    @Test
    public void testDIDOMRpcService() {
        Assert.assertNotNull(testService.getDomRpcService());
    }

    @Test
    public void testDIDOMRpcProviderService() {
        Assert.assertNotNull(testService.getDomRpcProviderService());
    }

    @Test
    public void testDIRpcProviderService() {
        Assert.assertNotNull(testService.getRpcProviderService());
    }

    @Test
    public void testDIMountPointService() {
        Assert.assertNotNull(testService.getMountPointService());
    }

    @Test
    public void testDINotificationService() {
        Assert.assertNotNull(testService.getNotificationService());
    }

    @Test
    public void testDINotificationPublishService() {
        Assert.assertNotNull(testService.getNotificationPublishService());
    }

    @Test
    public void testDILightyServices() {
        Assert.assertNotNull(testService.getLightyServices());
    }

    @Test
    public void testDILightyModuleRegistryService() {
        Assert.assertNotNull(testService.getLightyModuleRegistryService());
    }

    @Test
    public void testDIBindingDataBroker() {
        Assert.assertNotNull(testService.getBindingDataBroker());
    }

    @AfterClass
    public static void shutdown() {
        try {
            if (lightyController != null) {
                lightyController.shutdown();
            }
        } catch (Exception e) {
            Assert.fail();
        }
    }

}
