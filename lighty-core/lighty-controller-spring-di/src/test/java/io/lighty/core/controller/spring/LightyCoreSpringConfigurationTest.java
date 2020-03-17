/*
 * Copyright (c) 2019 Pantheon.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.spring;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModuleRegistryService;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Initializes Spring Boot application and check whether all the lighty.io beans has been correctly autowired.
 * <p/>
 * <p/>
 * Test does not succeed in IDE (Intellij IDEA) if it is started from root directory together with all other tests - it
 * can not create ApplicationContext properly.
 */
@SpringBootTest
public class LightyCoreSpringConfigurationTest extends AbstractTestNGSpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(LightyCoreSpringConfigurationTest.class);

    @Autowired
    LightyController lightyControllerTestProperty;

    @Autowired
    LightyModuleRegistryService lightyModuleRegistryServiceTestProperty;

    @Autowired
    DiagStatusService diagStatusServiceTestProperty;

    @Autowired
    ActorSystemProvider actorSystemProviderTestProperty;

    @Autowired
    SchemaContextProvider schemaContextProviderTestProperty;

    @Autowired
    DOMSchemaService domSchemaServiceTestProperty;

    @Autowired
    DOMYangTextSourceProvider domYangTextSourceProviderTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMMountPointService controllerDOMMountPointServiceTestProperty;

    @Autowired
    DOMMountPointService domMountPointServiceTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService
        controllerDOMNotificationPublishServiceTestProperty;

    @Autowired
    DOMNotificationPublishService domNotificationPublishServiceTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMNotificationService controllerDOMNotificationServiceTestProperty;

    @Autowired
    DOMNotificationService domNotificationServiceTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry
        controllerDOMNotificationSubscriptionListenerRegistryTestProperty;

    @Autowired
    DOMNotificationSubscriptionListenerRegistry domNotificationSubscriptionListenerRegistryTestProperty;

    @Autowired
    @Qualifier("ConfigDatastore")
    DistributedDataStoreInterface configDatastoreTestProperty;

    @Autowired
    @Qualifier("OperationalDatastore")
    DistributedDataStoreInterface operationalDatastoreTestProperty;

    @Autowired
    @Qualifier("ControllerClusteredDOMDataBroker")
    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker controllerClusteredDOMDataBrokerTestProperty;

    @Autowired
    DOMDataBroker clusteredDOMDataBrokerTestProperty;

    @Autowired
    @Qualifier("ControllerPingPongDataBroker")
    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker controllerPingPongDataBrokerTestProperty;

    @Autowired
    DOMDataTreeShardingService domDataTreeShardingServiceTestProperty;

    @Autowired
    DOMDataTreeService domDataTreeServiceTestProperty;

    @Autowired
    DistributedShardFactory distributedShardFactoryTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMRpcService controllerDOMRpcServiceTestProperty;

    @Autowired
    DOMRpcService domRpcServiceTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService controllerDOMRpcProviderServiceTestProperty;

    @Autowired
    DOMRpcProviderService domRpcProviderServiceTestProperty;

    @Autowired
    BindingNormalizedNodeSerializer bindingNormalizedNodeSerializerTestProperty;

    @Autowired
    BindingCodecTreeFactory bindingCodecTreeFactoryTestProperty;

    @Autowired
    DOMEntityOwnershipService domEntityOwnershipServiceTestProperty;

    @Autowired
    EntityOwnershipService entityOwnershipServiceTestProperty;

    @Autowired
    ClusterAdminService clusterAdminRPCServiceTestProperty;

    @Autowired
    ClusterSingletonServiceProvider clusterSingletonServiceProviderTestProperty;

    @Autowired
    org.opendaylight.controller.sal.binding.api.RpcProviderRegistry controllerRpcProviderRegistryTestProperty;

    @Autowired
    RpcProviderService rpcProviderRegistryTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.binding.api.MountPointService controllerBindingMountPointServiceTestProperty;

    @Autowired
    MountPointService bindingMountPointServiceTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.binding.api.NotificationService controllerBindingNotificationServiceTestProperty;

    @Autowired
    NotificationService notificationServiceTestProperty;

    @Autowired
    org.opendaylight.controller.md.sal.binding.api.NotificationPublishService
        controllerBindingNotificationPublishServiceTestProperty;

    @Autowired
    NotificationPublishService bindingNotificationPublishServiceTestProperty;

    @Autowired
    org.opendaylight.controller.sal.binding.api.NotificationProviderService notificationProviderServiceTestProperty;

    @Autowired
    org.opendaylight.controller.sal.binding.api.NotificationService controllerNotificationProviderServiceTestProperty;

    @Autowired
    @Qualifier("ControllerBindingDataBroker")
    org.opendaylight.controller.md.sal.binding.api.DataBroker controllerBindingDataBrokerTestProperty;

    @Autowired
    DataBroker bindingDataBrokerTestProperty;

    @Autowired
    @Qualifier("ControllerBindingPingPongDataBroker")
    org.opendaylight.controller.md.sal.binding.api.DataBroker controllerBindingPingPongDataBrokerTestProperty;

    @Autowired
    EventExecutor eventExecutorTestProperty;

    @Autowired
    @Qualifier("BossGroup")
    EventLoopGroup bossGroupTestProperty;

    @Autowired
    @Qualifier("WorkerGroup")
    EventLoopGroup workerGroupTestProperty;

    @Autowired
    ThreadPool threadPoolTestProperty;

    @Autowired
    ScheduledThreadPool scheduledThreadPoolTestProperty;

    @Autowired
    Timer timerTestProperty;

    @Test
    void testLightyBeansExists() {
        Assert.assertNotNull(lightyControllerTestProperty);
        Assert.assertNotNull(lightyModuleRegistryServiceTestProperty);
        Assert.assertNotNull(diagStatusServiceTestProperty);
        Assert.assertNotNull(actorSystemProviderTestProperty);
        Assert.assertNotNull(schemaContextProviderTestProperty);
        Assert.assertNotNull(domSchemaServiceTestProperty);
        Assert.assertNotNull(domYangTextSourceProviderTestProperty);
        Assert.assertNotNull(controllerDOMMountPointServiceTestProperty);
        Assert.assertNotNull(domMountPointServiceTestProperty);
        Assert.assertNotNull(controllerDOMNotificationPublishServiceTestProperty);
        Assert.assertNotNull(domNotificationPublishServiceTestProperty);
        Assert.assertNotNull(controllerDOMNotificationServiceTestProperty);
        Assert.assertNotNull(domNotificationServiceTestProperty);
        Assert.assertNotNull(controllerDOMNotificationSubscriptionListenerRegistryTestProperty);
        Assert.assertNotNull(domNotificationSubscriptionListenerRegistryTestProperty);
        Assert.assertNotNull(configDatastoreTestProperty);
        Assert.assertNotNull(operationalDatastoreTestProperty);
        Assert.assertNotNull(controllerClusteredDOMDataBrokerTestProperty);
        Assert.assertNotNull(clusteredDOMDataBrokerTestProperty);
        Assert.assertNotNull(controllerPingPongDataBrokerTestProperty);
        Assert.assertNotNull(domDataTreeShardingServiceTestProperty);
        Assert.assertNotNull(domDataTreeServiceTestProperty);
        Assert.assertNotNull(distributedShardFactoryTestProperty);
        Assert.assertNotNull(controllerDOMRpcServiceTestProperty);
        Assert.assertNotNull(domRpcServiceTestProperty);
        Assert.assertNotNull(controllerDOMRpcProviderServiceTestProperty);
        Assert.assertNotNull(domRpcProviderServiceTestProperty);
        Assert.assertNotNull(bindingNormalizedNodeSerializerTestProperty);
        Assert.assertNotNull(bindingCodecTreeFactoryTestProperty);
        Assert.assertNotNull(domEntityOwnershipServiceTestProperty);
        Assert.assertNotNull(entityOwnershipServiceTestProperty);
        Assert.assertNotNull(clusterAdminRPCServiceTestProperty);
        Assert.assertNotNull(clusterSingletonServiceProviderTestProperty);
        Assert.assertNotNull(controllerRpcProviderRegistryTestProperty);
        Assert.assertNotNull(rpcProviderRegistryTestProperty);
        Assert.assertNotNull(controllerBindingMountPointServiceTestProperty);
        Assert.assertNotNull(bindingMountPointServiceTestProperty);
        Assert.assertNotNull(controllerBindingNotificationServiceTestProperty);
        Assert.assertNotNull(notificationServiceTestProperty);
        Assert.assertNotNull(controllerBindingNotificationPublishServiceTestProperty);
        Assert.assertNotNull(bindingNotificationPublishServiceTestProperty);
        Assert.assertNotNull(notificationProviderServiceTestProperty);
        Assert.assertNotNull(controllerNotificationProviderServiceTestProperty);
        Assert.assertNotNull(controllerBindingDataBrokerTestProperty);
        Assert.assertNotNull(bindingDataBrokerTestProperty);
        Assert.assertNotNull(controllerBindingPingPongDataBrokerTestProperty);
        Assert.assertNotNull(eventExecutorTestProperty);
        Assert.assertNotNull(bossGroupTestProperty);
        Assert.assertNotNull(workerGroupTestProperty);
        Assert.assertNotNull(threadPoolTestProperty);
        Assert.assertNotNull(scheduledThreadPoolTestProperty);
        Assert.assertNotNull(timerTestProperty);
    }

    @TestConfiguration
    static class TestConfig extends LightyCoreSpringConfiguration {

        @Override
        public LightyController initLightyController() throws LightyLaunchException, InterruptedException {
            try {
                LOG.info("Building LightyController Core");
                final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
                final LightyController lightyController = lightyControllerBuilder
                        .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(Collections.emptySet()))
                        .build();
                LOG.info("Starting LightyController");
                final ListenableFuture<Boolean> started = lightyController.start();
                started.get();
                LOG.info("LightyController Core started");

                return lightyController;
            } catch (ConfigurationException | ExecutionException e) {
                throw new LightyLaunchException("Could not init LightyController", e);
            }
        }

        @Override
        @SuppressWarnings("checkstyle:illegalCatch")
        public void shutdownLightyController(LightyController lightyController) throws LightyLaunchException {
            try {
                LOG.info("Shutting down LightyController ...");
                lightyController.shutdown();
            } catch (Exception e) {
                throw new LightyLaunchException("Could not shutdown LightyController", e);
            }
        }
    }
}
