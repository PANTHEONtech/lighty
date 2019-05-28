/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.lighty.core.controller.api.LightyModuleRegistryService;
import io.lighty.core.controller.api.LightyServices;
import io.netty.channel.EventLoopGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
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

/**
 * This module provides google guice bindings for Lighty Core
 *
 * @author juraj.veverka
 */
public class LightyControllerModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerModule.class);

    private LightyServices lightyServices;

    public LightyControllerModule(LightyServices lightyServices) {
        this.lightyServices = lightyServices;
    }

    @Override
    protected void configure() {
        LOG.info("initializing Lighty bindings ...");
        bind(DiagStatusService.class)
                .toInstance(lightyServices.getDiagStatusService());
        bind(ActorSystemProvider.class)
                .toInstance(lightyServices.getActorSystemProvider());
        bind(SchemaContextProvider.class)
                .toInstance(lightyServices.getSchemaContextProvider());
        bind(DOMSchemaService.class)
                .toInstance(lightyServices.getDOMSchemaService());
        bind(DOMYangTextSourceProvider.class)
                .toInstance(lightyServices.getDOMYangTextSourceProvider());
        bind(DOMNotificationSubscriptionListenerRegistry.class)
                .toInstance(lightyServices.getDOMNotificationSubscriptionListenerRegistry());
        bind(DistributedDataStoreInterface.class)
                .annotatedWith(Names.named("ControllerConfigDatastore"))
                .toInstance(lightyServices.getConfigDatastore());
        bind(DistributedDataStoreInterface.class)
                .annotatedWith(Names.named("ControllerOperationalDatastore"))
                .toInstance(lightyServices.getOperationalDatastore());
        bind(DOMDataTreeShardingService.class)
                .toInstance(lightyServices.getDOMDataTreeShardingService());
        bind(DOMDataTreeService.class)
                .toInstance(lightyServices.getDOMDataTreeService());
        bind(DistributedShardFactory.class)
                .toInstance(lightyServices.getDistributedShardFactory());
        bind(BindingNormalizedNodeSerializer.class)
                .toInstance(lightyServices.getBindingNormalizedNodeSerializer());
        bind(BindingCodecTreeFactory.class)
                .toInstance(lightyServices.getBindingCodecTreeFactory());
        bind(DOMEntityOwnershipService.class)
                .toInstance(lightyServices.getDOMEntityOwnershipService());
        bind(EntityOwnershipService.class)
                .toInstance(lightyServices.getEntityOwnershipService());
        bind(ClusterAdminService.class)
                .toInstance(lightyServices.getClusterAdminRPCService());
        bind(ClusterSingletonServiceProvider.class)
                .toInstance(lightyServices.getClusterSingletonServiceProvider());
        bind(EventExecutor.class)
                .toInstance(lightyServices.getEventExecutor());
        bind(EventLoopGroup.class)
                .annotatedWith(Names.named("BossGroup"))
                .toInstance(lightyServices.getBossGroup());
        bind(EventLoopGroup.class)
                .annotatedWith(Names.named("WorkerGroup"))
                .toInstance(lightyServices.getWorkerGroup());
        bind(ThreadPool.class)
                .toInstance(lightyServices.getThreadPool());
        bind(ScheduledThreadPool.class)
                .toInstance(lightyServices.getScheduledThreaPool());
        bind(Timer.class)
                .toInstance(lightyServices.getTimer());
        bind(DOMMountPointService.class)
                .toInstance(lightyServices.getDOMMountPointService());
        bind(DOMNotificationPublishService.class)
                .toInstance(lightyServices.getDOMNotificationPublishService());
        bind(DOMNotificationService.class)
                .toInstance(lightyServices.getDOMNotificationService());
        bind(DOMDataBroker.class)
                .annotatedWith(Names.named("ClusteredDOMDataBroker"))
                .toInstance(lightyServices.getClusteredDOMDataBroker());
        bind(DOMRpcService.class)
                .toInstance(lightyServices.getDOMRpcService());
        bind(DOMRpcProviderService.class)
                .toInstance(lightyServices.getDOMRpcProviderService());
        bind(RpcProviderService.class)
                .toInstance(lightyServices.getRpcProviderService());
        bind(MountPointService.class)
                .toInstance(lightyServices.getBindingMountPointService());
        bind(NotificationService.class)
                .toInstance(lightyServices.getNotificationService());
        bind(NotificationPublishService.class)
                .toInstance(lightyServices.getBindingNotificationPublishService());
        bind(DataBroker.class)
                .annotatedWith(Names.named("BindingDataBroker"))
                .toInstance(lightyServices.getBindingDataBroker());
        // Deprecated services
        bind(NotificationProviderService.class)
                .toInstance(lightyServices.getControllerNotificationProviderService());
        bind(org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry.class)
                .toInstance(lightyServices.getControllerDOMNotificationSubscriptionListenerRegistry());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMMountPointService.class)
                .toInstance(lightyServices.getControllerDOMMountPointService());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService.class)
                .toInstance(lightyServices.getControllerDOMNotificationPublishService());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMNotificationService.class)
                .toInstance(lightyServices.getControllerDOMNotificationService());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMDataBroker.class)
                .annotatedWith(Names.named("ControllerClusteredDOMDataBroker"))
                .toInstance(lightyServices.getControllerClusteredDOMDataBroker());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMDataBroker.class)
                .annotatedWith(Names.named("ControllerPingPongDOMDataBroker"))
                .toInstance(lightyServices.getControllerPingPongDataBroker());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMRpcService.class)
                .toInstance(lightyServices.getControllerDOMRpcService());
        bind(org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService.class)
                .toInstance(lightyServices.getControllerDOMRpcProviderService());
        bind(RpcProviderRegistry.class)
                .toInstance(lightyServices.getControllerRpcProviderRegistry());
        bind(org.opendaylight.controller.md.sal.binding.api.MountPointService.class)
                .toInstance(lightyServices.getControllerBindingMountPointService());
        bind(org.opendaylight.controller.sal.binding.api.NotificationService.class)
                .toInstance(lightyServices.getControllerNotificationProviderService());
        bind(org.opendaylight.controller.md.sal.binding.api.DataBroker.class)
                .annotatedWith(Names.named("ControllerBindingDataBroker"))
                .toInstance(lightyServices.getControllerBindingDataBroker());
        bind(org.opendaylight.controller.md.sal.binding.api.DataBroker.class)
                .annotatedWith(Names.named("ControllerBindingPingPongDataBroker"))
                .toInstance(lightyServices.getControllerBindingPingPongDataBroker());
        bind(org.opendaylight.controller.md.sal.binding.api.NotificationPublishService.class)
                .toInstance(lightyServices.getControllerBindingNotificationPublishService());
        bind(LightyServices.class)
                .toInstance(lightyServices);
        bind(LightyModuleRegistryService.class)
                .toInstance(lightyServices);
        LOG.info("Lighty bindings initialized.");
    }

}
