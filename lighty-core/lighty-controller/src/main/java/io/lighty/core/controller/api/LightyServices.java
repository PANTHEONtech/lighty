/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.api;

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

/**
 * This interface provides methods to access ODL core services
 * provided by {@link LightyController} module.
 *
 * @author juraj.veverka
 */
public interface LightyServices {

    ActorSystemProvider getActorSystemProvider();

    SchemaContextProvider getSchemaContextProvider();

    DOMSchemaService getDOMSchemaService();

    DOMYangTextSourceProvider getDOMYangTextSourceProvider();

    DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry();

    DistributedDataStoreInterface getConfigDatastore();

    DistributedDataStoreInterface getOperationalDatastore();

    DOMDataTreeShardingService getDOMDataTreeShardingService();

    DOMDataTreeService getDOMDataTreeService();

    DistributedShardFactory getDistributedShardFactory();

    BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer();

    BindingCodecTreeFactory getBindingCodecTreeFactory();

    DOMEntityOwnershipService getDOMEntityOwnershipService();

    EntityOwnershipService getEntityOwnershipService();

    ClusterAdminService getClusterAdminRPCService();

    ClusterSingletonServiceProvider getClusterSingletonServiceProvider();

    EventExecutor getEventExecutor();

    EventLoopGroup getBossGroup();

    EventLoopGroup getWorkerGroup();

    ThreadPool getThreadPool();

    ScheduledThreadPool getScheduledThreaPool();

    Timer getTimer();

    DOMMountPointService getDOMMountPointService();

    DOMNotificationPublishService getDOMNotificationPublishService();

    DOMNotificationService getDOMNotificationService();

    DOMDataBroker getClusteredDOMDataBroker();

    DOMDataBroker getPingPongDataBroker();

    DOMRpcService getDOMRpcService();

    DOMRpcProviderService getDOMRpcProviderService();

    RpcProviderService getRpcProviderRegistry();

    MountPointService getBindingMountPointService();

    NotificationService getNotificationService();

    NotificationPublishService getBindingNotificationPublishService();

    DataBroker getBindingDataBroker();

    DataBroker getBindingPingPongDataBroker();

    NotificationProviderService getControllerNotificationProviderService();

    org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry
            getControllerDOMNotificationSubscriptionListenerRegistry();

    org.opendaylight.controller.md.sal.dom.api.DOMMountPointService getControllerDOMMountPointService();

    org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService
            getControllerDOMNotificationPublishService();

    org.opendaylight.controller.md.sal.dom.api.DOMNotificationService getControllerDOMNotificationService();

    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getControllerClusteredDOMDataBroker();

    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getControllerPingPongDataBroker();

    org.opendaylight.controller.md.sal.dom.api.DOMRpcService getControllerDOMRpcService();

    org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService getControllerDOMRpcProviderService();

    RpcProviderRegistry getControllerRpcProviderRegistry();

    org.opendaylight.controller.md.sal.binding.api.MountPointService getControllerBindingMountPointService();

    org.opendaylight.controller.md.sal.binding.api.NotificationService getControllerBindingNotificationService();

    org.opendaylight.controller.md.sal.binding.api.DataBroker getControllerBindingDataBroker();

    org.opendaylight.controller.md.sal.binding.api.DataBroker getControllerBindingPingPongDataBroker();

    org.opendaylight.controller.md.sal.binding.api.NotificationPublishService
            getControllerBindingNotificationPublishService();
}
