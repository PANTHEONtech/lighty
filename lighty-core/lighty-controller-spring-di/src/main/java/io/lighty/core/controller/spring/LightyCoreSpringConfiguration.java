/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.spring;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModuleRegistryService;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base lighty.io Configuration class for spring DI.
 * <p>
 * This configuration needs to implement abstract method {@link #initLightyController()} which returns initialized
 * {@link LightyController} and {@link #shutdownLightyController(LightyController)} which should handle proper
 * {@link LightyController} shutdown process.</p>
 * <p>This configuration initializes all core lighty.io services as spring
 * beans.</p>
 * <p>
 * Example:
 * <pre>
 * &#64;Configuration
 * public class LightyConfiguration extends LightyCoreSprigConfiguration {
 *     &#64;Override
 *     public LightyController initLightyController() throws ConfigurationException {
 *
 *         LightyController lightyController = ...
 *
 *         return lightyController;
 *     }
 *
 *     &#64;Override
 *     public void shutdownLightyController(&#64;Nonnull LightyController lightyController) throws LightyLaunchException {
 *         ...
 *         lightyController.shutdown();
 *         ...
 *     }
 * }
 * </pre>
 *
 * @author juraj.veverka
 */
@Configuration
public abstract class LightyCoreSpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyCoreSpringConfiguration.class);

    private LightyController lightyController;

    /**
     * Initializes {@link LightyController} instance, which will be used in Spring to initializes all lighty.io services
     * as beans.
     *
     * @return initialized lightyController
     * @throws LightyLaunchException if any problem occurred during initialization
     */
    protected abstract LightyController initLightyController() throws LightyLaunchException, InterruptedException;

    /**
     * Method responsible for proper LightyController shutdown process. Possibly application can handle custom shutdown
     * process of LightyController.
     *
     * @param lightyController lightyController previously initialized which needs to be shutdown
     * @throws LightyLaunchException if any problem occurred during shutdown
     */
    protected abstract void shutdownLightyController(LightyController lightyController)
            throws LightyLaunchException;

    @PostConstruct
    public void init() throws LightyLaunchException, InterruptedException {
        lightyController = this.initLightyController();
        LOG.debug("LightyCoreSpringConfiguration initialized {}", lightyController);
    }

    @PreDestroy
    public void preDestroy() throws LightyLaunchException {
        if (this.lightyController != null) {
            this.shutdownLightyController(this.lightyController);
        }
        LOG.info("LightyCoreSpringConfiguration destroy");
    }

    @Bean(name = "LightyController", destroyMethod = "")
    public LightyController getLightyController() {
        return lightyController;
    }

    @Bean(name = "LightyModuleRegistryService", destroyMethod = "")
    public LightyModuleRegistryService lightyModuleRegistryService() {
        return this.lightyController.getServices();
    }

    @Bean(name = "DiagStatusService", destroyMethod = "")
    public DiagStatusService getDiagStatusService() {
        return this.lightyController.getServices().getDiagStatusService();
    }

    @Bean(name = "ActorSystemProvider", destroyMethod = "")
    public ActorSystemProvider getActorSystemProvider() {
        return this.lightyController.getServices().getActorSystemProvider();
    }

    @Bean(name = "SchemaContextProvider", destroyMethod = "")
    public SchemaContextProvider getSchemaContextProvider() {
        return this.lightyController.getServices().getSchemaContextProvider();
    }

    @Bean(name = "DOMSchemaService", destroyMethod = "")
    public DOMSchemaService getDOMSchemaService() {
        return this.lightyController.getServices().getDOMSchemaService();
    }

    @Bean(name = "DOMYangTextSourceProvider", destroyMethod = "")
    public DOMYangTextSourceProvider getDOMYangTextSourceProvider() {
        return this.lightyController.getServices().getDOMYangTextSourceProvider();
    }

    @Bean(name = "ControllerDOMMountPointService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMMountPointService getControllerDOMMountPointService() {
        return this.lightyController.getServices().getControllerDOMMountPointService();
    }

    @Bean(name = "DOMMountPointService", destroyMethod = "")
    public DOMMountPointService getDOMMountPointService() {
        return this.lightyController.getServices().getDOMMountPointService();
    }

    @Bean(name = "ControllerDOMNotificationPublishService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService getControllerDOMNotificationPublishService() {
        return this.lightyController.getServices().getControllerDOMNotificationPublishService();
    }

    @Bean(name = "DOMNotificationPublishService", destroyMethod = "")
    public DOMNotificationPublishService getDOMNotificationPublishService() {
        return this.lightyController.getServices().getDOMNotificationPublishService();
    }

    @Bean(name = "ControllerDOMNotificationService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMNotificationService getControllerDOMNotificationService() {
        return this.lightyController.getServices().getControllerDOMNotificationService();
    }

    @Bean(name = "DOMNotificationService", destroyMethod = "")
    public DOMNotificationService getDOMNotificationService() {
        return this.lightyController.getServices().getDOMNotificationService();
    }

    @Bean(name = "ControllerDOMNotificationSubscriptionListenerRegistry", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry getControllerDOMNotificationSubscriptionListenerRegistry() {
        return this.lightyController.getServices().getControllerDOMNotificationSubscriptionListenerRegistry();
    }

    @Bean(name = "DOMNotificationSubscriptionListenerRegistry", destroyMethod = "")
    public DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry() {
        return this.lightyController.getServices().getDOMNotificationSubscriptionListenerRegistry();
    }

    @Bean(name = "ConfigDatastore", destroyMethod = "")
    public DistributedDataStoreInterface getConfigDatastore() {
        return this.lightyController.getServices().getConfigDatastore();
    }

    @Bean(name = "OperationalDatastore", destroyMethod = "")
    public DistributedDataStoreInterface getOperationalDatastore() {
        return this.lightyController.getServices().getOperationalDatastore();
    }

    @Bean(name = "ControllerClusteredDOMDataBroker", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getControllerClusteredDOMDataBroker() {
        return this.lightyController.getServices().getControllerClusteredDOMDataBroker();
    }

    @Bean(name = "ClusteredDOMDataBroker", destroyMethod = "")
    public DOMDataBroker getClusteredDOMDataBroker() {
        return this.lightyController.getServices().getClusteredDOMDataBroker();
    }

    @Bean(name = "ControllerPingPongDataBroker", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getControllerPingPongDataBroker() {
        return this.lightyController.getServices().getControllerPingPongDataBroker();
    }

    @Bean(name = "DOMDataTreeShardingService", destroyMethod = "")
    public DOMDataTreeShardingService getDOMDataTreeShardingService() {
        return this.lightyController.getServices().getDOMDataTreeShardingService();
    }

    @Bean(name = "DOMDataTreeService", destroyMethod = "")
    public DOMDataTreeService getDOMDataTreeService() {
        return this.lightyController.getServices().getDOMDataTreeService();
    }

    @Bean(name = "DistributedShardFactory", destroyMethod = "")
    public DistributedShardFactory getDistributedShardFactory() {
        return this.lightyController.getServices().getDistributedShardFactory();
    }

    @Bean(name = "ControllerDOMRpcService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMRpcService getControllerDOMRpcService() {
        return this.lightyController.getServices().getControllerDOMRpcService();
    }

    @Bean(name = "DOMRpcService", destroyMethod = "")
    public DOMRpcService getDOMRpcService() {
        return this.lightyController.getServices().getDOMRpcService();
    }

    @Bean(name = "ControllerDOMRpcProviderService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService getControllerDOMRpcProviderService() {
        return this.lightyController.getServices().getControllerDOMRpcProviderService();
    }

    @Bean(name = "DOMRpcProviderService", destroyMethod = "")
    public DOMRpcProviderService getDOMRpcProviderService() {
        return this.lightyController.getServices().getDOMRpcProviderService();
    }

    @Bean(name = "BindingNormalizedNodeSerializer", destroyMethod = "")
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return this.lightyController.getServices().getBindingNormalizedNodeSerializer();
    }

    @Bean(name = "BindingCodecTreeFactory", destroyMethod = "")
    public BindingCodecTreeFactory getBindingCodecTreeFactory() {
        return this.lightyController.getServices().getBindingCodecTreeFactory();
    }

    @Bean(name = "DOMEntityOwnershipService", destroyMethod = "")
    public DOMEntityOwnershipService getDOMEntityOwnershipService() {
        return this.lightyController.getServices().getDOMEntityOwnershipService();
    }

    @Bean(name = "EntityOwnershipService", destroyMethod = "")
    public EntityOwnershipService getEntityOwnershipService() {
        return this.lightyController.getServices().getEntityOwnershipService();
    }

    @Bean(name = "ClusterAdminRPCService", destroyMethod = "")
    public ClusterAdminService getClusterAdminRPCService() {
        return this.lightyController.getServices().getClusterAdminRPCService();
    }

    @Bean(name = "ClusterSingletonServiceProvider", destroyMethod = "")
    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return this.lightyController.getServices().getClusterSingletonServiceProvider();
    }

    @Bean(name = "ControllerRpcProviderRegistry", destroyMethod = "")
    public org.opendaylight.controller.sal.binding.api.RpcProviderRegistry getControllerRpcProviderRegistry() {
        return this.lightyController.getServices().getControllerRpcProviderRegistry();
    }

    @Bean(name = "RpcProviderRegistry", destroyMethod = "")
    public RpcProviderService getRpcProviderRegistry() {
        return this.lightyController.getServices().getRpcProviderService();
    }

    @Bean(name = "ControllerBindingMountPointService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.binding.api.MountPointService getControllerBindingMountPointService() {
        return this.lightyController.getServices().getControllerBindingMountPointService();
    }

    @Bean(name = "BindingMountPointService", destroyMethod = "")
    public MountPointService getBindingMountPointService() {
        return this.lightyController.getServices().getBindingMountPointService();
    }

    @Bean(name = "ControllerBindingNotificationService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.binding.api.NotificationService getControllerBindingNotificationService() {
        return this.lightyController.getServices().getControllerBindingNotificationService();
    }

    @Bean(name = "NotificationService", destroyMethod = "")
    public NotificationService getNotificationService() {
        return this.lightyController.getServices().getNotificationService();
    }

    @Bean(name = "ControllerBindingNotificationPublishService", destroyMethod = "")
    public org.opendaylight.controller.md.sal.binding.api.NotificationPublishService getControllerBindingNotificationPublishService() {
        return this.lightyController.getServices().getControllerBindingNotificationPublishService();
    }

    @Bean(name = "BindingNotificationPublishService", destroyMethod = "")
    public NotificationPublishService getBindingNotificationPublishService() {
        return this.lightyController.getServices().getBindingNotificationPublishService();
    }

    @Bean(name = "NotificationProviderService", destroyMethod = "")
    public org.opendaylight.controller.sal.binding.api.NotificationProviderService getNotificationProviderService() {
        return this.lightyController.getServices().getControllerNotificationProviderService();
    }

    @Bean(name = "ControllerNotificationProviderService", destroyMethod = "")
    public org.opendaylight.controller.sal.binding.api.NotificationService getControllerNotificationProviderService() {
        return this.lightyController.getServices().getControllerNotificationProviderService();
    }

    @Bean(name = "ControllerBindingDataBroker", destroyMethod = "")
    public org.opendaylight.controller.md.sal.binding.api.DataBroker getControllerBindingDataBroker() {
        return this.lightyController.getServices().getControllerBindingDataBroker();
    }

    @Bean(name = "BindingDataBroker", destroyMethod = "")
    public DataBroker getBindingDataBroker() {
        return this.lightyController.getServices().getBindingDataBroker();
    }

    @Bean(name = "ControllerBindingPingPongDataBroker", destroyMethod = "")
    public org.opendaylight.controller.md.sal.binding.api.DataBroker getControllerBindingPingPongDataBroker() {
        return this.lightyController.getServices().getControllerBindingPingPongDataBroker();
    }

    @Bean(name = "EventExecutor", destroyMethod = "")
    public EventExecutor getEventExecutor() {
        return this.lightyController.getServices().getEventExecutor();
    }

    @Bean(name = "BossGroup", destroyMethod = "")
    public EventLoopGroup getBossGroup() {
        return this.lightyController.getServices().getBossGroup();
    }

    @Bean(name = "WorkerGroup", destroyMethod = "")
    public EventLoopGroup getWorkerGroup() {
        return this.lightyController.getServices().getWorkerGroup();
    }

    @Bean(name = "ThreadPool", destroyMethod = "")
    public ThreadPool getThreadPool() {
        return this.lightyController.getServices().getThreadPool();
    }

    @Bean(name = "ScheduledThreadPool", destroyMethod = "")
    public ScheduledThreadPool getScheduledThreadPool() {
        return this.lightyController.getServices().getScheduledThreaPool();
    }

    @Bean(name = "Timer", destroyMethod = "")
    public Timer getTimer() {
        return this.lightyController.getServices().getTimer();
    }

}
