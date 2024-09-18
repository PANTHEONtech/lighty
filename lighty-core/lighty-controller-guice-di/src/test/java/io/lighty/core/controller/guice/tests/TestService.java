/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.guice.tests;

import io.lighty.core.controller.api.LightyModuleRegistryService;
import io.lighty.core.controller.api.LightyServices;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
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
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultDynamicBindingDataCodec;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class TestService {

    @Inject
    private DiagStatusService diagStatusService;

    @Inject
    private ActorSystemProvider actorSystemProvider;

    @Inject
    private DOMSchemaService domSchemaService;

    @Inject
    private DOMNotificationRouter domNotificationSubscriptionListenerRegistry;

    @Inject
    @Named("ControllerConfigDatastore")
    private DistributedDataStoreInterface distributedDataStoreInterfaceConfig;

    @Inject
    @Named("ControllerOperationalDatastore")
    private DistributedDataStoreInterface distributedDataStoreInterfaceOperational;

    @Inject
    private BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;

    @Inject
    private DefaultDynamicBindingDataCodec bindingCodecTreeFactory;

    @Inject
    private DOMEntityOwnershipService domEntityOwnershipService;

    @Inject
    private EntityOwnershipService entityOwnershipService;

    @Inject
    private ClusterAdminRpcService clusterAdminService;

    @Inject
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    @Inject
    private DOMMountPointService domMountPointService;

    @Inject
    private DOMNotificationPublishService domNotificationPublishService;

    @Inject
    private DOMNotificationService domNotificationService;

    @Inject
    @Named("ClusteredDOMDataBroker")
    private DOMDataBroker domDataBroker;

    @Inject
    private DOMRpcService domRpcService;

    @Inject
    private DOMRpcProviderService domRpcProviderService;

    @Inject
    private RpcProviderService rpcProviderService;

    @Inject
    private MountPointService mountPointService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private NotificationPublishService notificationPublishService;

    @Inject
    @Named("BindingDataBroker")
    private DataBroker bindingDataBroker;

    @Inject
    private LightyServices lightyServices;

    @Inject
    private LightyModuleRegistryService lightyModuleRegistryService;

    public DiagStatusService getDiagStatusService() {
        return diagStatusService;
    }

    public ActorSystemProvider getActorSystemProvider() {
        return actorSystemProvider;
    }

    public EffectiveModelContext getSchemaContext() {
        return getDomSchemaService().getGlobalContext();
    }

    public DOMSchemaService getDomSchemaService() {
        return domSchemaService;
    }

    public DOMNotificationRouter getDomNotificationSubscriptionListenerRegistry() {
        return domNotificationSubscriptionListenerRegistry;
    }

    public DistributedDataStoreInterface getDistributedDataStoreInterfaceConfig() {
        return distributedDataStoreInterfaceConfig;
    }

    public DistributedDataStoreInterface getDistributedDataStoreInterfaceOperational() {
        return distributedDataStoreInterfaceOperational;
    }

    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return bindingNormalizedNodeSerializer;
    }

    public DefaultDynamicBindingDataCodec getBindingCodecTreeFactory() {
        return bindingCodecTreeFactory;
    }

    public DOMEntityOwnershipService getDomEntityOwnershipService() {
        return domEntityOwnershipService;
    }

    public EntityOwnershipService getEntityOwnershipService() {
        return entityOwnershipService;
    }

    public ClusterAdminRpcService getClusterAdminService() {
        return clusterAdminService;
    }

    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return clusterSingletonServiceProvider;
    }

    public DOMMountPointService getDomMountPointService() {
        return domMountPointService;
    }

    public DOMNotificationPublishService getDomNotificationPublishService() {
        return domNotificationPublishService;
    }

    public DOMNotificationService getDomNotificationService() {
        return domNotificationService;
    }

    public DOMDataBroker getDomDataBroker() {
        return domDataBroker;
    }

    public DOMRpcService getDomRpcService() {
        return domRpcService;
    }

    public DOMRpcProviderService getDomRpcProviderService() {
        return domRpcProviderService;
    }

    public RpcProviderService getRpcProviderService() {
        return rpcProviderService;
    }

    public MountPointService getMountPointService() {
        return mountPointService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public NotificationPublishService getNotificationPublishService() {
        return notificationPublishService;
    }

    public DataBroker getBindingDataBroker() {
        return bindingDataBroker;
    }

    public LightyServices getLightyServices() {
        return lightyServices;
    }

    public LightyModuleRegistryService getLightyModuleRegistryService() {
        return lightyModuleRegistryService;
    }

}
