/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.bgp.deployer;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.bgp.config.InitialBgpConfigLoader;
import io.netty.channel.EventLoopGroup;
import java.util.List;
import org.opendaylight.bgpcep.bgp.topology.provider.config.BgpTopologyDeployerImpl;
import org.opendaylight.bgpcep.bgp.topology.provider.config.Ipv4TopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.config.Ipv6TopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.config.LinkstateGraphProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.config.LinkstateTopologyProvider;
import org.opendaylight.graph.impl.ConnectedGraphServer;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.DefaultBGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.SimpleStatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.StatementActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.openconfig.spi.DefaultBGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.DefaultBGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPNettyGroups;
import org.opendaylight.protocol.bgp.rib.impl.ConstantCodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.config.DefaultBgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPStateCollector;
import org.opendaylight.protocol.bgp.rib.spi.DefaultRIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.state.StateProviderImpl;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingCodecContext;
import org.opendaylight.yangtools.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpModule extends AbstractLightyModule {
    private static final Logger LOG = LoggerFactory.getLogger(BgpModule.class);
    private static final String DEFAULT_BGP_NETWORK_INSTANCE_NAME = "global-bgp";

    //Basic BGP
    private final SimpleStatementRegistry simpleStatementRegistry;
    private final BGPNettyGroups bgpNettyGroups;
    private final DefaultBgpDeployer bgpDeployer;
    private final StateProviderImpl stateProvider;
    private final StrictBGPPeerRegistry peerRegistry;
    private final InitialBgpConfigLoader initialConfigLoader;
    private final BgpTopologyDeployerImpl bgpTopologyDeployer;

    //Topologies
    private final Ipv4TopologyProvider ipv4TopologyProvider;
    private final Ipv6TopologyProvider ipv6TopologyProvider;
    private final LinkstateTopologyProvider linkstateTopologyProvider;
    //Linkstate graph
    private final ConnectedGraphServer graphServer;
    private final LinkstateGraphProvider linkstateGraphProvider;

    public BgpModule(final EffectiveModelContext modelContext, final DataBroker dataBroker,
            final DOMDataBroker domDataBroker, final BindingCodecTree codecTree, final RpcProviderService rpcProvider,
            final ClusterSingletonServiceProvider cssProvider, final BindingNormalizedNodeSerializer serializer,
            final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        initialConfigLoader = new InitialBgpConfigLoader(domDataBroker, modelContext);
        peerRegistry = new StrictBGPPeerRegistry();
        bgpNettyGroups = new BGPNettyGroups();
        final var bgpDispatcher = new BGPDispatcherImpl(createBgpExtensions(), bgpNettyGroups, peerRegistry);
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
        //Topologies
        bgpTopologyDeployer = new BgpTopologyDeployerImpl(dataBroker, cssProvider);
        ipv4TopologyProvider = new Ipv4TopologyProvider(bgpTopologyDeployer);
        ipv6TopologyProvider = new Ipv6TopologyProvider(bgpTopologyDeployer);
        linkstateTopologyProvider = new LinkstateTopologyProvider(bgpTopologyDeployer);
        graphServer = new ConnectedGraphServer(dataBroker);
        linkstateGraphProvider = new LinkstateGraphProvider(bgpTopologyDeployer, graphServer);
    }

    public BgpModule(final EffectiveModelContext modelContext, final DataBroker dataBroker,
            final DOMDataBroker domDataBroker, final BindingRuntimeContext runtimeContext,
            final RpcProviderService rpcProvider, final ClusterSingletonServiceProvider cssProvider,
            final BindingNormalizedNodeSerializer serializer,
            final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(modelContext, dataBroker, domDataBroker, new BindingCodecContext(runtimeContext),
                rpcProvider, cssProvider, serializer, bossGroup, workerGroup);
    }

    public BgpModule(final LightyServices lightyServices) {
        this(lightyServices.getDOMSchemaService().getGlobalContext(),
                lightyServices.getBindingDataBroker(), lightyServices.getClusteredDOMDataBroker(),
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
            closeSuccess = false;
        }
        try {
            peerRegistry.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP peer registry", e);
            closeSuccess = false;
        }
        try {
            bgpNettyGroups.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP Netty groups", e);
            closeSuccess = false;
        }
        try {
            bgpDeployer.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP deployer", e);
            closeSuccess = false;
        }
        if (!closeTopologies()) {
            closeSuccess = false;
        }
        try {
            bgpTopologyDeployer.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP topology deployer", e);
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

    @SuppressWarnings({"checkstyle:illegalCatch"})
    private boolean closeTopologies() {
        boolean closeSuccess = true;
        try {
            ipv4TopologyProvider.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP IPV4 topology provider", e);
            closeSuccess = false;
        }
        try {
            ipv6TopologyProvider.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP IPV6 topology provider", e);
            closeSuccess = false;
        }
        try {
            linkstateGraphProvider.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP link state graph provider", e);
            closeSuccess = false;
        }
        try {
            graphServer.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP graph server", e);
            closeSuccess = false;
        }
        try {
            linkstateTopologyProvider.close();
        } catch (Exception e) {
            LOG.warn("Failed to stop BGP link state topology provider", e);
            closeSuccess = false;
        }
        return closeSuccess;
    }

    private static RIBExtensionConsumerContext createRibExtensions(final BindingNormalizedNodeSerializer serializer) {
        return new DefaultRIBExtensionConsumerContext(serializer, List.of(
                new org.opendaylight.protocol.bgp.inet.RIBActivator(),
                new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.RIBActivator(),
                new org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator()));
    }

    private static BGPExtensionConsumerContext createBgpExtensions() {
        return new DefaultBGPExtensionConsumerContext(List.of(
                new org.opendaylight.protocol.bgp.inet.BGPActivator(),
                new org.opendaylight.protocol.bgp.parser.impl.BGPActivator(),
                new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.BGPActivator(),
                new org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator()));
    }

    private static BGPTableTypeRegistryConsumer createBgpTableTypes() {
        return new DefaultBGPTableTypeRegistryConsumer(List.of(
                new org.opendaylight.protocol.bgp.inet.TableTypeActivator(),
                new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.TableTypeActivator(),
                new org.opendaylight.protocol.bgp.linkstate.impl.TableTypeActivator()));
    }

    private static SimpleStatementRegistry createStatementRegistry(final DataBroker dataBroker) {
        return new SimpleStatementRegistry(
                List.of(new StatementActivator(dataBroker),
                        new org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.StatementActivator()));
    }

}
