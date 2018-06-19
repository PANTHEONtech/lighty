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
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
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

    DOMMountPointService getDOMMountPointService();

    DOMNotificationPublishService getDOMNotificationPublishService();

    DOMNotificationService getDOMNotificationService();

    DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry();

    DistributedDataStoreInterface getConfigDatastore();

    DistributedDataStoreInterface getOperationalDatastore();

    DOMDataBroker getClusteredDOMDataBroker();

    DOMDataBroker getPingPongDataBroker();

    DOMDataTreeShardingService getDOMDataTreeShardingService();

    DOMDataTreeService getDOMDataTreeService();

    DistributedShardFactory getDistributedShardFactory();

    DOMRpcService getDOMRpcService();

    DOMRpcProviderService getDOMRpcProviderService();

    SchemaService getSchemaService();

    YangTextSourceProvider getYangTextSourceProvider();

    BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer();

    BindingCodecTreeFactory getBindingCodecTreeFactory();

    DOMEntityOwnershipService getDOMEntityOwnershipService();

    EntityOwnershipService getEntityOwnershipService();

    ClusterAdminService getClusterAdminRPCService();

    ClusterSingletonServiceProvider getClusterSingletonServiceProvider();

    RpcProviderRegistry getRpcProviderRegistry();

    MountPointService getBindingMountPointService();

    NotificationService getBindingNotificationService();

    NotificationPublishService getBindingNotificationPublishService();

    NotificationProviderService getNotificationProviderService();

    org.opendaylight.controller.sal.binding.api.NotificationService getNotificationService();

    DataBroker getBindingDataBroker();

    DataBroker getBindingPingPongDataBroker();

    EventExecutor getEventExecutor();

    EventLoopGroup getBossGroup();

    EventLoopGroup getWorkerGroup();

    ThreadPool getThreadPool();

    ScheduledThreadPool getScheduledThreaPool();

    Timer getTimer();

}
