/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
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
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Base lighty.io Configuration class for spring DI.
 * This configuration needs to implement abstract method {@link #initLightyController()} which returns initialized
 * {@link LightyController} and {@link #shutdownLightyController(LightyController)} which should handle proper
 * {@link LightyController} shutdown process. This configuration initializes all core lighty.io services as spring
 * beans.
 *
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
 *     public void shutdownLightyController(&#64;Nonnull LightyController lightyController)
 *             throws LightyLaunchException {
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
     * @throws InterruptedException if interrupted while initializing
     */
    protected abstract LightyController initLightyController() throws LightyLaunchException, InterruptedException;

    /**
     * Method responsible for proper LightyController shutdown process. Possibly application can handle custom shutdown
     * process of LightyController.
     *
     * @param lightyController lightyController previously initialized which needs to be shutdown
     * @throws LightyLaunchException if any problem occurred during shutdown
     */
    protected abstract void shutdownLightyController(LightyController lightyController) throws LightyLaunchException;

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

    @Bean(destroyMethod = "")
    @Primary
    public LightyController lightyController() {
        return lightyController;
    }

    @Bean(destroyMethod = "")
    public LightyModuleRegistryService lightyModuleRegistryService() {
        return this.lightyController.getServices();
    }

    @Bean(destroyMethod = "")
    public DiagStatusService diagStatusService() {
        return this.lightyController.getServices().getDiagStatusService();
    }

    @Bean(destroyMethod = "")
    public ActorSystemProvider actorSystemProvider() {
        return this.lightyController.getServices().getActorSystemProvider();
    }

    @Bean(destroyMethod = "")
    public EffectiveModelContext effectiveModelContext() {
        return this.lightyController.getServices().getEffectiveModelContext();
    }

    @Bean(destroyMethod = "")
    @Primary
    public DOMSchemaService domSchemaService() {
        return this.lightyController.getServices().getDOMSchemaService();
    }

    @Bean(destroyMethod = "")
    public DOMSchemaService.YangTextSourceExtension domYangTextSourceProvider() {
        return this.lightyController.getServices().getYangTextSourceExtension();
    }

    @Bean(destroyMethod = "")
    public DOMMountPointService domMountPointService() {
        return this.lightyController.getServices().getDOMMountPointService();
    }

    @Bean(destroyMethod = "")
    @Primary
    public DOMNotificationPublishService domNotificationPublishService() {
        return this.lightyController.getServices().getDOMNotificationPublishService();
    }

    @Bean(destroyMethod = "")
    public DOMNotificationService domNotificationService() {
        return this.lightyController.getServices().getDOMNotificationService();
    }

    @Bean(destroyMethod = "")
    public DOMNotificationRouter domNotificationRouter() {
        return this.lightyController.getServices().getDOMNotificationRouter();
    }

    @Bean(name = "ConfigDatastore", destroyMethod = "")
    public DistributedDataStoreInterface configDatastore() {
        return this.lightyController.getServices().getConfigDatastore();
    }

    @Bean(name = "OperationalDatastore", destroyMethod = "")
    public DistributedDataStoreInterface operationalDatastore() {
        return this.lightyController.getServices().getOperationalDatastore();
    }

    @Bean(destroyMethod = "")
    public DOMDataBroker clusteredDOMDataBroker() {
        return this.lightyController.getServices().getClusteredDOMDataBroker();
    }

    @Bean(destroyMethod = "")
    public DOMRpcService domRpcService() {
        return this.lightyController.getServices().getDOMRpcService();
    }

    @Bean(destroyMethod = "")
    public DOMRpcProviderService domRpcProviderService() {
        return this.lightyController.getServices().getDOMRpcProviderService();
    }

    @Bean(destroyMethod = "")
    @Primary
    public BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer() {
        return this.lightyController.getServices().getBindingNormalizedNodeSerializer();
    }

    @Bean(destroyMethod = "")
    public BindingCodecTreeFactory bindingCodecTreeFactory() {
        return this.lightyController.getServices().getBindingCodecTreeFactory();
    }

    @Bean(destroyMethod = "")
    public DOMEntityOwnershipService domEntityOwnershipService() {
        return this.lightyController.getServices().getDOMEntityOwnershipService();
    }

    @Bean(destroyMethod = "")
    public EntityOwnershipService entityOwnershipService() {
        return this.lightyController.getServices().getEntityOwnershipService();
    }

    @Bean(destroyMethod = "")
    public ClusterAdminRpcService clusterAdminRPCService() {
        return this.lightyController.getServices().getClusterAdminRPCService();
    }

    @Bean(destroyMethod = "")
    public ClusterSingletonServiceProvider clusterSingletonServiceProvider() {
        return this.lightyController.getServices().getClusterSingletonServiceProvider();
    }

    @Bean(destroyMethod = "")
    public RpcProviderService rpcProviderRegistry() {
        return this.lightyController.getServices().getRpcProviderService();
    }

    @Bean(destroyMethod = "")
    public MountPointService bindingMountPointService() {
        return this.lightyController.getServices().getBindingMountPointService();
    }

    @Bean(destroyMethod = "")
    public NotificationService notificationService() {
        return this.lightyController.getServices().getNotificationService();
    }

    @Bean(destroyMethod = "")
    public NotificationPublishService bindingNotificationPublishService() {
        return this.lightyController.getServices().getBindingNotificationPublishService();
    }

    @Bean(name = "BindingDataBroker", destroyMethod = "")
    public DataBroker getBindingDataBroker() {
        return this.lightyController.getServices().getBindingDataBroker();
    }

    @Bean(destroyMethod = "")
    public EventExecutor eventExecutor() {
        return this.lightyController.getServices().getEventExecutor();
    }

    @Bean(name = "BossGroup", destroyMethod = "")
    public EventLoopGroup bossGroup() {
        return this.lightyController.getServices().getBossGroup();
    }

    @Bean(name = "WorkerGroup", destroyMethod = "")
    public EventLoopGroup workerGroup() {
        return this.lightyController.getServices().getWorkerGroup();
    }

    @Bean(destroyMethod = "")
    @Primary
    public ThreadPool threadPool() {
        return this.lightyController.getServices().getThreadPool();
    }

    @Bean(destroyMethod = "")
    public ScheduledThreadPool scheduledThreadPool() {
        return this.lightyController.getServices().getScheduledThreadPool();
    }

    @Bean(destroyMethod = "")
    public Timer timer() {
        return this.lightyController.getServices().getTimer();
    }

}
