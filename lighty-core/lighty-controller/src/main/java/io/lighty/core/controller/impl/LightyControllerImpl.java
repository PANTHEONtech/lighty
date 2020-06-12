/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl;

import akka.actor.Props;
import com.typesafe.config.Config;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.actor.ClusterEventActor;
import io.lighty.core.controller.impl.actor.ClusterEventActorCreator;
import io.lighty.core.controller.impl.services.LightyDiagStatusServiceImpl;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.cluster.ActorSystemProvider;
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
import org.opendaylight.controller.config.yang.config.actor_system_provider.impl.ActorSystemProviderImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.md.sal.dom.clustering.impl.LegacyEntityOwnershipServiceAdapter;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.remote.rpc.RemoteRpcProvider;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderFactory;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.schema.service.impl.GlobalBundleScanningSchemaServiceImpl;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.mdsal.dom.broker.schema.ScanningSchemaServiceProvider;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.binding.dom.adapter.BindingDOMEntityOwnershipServiceAdapter;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.dom.impl.DOMClusterSingletonServiceProviderImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightyControllerImpl extends AbstractLightyModule implements LightyController, LightyServices {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerImpl.class);
    private final Config actorSystemConfig;
    private final ClassLoader actorSystemClassLoader;
    private final ScanningSchemaServiceProvider scanningSchemaService;
    private final DOMMountPointServiceImpl domMountPointService;
    private final DOMNotificationRouter domNotificationRouter;
    private final String restoreDirectoryPath;
    private final Properties distributedEosProperties;
    private final int maxDataBrokerFutureCallbackQueueSize;
    private final int maxDataBrokerFutureCallbackPoolSize;
    private final boolean metricCaptureEnabled;
    private final int mailboxCapacity;
    private final String moduleShardsConfig;
    private final String modulesConfig;
    private final LightyDiagStatusServiceImpl lightyDiagStatusService;
    private ActorSystemProviderImpl actorSystemProvider;
    private DatastoreSnapshotRestore datastoreSnapshotRestore;
    private final DatastoreContext configDatastoreContext;
    private final DatastoreContext operDatastoreContext;
    private AbstractDataStore configDatastore;
    private AbstractDataStore operDatastore;
    private ExecutorService listenableFutureExecutor;
    private DurationStatisticsTracker commitStatsTracker;
    private ConcurrentDOMDataBroker concurrentDOMDataBroker;
    private ClassLoadingStrategy classLoadingStrategy;
    private BindingToNormalizedNodeCodec bindingToNormalizedNodeCodec;
    private DistributedShardedDOMDataTree distributedShardedDOMDataTree;
    private PingPongDataBroker pingPongDataBroker;
    private GlobalBundleScanningSchemaServiceImpl globalBundleScanningSchemaService;
    private DOMRpcRouter domRpcRouter;
    private RemoteRpcProvider remoteRpcProvider;
    private DistributedEntityOwnershipService distributedEntityOwnershipService;
    private BindingDOMEntityOwnershipServiceAdapter bindingDOMEntityOwnershipServiceAdapter;
    private LegacyEntityOwnershipServiceAdapter legacyEntityOwnershipServiceAdapter;
    private ClusterAdminRpcService clusterAdminRpcService;
    private ListenerRegistration<SchemaContextListener> bindingCodecRegistration;
    private DOMClusterSingletonServiceProviderImpl clusterSingletonServiceProvider;
    private HeliumRpcProviderRegistry bindingRpcRegistry;
    private BindingDOMMountPointServiceAdapter bindingDOMMountPointService;
    private BindingDOMNotificationServiceAdapter bindingDOMNotificationServiceAdapter;
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

    public LightyControllerImpl(final ExecutorService executorService, final Config actorSystemConfig,
                                final ClassLoader actorSystemClassLoader,
                                final ScanningSchemaServiceProvider scanningSchemaService,
                                final DOMNotificationRouter domNotificationRouter, final String restoreDirectoryPath,
                                final int maxDataBrokerFutureCallbackQueueSize,
                                final int maxDataBrokerFutureCallbackPoolSize,
                                final boolean metricCaptureEnabled, final int mailboxCapacity,
                                final Properties distributedEosProperties,
                                final String moduleShardsConfig, final String modulesConfig,
                                final DatastoreContext configDatastoreContext,
                                final DatastoreContext operDatastoreContext) {
        super(executorService);
        initSunXMLWriterProperty();
        this.actorSystemConfig = actorSystemConfig;
        this.actorSystemClassLoader = actorSystemClassLoader;
        this.scanningSchemaService = scanningSchemaService;
        LOG.info("SCANnING SCHEMA SERVICE. {}", scanningSchemaService);
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
        this.lightyDiagStatusService = new LightyDiagStatusServiceImpl();
    }

    /**
     * This method replace property of writer from implementation of woodstox writer to internal sun writer.
     */
    private static void initSunXMLWriterProperty() {
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
    }

    @Override
    protected boolean initProcedure() {
        final long startTime = System.nanoTime();
        final ConfigurationImpl configConfiguration = new ConfigurationImpl(moduleShardsConfig, modulesConfig);
        final ConfigurationImpl operConfiguration = new ConfigurationImpl(moduleShardsConfig, modulesConfig);

        actorSystemProvider = new ActorSystemProviderImpl(actorSystemClassLoader,
                QuarantinedMonitorActor.props(() -> {
                }), actorSystemConfig);
        datastoreSnapshotRestore = DatastoreSnapshotRestore.instance(restoreDirectoryPath);
        // CONFIG DATASTORE
        configDatastore = prepareDataStore(configDatastoreContext, configConfiguration);
        // OPERATIONAL DATASTORE
        operDatastore = prepareDataStore(operDatastoreContext, operConfiguration);
        createConcurrentDOMDataBroker();
        distributedShardedDOMDataTree = new DistributedShardedDOMDataTree(actorSystemProvider, operDatastore,
                configDatastore);
        distributedShardedDOMDataTree.init();
        pingPongDataBroker = new PingPongDataBroker(concurrentDOMDataBroker);
        globalBundleScanningSchemaService =
                GlobalBundleScanningSchemaServiceImpl.createInstance(scanningSchemaService);
        domRpcRouter = DOMRpcRouter.newInstance(globalBundleScanningSchemaService);
        createRemoteRPCProvider();
        classLoadingStrategy = ModuleInfoBackedContext.create();

        // INIT CODEC FACTORY
        bindingToNormalizedNodeCodec = BindingToNormalizedNodeCodecFactory.newInstance(classLoadingStrategy);

        // ENTITY OWNERSHIP
        distributedEntityOwnershipService = DistributedEntityOwnershipService.start(operDatastore.getActorContext(),
                EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(distributedEosProperties));

        bindingDOMEntityOwnershipServiceAdapter = new BindingDOMEntityOwnershipServiceAdapter(
                distributedEntityOwnershipService, bindingToNormalizedNodeCodec);
        legacyEntityOwnershipServiceAdapter =
                new LegacyEntityOwnershipServiceAdapter(distributedEntityOwnershipService);
        clusterAdminRpcService =
                new ClusterAdminRpcService(configDatastore, operDatastore, bindingToNormalizedNodeCodec);

        //register mapping codec
        bindingCodecRegistration = BindingToNormalizedNodeCodecFactory
                .registerInstance(bindingToNormalizedNodeCodec, globalBundleScanningSchemaService);

        clusterSingletonServiceProvider =
                new DOMClusterSingletonServiceProviderImpl(distributedEntityOwnershipService);
        clusterSingletonServiceProvider.initializeProvider();
        createBindingRPCRegistry();
        //create binding mount point service
        bindingDOMMountPointService = new BindingDOMMountPointServiceAdapter(
                domMountPointService, bindingToNormalizedNodeCodec);
        //create binding notification service
        final BindingNormalizedNodeCodecRegistry codecRegistry = bindingToNormalizedNodeCodec.getCodecRegistry();
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
        bindingDOMDataBroker = new BindingDOMDataBrokerAdapter(concurrentDOMDataBroker,
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

        LOG.debug("Creating ClusterEventActor");
        CountDownLatch clusterCountDownLatch = new CountDownLatch(1);
        actorSystemProvider.getActorSystem().actorOf(
                Props.create(new ClusterEventActorCreator(clusterCountDownLatch, 3000)),
                ClusterEventActor.CLUSTER_EVENT_ACTOR_NAME);
        LOG.info("Waiting at most 3 seconds for cluster to form");
        try {
            clusterCountDownLatch.await();
        } catch (InterruptedException e) {
            LOG.error("Exception thrown while waiting for cluster to form!", e);
            return false;
        }
        delay = (System.nanoTime() - startTime) / 1_000_000f;
        LOG.info("Lighty controller initialization finished in {}ms", delay);
        return true;
    }

    private AbstractDataStore prepareDataStore(final DatastoreContext datastoreContext,
                                               final Configuration configuration) {
        final DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext);
        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector, null);
        return DistributedDataStoreFactory.createInstance(scanningSchemaService, datastoreContext,
                datastoreSnapshotRestore, actorSystemProvider, introspector, updater, configuration);
    }

    @Override
    protected boolean stopProcedure() {
        if (bindingCodecRegistration != null) {
            bindingCodecRegistration.close();
        }
        if (bindingDOMEntityOwnershipServiceAdapter != null) {
            bindingDOMEntityOwnershipServiceAdapter.close();
        }
        if (distributedEntityOwnershipService != null) {
            distributedEntityOwnershipService.close();
        }
        if (legacyEntityOwnershipServiceAdapter != null) {
            legacyEntityOwnershipServiceAdapter.close();
        }
        if (operDatastore != null) {
            operDatastore.close();
        }
        if (configDatastore != null) {
            configDatastore.close();
        }
        if (scanningSchemaService != null) {
            scanningSchemaService.close();
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
        remoteRpcProvider = RemoteRpcProviderFactory.createInstance(domRpcRouter,
                domRpcRouter, actorSystemProvider.getActorSystem(), remoteRpcProviderConfig);
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
                domRpcRouter, bindingToNormalizedNodeCodec);
        final BindingDOMRpcProviderServiceAdapter bindingDOMRpcProviderServiceAdapter
                = new BindingDOMRpcProviderServiceAdapter(domRpcRouter, bindingToNormalizedNodeCodec);
        bindingRpcRegistry = new HeliumRpcProviderRegistry(bindingDOMRpcServiceAdapter,
                bindingDOMRpcProviderServiceAdapter);
    }

    @Override
    public LightyServices getServices() {
        return this;
    }

    public LightyDiagStatusServiceImpl getLightyDiagStatusService() {
        return lightyDiagStatusService;
    }

    @Override
    public ActorSystemProvider getActorSystemProvider() {
        return actorSystemProvider;
    }

    @Override
    public SchemaContextProvider getSchemaContextProvider() {
        return scanningSchemaService;
    }

    @Override
    public DOMSchemaService getDOMSchemaService() {
        return scanningSchemaService;
    }

    @Override
    public DOMYangTextSourceProvider getDOMYangTextSourceProvider() {
        return scanningSchemaService;
    }

    @Override
    public DOMMountPointService getDOMMountPointService() {
        return domMountPointService;
    }

    @Override
    public DOMNotificationPublishService getDOMNotificationPublishService() {
        return domNotificationRouter;
    }

    @Override
    public DOMNotificationService getDOMNotificationService() {
        return domNotificationRouter;
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
    public DOMDataBroker getClusteredDOMDataBroker() {
        return concurrentDOMDataBroker;
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
    public DOMDataBroker getPingPongDataBroker() {
        return pingPongDataBroker;
    }

    @Override
    public DOMRpcService getDOMRpcService() {
        return domRpcRouter;
    }

    @Override
    public DOMRpcProviderService getDOMRpcProviderService() {
        return domRpcRouter;
    }

    @Override
    public SchemaService getSchemaService() {
        return globalBundleScanningSchemaService;
    }

    @Override
    public YangTextSourceProvider getYangTextSourceProvider() {
        return globalBundleScanningSchemaService;
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
    public LegacyEntityOwnershipServiceAdapter getLegacyEntityOwnershipService() {
        return legacyEntityOwnershipServiceAdapter;
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
    public RpcProviderRegistry getRpcProviderRegistry() {
        return bindingRpcRegistry;
    }

    @Override
    public MountPointService getBindingMountPointService() {
        return bindingDOMMountPointService;
    }

    @Override
    public NotificationService getBindingNotificationService() {
        return bindingDOMNotificationServiceAdapter;
    }

    @Override
    public NotificationPublishService getBindingNotificationPublishService() {
        return bindingDOMNotificationPublishServiceAdapter;
    }

    @Override
    public NotificationProviderService getNotificationProviderService() {
        return bindingNotificationProviderService;
    }

    @Override
    public org.opendaylight.controller.sal.binding.api.NotificationService getNotificationService() {
        return bindingNotificationProviderService;
    }

    @Override
    public DataBroker getBindingDataBroker() {
        return bindingDOMDataBroker;
    }

    @Override
    public DataBroker getBindingPingPongDataBroker() {
        return bindingPingPongDataBroker;
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
}
