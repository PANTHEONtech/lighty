/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl;

import static io.lighty.core.controller.impl.util.ControllerConfigUtils.MODULE_SHARDS_TMP_PATH;
import static io.lighty.core.controller.impl.util.ControllerConfigUtils.generateModuleShardsForMembers;
import static io.lighty.core.controller.impl.util.ControllerConfigUtils.isKubernetesDeployment;

import akka.cluster.Cluster;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.cluster.UnreachableListenerService;
import io.lighty.core.controller.impl.schema.SchemaServiceProvider;
import io.lighty.core.controller.impl.services.LightyDiagStatusServiceImpl;
import io.lighty.core.controller.impl.services.LightySystemReadyMonitorImpl;
import io.lighty.core.controller.impl.services.LightySystemReadyService;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcServiceAdapter;
import org.opendaylight.controller.remote.rpc.RemoteRpcProvider;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderFactory;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.compat.LegacyDOMDataBrokerAdapter;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMMountPointServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
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
import org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.mdsal.dom.broker.pingpong.PingPongDataBroker;
import org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.binding.dom.adapter.BindingDOMEntityOwnershipServiceAdapter;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.dom.impl.DOMClusterSingletonServiceProviderImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightyControllerImpl extends AbstractLightyModule implements LightyController, LightyServices {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerImpl.class);

    private final Config actorSystemConfig;
    private final ClassLoader actorSystemClassLoader;
    private final DOMNotificationRouter domNotificationRouter;
    private final org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter domNotificationRouterOld;
    private final DOMMountPointService domMountPointService;
    private final org.opendaylight.controller.md.sal.dom.api.DOMMountPointService domMountPointServiceOld;
    private final Set<YangModuleInfo> modelSet;
    private final Properties distributedEosProperties;
    private final DatastoreContext configDatastoreContext;
    private final DatastoreContext operDatastoreContext;
    private final Map<String, Object> datastoreProperties;
    private final String modulesConfig;
    private final String restoreDirectoryPath;
    private final int maxDataBrokerFutureCallbackQueueSize;
    private final int maxDataBrokerFutureCallbackPoolSize;
    private final int mailboxCapacity;
    private final boolean metricCaptureEnabled;

    private String moduleShardsConfig;
    private ActorSystemProviderImpl actorSystemProvider;
    private DatastoreSnapshotRestore datastoreSnapshotRestore;
    private AbstractDataStore configDatastore;
    private AbstractDataStore operDatastore;
    private ExecutorService listenableFutureExecutor;
    private DurationStatisticsTracker commitStatsTracker;
    private DOMDataBroker concurrentDOMDataBroker;
    private org.opendaylight.controller.md.sal.dom.api.DOMDataBroker concurrentDOMDataBrokerOld;
    private DistributedShardedDOMDataTree distributedShardedDOMDataTree;
    private DOMRpcRouter domRpcRouter;
    private org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter domRpcRouterOld;
    private RemoteRpcProvider remoteRpcProvider;
    private DistributedEntityOwnershipService distributedEntityOwnershipService;
    private BindingDOMEntityOwnershipServiceAdapter bindingDOMEntityOwnershipServiceAdapter;
    private ClusterAdminRpcService clusterAdminRpcService;
    private DOMClusterSingletonServiceProviderImpl clusterSingletonServiceProvider;
    private NotificationService notificationService;
    private NotificationPublishService notificationPublishService;
    private org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter notificationPublishServiceOld;
    private BindingDOMDataBrokerAdapter domDataBroker;
    private org.opendaylight.controller.md.sal.binding.api.DataBroker domDataBrokerOld;
    private BindingDOMDataBrokerAdapter domPingPongDataBroker;
    private final LightyDiagStatusServiceImpl lightyDiagStatusService;
    private EventExecutor eventExecutor;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ThreadPool threadPool;
    private ScheduledThreadPool scheduledThreadPool;
    private Timer timer;
    private ModuleInfoBackedContext moduleInfoBackedContext;
    private SchemaServiceProvider schemaServiceProvider;
    private BindingToNormalizedNodeCodec codec;
    private org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec codecOld;
    private DOMDataBroker pingPongDataBroker;
    private org.opendaylight.controller.md.sal.dom.api.DOMDataBroker pingPongDataBrokerOld;
    private RpcProviderService rpcProviderService;
    private RpcProviderRegistry rpcProviderRegistry;
    private MountPointService mountPointService;
    private org.opendaylight.controller.md.sal.binding.api.MountPointService mountPointServiceOld;
    private HeliumNotificationProviderServiceWithInterestListeners notificationProviderServiceOld;
    private org.opendaylight.controller.md.sal.binding.api.DataBroker domPingPongDataBrokerOld;
    private org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter notificatoinServiceOld;
    private final LightySystemReadyMonitorImpl systemReadyMonitor;
    private AkkaManagement akkaManagement;

    public LightyControllerImpl(final ExecutorService executorService, final Config actorSystemConfig,
                                final ClassLoader actorSystemClassLoader,
                                final DOMNotificationRouter domNotificationRouter, final String restoreDirectoryPath,
                                final int maxDataBrokerFutureCallbackQueueSize, final int maxDataBrokerFutureCallbackPoolSize,
                                final boolean metricCaptureEnabled, final int mailboxCapacity, final Properties distributedEosProperties,
                                final String moduleShardsConfig, final String modulesConfig, final DatastoreContext configDatastoreContext,
                                final DatastoreContext operDatastoreContext, final Map<String, Object> datastoreProperties,
                                final Set<YangModuleInfo> modelSet) {
        super(executorService);
        initSunXMLWriterProperty();
        this.actorSystemConfig = actorSystemConfig;
        this.actorSystemClassLoader = actorSystemClassLoader;
        this.domMountPointService = new DOMMountPointServiceImpl();
        this.domMountPointServiceOld =
                new org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl(
                        this.domMountPointService);
        this.domNotificationRouter = domNotificationRouter;
        this.domNotificationRouterOld = org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter.create(
                this.domNotificationRouter, this.domNotificationRouter, this.domNotificationRouter);
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
        this.datastoreProperties = datastoreProperties;
        this.modelSet = modelSet;
        this.systemReadyMonitor = new LightySystemReadyMonitorImpl();
        this.lightyDiagStatusService = new LightyDiagStatusServiceImpl(systemReadyMonitor);
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
        final boolean k8sDeployment = isKubernetesDeployment(this.actorSystemConfig);

        //INIT actor system provider
        this.actorSystemProvider = new ActorSystemProviderImpl(this.actorSystemClassLoader,
                QuarantinedMonitorActor.props(() -> {
                }), this.actorSystemConfig);

        this.akkaManagement = AkkaManagement.get(actorSystemProvider.getActorSystem());
        akkaManagement.start();
        //INIT cluster bootstrap, if this is Kubernetes Deployment
        if (k8sDeployment) {
            if (!initializeK8sClustering()) {
                return false;
            }
        }

        this.datastoreSnapshotRestore = DatastoreSnapshotRestore.instance(this.restoreDirectoryPath);

        //INIT schema context
        this.moduleInfoBackedContext = ModuleInfoBackedContext.create();
        this.modelSet.forEach(m -> {
            this.moduleInfoBackedContext.registerModuleInfo(m);
        });
        this.schemaServiceProvider = new SchemaServiceProvider(this.moduleInfoBackedContext);
        // INIT CODEC FACTORY
        this.codec = BindingToNormalizedNodeCodec.newInstance(this.moduleInfoBackedContext, this.schemaServiceProvider);
        this.schemaServiceProvider.registerSchemaContextListener(this.codec);

        final BindingRuntimeContext bindingRuntimeContext =
                BindingRuntimeContext.create(this.moduleInfoBackedContext, this.moduleInfoBackedContext
                        .getSchemaContext());
        //create binding notification service
        final BindingNormalizedNodeCodecRegistry codecRegistry = this.codec.getCodecRegistry();
        codecRegistry.onBindingRuntimeContextUpdated(bindingRuntimeContext);

        this.codecOld = new org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec(
                this.moduleInfoBackedContext, codecRegistry);

        this.schemaServiceProvider.registerSchemaContextListener(this.codecOld);

        // CONFIG DATASTORE
        this.configDatastore = prepareDataStore(this.configDatastoreContext, this.moduleShardsConfig,
                this.modulesConfig, this.schemaServiceProvider, this.datastoreSnapshotRestore,
                this.actorSystemProvider);
        // OPERATIONAL DATASTORE
        this.operDatastore = prepareDataStore(this.operDatastoreContext, this.moduleShardsConfig, this.modulesConfig,
                this.schemaServiceProvider, this.datastoreSnapshotRestore, this.actorSystemProvider);

        createConcurrentDOMDataBroker();
        this.distributedShardedDOMDataTree = new DistributedShardedDOMDataTree(this.actorSystemProvider,
                this.operDatastore,
                this.configDatastore);
        this.distributedShardedDOMDataTree.init();

        this.pingPongDataBroker = new PingPongDataBroker(this.concurrentDOMDataBroker);
        final LegacyDOMDataBrokerAdapter pingPongLegacyDOMDataBrokerAdapter = new LegacyDOMDataBrokerAdapter(
                this.pingPongDataBroker);
        this.pingPongDataBrokerOld = new org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker(
                pingPongLegacyDOMDataBrokerAdapter);

        this.domRpcRouter = DOMRpcRouter.newInstance(this.schemaServiceProvider);
        this.domRpcRouterOld = new org.opendaylight.controller.md.sal.dom.broker.impl.DOMRpcRouter(this.domRpcRouter
                .getRpcService(), this.domRpcRouter.getRpcProviderService());
        createRemoteRPCProvider();

        // ENTITY OWNERSHIP
        this.distributedEntityOwnershipService = DistributedEntityOwnershipService.start(this.operDatastore
                .getActorContext(), EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(
                this.distributedEosProperties));

        this.bindingDOMEntityOwnershipServiceAdapter = new BindingDOMEntityOwnershipServiceAdapter(
                this.distributedEntityOwnershipService, this.codec);
        this.clusterAdminRpcService =
                new ClusterAdminRpcService(this.configDatastore, this.operDatastore, this.codec);

        this.clusterSingletonServiceProvider =
                new DOMClusterSingletonServiceProviderImpl(this.distributedEntityOwnershipService);
        this.clusterSingletonServiceProvider.initializeProvider();
        this.rpcProviderService = new BindingDOMRpcProviderServiceAdapter(this.domRpcRouter
                .getRpcProviderService(), this.codec);
        final org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter domRpcProvAdapterOld =
                new org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter(
                        this.domRpcRouterOld, this.codecOld);
        final RpcConsumerRegistry rpcServiceAdapterOld = new BindingDOMRpcServiceAdapter(this.domRpcRouterOld,
                this.codecOld);
        this.rpcProviderRegistry = new HeliumRpcProviderRegistry(rpcServiceAdapterOld, domRpcProvAdapterOld);

        //create binding mount point service
        this.mountPointService = new BindingDOMMountPointServiceAdapter(this.domMountPointService, this.codec);
        this.mountPointServiceOld =
                new org.opendaylight.controller.md.sal.binding.impl.BindingDOMMountPointServiceAdapter(
                        this.domMountPointServiceOld, this.codecOld);

        this.notificationService = new BindingDOMNotificationServiceAdapter(this.domNotificationRouter,
                this.codec);
        this.notificationPublishService =
                new BindingDOMNotificationPublishServiceAdapter(this.domNotificationRouter, this.codec);
        this.notificationPublishServiceOld =
                new org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter(
                        this.codecOld, this.domNotificationRouterOld);
        this.notificatoinServiceOld =
                new org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter(this.codecOld,
                        this.domNotificationRouterOld);
        this.notificationProviderServiceOld = new HeliumNotificationProviderServiceWithInterestListeners(
                this.notificationPublishServiceOld, this.notificatoinServiceOld, this.domNotificationRouterOld);

        //create binding data broker
        this.domDataBroker = new BindingDOMDataBrokerAdapter(this.concurrentDOMDataBroker, this.codec);
        this.domDataBrokerOld = new org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter(
                this.concurrentDOMDataBrokerOld, this.codecOld);

        this.domPingPongDataBroker = new BindingDOMDataBrokerAdapter(this.pingPongDataBroker, this.codec);
        this.domPingPongDataBrokerOld = new org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter(
                this.pingPongDataBrokerOld, this.codecOld);

        if (k8sDeployment) {
            Long podRestartTimeout = null;
            if (this.actorSystemConfig.hasPath(ControllerConfigUtils.K8S_POD_RESTART_TIMEOUT_PATH)) {
                podRestartTimeout = this.actorSystemConfig.getLong(ControllerConfigUtils.K8S_POD_RESTART_TIMEOUT_PATH);
            }
            this.clusterSingletonServiceProvider.registerClusterSingletonService(new UnreachableListenerService(
                    this.actorSystemProvider.getActorSystem(), this.domDataBroker, this.clusterAdminRpcService,
                    podRestartTimeout));
            if (!askForShards()) {
                return false;
            }
        }

        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.eventExecutor = new DefaultEventExecutor();
        this.timer = new HashedWheelTimer();
        this.threadPool =
                new FixedThreadPoolWrapper(2, new DefaultThreadFactory("default-pool"));
        this.scheduledThreadPool =
                new ScheduledThreadPoolWrapper(2, new DefaultThreadFactory("default-scheduled-pool"));
        final float delay = (System.nanoTime() - startTime) / 1_000_000f;
        LOG.info("Lighty controller started in {}ms", delay);
        return true;
    }

    private AbstractDataStore prepareDataStore(final DatastoreContext datastoreContext, final String moduleShardsConfig,
                                               final String modulesConfig,
                                               final DOMSchemaService domSchemaService,
                                               final DatastoreSnapshotRestore datastoreSnapshotRestore,
                                               final ActorSystemProvider actorSystemProvider) {
        final ConfigurationImpl configuration = new ConfigurationImpl(moduleShardsConfig, modulesConfig);
        final DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(datastoreContext,
                this.codecOld);
        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector,
                datastoreProperties);
        return DistributedDataStoreFactory.createInstance(domSchemaService, datastoreContext,
                datastoreSnapshotRestore, actorSystemProvider, introspector, updater, configuration);
    }

    @Override
    protected boolean stopProcedure() {
        if (this.bindingDOMEntityOwnershipServiceAdapter != null) {
            this.bindingDOMEntityOwnershipServiceAdapter.close();
        }
        if (this.distributedEntityOwnershipService != null) {
            this.distributedEntityOwnershipService.close();
        }
        if (this.operDatastore != null) {
            this.operDatastore.close();
        }
        if (this.configDatastore != null) {
            this.configDatastore.close();
        }
        if (this.remoteRpcProvider != null) {
            this.remoteRpcProvider.close();
        }
        if (this.akkaManagement != null) {
            this.akkaManagement.stop();
        }
        if (this.actorSystemProvider != null) {
            this.actorSystemProvider.close();
        }
        return true;
    }

    private void createRemoteRPCProvider() {
        final RemoteRpcProviderConfig remoteRpcProviderConfig = RemoteRpcProviderConfig.newInstance(
                this.actorSystemProvider.getActorSystem().name(), this.metricCaptureEnabled, this.mailboxCapacity);
        this.remoteRpcProvider = RemoteRpcProviderFactory.createInstance(this.domRpcRouter.getRpcProviderService(),
                this.domRpcRouter.getRpcService(), this.actorSystemProvider.getActorSystem(), remoteRpcProviderConfig);
        this.remoteRpcProvider.start();
    }

    private void createConcurrentDOMDataBroker() {
        this.listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                this.maxDataBrokerFutureCallbackPoolSize, this.maxDataBrokerFutureCallbackQueueSize,
                "CommitFutures", Logger.class);
        this.commitStatsTracker = DurationStatisticsTracker.createConcurrent();
        final Map<LogicalDatastoreType, DOMStore> datastores = new HashMap<>();
        datastores.put(LogicalDatastoreType.CONFIGURATION, this.configDatastore);
        datastores.put(LogicalDatastoreType.OPERATIONAL, this.operDatastore);
        this.concurrentDOMDataBroker = new ConcurrentDOMDataBroker(datastores,
                this.listenableFutureExecutor, this.commitStatsTracker);
        this.concurrentDOMDataBrokerOld = new LegacyDOMDataBrokerAdapter(this.concurrentDOMDataBroker);
    }

    /**
     * The first member of the cluster (leader) will create his shards. Other joining members will query
     * the leader for snapshots of the shards.
     */
    private boolean askForShards() {
        if (!Cluster.get(this.actorSystemProvider.getActorSystem()).selfAddress().
                equals(Cluster.get(this.actorSystemProvider.getActorSystem()).state().getLeader())) {
            LOG.debug("RPC call - Asking for Shard Snapshots");
            try {
                RpcResult<AddReplicasForAllShardsOutput> rpcResult =
                        this.clusterAdminRpcService.addReplicasForAllShards(
                                new AddReplicasForAllShardsInputBuilder().build()).get();
                LOG.debug("RPC call - Asking for Shard Snapshots result: {}", rpcResult.getResult());
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("RPC call - Asking for Shard Snapshots failed: {}", e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * Initialize Cluster Bootstrap. If this instance is the cluster leader, create custom module-shards.conf
     * specifying that all shards should be created on this member. If this instance is not the leader the
     * default module-shards.conf will be used. In this case shards will not be created but received from leader
     * as snapshots and installed.
     */
    private boolean initializeK8sClustering() {
        LOG.info("Starting ClusterBootstrap");
        ClusterBootstrap clusterBootstrap = ClusterBootstrap.get(actorSystemProvider.getActorSystem());
        clusterBootstrap.start();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            LOG.info("Waiting for cluster to form");
            ListenableScheduledFuture clusterLeaderElectionFuture = getClusterLeaderElectionFuture(latch);
            latch.await();
            clusterLeaderElectionFuture.cancel(true);
        } catch (InterruptedException e) {
            LOG.error("Error occurred while waiting for the Cluster to form: {}", e.getMessage(), e);
            return false;
        }

        LOG.info("Cluster is formed, leader= {}", Cluster.get(this.actorSystemProvider.getActorSystem()).state().getLeader());
        if (Cluster.get(this.actorSystemProvider.getActorSystem()).selfAddress().
                equals(Cluster.get(this.actorSystemProvider.getActorSystem()).state().getLeader())) {
            LOG.info("I am leader, generating custom module-shards.conf");
            try {
                List<String> memberRoles = this.actorSystemConfig.getStringList("akka.cluster.roles");
                String data = generateModuleShardsForMembers(memberRoles);
                Files.write(Paths.get(MODULE_SHARDS_TMP_PATH), data.getBytes());
                this.moduleShardsConfig = MODULE_SHARDS_TMP_PATH;
                return true;
            } catch (Exception e) {
                LOG.info("Tmp module-shards.conf file was not created - error received {}", e.getMessage());
                return false;
            }
        }
        LOG.info("Using default module-shards.conf");
        return true;
    }

    /**
     * Wait for the cluster to form and then release the latch.
     *
     * @param latch
     * @return
     */
    private ListenableScheduledFuture getClusterLeaderElectionFuture(CountDownLatch latch) {
        ListeningScheduledExecutorService listeningScheduledExecutorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        return listeningScheduledExecutorService.scheduleAtFixedRate(() -> {
            if (Cluster.get(this.actorSystemProvider.getActorSystem()).state().getLeader() != null) {
                latch.countDown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public LightyServices getServices() {
        return this;
    }

    @Override
    public DiagStatusService getDiagStatusService() {
        return lightyDiagStatusService;
    }

    @Override
    public SystemReadyMonitor getSystemReadyMonitor() {
        return this.systemReadyMonitor;
    }

    @Override
    public LightySystemReadyService getLightySystemReadyService() {
        return this.systemReadyMonitor;
    }

    @Override
    public ActorSystemProvider getActorSystemProvider() {
        return this.actorSystemProvider;
    }

    @Override
    public SchemaContextProvider getSchemaContextProvider() {
        return this.moduleInfoBackedContext;
    }

    @Override
    public DOMSchemaService getDOMSchemaService() {
        return this.schemaServiceProvider;
    }

    @Override
    public DOMYangTextSourceProvider getDOMYangTextSourceProvider() {
        return this.schemaServiceProvider;
    }

    @Override
    public DOMNotificationSubscriptionListenerRegistry getDOMNotificationSubscriptionListenerRegistry() {
        return this.domNotificationRouter;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry
    getControllerDOMNotificationSubscriptionListenerRegistry() {
        return this.domNotificationRouterOld;
    }

    @Override
    public DistributedDataStoreInterface getConfigDatastore() {
        return this.configDatastore;
    }

    @Override
    public DistributedDataStoreInterface getOperationalDatastore() {
        return this.operDatastore;
    }

    @Override
    public DOMDataTreeShardingService getDOMDataTreeShardingService() {
        return this.distributedShardedDOMDataTree;
    }

    @Override
    public DOMDataTreeService getDOMDataTreeService() {
        return this.distributedShardedDOMDataTree;
    }

    @Override
    public DistributedShardFactory getDistributedShardFactory() {
        return this.distributedShardedDOMDataTree;
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return this.codec;
    }

    @Override
    public BindingCodecTreeFactory getBindingCodecTreeFactory() {
        return this.codec;
    }

    @Override
    public DOMEntityOwnershipService getDOMEntityOwnershipService() {
        return this.distributedEntityOwnershipService;
    }

    @Override
    public EntityOwnershipService getEntityOwnershipService() {
        return this.bindingDOMEntityOwnershipServiceAdapter;
    }

    @Override
    public ClusterAdminService getClusterAdminRPCService() {
        return this.clusterAdminRpcService;
    }

    @Override
    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return this.clusterSingletonServiceProvider;
    }

    @Override
    public EventExecutor getEventExecutor() {
        return this.eventExecutor;
    }

    @Override
    public EventLoopGroup getBossGroup() {
        return this.bossGroup;
    }

    @Override
    public EventLoopGroup getWorkerGroup() {
        return this.workerGroup;
    }

    @Override
    public ThreadPool getThreadPool() {
        return this.threadPool;
    }

    @Override
    public ScheduledThreadPool getScheduledThreaPool() {
        return this.scheduledThreadPool;
    }

    @Override
    public Timer getTimer() {
        return this.timer;
    }

    @Override
    public DOMMountPointService getDOMMountPointService() {
        return this.domMountPointService;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMMountPointService getControllerDOMMountPointService() {
        return this.domMountPointServiceOld;
    }

    @Override
    public DOMNotificationPublishService getDOMNotificationPublishService() {
        return this.domNotificationRouter;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService
    getControllerDOMNotificationPublishService() {
        return this.domNotificationRouterOld;
    }

    @Override
    public DOMNotificationService getDOMNotificationService() {
        return this.domNotificationRouter;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMNotificationService getControllerDOMNotificationService() {
        return this.domNotificationRouterOld;
    }

    @Override
    public DOMDataBroker getClusteredDOMDataBroker() {
        return this.concurrentDOMDataBroker;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getControllerClusteredDOMDataBroker() {
        return this.concurrentDOMDataBrokerOld;
    }

    @Override
    public DOMDataBroker getPingPongDataBroker() {
        return this.pingPongDataBroker;
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMDataBroker getControllerPingPongDataBroker() {
        return this.pingPongDataBrokerOld;
    }

    @Override
    public DOMRpcService getDOMRpcService() {
        return this.domRpcRouter.getRpcService();
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMRpcService getControllerDOMRpcService() {
        return this.domRpcRouterOld;
    }

    @Override
    public DOMRpcProviderService getDOMRpcProviderService() {
        return this.domRpcRouter.getRpcProviderService();
    }

    @Override
    public org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService getControllerDOMRpcProviderService() {
        return this.domRpcRouterOld;
    }

    @Override
    public RpcProviderService getRpcProviderService() {
        return this.rpcProviderService;
    }

    @Override
    public RpcProviderRegistry getControllerRpcProviderRegistry() {
        return this.rpcProviderRegistry;
    }

    @Override
    public MountPointService getBindingMountPointService() {
        return this.mountPointService;
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.MountPointService getControllerBindingMountPointService() {
        return this.mountPointServiceOld;
    }

    @Override
    public NotificationService getNotificationService() {
        return this.notificationService;
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.NotificationService getControllerBindingNotificationService() {
        return this.notificatoinServiceOld;
    }

    @Override
    public NotificationPublishService getBindingNotificationPublishService() {
        return this.notificationPublishService;
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.NotificationPublishService
    getControllerBindingNotificationPublishService() {
        return this.notificationPublishServiceOld;
    }

    @Override
    public NotificationProviderService getControllerNotificationProviderService() {
        return this.notificationProviderServiceOld;
    }

    @Override
    public DataBroker getBindingDataBroker() {
        return this.domDataBroker;
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.DataBroker getControllerBindingDataBroker() {
        return this.domDataBrokerOld;
    }

    @Override
    public DataBroker getBindingPingPongDataBroker() {
        return this.domPingPongDataBroker;
    }

    @Override
    public ObjectRegistration<YangModuleInfo> registerModuleInfo(final YangModuleInfo yangModuleInfo) {
        return moduleInfoBackedContext.registerModuleInfo(yangModuleInfo);
    }

    @Override
    public org.opendaylight.controller.md.sal.binding.api.DataBroker getControllerBindingPingPongDataBroker() {
        return this.domPingPongDataBrokerOld;
    }
}
