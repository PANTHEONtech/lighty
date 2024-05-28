/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl;

import akka.actor.Terminated;
import akka.management.javadsl.AkkaManagement;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Futures;
import com.typesafe.config.Config;
import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.core.cluster.ClusteringHandler;
import io.lighty.core.cluster.ClusteringHandlerProvider;
import io.lighty.core.common.SocketAnalyzer;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.config.ControllerConfiguration.InitialConfigData;
import io.lighty.core.controller.impl.services.LightyDiagStatusServiceImpl;
import io.lighty.core.controller.impl.services.LightySystemReadyMonitorImpl;
import io.lighty.core.controller.impl.services.LightySystemReadyService;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.akka.impl.ActorSystemProviderImpl;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextIntrospector;
import org.opendaylight.controller.cluster.datastore.DatastoreContextPropertiesUpdater;
import org.opendaylight.controller.cluster.datastore.DatastoreSnapshotRestore;
import org.opendaylight.controller.cluster.datastore.DefaultDatastoreContextIntrospectorFactory;
import org.opendaylight.controller.cluster.datastore.DefaultDatastoreSnapshotRestore;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.HybridModuleShardConfigProvider;
import org.opendaylight.controller.eos.akka.AkkaEntityOwnershipService;
import org.opendaylight.controller.remote.rpc.RemoteOpsProvider;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderConfig;
import org.opendaylight.controller.remote.rpc.RemoteOpsProviderFactory;
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
import org.opendaylight.mdsal.binding.dom.adapter.BindingAdapterFactory;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMMountPointServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.dom.codec.impl.di.DefaultBindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeTypes;
import org.opendaylight.mdsal.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.api.ModuleInfoSnapshot;
import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotResolver;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.binding.dom.adapter.DefaultEntityOwnershipService;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.impl.EOSClusterSingletonServiceProvider;
import org.opendaylight.netconf.yanglib.writer.YangLibraryWriterSingleton;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.repo.api.MissingSchemaSourceException;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightyControllerImpl extends AbstractLightyModule implements LightyController, LightyServices {

    private static final Logger LOG = LoggerFactory.getLogger(LightyControllerImpl.class);
    private static final int ACTOR_SYSTEM_TERMINATE_TIMEOUT = 30;

    private final Config actorSystemConfig;
    private final ClassLoader actorSystemClassLoader;
    private final DOMNotificationRouter domNotificationRouter;
    private final DOMMountPointService domMountPointService;
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

    private Configuration clusterConfiguration;
    private ActorSystemProviderImpl actorSystemProvider;
    private DatastoreSnapshotRestore datastoreSnapshotRestore;
    private AbstractDataStore configDatastore;
    private AbstractDataStore operDatastore;
    private ExecutorService listenableFutureExecutor;
    private DurationStatisticsTracker commitStatsTracker;
    private DOMDataBroker concurrentDOMDataBroker;
    private DOMRpcRouter domRpcRouter;
    private RemoteOpsProvider remoteOpsProvider;
    private DOMActionService domActionService;
    private DOMActionProviderService domActionProviderService;
    private AkkaEntityOwnershipService akkaEntityOwnershipService;
    private DefaultEntityOwnershipService defaultEntityOwnershipService;
    private ClusterAdminRpcService clusterAdminRpcService;
    private EOSClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private NotificationService notificationService;
    private NotificationPublishService notificationPublishService;
    private DataBroker dataBroker;
    private final LightyDiagStatusServiceImpl lightyDiagStatusService;
    private EventExecutor eventExecutor;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduledThreadPool;
    private Timer timer;
    private ModuleInfoSnapshot moduleInfoSnapshot;
    private ModuleInfoSnapshotResolver snapshotResolver;
    private DOMSchemaService schemaService;
    private ConstantAdapterContext codec;
    private BindingCodecTreeFactory bindingCodecTreeFactory;
    private DefaultYangParserFactory yangParserFactory;
    private BindingAdapterFactory bindingAdapterFactory;
    private RpcProviderService rpcProviderService;
    private MountPointService mountPointService;
    private ActionService actionService;
    private ActionProviderService actionProviderService;
    private final LightySystemReadyMonitorImpl systemReadyMonitor;
    private List<Registration> modelsRegistration = new ArrayList<>();
    private AkkaManagement akkaManagement;
    private Optional<ClusteringHandler> clusteringHandler;
    private Optional<InitialConfigData> initialConfigData;
    private RpcService rpcConsumerRegistry;
    private YangLibraryWriterSingleton yangLibraryWriter;


    public LightyControllerImpl(final ExecutorService executorService, final Config actorSystemConfig,
                                final ClassLoader actorSystemClassLoader,
                                final ControllerConfiguration.DOMNotificationRouterConfig domNotificationRouterConfig,
                                final String restoreDirectoryPath, final int maxDataBrokerFutureCallbackQueueSize,
                                final int maxDataBrokerFutureCallbackPoolSize, final boolean metricCaptureEnabled,
                                final int mailboxCapacity, final Properties distributedEosProperties,
                                final String moduleShardsConfig,
                                final String modulesConfig, final DatastoreContext configDatastoreContext,
                                final DatastoreContext operDatastoreContext,
                                final Map<String, Object> datastoreProperties,
                                final Set<YangModuleInfo> modelSet,
                                final @Nullable InitialConfigData initialConfigData) {
        super(executorService);
        initSunXMLWriterProperty();
        this.actorSystemConfig = actorSystemConfig;
        this.actorSystemClassLoader = actorSystemClassLoader;
        this.domMountPointService = new DOMMountPointServiceImpl();
        this.domNotificationRouter = new DOMNotificationRouter(domNotificationRouterConfig.getQueueDepth());
        this.restoreDirectoryPath = restoreDirectoryPath;
        this.maxDataBrokerFutureCallbackQueueSize = maxDataBrokerFutureCallbackQueueSize;
        this.maxDataBrokerFutureCallbackPoolSize = maxDataBrokerFutureCallbackPoolSize;
        this.metricCaptureEnabled = metricCaptureEnabled;
        this.mailboxCapacity = mailboxCapacity;
        this.distributedEosProperties = distributedEosProperties;
        this.modulesConfig = modulesConfig;
        this.clusterConfiguration = new ConfigurationImpl(moduleShardsConfig, modulesConfig);
        this.configDatastoreContext = configDatastoreContext;
        this.operDatastoreContext = operDatastoreContext;
        this.datastoreProperties = datastoreProperties;
        this.modelSet = modelSet;
        this.systemReadyMonitor = new LightySystemReadyMonitorImpl();
        this.lightyDiagStatusService = new LightyDiagStatusServiceImpl(systemReadyMonitor);
        this.initialConfigData = Optional.ofNullable(initialConfigData);
    }

    /**
     * This method replace property of writer from implementation of woodstox writer to internal sun writer.
     */
    private static void initSunXMLWriterProperty() {
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
    }

    @Override
    protected boolean initProcedure() {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        //INIT actor system provider
        this.actorSystemProvider = new ActorSystemProviderImpl(this.actorSystemClassLoader,
                QuarantinedMonitorActor.props(() -> { }), this.actorSystemConfig);

        this.akkaManagement = AkkaManagement.get(actorSystemProvider.getActorSystem());
        akkaManagement.start();

        //INIT cluster bootstrap
        this.clusteringHandler = ClusteringHandlerProvider.getClusteringHandler(actorSystemProvider,
                this.actorSystemConfig);
        this.clusteringHandler.ifPresent(handler -> {
            handler.initClustering();
            if (handler.getModuleShardsConfig().isPresent()) {
                final HybridModuleShardConfigProvider shardConfigProvider = new HybridModuleShardConfigProvider(
                        handler.getModuleShardsConfig().get(), this.modulesConfig);
                this.clusterConfiguration = new ConfigurationImpl(shardConfigProvider);
            }
        });

        this.datastoreSnapshotRestore = new DefaultDatastoreSnapshotRestore(this.restoreDirectoryPath);

        // INIT yang parser factory
        final YangXPathParserFactory xpathFactory = new AntlrXPathParserFactory();
        this.yangParserFactory = new DefaultYangParserFactory(xpathFactory);

        //INIT schema context
        this.snapshotResolver = new ModuleInfoSnapshotResolver("binding-dom-codec", yangParserFactory);
        this.modelsRegistration = snapshotResolver.registerModuleInfos(modelSet);
        this.moduleInfoSnapshot = snapshotResolver.takeSnapshot();
        this.schemaService = new FixedDOMSchemaService(() -> moduleInfoSnapshot.modelContext(), sourceId -> {
            try {
                return Futures.immediateFuture(moduleInfoSnapshot.getYangTextSource(sourceId));
            } catch (MissingSchemaSourceException e) {
                return Futures.immediateFailedFuture(e);
            }
        });

        // INIT CODEC FACTORY

        final BindingRuntimeGenerator bindingRuntimeGenerator = new DefaultBindingRuntimeGenerator();
        final BindingRuntimeTypes bindingRuntimeTypes = bindingRuntimeGenerator
                .generateTypeMapping(moduleInfoSnapshot.modelContext());
        final DefaultBindingRuntimeContext bindingRuntimeContext
                = new DefaultBindingRuntimeContext(bindingRuntimeTypes, moduleInfoSnapshot);

        this.bindingCodecTreeFactory = new DefaultBindingCodecTreeFactory();

        final BindingCodecContext bindingCodecContext = new BindingCodecContext(bindingRuntimeContext);
        this.codec = new ConstantAdapterContext(bindingCodecContext);

        // CONFIG DATASTORE
        this.configDatastore = prepareDataStore(this.configDatastoreContext, this.clusterConfiguration,
                this.schemaService, this.datastoreSnapshotRestore, this.actorSystemProvider);
        // OPERATIONAL DATASTORE
        this.operDatastore = prepareDataStore(this.operDatastoreContext, this.clusterConfiguration,
                this.schemaService, this.datastoreSnapshotRestore, this.actorSystemProvider);

        createConcurrentDOMDataBroker();

        this.domRpcRouter = DOMRpcRouter.newInstance(this.schemaService);
        this.domActionProviderService = domRpcRouter.actionProviderService();
        this.domActionService = domRpcRouter.actionService();
        createRemoteOpsProvider();

        this.bindingAdapterFactory = new BindingAdapterFactory(getAdapterContext());
        this.actionProviderService = this.bindingAdapterFactory.createActionProviderService(
                this.domActionProviderService);
        this.actionService = bindingAdapterFactory.createActionService(this.domActionService);
        rpcConsumerRegistry = bindingAdapterFactory.createRpcService(domRpcRouter.rpcService());

        this.rpcProviderService = new BindingDOMRpcProviderServiceAdapter(this.codec,
                this.domRpcRouter.rpcProviderService());

        // ENTITY OWNERSHIP
        try {
            this.akkaEntityOwnershipService = new AkkaEntityOwnershipService(this.actorSystemProvider,
                    this.rpcProviderService, bindingCodecContext);
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Exception occurred while creating AkkaEntityOwnershipService", e);
            Thread.currentThread().interrupt();
            return false;
        }

        this.defaultEntityOwnershipService = new DefaultEntityOwnershipService(
                akkaEntityOwnershipService, this.codec);
        this.clusterAdminRpcService =
                new ClusterAdminRpcService(this.configDatastore, this.operDatastore, this.akkaEntityOwnershipService);

        this.clusterSingletonServiceProvider =
                new EOSClusterSingletonServiceProvider(this.akkaEntityOwnershipService);

        //create binding mount point service
        this.mountPointService = new BindingDOMMountPointServiceAdapter(this.codec, this.domMountPointService);

        this.notificationService = new BindingDOMNotificationServiceAdapter(
                this.codec, this.domNotificationRouter.notificationService());
        this.notificationPublishService = new BindingDOMNotificationPublishServiceAdapter(
                this.codec, this.domNotificationRouter.notificationPublishService());

        //create data broker
        this.dataBroker = bindingAdapterFactory.createDataBroker(concurrentDOMDataBroker);

        this.clusteringHandler.ifPresent(handler -> handler.start(rpcConsumerRegistry));

        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.eventExecutor = new DefaultEventExecutor();
        this.timer = new HashedWheelTimer();
        this.threadPool =
                Executors.newFixedThreadPool(2, new DefaultThreadFactory("default-pool"));
        this.scheduledThreadPool =
                new ScheduledThreadPoolExecutor(2, new DefaultThreadFactory("default-scheduled-pool"));
        this.yangLibraryWriter = new YangLibraryWriterSingleton(clusterSingletonServiceProvider, schemaService,
                dataBroker, true);
        yangLibraryWriter.instantiateServiceInstance();

        if (this.initialConfigData.isPresent()) {
            final InitialConfigData initialData = this.initialConfigData.get();
            try (InputStream inputStream = initialData.getAsInputStream()) {
                FileToDatastoreUtils.importConfigDataFile(inputStream, initialData.getFormat(),
                        moduleInfoSnapshot.modelContext(), this.getClusteredDOMDataBroker(), true);
            } catch (TimeoutException | ExecutionException | IOException | DeserializationException e) {
                LOG.error("Exception occurred while importing config data from file", e);
                return false;
            } catch (InterruptedException e) {
                LOG.error("Interrupted while importing config data from file", e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        LOG.info("Lighty controller started in {}", stopwatch.stop());
        return true;
    }

    private AbstractDataStore prepareDataStore(final DatastoreContext datastoreContext,
            final Configuration configuration, final DOMSchemaService domSchemaService,
            final DatastoreSnapshotRestore newDatastoreSnapshotRestore,
            final ActorSystemProvider newActorSystemProvider) {
        final DefaultDatastoreContextIntrospectorFactory introspectorFactory
                = new DefaultDatastoreContextIntrospectorFactory(this.codec.currentSerializer());
        final DatastoreContextIntrospector introspector = introspectorFactory
                .newInstance(datastoreContext.getLogicalStoreType(), datastoreProperties);
        final DatastoreContextPropertiesUpdater updater = new DatastoreContextPropertiesUpdater(introspector,
                datastoreProperties);
        return DistributedDataStoreFactory.createInstance(domSchemaService, datastoreContext,
                newDatastoreSnapshotRestore, newActorSystemProvider, introspector, updater, configuration);
    }

    @Override
    protected boolean stopProcedure() throws InterruptedException, ExecutionException {
        LOG.debug("Lighty Controller stopProcedure");
        if (this.timer != null) {
            this.timer.stop();
        }
        if (this.threadPool != null) {
            this.threadPool.shutdown();
        }
        if (this.scheduledThreadPool != null) {
            this.scheduledThreadPool.shutdown();
        }
        if (this.clusterSingletonServiceProvider != null) {
            this.clusterSingletonServiceProvider.close();
        }
        boolean stopSuccessful = true;
        if (this.akkaEntityOwnershipService != null) {
            try {
                this.akkaEntityOwnershipService.close();
            } catch (ExecutionException e) {
                LOG.error("Closing akka AkkaEntityOwnershipService failed!", e);
                stopSuccessful = false;
            }
        }
        if (this.listenableFutureExecutor != null) {
            this.listenableFutureExecutor.shutdown();
        }
        if (this.operDatastore != null) {
            this.operDatastore.close();
        }
        if (this.configDatastore != null) {
            this.configDatastore.close();
        }
        if (this.remoteOpsProvider != null) {
            this.remoteOpsProvider.close();
        }
        if (this.domNotificationRouter != null) {
            this.domNotificationRouter.close();
        }

        modelsRegistration.forEach(Registration::close);

        if (this.akkaManagement != null) {
            this.akkaManagement.stop();
        }
        if (this.actorSystemProvider != null) {

            final CompletableFuture<Terminated> actorSystemTerminatedFuture = this.actorSystemProvider
                    .getActorSystem()
                    .getWhenTerminated().toCompletableFuture();
            final int actorSystemPort = this.actorSystemConfig.getInt("akka.remote.artery.canonical.port");

            try {
                this.actorSystemProvider.close();
            } catch (TimeoutException e) {
                LOG.error("Closing akka ActorSystemProvider timed out!", e);
                stopSuccessful = false;
            }

            try {
                actorSystemTerminatedFuture.get(ACTOR_SYSTEM_TERMINATE_TIMEOUT, TimeUnit.SECONDS);
                SocketAnalyzer.awaitPortAvailable(actorSystemPort, ACTOR_SYSTEM_TERMINATE_TIMEOUT, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                LOG.error("Actor system port {} not released in last {} {}", actorSystemPort,
                        ACTOR_SYSTEM_TERMINATE_TIMEOUT, TimeUnit.SECONDS, e);
                stopSuccessful = false;
            }
        }
        return stopSuccessful;
    }

    private void createRemoteOpsProvider() {
        final RemoteOpsProviderConfig remoteOpsProviderConfig = RemoteOpsProviderConfig.newInstance(
                this.actorSystemProvider.getActorSystem().name(), this.metricCaptureEnabled, this.mailboxCapacity);
        this.remoteOpsProvider = RemoteOpsProviderFactory.createInstance(this.domRpcRouter.rpcProviderService(),
                this.domRpcRouter.rpcService(), this.actorSystemProvider.getActorSystem(), remoteOpsProviderConfig,
                this.domActionProviderService, this.domActionService);
        this.remoteOpsProvider.start();
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
    public DOMSchemaService getDOMSchemaService() {
        return this.schemaService;
    }

    @Override
    public DOMSchemaService.YangTextSourceExtension getYangTextSourceExtension() {
        return getDOMSchemaService().findExtension(DOMSchemaService.YangTextSourceExtension.class).orElse(null);
    }

    @Override
    public DOMNotificationRouter getDOMNotificationRouter() {
        return this.domNotificationRouter;
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
    public YangParserFactory getYangParserFactory() {
        return yangParserFactory;
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return this.codec.currentSerializer();
    }

    @Override
    public BindingCodecTreeFactory getBindingCodecTreeFactory() {
        return this.bindingCodecTreeFactory;
    }

    @Override
    public DOMEntityOwnershipService getDOMEntityOwnershipService() {
        return this.akkaEntityOwnershipService;
    }

    @Override
    public EntityOwnershipService getEntityOwnershipService() {
        return this.defaultEntityOwnershipService;
    }

    @Override
    public ClusterAdminRpcService getClusterAdminRPCService() {
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
    public ExecutorService getThreadPool() {
        return this.threadPool;
    }

    @Override
    public ScheduledExecutorService getScheduledThreadPool() {
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
    public DOMNotificationPublishService getDOMNotificationPublishService() {
        return domNotificationRouter.notificationPublishService();
    }

    @Override
    public DOMNotificationService getDOMNotificationService() {
        return domNotificationRouter.notificationService();
    }

    @Override
    public DOMDataBroker getClusteredDOMDataBroker() {
        return this.concurrentDOMDataBroker;
    }

    @Override
    public DOMRpcService getDOMRpcService() {
        return this.domRpcRouter.rpcService();
    }

    @Override
    public DOMRpcProviderService getDOMRpcProviderService() {
        return this.domRpcRouter.rpcProviderService();
    }

    @Override
    public RpcProviderService getRpcProviderService() {
        return this.rpcProviderService;
    }

    @Override
    public MountPointService getBindingMountPointService() {
        return this.mountPointService;
    }

    @Override
    public NotificationService getNotificationService() {
        return this.notificationService;
    }

    @Override
    public NotificationPublishService getBindingNotificationPublishService() {
        return this.notificationPublishService;
    }

    @Override
    public DataBroker getBindingDataBroker() {
        return this.dataBroker;
    }

    @Override
    public ConstantAdapterContext getAdapterContext() {
        return codec;
    }

    @Override
    public ActionProviderService getActionProviderService() {
        return actionProviderService;
    }

    @Override
    public RpcService getRpcConsumerRegistry() {
        return rpcConsumerRegistry;
    }

    @Override
    public ActionService getActionService() {
        return actionService;
    }

    @Override
    public List<Registration> registerModuleInfos(
            Iterable<? extends YangModuleInfo> yangModuleInfos) {
        return this.snapshotResolver.registerModuleInfos(yangModuleInfos);
    }

    @Override
    public DOMActionService getDOMActionService() {
        return domActionService;
    }

    @Override
    public DOMActionProviderService getDOMActionProviderService() {
        return domActionProviderService;
    }
}
