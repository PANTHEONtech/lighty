package io.lighty.modules.bgp.deployer;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.bgp.config.InitialBgpConfigLoader;
import io.netty.channel.EventLoopGroup;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.DefaultBGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.SimpleStatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.StatementActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.openconfig.spi.DefaultBGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.DefaultBGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.ConstantCodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.config.DefaultBgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPStateCollector;
import org.opendaylight.protocol.bgp.rib.spi.DefaultRIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.state.StateProviderImpl;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpModule extends AbstractLightyModule {
    private static final Logger LOG = LoggerFactory.getLogger(BgpModule.class);
    private static final String DEFAULT_BGP_NETWORK_INSTANCE_NAME = "global-bgp";

    private final SimpleStatementRegistry simpleStatementRegistry;
    private final BGPDispatcherImpl bgpDispatcher;
    private final DefaultBgpDeployer bgpDeployer;
    private final StateProviderImpl stateProvider;
    private final StrictBGPPeerRegistry peerRegistry;
    private final InitialBgpConfigLoader initialConfigLoader;

    public BgpModule(final EffectiveModelContext modelContext, final DataBroker dataBroker,
            final DOMDataBroker domDataBroker, final BindingCodecTree codecTree, final RpcProviderService rpcProvider,
            final ClusterSingletonServiceProvider cssProvider, final BindingNormalizedNodeSerializer serializer,
            final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        initialConfigLoader = new InitialBgpConfigLoader(domDataBroker, modelContext);
        peerRegistry = new StrictBGPPeerRegistry();
        bgpDispatcher = new BGPDispatcherImpl(createBgpExtensions(), bossGroup, workerGroup, peerRegistry);
        simpleStatementRegistry = createStatementRegistry(dataBroker);
        final DefaultBGPRibRoutingPolicyFactory routingPolicyFactory = new DefaultBGPRibRoutingPolicyFactory(
                dataBroker, simpleStatementRegistry);
        final ConstantCodecsRegistry codecsRegistry = new ConstantCodecsRegistry(codecTree);
        final BGPTableTypeRegistryConsumer tableTypeRegistryConsumer = createBgpTableTypes();
        final BGPStateCollector stateCollector = new BGPStateCollector();
        final RIBExtensionConsumerContext ribExtensionConsumerContext = createRibExtensions(serializer);
        stateProvider = new StateProviderImpl(dataBroker, 5, tableTypeRegistryConsumer, stateCollector,
                DEFAULT_BGP_NETWORK_INSTANCE_NAME);
        bgpDeployer = new DefaultBgpDeployer(DEFAULT_BGP_NETWORK_INSTANCE_NAME, cssProvider, rpcProvider,
                ribExtensionConsumerContext, bgpDispatcher, routingPolicyFactory, codecsRegistry, domDataBroker,
                dataBroker, tableTypeRegistryConsumer, stateCollector);
    }

    public BgpModule(final EffectiveModelContext modelContext, final DataBroker dataBroker,
            final DOMDataBroker domDataBroker, final BindingCodecTreeFactory codecTreeFactory,
            final BindingRuntimeContext runtimeContext, final RpcProviderService rpcProvider,
            final ClusterSingletonServiceProvider cssProvider, final BindingNormalizedNodeSerializer serializer,
            final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(modelContext, dataBroker, domDataBroker, codecTreeFactory.create(runtimeContext),
                rpcProvider, cssProvider, serializer, bossGroup, workerGroup);
    }

    public BgpModule(final LightyServices lightyServices) {
        this(lightyServices.getEffectiveModelContextProvider().getEffectiveModelContext(),
                lightyServices.getBindingDataBroker(), lightyServices.getClusteredDOMDataBroker(),
                lightyServices.getBindingCodecTreeFactory(),
                lightyServices.getAdapterContext().currentSerializer().getRuntimeContext(),
                lightyServices.getRpcProviderService(), lightyServices.getClusterSingletonServiceProvider(),
                lightyServices.getBindingNormalizedNodeSerializer(), lightyServices.getBossGroup(),
                lightyServices.getWorkerGroup());
    }


    @Override
    @SuppressWarnings({"checkstyle:illegalCatch"})
    protected boolean initProcedure() {
        try {
            //Load initial routing policies into datastore
            initialConfigLoader.init();
            simpleStatementRegistry.start();
            bgpDeployer.init();
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to initialize BGPModule", e);
            return false;
        }
    }

    @Override
    @SuppressWarnings({"checkstyle:illegalCatch"})
    protected boolean stopProcedure() {
        boolean closeSuccess = true;
        try {
            simpleStatementRegistry.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP statement registry", e);
            return false;
        }
        try {
            peerRegistry.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP peer registry", e);
            closeSuccess = false;
        }
        try {
            bgpDispatcher.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP dispatcher", e);
            closeSuccess = false;
        }
        try {
            bgpDeployer.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP deployer", e);
            closeSuccess = false;
        }
        try {
            stateProvider.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP state provider", e);
            closeSuccess = false;
        }
        return closeSuccess;
    }

    private RIBExtensionConsumerContext createRibExtensions(final BindingNormalizedNodeSerializer serializer) {
        return new DefaultRIBExtensionConsumerContext(serializer, List.of(
                new org.opendaylight.protocol.bgp.inet.RIBActivator(),
                new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.RIBActivator(),
                new org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator()));
    }

    private BGPExtensionConsumerContext createBgpExtensions() {
        return new DefaultBGPExtensionConsumerContext(List.of(
                new org.opendaylight.protocol.bgp.inet.BGPActivator(),
                new org.opendaylight.protocol.bgp.parser.impl.BGPActivator(),
                new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.BGPActivator(),
                new org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator()));
    }

    private BGPTableTypeRegistryConsumer createBgpTableTypes() {
        return new DefaultBGPTableTypeRegistryConsumer(List.of(
                new org.opendaylight.protocol.bgp.inet.TableTypeActivator(),
                new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.TableTypeActivator(),
                new org.opendaylight.protocol.bgp.linkstate.impl.TableTypeActivator()));
    }

    private SimpleStatementRegistry createStatementRegistry(final DataBroker dataBroker) {
        return new SimpleStatementRegistry(
                List.of(new StatementActivator(dataBroker),
                        new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.StatementActivator()));
    }

}
