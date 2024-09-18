/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import io.lighty.core.controller.impl.services.LightySystemReadyService;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.mdsal.binding.api.ActionProviderService;
import org.opendaylight.mdsal.binding.api.ActionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
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
import org.opendaylight.restconf.server.jaxrs.JaxRsEndpoint;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultDynamicBindingDataCodec;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;

/**
 * This interface provides methods to access ODL core services
 * provided by {@link LightyController} module.
 *
 * @author juraj.veverka
 */
public interface LightyServices extends LightyModuleRegistryService {

    DiagStatusService getDiagStatusService();

    SystemReadyMonitor getSystemReadyMonitor();

    LightySystemReadyService getLightySystemReadyService();

    ActorSystemProvider getActorSystemProvider();

    DOMSchemaService getDOMSchemaService();

    DOMSchemaService.YangTextSourceExtension getYangTextSourceExtension();

    DOMNotificationRouter getDOMNotificationRouter();

    DistributedDataStoreInterface getConfigDatastore();

    DistributedDataStoreInterface getOperationalDatastore();

    YangParserFactory getYangParserFactory();

    BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer();

    DefaultDynamicBindingDataCodec getBindingCodecTreeFactory();

    DOMEntityOwnershipService getDOMEntityOwnershipService();

    EntityOwnershipService getEntityOwnershipService();

    ClusterAdminRpcService getClusterAdminRPCService();

    ClusterSingletonServiceProvider getClusterSingletonServiceProvider();

    DOMMountPointService getDOMMountPointService();

    DOMNotificationPublishService getDOMNotificationPublishService();

    DOMNotificationService getDOMNotificationService();

    DOMDataBroker getClusteredDOMDataBroker();

    DOMRpcService getDOMRpcService();

    DOMRpcProviderService getDOMRpcProviderService();

    DOMActionService getDOMActionService();

    DOMActionProviderService getDOMActionProviderService();

    RpcProviderService getRpcProviderService();

    MountPointService getBindingMountPointService();

    NotificationService getNotificationService();

    NotificationPublishService getBindingNotificationPublishService();

    DataBroker getBindingDataBroker();

    ConstantAdapterContext getAdapterContext();

    ActionProviderService getActionProviderService();

    ActionService getActionService();

    RpcService getRpcConsumerRegistry();

    JaxRsEndpoint getJaxRsEndpoint();

    void withJaxRsEndpoint(JaxRsEndpoint endpoint);

}
