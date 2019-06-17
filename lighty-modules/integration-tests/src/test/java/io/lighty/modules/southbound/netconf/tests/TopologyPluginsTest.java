/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.netty.util.concurrent.Future;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.netconf.client.NetconfClientDispatcher;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.Credentials;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPasswordBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class TopologyPluginsTest {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyPluginsTest.class);

    private LightyController lightyController;
    private CommunityRestConf restConf;
    private LightyModule netconfPlugin;
    @Mock
    private NetconfClientDispatcher dispatcher;
    @Mock
    private Future<Void> initFuture;

    private static LightyModule startSingleNodeNetconf(final LightyServices services,
                                                       final NetconfClientDispatcher dispatcher)
            throws ConfigurationException {

            final NetconfConfiguration config = NetconfConfigUtils.createDefaultNetconfConfiguration();
            NetconfConfigUtils.injectServicesToConfig(config);
            config.setClientDispatcher(dispatcher);
            return new NetconfTopologyPluginBuilder()
                    .from(config, services)
                    .build();
    }

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        MockitoAnnotations.initMocks(this);
        when(this.dispatcher.createReconnectingClient(any())).thenReturn(this.initFuture);

        this.lightyController = LightyTestUtils.startController();

        this.restConf = LightyTestUtils.startRestconf(this.lightyController.getServices());
        this.netconfPlugin = startSingleNodeNetconf(this.lightyController.getServices(), this.dispatcher);
        this.netconfPlugin.start();
    }

    @AfterClass
    public void afterClass() throws Exception {
        if (this.netconfPlugin != null) {
            LOG.info("Shutting down Netconf topology Plugin");
            final ListenableFuture<Boolean> shutdown = this.netconfPlugin.shutdown();
            shutdown.get();
        }
        if (this.restConf != null) {
            LOG.info("Shutting down CommunityRestConf");
            final ListenableFuture<Boolean> shutdown = this.restConf.shutdown();
            shutdown.get();
            Thread.sleep(3_000);
        }
        if (this.lightyController != null) {
            LOG.info("Shutting down LightyController");
            final ListenableFuture<Boolean> shutdown = this.lightyController.shutdown();
            shutdown.get();
            Thread.sleep(1_000);
        }
    }

    @Test
    public void testMountDevice() throws Exception {
        final NodeId nodeId = new NodeId("device1");
        final Credentials loginPassword = new LoginPasswordBuilder()
                .setUsername("user1")
                .setPassword("password1")
                .build();
        final NetconfNode netconfNode = new NetconfNodeBuilder()
                .setHost(new Host(new IpAddress(new Ipv4Address("10.10.8.8"))))
                .setPort(new PortNumber(17830))
                .setCredentials(loginPassword)
                .setReconnectOnChangedSchema(true)
                .setTcpOnly(false)
                .build();
        final NodeKey nodeKey = new NodeKey(nodeId);
        final Node node = new NodeBuilder()
                .setNodeId(nodeId)
                .addAugmentation(NetconfNode.class, netconfNode)
                .build();
        final InstanceIdentifier<Node> path = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("topology-netconf")))
                .child(Node.class, nodeKey);
        final DataBroker bindingDataBroker = this.lightyController.getServices().getControllerBindingDataBroker();
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, path, node);
        writeTransaction.submit().get();
        verify(this.dispatcher, timeout(20000)).createReconnectingClient(any());
    }

}
