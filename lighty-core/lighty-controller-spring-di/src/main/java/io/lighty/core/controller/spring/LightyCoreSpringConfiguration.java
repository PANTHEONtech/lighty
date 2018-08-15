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
        return this.lightyController.getServices().getActorSystemProvider();
    }

    @Bean
    public SchemaContextProvider getSchemaContextProvider() {
        return this.lightyController.getServices().getSchemaContextProvider();
    }

    @Bean
    public DOMSchemaService getDOMSchemaService() {
        return this.lightyController.getServices().getDOMSchemaService();
    }

    @Bean
    public DOMYangTextSourceProvider getDOMYangTextSourceProvider() {
        return this.lightyController.getServices().getDOMYangTextSourceProvider();
    }

    @Bean
    public DOMMountPointService getDOMMountPointService() {
        return this.lightyController.getServices().getDOMMountPointServiceOld();
    }

    @Bean
    public DOMNotificationPublishService getDOMNotificationPublishService() {
        return this.lightyController.getServices().getDOMNotificationPublishServiceOld();
    }

    @Bean
    public DOMNotificationService getDOMNotificationService() {
        return this.lightyController.getServices().getDOMNotificationServiceOld();
    }

    @Bean
    public DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry() {
        return this.lightyController.getServices().getDOMNotificationSubscriptionListenerRegistryOld();
    }

    @Bean(name = "ConfigDatastore")
    public DistributedDataStoreInterface getConfigDatastore() {
        return this.lightyController.getServices().getConfigDatastore();
    }

    @Bean(name = "OperationalDatastore")
    public DistributedDataStoreInterface getOperationalDatastore() {
        return this.lightyController.getServices().getOperationalDatastore();
    }

    @Bean(name = "ClusteredDOMDataBroker")
    public DOMDataBroker getClusteredDOMDataBroker() {
        return this.lightyController.getServices().getClusteredDOMDataBrokerOld();
    }

    @Bean(name = "PingPongDataBroker")
    public DOMDataBroker getPingPongDataBroker() {
        return this.lightyController.getServices().getPingPongDataBrokerOld();
    }

    @Bean
    public DOMDataTreeShardingService getDOMDataTreeShardingService() {
        return this.lightyController.getServices().getDOMDataTreeShardingService();
    }

    @Bean
    public DOMDataTreeService getDOMDataTreeService() {
        return this.lightyController.getServices().getDOMDataTreeService();
    }

    @Bean
    public DistributedShardFactory getDistributedShardFactory() {
        return this.lightyController.getServices().getDistributedShardFactory();
    }

    @Bean
    public DOMRpcService getDOMRpcService() {
        return this.lightyController.getServices().getDOMRpcServiceOld();
    }

    @Bean
    public DOMRpcProviderService getDOMRpcProviderService() {
        return this.lightyController.getServices().getDOMRpcProviderServiceOld();
    }

    @Bean
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return this.lightyController.getServices().getBindingNormalizedNodeSerializer();
    }

    @Bean
    public BindingCodecTreeFactory getBindingCodecTreeFactory() {
        return this.lightyController.getServices().getBindingCodecTreeFactory();
    }

    @Bean
    public DOMEntityOwnershipService getDOMEntityOwnershipService() {
        return this.lightyController.getServices().getDOMEntityOwnershipService();
    }

    @Bean
    public EntityOwnershipService getEntityOwnershipService() {
        return this.lightyController.getServices().getEntityOwnershipService();
    }

    @Bean
    public ClusterAdminService getClusterAdminRPCService() {
        return this.lightyController.getServices().getClusterAdminRPCService();
    }

    @Bean
    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return this.lightyController.getServices().getClusterSingletonServiceProvider();
    }

    @Bean
    public RpcProviderRegistry getRpcProviderRegistry() {
        return this.lightyController.getServices().getRpcProviderRegistryOld();
    }

    @Bean
    public MountPointService getBindingMountPointService() {
        return this.lightyController.getServices().getBindingMountPointServiceOld();
    }

    @Bean
    public NotificationService getBindingNotificationService() {
        return this.lightyController.getServices().getBindingNotificationServiceOld();
    }

    @Bean
    public NotificationPublishService getBindingNotificationPublishService() {
        return this.lightyController.getServices().getBindingNotificationPublishServiceOld();
    }

    @Bean
    public NotificationProviderService getNotificationProviderService() {
        return this.lightyController.getServices().getNotificationProviderService();
    }

    @Bean
    public org.opendaylight.controller.sal.binding.api.NotificationService getNotificationService() {
        return this.lightyController.getServices().getNotificationProviderService();
    }

    @Bean(name = "BindingDataBroker")
    public DataBroker getBindingDataBroker() {
        return this.lightyController.getServices().getBindingDataBrokerOld();
    }

    @Bean(name = "BindingPingPongDataBroker")
    public DataBroker getBindingPingPongDataBroker() {
        return this.lightyController.getServices().getBindingPingPongDataBrokerOld();
    }

    @Bean
    public EventExecutor getEventExecutor() {
        return this.lightyController.getServices().getEventExecutor();
    }

    @Bean(name = "BossGroup")
    public EventLoopGroup getBossGroup() {
        return this.lightyController.getServices().getBossGroup();
    }

    @Bean(name = "WorkerGroup")
    public EventLoopGroup getWorkerGroup() {
        return this.lightyController.getServices().getWorkerGroup();
    }

    @Bean
    public ThreadPool getThreadPool() {
        return this.lightyController.getServices().getThreadPool();
    }

    @Bean
    public ScheduledThreadPool getScheduledThreaPool() {
        return this.lightyController.getServices().getScheduledThreaPool();
    }

    @Bean
    public Timer getTimer() {
        return this.lightyController.getServices().getTimer();
    }

}
