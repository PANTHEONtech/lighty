/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.spring;

import io.lighty.core.controller.api.LightyController;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base lighty.io Configuration class for spring DI.
 * <p>
 * This configuration needs LightyController bean initialized in spring environment. If LightyController bean exists in
 * spring environment, this configuration initializes all core lighty.io services as spring beans.</p>
 * <p>
 * Example:
 * <pre>
 * &#64;Configuration
 * public class LightyConfiguration extends LightyCoreSprigConfiguration {
 *     &#64;Bean
 *     LightyController initLightyController() throws Exception {
 *
 *         LightyController lightyController = ...
 *
 *         return lightyController;
 *     }
 * }
 * </pre>
 *
 * @author juraj.veverka
 */
@Configuration
public class LightyCoreSpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyCoreSpringConfiguration.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private LightyController lightyController;

    @Bean
    public ActorSystemProvider getActorSystemProvider() {
        return lightyController.getServices().getActorSystemProvider();
    }

    @Bean
    public SchemaContextProvider getSchemaContextProvider() {
        return lightyController.getServices().getSchemaContextProvider();
    }

    @Bean
    public DOMSchemaService getDOMSchemaService() {
        return lightyController.getServices().getDOMSchemaService();
    }

    @Bean
    public DOMYangTextSourceProvider getDOMYangTextSourceProvider() {
        return lightyController.getServices().getDOMYangTextSourceProvider();
    }

    @Bean
    public DOMMountPointService getDOMMountPointService() {
        return lightyController.getServices().getDOMMountPointService();
    }

    @Bean
    public DOMNotificationPublishService getDOMNotificationPublishService() {
        return lightyController.getServices().getDOMNotificationPublishService();
    }

    @Bean
    public DOMNotificationService getDOMNotificationService() {
        return lightyController.getServices().getDOMNotificationService();
    }

    @Bean
    public DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry() {
        return lightyController.getServices().getDOMNotificationSubscriptionListenerRegistry();
    }

    @Bean(name = "ConfigDatastore")
    public DistributedDataStoreInterface getConfigDatastore() {
        return lightyController.getServices().getConfigDatastore();
    }

    @Bean(name = "OperationalDatastore")
    public DistributedDataStoreInterface getOperationalDatastore() {
        return lightyController.getServices().getOperationalDatastore();
    }

    @Bean(name = "ClusteredDOMDataBroker")
    public DOMDataBroker getClusteredDOMDataBroker() {
        return lightyController.getServices().getClusteredDOMDataBroker();
    }

    @Bean(name = "PingPongDataBroker")
    public DOMDataBroker getPingPongDataBroker() {
        return lightyController.getServices().getPingPongDataBroker();
    }

    @Bean
    public DOMDataTreeShardingService getDOMDataTreeShardingService() {
        return lightyController.getServices().getDOMDataTreeShardingService();
    }

    @Bean
    public DOMDataTreeService getDOMDataTreeService() {
        return lightyController.getServices().getDOMDataTreeService();
    }

    @Bean
    public DistributedShardFactory getDistributedShardFactory() {
        return lightyController.getServices().getDistributedShardFactory();
    }

    @Bean
    public DOMRpcService getDOMRpcService() {
        return lightyController.getServices().getDOMRpcService();
    }

    @Bean
    public DOMRpcProviderService getDOMRpcProviderService() {
        return lightyController.getServices().getDOMRpcProviderService();
    }

    @Bean
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return lightyController.getServices().getBindingNormalizedNodeSerializer();
    }

    @Bean
    public BindingCodecTreeFactory getBindingCodecTreeFactory() {
        return lightyController.getServices().getBindingCodecTreeFactory();
    }

    @Bean
    public DOMEntityOwnershipService getDOMEntityOwnershipService() {
        return lightyController.getServices().getDOMEntityOwnershipService();
    }

    @Bean
    public EntityOwnershipService getEntityOwnershipService() {
        return lightyController.getServices().getEntityOwnershipService();
    }

    @Bean
    public ClusterAdminService getClusterAdminRPCService() {
        return lightyController.getServices().getClusterAdminRPCService();
    }

    @Bean
    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return lightyController.getServices().getClusterSingletonServiceProvider();
    }

    @Bean
    public RpcProviderRegistry getRpcProviderRegistry() {
        return lightyController.getServices().getRpcProviderRegistry();
    }

    @Bean
    public MountPointService getBindingMountPointService() {
        return lightyController.getServices().getBindingMountPointService();
    }

    @Bean
    public NotificationService getBindingNotificationService() {
        return lightyController.getServices().getBindingNotificationService();
    }

    @Bean
    public NotificationPublishService getBindingNotificationPublishService() {
        return lightyController.getServices().getBindingNotificationPublishService();
    }

    @Bean
    public NotificationProviderService getNotificationProviderService() {
        return lightyController.getServices().getNotificationProviderService();
    }

    @Bean
    public org.opendaylight.controller.sal.binding.api.NotificationService getNotificationService() {
        return lightyController.getServices().getNotificationService();
    }

    @Bean(name = "BindingDataBroker")
    public DataBroker getBindingDataBroker() {
        return lightyController.getServices().getBindingDataBroker();
    }

    @Bean(name = "BindingPingPongDataBroker")
    public DataBroker getBindingPingPongDataBroker() {
        return lightyController.getServices().getBindingPingPongDataBroker();
    }

    @Bean
    public EventExecutor getEventExecutor() {
        return lightyController.getServices().getEventExecutor();
    }

    @Bean(name = "BossGroup")
    public EventLoopGroup getBossGroup() {
        return lightyController.getServices().getBossGroup();
    }

    @Bean(name = "WorkerGroup")
    public EventLoopGroup getWorkerGroup() {
        return lightyController.getServices().getWorkerGroup();
    }

    @Bean
    public ThreadPool getThreadPool() {
        return lightyController.getServices().getThreadPool();
    }

    @Bean
    public ScheduledThreadPool getScheduledThreaPool() {
        return lightyController.getServices().getScheduledThreaPool();
    }

    @Bean
    public Timer getTimer() {
        return lightyController.getServices().getTimer();
    }

}
