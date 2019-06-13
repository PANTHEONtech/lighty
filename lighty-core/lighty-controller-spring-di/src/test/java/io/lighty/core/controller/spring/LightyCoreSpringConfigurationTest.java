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
    DiagStatusService diagStatusService;

    @Autowired
    ActorSystemProvider actorSystemProvider;

    @Autowired
    SchemaContextProvider schemaContextProvider;

    @Autowired
    DOMSchemaService domSchemaService;

    @Autowired
    DOMYangTextSourceProvider domYangTextSourceProvider;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMMountPointService controllerDOMMountPointService;

    @Autowired
    DOMMountPointService domMountPointService;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService controllerDOMNotificationPublishService;

    @Autowired
    DOMNotificationPublishService domNotificationPublishService;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMNotificationService controllerDOMNotificationService;

    @Autowired
    DOMNotificationService domNotificationService;

    @Autowired
    org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry controllerDOMNotificationSubscriptionListenerRegistry;

    @Autowired
    DOMNotificationSubscriptionListenerRegistry domNotificationSubscriptionListenerRegistry;

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
    DOMDataBroker clusteredDOMDataBroker;

    @Autowired
    @Qualifier("ControllerPingPongDataBroker")
    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker controllerPingPongDataBroker;

    @Autowired
    DOMDataTreeShardingService domDataTreeShardingService;

    @Autowired
    DOMDataTreeService domDataTreeService;

    @Autowired
    DistributedShardFactory distributedShardFactory;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMRpcService controllerDOMRpcService;

    @Autowired
    DOMRpcService domRpcService;

    @Autowired
    org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService controllerDOMRpcProviderService;

    @Autowired
    DOMRpcProviderService domRpcProviderService;

    @Autowired
    BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    @Autowired
    BindingCodecTreeFactory bindingCodecTreeFactory;

    @Autowired
    DOMEntityOwnershipService domEntityOwnershipService;

    @Autowired
    EntityOwnershipService entityOwnershipService;

    @Autowired
    ClusterAdminService clusterAdminRPCService;

    @Autowired
    ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    @Autowired
    org.opendaylight.controller.sal.binding.api.RpcProviderRegistry controllerRpcProviderRegistry;

    @Autowired
    RpcProviderService rpcProviderRegistry;

    @Autowired
    org.opendaylight.controller.md.sal.binding.api.MountPointService controllerBindingMountPointService;

    @Autowired
    MountPointService bindingMountPointService;

    @Autowired
    org.opendaylight.controller.md.sal.binding.api.NotificationService controllerBindingNotificationService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    org.opendaylight.controller.md.sal.binding.api.NotificationPublishService controllerBindingNotificationPublishService;

    @Autowired
    NotificationPublishService bindingNotificationPublishService;

    @Autowired
    org.opendaylight.controller.sal.binding.api.NotificationProviderService notificationProviderService;

    @Autowired
    @Qualifier("ControllerNotificationProviderService")
    org.opendaylight.controller.sal.binding.api.NotificationService controllerNotificationProviderService;

    @Autowired
    @Qualifier("ControllerBindingDataBroker")
    org.opendaylight.controller.md.sal.binding.api.DataBroker controllerBindingDataBroker;

    @Autowired
    DataBroker bindingDataBroker;

    @Autowired
    @Qualifier("ControllerBindingPingPongDataBroker")
    org.opendaylight.controller.md.sal.binding.api.DataBroker controllerBindingPingPongDataBroker;

    @Autowired
    EventExecutor eventExecutor;

    @Autowired
    @Qualifier("BossGroup")
    EventLoopGroup bossGroup;

    @Autowired
    @Qualifier("WorkerGroup")
    EventLoopGroup workerGroup;

    @Autowired
    ThreadPool threadPool;

    @Autowired
    ScheduledThreadPool scheduledThreadPool;

    @Autowired
    Timer timer;

    @Test
    void testLightyBeansExists() {
        Assert.assertNotNull(lightyController);
        Assert.assertNotNull(lightyModuleRegistryService);
        Assert.assertNotNull(diagStatusService);
        Assert.assertNotNull(actorSystemProvider);
        Assert.assertNotNull(schemaContextProvider);
        Assert.assertNotNull(domSchemaService);
        Assert.assertNotNull(domYangTextSourceProvider);
        Assert.assertNotNull(controllerDOMMountPointService);
        Assert.assertNotNull(domMountPointService);
        Assert.assertNotNull(controllerDOMNotificationPublishService);
        Assert.assertNotNull(domNotificationPublishService);
        Assert.assertNotNull(controllerDOMNotificationService);
        Assert.assertNotNull(domNotificationService);
        Assert.assertNotNull(controllerDOMNotificationSubscriptionListenerRegistry);
        Assert.assertNotNull(domNotificationSubscriptionListenerRegistry);
        Assert.assertNotNull(configDatastore);
        Assert.assertNotNull(operationalDatastore);
        Assert.assertNotNull(controllerClusteredDOMDataBroker);
        Assert.assertNotNull(clusteredDOMDataBroker);
        Assert.assertNotNull(controllerPingPongDataBroker);
        Assert.assertNotNull(domDataTreeShardingService);
        Assert.assertNotNull(domDataTreeService);
        Assert.assertNotNull(distributedShardFactory);
        Assert.assertNotNull(controllerDOMRpcService);
        Assert.assertNotNull(domRpcService);
        Assert.assertNotNull(controllerDOMRpcProviderService);
        Assert.assertNotNull(domRpcProviderService);
        Assert.assertNotNull(bindingNormalizedNodeSerializer);
        Assert.assertNotNull(bindingCodecTreeFactory);
        Assert.assertNotNull(domEntityOwnershipService);
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
