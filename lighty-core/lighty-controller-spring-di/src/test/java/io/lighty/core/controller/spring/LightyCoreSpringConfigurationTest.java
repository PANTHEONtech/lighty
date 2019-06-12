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

@SpringBootTest
public class LightyCoreSpringConfigurationTest extends AbstractTestNGSpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(LightyCoreSpringConfigurationTest.class);

    @Autowired
    @Qualifier("LightyController")
    LightyController lightyController;

    @Autowired
    @Qualifier("LightyModuleRegistryService")
    LightyModuleRegistryService lightyModuleRegistryService;

    @Autowired
    @Qualifier("DiagStatusService")
    DiagStatusService diagStatusService;

    @Autowired
    @Qualifier("ActorSystemProvider")
    ActorSystemProvider actorSystemProvider;

    @Autowired
    @Qualifier("SchemaContextProvider")
    SchemaContextProvider schemaContextProvider;

    @Autowired
    @Qualifier("DOMSchemaService")
    DOMSchemaService dOMSchemaService;

    @Autowired
    @Qualifier("DOMYangTextSourceProvider")
    DOMYangTextSourceProvider dOMYangTextSourceProvider;

    @Autowired
    @Qualifier("ControllerDOMMountPointService")
    org.opendaylight.controller.md.sal.dom.api.DOMMountPointService controllerDOMMountPointService;

    @Autowired
    @Qualifier("DOMMountPointService")
    DOMMountPointService dOMMountPointService;

    @Autowired
    @Qualifier("ControllerDOMNotificationPublishService")
    org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService controllerDOMNotificationPublishService;

    @Autowired
    @Qualifier("DOMNotificationPublishService")
    DOMNotificationPublishService dOMNotificationPublishService;

    @Autowired
    @Qualifier("ControllerDOMNotificationService")
    org.opendaylight.controller.md.sal.dom.api.DOMNotificationService controllerDOMNotificationService;

    @Autowired
    @Qualifier("DOMNotificationService")
    DOMNotificationService dOMNotificationService;

    @Autowired
    @Qualifier("ControllerDOMNotificationSubscriptionListenerRegistry")
    org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry controllerDOMNotificationSubscriptionListenerRegistry;

    @Autowired
    @Qualifier("DOMNotificationSubscriptionListenerRegistry")
    DOMNotificationSubscriptionListenerRegistry dOMNotificationSubscriptionListenerRegistry;

    @Autowired
    @Qualifier("ConfigDatastore")
    DistributedDataStoreInterface configDatastore;

    @Autowired
    @Qualifier("OperationalDatastore")
    DistributedDataStoreInterface operationalDatastore;

    @Autowired
    @Qualifier("ControllerClusteredDOMDataBroker")
    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker controllerClusteredDOMDataBroker;

    @Autowired
    @Qualifier("ClusteredDOMDataBroker")
    DOMDataBroker clusteredDOMDataBroker;

    @Autowired
    @Qualifier("ControllerPingPongDataBroker")
    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker controllerPingPongDataBroker;

    @Autowired
    @Qualifier("DOMDataTreeShardingService")
    DOMDataTreeShardingService dOMDataTreeShardingService;

    @Autowired
    @Qualifier("DOMDataTreeService")
    DOMDataTreeService dOMDataTreeService;

    @Autowired
    @Qualifier("DistributedShardFactory")
    DistributedShardFactory distributedShardFactory;

    @Autowired
    @Qualifier("ControllerDOMRpcService")
    org.opendaylight.controller.md.sal.dom.api.DOMRpcService controllerDOMRpcService;

    @Autowired
    @Qualifier("DOMRpcService")
    DOMRpcService dOMRpcService;

    @Autowired
    @Qualifier("ControllerDOMRpcProviderService")
    org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService controllerDOMRpcProviderService;

    @Autowired
    @Qualifier("DOMRpcProviderService")
    DOMRpcProviderService dOMRpcProviderService;

    @Autowired
    @Qualifier("BindingNormalizedNodeSerializer")
    BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    @Autowired
    @Qualifier("BindingCodecTreeFactory")
    BindingCodecTreeFactory bindingCodecTreeFactory;

    @Autowired
    @Qualifier("DOMEntityOwnershipService")
    DOMEntityOwnershipService dOMEntityOwnershipService;

    @Autowired
    @Qualifier("EntityOwnershipService")
    EntityOwnershipService entityOwnershipService;

    @Autowired
    @Qualifier("ClusterAdminRPCService")
    ClusterAdminService clusterAdminRPCService;

    @Autowired
    @Qualifier("ClusterSingletonServiceProvider")
    ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    @Autowired
    @Qualifier("ControllerRpcProviderRegistry")
    org.opendaylight.controller.sal.binding.api.RpcProviderRegistry controllerRpcProviderRegistry;

    @Autowired
    @Qualifier("RpcProviderRegistry")
    RpcProviderService rpcProviderRegistry;

    @Autowired
    @Qualifier("ControllerBindingMountPointService")
    org.opendaylight.controller.md.sal.binding.api.MountPointService controllerBindingMountPointService;

    @Autowired
    @Qualifier("BindingMountPointService")
    MountPointService bindingMountPointService;

    @Autowired
    @Qualifier("ControllerBindingNotificationService")
    org.opendaylight.controller.md.sal.binding.api.NotificationService controllerBindingNotificationService;

    @Autowired
    @Qualifier("NotificationService")
    NotificationService notificationService;

    @Autowired
    @Qualifier("ControllerBindingNotificationPublishService")
    org.opendaylight.controller.md.sal.binding.api.NotificationPublishService controllerBindingNotificationPublishService;

    @Autowired
    @Qualifier("BindingNotificationPublishService")
    NotificationPublishService bindingNotificationPublishService;

    @Autowired
    @Qualifier("NotificationProviderService")
    org.opendaylight.controller.sal.binding.api.NotificationProviderService notificationProviderService;

    @Autowired
    @Qualifier("ControllerNotificationProviderService")
    org.opendaylight.controller.sal.binding.api.NotificationService controllerNotificationProviderService;

    @Autowired
    @Qualifier("ControllerBindingDataBroker")
    org.opendaylight.controller.md.sal.binding.api.DataBroker controllerBindingDataBroker;

    @Autowired
    @Qualifier("BindingDataBroker")
    DataBroker bindingDataBroker;

    @Autowired
    @Qualifier("ControllerBindingPingPongDataBroker")
    org.opendaylight.controller.md.sal.binding.api.DataBroker controllerBindingPingPongDataBroker;

    @Autowired
    @Qualifier("EventExecutor")
    EventExecutor eventExecutor;

    @Autowired
    @Qualifier("BossGroup")
    EventLoopGroup bossGroup;

    @Autowired
    @Qualifier("WorkerGroup")
    EventLoopGroup workerGroup;

    @Autowired
    @Qualifier("ThreadPool")
    ThreadPool threadPool;

    @Autowired
    @Qualifier("ScheduledThreadPool")
    ScheduledThreadPool scheduledThreadPool;

    @Autowired
    @Qualifier("Timer")
    Timer timer;

    @Test
    void testLightyBeansExists() {
        Assert.assertNotNull(lightyController);
        Assert.assertNotNull(lightyModuleRegistryService);
        Assert.assertNotNull(diagStatusService);
        Assert.assertNotNull(actorSystemProvider);
        Assert.assertNotNull(schemaContextProvider);
        Assert.assertNotNull(dOMSchemaService);
        Assert.assertNotNull(dOMYangTextSourceProvider);
        Assert.assertNotNull(controllerDOMMountPointService);
        Assert.assertNotNull(dOMMountPointService);
        Assert.assertNotNull(controllerDOMNotificationPublishService);
        Assert.assertNotNull(dOMNotificationPublishService);
        Assert.assertNotNull(controllerDOMNotificationService);
        Assert.assertNotNull(dOMNotificationService);
        Assert.assertNotNull(controllerDOMNotificationSubscriptionListenerRegistry);
        Assert.assertNotNull(dOMNotificationSubscriptionListenerRegistry);
        Assert.assertNotNull(configDatastore);
        Assert.assertNotNull(operationalDatastore);
        Assert.assertNotNull(controllerClusteredDOMDataBroker);
        Assert.assertNotNull(clusteredDOMDataBroker);
        Assert.assertNotNull(controllerPingPongDataBroker);
        Assert.assertNotNull(dOMDataTreeShardingService);
        Assert.assertNotNull(dOMDataTreeService);
        Assert.assertNotNull(distributedShardFactory);
        Assert.assertNotNull(controllerDOMRpcService);
        Assert.assertNotNull(dOMRpcService);
        Assert.assertNotNull(controllerDOMRpcProviderService);
        Assert.assertNotNull(dOMRpcProviderService);
        Assert.assertNotNull(bindingNormalizedNodeSerializer);
        Assert.assertNotNull(bindingCodecTreeFactory);
        Assert.assertNotNull(dOMEntityOwnershipService);
        Assert.assertNotNull(entityOwnershipService);
        Assert.assertNotNull(clusterAdminRPCService);
        Assert.assertNotNull(clusterSingletonServiceProvider);
        Assert.assertNotNull(controllerRpcProviderRegistry);
        Assert.assertNotNull(rpcProviderRegistry);
        Assert.assertNotNull(controllerBindingMountPointService);
        Assert.assertNotNull(bindingMountPointService);
        Assert.assertNotNull(controllerBindingNotificationService);
        Assert.assertNotNull(notificationService);
        Assert.assertNotNull(controllerBindingNotificationPublishService);
        Assert.assertNotNull(bindingNotificationPublishService);
        Assert.assertNotNull(notificationProviderService);
        Assert.assertNotNull(controllerNotificationProviderService);
        Assert.assertNotNull(controllerBindingDataBroker);
        Assert.assertNotNull(bindingDataBroker);
        Assert.assertNotNull(controllerBindingPingPongDataBroker);
        Assert.assertNotNull(eventExecutor);
        Assert.assertNotNull(bossGroup);
        Assert.assertNotNull(workerGroup);
        Assert.assertNotNull(threadPool);
        Assert.assertNotNull(scheduledThreadPool);
        Assert.assertNotNull(timer);
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
