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
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
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

    org.opendaylight.controller.md.sal.dom.api.DOMMountPointService getDOMMountPointService();

    DOMMountPointService getMdSalDOMMountPointService();

    org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService getDOMNotificationPublishService();

    DOMNotificationPublishService getMdSalDOMNotificationPublishService();

    org.opendaylight.controller.md.sal.dom.api.DOMNotificationService getDOMNotificationService();

    DOMNotificationService getMdSalDOMNotificationService();

    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getClusteredDOMDataBroker();

    DOMDataBroker getMdSalClusteredDOMDataBroker();

    org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getPingPongDataBroker();

    DOMDataBroker getMdSalPingPongDataBroker();

    org.opendaylight.controller.md.sal.dom.api.DOMRpcService getDOMRpcService();

    DOMRpcService getMdSalDOMRpcService();

    org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService getDOMRpcProviderService();

    DOMRpcProviderService getMdSalDOMRpcProviderService();

    org.opendaylight.controller.sal.binding.api.RpcProviderRegistry getRpcProviderRegistry();

    RpcProviderService getMdSalRpcProviderService();

    org.opendaylight.controller.md.sal.binding.api.MountPointService getBindingMountPointService();

    MountPointService getMdSalMountPointService();

    org.opendaylight.controller.md.sal.binding.api.NotificationService getBindingNotificationService();

    NotificationService getMdSalBindingNotificationService();

    org.opendaylight.controller.md.sal.binding.api.NotificationPublishService getBindingNotificationPublishService();

    NotificationPublishService getMdSalNotificationPublishService();

    org.opendaylight.controller.sal.binding.api.NotificationProviderService getNotificationProviderService();

    org.opendaylight.controller.sal.binding.api.NotificationService getNotificationService();

    NotificationService getMdSalNotificationService();

    org.opendaylight.controller.md.sal.binding.api.DataBroker getBindingDataBroker();

    DataBroker getMdSalBindingDataBroker();

    org.opendaylight.controller.md.sal.binding.api.DataBroker getBindingPingPongDataBroker();

    DataBroker getMdSalBindingPingPongDataBroker();

}
