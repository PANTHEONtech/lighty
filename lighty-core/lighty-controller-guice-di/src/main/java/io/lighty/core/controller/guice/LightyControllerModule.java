/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
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
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
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
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module provides google guice bindings for Lighty Core.
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
        bind(DOMSchemaService.class)
                .toInstance(lightyServices.getDOMSchemaService());
        bind(DOMNotificationRouter.class)
                .toInstance(lightyServices.getDOMNotificationRouter());
        bind(DistributedDataStoreInterface.class)
                .annotatedWith(Names.named("ControllerConfigDatastore"))
                .toInstance(lightyServices.getConfigDatastore());
        bind(DistributedDataStoreInterface.class)
                .annotatedWith(Names.named("ControllerOperationalDatastore"))
                .toInstance(lightyServices.getOperationalDatastore());
        bind(BindingNormalizedNodeSerializer.class)
                .toInstance(lightyServices.getBindingNormalizedNodeSerializer());
        bind(BindingCodecTreeFactory.class)
                .toInstance(lightyServices.getBindingCodecTreeFactory());
        bind(DOMEntityOwnershipService.class)
                .toInstance(lightyServices.getDOMEntityOwnershipService());
        bind(EntityOwnershipService.class)
                .toInstance(lightyServices.getEntityOwnershipService());
        bind(ClusterAdminRpcService.class)
                .toInstance(lightyServices.getClusterAdminRPCService());
        bind(ClusterSingletonServiceProvider.class)
                .toInstance(lightyServices.getClusterSingletonServiceProvider());
        bind(EventLoopGroup.class)
                .annotatedWith(Names.named("BossGroup"))
                .toInstance(lightyServices.getBossGroup());
        bind(EventLoopGroup.class)
                .annotatedWith(Names.named("WorkerGroup"))
                .toInstance(lightyServices.getWorkerGroup());
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
        bind(LightyServices.class)
                .toInstance(lightyServices);
        bind(LightyModuleRegistryService.class)
                .toInstance(lightyServices);
        LOG.info("Lighty bindings initialized.");
    }

}
