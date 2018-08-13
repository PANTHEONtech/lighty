/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl;

import com.typesafe.config.Config;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.schema.DOMSchemaServiceImpl;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.akka.impl.ActorSystemProviderImpl;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextIntrospector;
import org.opendaylight.controller.cluster.datastore.DatastoreContextPropertiesUpdater;
import org.opendaylight.controller.cluster.datastore.DatastoreSnapshotRestore;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.entityownership.DistributedEntityOwnershipService;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfigReader;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.cluster.sharding.DistributedShardedDOMDataTree;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.config.threadpool.util.FixedThreadPoolWrapper;
import org.opendaylight.controller.config.threadpool.util.ScheduledThreadPoolWrapper;
import org.opendaylight.controller.md.sal.binding.compat.HeliumNotificationProviderServiceWithInterestListeners;
import org.opendaylight.controller.md.sal.binding.compat.HeliumRpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodecFactory;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.remote.rpc.RemoteRpcProvider;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderFactory;
import org.opendaylight.controller.sal.core.compat.LegacyDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.binding.dom.adapter.BindingDOMEntityOwnershipServiceAdapter;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.dom.impl.DOMClusterSingletonServiceProviderImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightyControllerImpl extends AbstractLightyModule implements LightyController, LightyServices {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerImpl.class);

    private ActorSystemProviderImpl actorSystemProvider;
    private final Config actorSystemConfig;
    private final ClassLoader actorSystemClassLoader;
    private final DOMMountPointServiceImpl domMountPointService;
    private final DOMNotificationRouter domNotificationRouter;
    private final String restoreDirectoryPath;
    private final Properties distributedEosProperties;
    private DatastoreSnapshotRestore datastoreSnapshotRestore;
    private final DatastoreContext configDatastoreContext;
    private final DatastoreContext operDatastoreContext;
    private AbstractDataStore configDatastore;
    private AbstractDataStore operDatastore;
    private ExecutorService listenableFutureExecutor;
    private DurationStatisticsTracker commitStatsTracker;
    private ConcurrentDOMDataBroker concurrentDOMDataBroker;
    private BindingToNormalizedNodeCodec bindingToNormalizedNodeCodec;
    private DistributedShardedDOMDataTree distributedShardedDOMDataTree;
    private PingPongDataBroker pingPongDataBroker;
    private DOMRpcRouter domRpcRouter;
    private org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter legacyDomRpcRouter;
    private RemoteRpcProvider remoteRpcProvider;
    private DistributedEntityOwnershipService distributedEntityOwnershipService;
    private BindingDOMEntityOwnershipServiceAdapter bindingDOMEntityOwnershipServiceAdapter;
    private ClusterAdminRpcService clusterAdminRpcService;
    private DOMClusterSingletonServiceProviderImpl clusterSingletonServiceProvider;
    private HeliumRpcProviderRegistry bindingRpcRegistry;
    private BindingDOMMountPointServiceAdapter bindingDOMMountPointService;
    private BindingDOMNotificationServiceAdapter bindingDOMNotificationServiceAdapter;
    private LegacyDOMDataBrokerAdapter legacyDOMDataBrokerAdapter;

    private final int maxDataBrokerFutureCallbackQueueSize;
    private final int maxDataBrokerFutureCallbackPoolSize;
    private final boolean metricCaptureEnabled;
    private final int mailboxCapacity;
    private final String moduleShardsConfig;
    private final String modulesConfig;

    private BindingDOMNotificationPublishServiceAdapter bindingDOMNotificationPublishServiceAdapter;
    private HeliumNotificationProviderServiceWithInterestListeners bindingNotificationProviderService;
    private BindingDOMDataBrokerAdapter bindingDOMDataBroker;
    private BindingDOMDataBrokerAdapter bindingPingPongDataBroker;
    private EventExecutor eventExecutor;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ThreadPool threadPool;
    private ScheduledThreadPool scheduledThreadPool;
    private Timer timer;

    private ModuleInfoBackedContext moduleInfoBackedContext;
    private Set<YangModuleInfo> modelSet;
    private DOMSchemaService domSchemaService;

    public LightyControllerImpl(final ExecutorService executorService, final Config actorSystemConfig,
                                final ClassLoader actorSystemClassLoader,
                                final DOMNotificationRouter domNotificationRouter, final String restoreDirectoryPath,
                                final int maxDataBrokerFutureCallbackQueueSize, final int maxDataBrokerFutureCallbackPoolSize,
                                final boolean metricCaptureEnabled, final int mailboxCapacity, final Properties distributedEosProperties,
                                final String moduleShardsConfig, final String modulesConfig, final DatastoreContext configDatastoreContext,
                                final DatastoreContext operDatastoreContext, Set<YangModuleInfo> modelSet) {
        super(executorService);
        initSunXMLWriterProperty();
        this.actorSystemConfig = actorSystemConfig;
        this.actorSystemClassLoader = actorSystemClassLoader;
        this.domMountPointService = new DOMMountPointServiceImpl();
        this.domNotificationRouter = domNotificationRouter;
        this.restoreDirectoryPath = restoreDirectoryPath;
        this.maxDataBrokerFutureCallbackQueueSize = maxDataBrokerFutureCallbackQueueSize;
        this.maxDataBrokerFutureCallbackPoolSize = maxDataBrokerFutureCallbackPoolSize;
        this.metricCaptureEnabled = metricCaptureEnabled;
        this.mailboxCapacity = mailboxCapacity;
        this.distributedEosProperties = distributedEosProperties;
        this.modulesConfig = modulesConfig;
        this.moduleShardsConfig = moduleShardsConfig;
        this.configDatastoreContext = configDatastoreContext;
        this.operDatastoreContext = operDatastoreContext;
        this.modelSet = modelSet;
    }

    /**
     * This method replace property of writer from implementation of woodstox writer to internal sun writer.
     */
    private static final void initSunXMLWriterProperty() {
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
    }

    @Override
    protected boolean initProcedure() {
        final long startTime = System.nanoTime();

        //INIT actor system provider
        actorSystemProvider = new ActorSystemProviderImpl(actorSystemClassLoader,
                QuarantinedMonitorActor.props(() -> {}), actorSystemConfig);
        datastoreSnapshotRestore = DatastoreSnapshotRestore.instance(restoreDirectoryPath);

        //INIT schema context
        moduleInfoBackedContext = ModuleInfoBackedContext.create();
        modelSet.forEach( m -> {
            moduleInfoBackedContext.registerModuleInfo(m);
        });
        domSchemaService = new DOMSchemaServiceImpl(moduleInfoBackedContext);
        // INIT CODEC FACTORY
        bindingToNormalizedNodeCodec = BindingToNormalizedNodeCodecFactory.newInstance(moduleInfoBackedContext);
        BindingRuntimeContext bindingRuntimeContext =
                BindingRuntimeContext.create(moduleInfoBackedContext, moduleInfoBackedContext.getSchemaContext());
        //create binding notification service
        final BindingNormalizedNodeCodecRegistry codecRegistry = bindingToNormalizedNodeCodec.getCodecRegistry();
        codecRegistry.onBindingRuntimeContextUpdated(bindingRuntimeContext);

        // CONFIG DATASTORE
        configDatastore = prepareDataStore(configDatastoreContext, moduleShardsConfig, modulesConfig, bindingToNormalizedNodeCodec,
                domSchemaService, datastoreSnapshotRestore, actorSystemProvider);
        // OPERATIONAL DATASTORE
        operDatastore = prepareDataStore(operDatastoreContext, moduleShardsConfig, modulesConfig, bindingToNormalizedNodeCodec,
                domSchemaService, datastoreSnapshotRestore, actorSystemProvider);

        createConcurrentDOMDataBroker();
        distributedShardedDOMDataTree = new DistributedShardedDOMDataTree(actorSystemProvider, operDatastore,
                configDatastore);
        distributedShardedDOMDataTree.init();
        legacyDOMDataBrokerAdapter = new LegacyDOMDataBrokerAdapter(concurrentDOMDataBroker);
        pingPongDataBroker = new PingPongDataBroker(legacyDOMDataBrokerAdapter);

        domRpcRouter = DOMRpcRouter.newInstance(domSchemaService);
        legacyDomRpcRouter = new org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter(
                domRpcRouter.getRpcService(), domRpcRouter.getRpcProviderService());

        createRemoteRPCProvider();

        // ENTITY OWNERSHIP
        distributedEntityOwnershipService = DistributedEntityOwnershipService.start(operDatastore.getActorContext(),
                EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(distributedEosProperties));

        bindingDOMEntityOwnershipServiceAdapter = new BindingDOMEntityOwnershipServiceAdapter(
                distributedEntityOwnershipService, bindingToNormalizedNodeCodec);
        clusterAdminRpcService =
                new ClusterAdminRpcService(configDatastore, operDatastore, bindingToNormalizedNodeCodec);

        clusterSingletonServiceProvider =
                new DOMClusterSingletonServiceProviderImpl(distributedEntityOwnershipService);
        clusterSingletonServiceProvider.initializeProvider();
        createBindingRPCRegistry();
        //create binding mount point service
        bindingDOMMountPointService = new BindingDOMMountPointServiceAdapter(
                domMountPointService, bindingToNormalizedNodeCodec);

        bindingDOMNotificationServiceAdapter = new BindingDOMNotificationServiceAdapter(
                codecRegistry, domNotificationRouter);
        //create binding notificacatio publish service
        bindingDOMNotificationPublishServiceAdapter = new BindingDOMNotificationPublishServiceAdapter(
                bindingToNormalizedNodeCodec, domNotificationRouter);
        //create bindingNotificationProviderService
        bindingNotificationProviderService =
                new HeliumNotificationProviderServiceWithInterestListeners(
                        bindingDOMNotificationPublishServiceAdapter, bindingDOMNotificationServiceAdapter,
                        domNotificationRouter);
        //create binding data broker
        bindingDOMDataBroker = new BindingDOMDataBrokerAdapter(legacyDOMDataBrokerAdapter,
                bindingToNormalizedNodeCodec);
        //create binding ping pong broker
        bindingPingPongDataBroker = new BindingDOMDataBrokerAdapter(pingPongDataBroker,
                bindingToNormalizedNodeCodec);
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        eventExecutor = new DefaultEventExecutor();
        timer = new HashedWheelTimer();
        threadPool =
                new FixedThreadPoolWrapper(2, new DefaultThreadFactory("default-pool"));
        scheduledThreadPool =
                new ScheduledThreadPoolWrapper(2, new DefaultThreadFactory("default-scheduled-pool"));
        float delay = (System.nanoTime() - startTime) / 1_000_000f;
        LOG.info("Lighty controller started in {}ms", delay);
        return true;
    }

    private AbstractDataStore prepareDataStore(final DatastoreContext datastoreContext, final String moduleShardsConfig,
                                               final String modulesConfig,
                                               final BindingToNormalizedNodeCodec bindingToNormalizedNodeCodec,
                                               final DOMSchemaService domSchemaService,
                                               final DatastoreSnapshotRestore datastoreSnapshotRestore,
                                               final ActorSystemProvider actorSystemProvider) {
        final ConfigurationImpl configuration = new ConfigurationImpl(moduleShardsConfig, modulesConfig);
        final DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext, bindingToNormalizedNodeCodec);
        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector, null);
        return DistributedDataStoreFactory.createInstance(domSchemaService, datastoreContext,
                datastoreSnapshotRestore, actorSystemProvider, introspector, updater, configuration);
    }

    @Override
    protected boolean stopProcedure() {
        if (bindingDOMEntityOwnershipServiceAdapter != null) {
            bindingDOMEntityOwnershipServiceAdapter.close();
        }
        if (distributedEntityOwnershipService != null) {
            distributedEntityOwnershipService.close();
        }
        if (operDatastore != null) {
            operDatastore.close();
        }
        if (configDatastore != null) {
            configDatastore.close();
        }
        if (remoteRpcProvider != null) {
            remoteRpcProvider.close();
        }
        if (actorSystemProvider != null) {
            actorSystemProvider.close();
        }
        return true;
    }

    private void createRemoteRPCProvider() {
        final RemoteRpcProviderConfig remoteRpcProviderConfig = RemoteRpcProviderConfig.newInstance(
                actorSystemProvider.getActorSystem().name(), metricCaptureEnabled, mailboxCapacity);
        remoteRpcProvider = RemoteRpcProviderFactory.createInstance(domRpcRouter.getRpcProviderService(),
                domRpcRouter.getRpcService(), actorSystemProvider.getActorSystem(), remoteRpcProviderConfig);
        remoteRpcProvider.start();
    }

    private void createConcurrentDOMDataBroker() {
        listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                maxDataBrokerFutureCallbackPoolSize, maxDataBrokerFutureCallbackQueueSize,
                "CommitFutures", Logger.class);
        commitStatsTracker = DurationStatisticsTracker.createConcurrent();
        final Map<LogicalDatastoreType, DOMStore> datastores = new HashMap<>();
        datastores.put(LogicalDatastoreType.CONFIGURATION, configDatastore);
        datastores.put(LogicalDatastoreType.OPERATIONAL, operDatastore);
        concurrentDOMDataBroker = new ConcurrentDOMDataBroker(datastores,
                listenableFutureExecutor, commitStatsTracker);
    }

    private void createBindingRPCRegistry() {
        //create bindingRPCServiceAdapter
        final BindingDOMRpcServiceAdapter bindingDOMRpcServiceAdapter = new BindingDOMRpcServiceAdapter(
                legacyDomRpcRouter, bindingToNormalizedNodeCodec);
        final BindingDOMRpcProviderServiceAdapter bindingDOMRpcProviderServiceAdapter
                = new BindingDOMRpcProviderServiceAdapter(legacyDomRpcRouter, bindingToNormalizedNodeCodec);
        bindingRpcRegistry = new HeliumRpcProviderRegistry(bindingDOMRpcServiceAdapter,
                bindingDOMRpcProviderServiceAdapter);
    }

    @Override
    public LightyServices getServices() {
        return this;
    }

    @Override
    public ActorSystemProvider getActorSystemProvider() {
        return actorSystemProvider;
    }

    @Override
    public SchemaContextProvider getSchemaContextProvider() {
        return moduleInfoBackedContext;
    }

    @Override
    public DOMSchemaService getDOMSchemaService() {
        return domSchemaService;
    }

    @Override
    public DOMYangTextSourceProvider getDOMYangTextSourceProvider() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry() {
        return domNotificationRouter;
    }

    @Override
    public DistributedDataStoreInterface getConfigDatastore() {
        return configDatastore;
    }

    @Override
    public DistributedDataStoreInterface getOperationalDatastore() {
        return operDatastore;
    }

    @Override
    public DOMDataTreeShardingService getDOMDataTreeShardingService() {
        return distributedShardedDOMDataTree;
    }

    @Override
    public DOMDataTreeService getDOMDataTreeService() {
        return distributedShardedDOMDataTree;
    }

    @Override
    public DistributedShardFactory getDistributedShardFactory() {
        return distributedShardedDOMDataTree;
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return bindingToNormalizedNodeCodec;
    }

    @Override
    public BindingCodecTreeFactory getBindingCodecTreeFactory() {
        return bindingToNormalizedNodeCodec;
    }

    @Override
    public DOMEntityOwnershipService getDOMEntityOwnershipService() {
        return distributedEntityOwnershipService;
    }

    @Override
    public EntityOwnershipService getEntityOwnershipService() {
        return bindingDOMEntityOwnershipServiceAdapter;
    }

    @Override
    public ClusterAdminService getClusterAdminRPCService() {
        return clusterAdminRpcService;
    }

    @Override
    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return clusterSingletonServiceProvider;
    }

    @Override
    public EventExecutor getEventExecutor() {
        return eventExecutor;
    }

    @Override
    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    @Override
    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    @Override
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    @Override
    public ScheduledThreadPool getScheduledThreaPool() {
        return scheduledThreadPool;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMMountPointService getDOMMountPointService() {
        return domMountPointService;
    }

    @Override
    public DOMMountPointService getMdSalDOMMountPointService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService getDOMNotificationPublishService() {
        return domNotificationRouter;
    }

    @Override
    public DOMNotificationPublishService getMdSalDOMNotificationPublishService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMNotificationService getDOMNotificationService() {
        return domNotificationRouter;
    }

    @Override
    public DOMNotificationService getMdSalDOMNotificationService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getClusteredDOMDataBroker() {
        return legacyDOMDataBrokerAdapter;
    }

    @Override
    public DOMDataBroker getMdSalClusteredDOMDataBroker() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getPingPongDataBroker() {
        return pingPongDataBroker;
    }

    @Override
    public DOMDataBroker getMdSalPingPongDataBroker() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMRpcService getDOMRpcService() {
        return legacyDomRpcRouter;
    }

    @Override
    public DOMRpcService getMdSalDOMRpcService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService getDOMRpcProviderService() {
        return legacyDomRpcRouter;
    }

    @Override
    public DOMRpcProviderService getMdSalDOMRpcProviderService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.sal.binding.api.RpcProviderRegistry getRpcProviderRegistry() {
        return bindingRpcRegistry;
    }

    @Override
    public RpcProviderService getMdSalRpcProviderService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.MountPointService getBindingMountPointService() {
        return bindingDOMMountPointService;
    }

    @Override
    public MountPointService getMdSalMountPointService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.NotificationService getBindingNotificationService() {
        return bindingDOMNotificationServiceAdapter;
    }

    @Override
    public NotificationService getMdSalBindingNotificationService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.NotificationPublishService getBindingNotificationPublishService() {
        return bindingDOMNotificationPublishServiceAdapter;
    }

    @Override
    public NotificationPublishService getMdSalNotificationPublishService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.sal.binding.api.NotificationProviderService getNotificationProviderService(){
        return bindingNotificationProviderService;
    }

    @Override
    public org.opendaylight.controller.sal.binding.api.NotificationService getNotificationService() {
        return bindingNotificationProviderService;
    }

    @Override
    public NotificationService getMdSalNotificationService() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.DataBroker getBindingDataBroker() {
        return bindingDOMDataBroker;
    }

    @Override
    public DataBroker getMdSalBindingDataBroker() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.DataBroker getBindingPingPongDataBroker() {
        return bindingPingPongDataBroker;
    }

    @Override
    public DataBroker getMdSalBindingPingPongDataBroker() {
        //TODO: finish implementation
        throw new UnsupportedOperationException("not implemented");
    }

}
