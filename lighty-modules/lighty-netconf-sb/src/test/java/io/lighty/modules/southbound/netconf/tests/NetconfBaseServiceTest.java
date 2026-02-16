/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.lighty.modules.southbound.netconf.impl.NetconfBaseService;
import io.lighty.modules.southbound.netconf.impl.NetconfBaseServiceImpl;
import java.util.Collection;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.netconf.api.EffectiveOperation;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformer;
import org.opendaylight.netconf.databind.DatabindContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

class NetconfBaseServiceTest extends NetconfBaseServiceBaseTest {

    private static final QName QNAME_BASE =
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601
                    .YangModuleInfoImpl.getInstance().getName();
    private static final QName RUNNING_DATASTORE =
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601
                    .get.config.input.source.config.source.Running.QNAME;

    @Test
    void testBaseServiceGetMock() {
        YangInstanceIdentifier yangInstanceId = YangInstanceIdentifier.builder()
                .node(NetconfState.QNAME)
                .node(Schemas.QNAME)
                .node(Schema.QNAME)
                .nodeWithKey(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1")
                .build();

        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);

        baseService.get(Optional.of(yangInstanceId));
        ArgumentCaptor<ContainerNode> capturedNN = ArgumentCaptor.forClass(ContainerNode.class);

        Mockito.verify(domRpcService, times(1))
                .invokeRpc(any(QName.class), capturedNN.capture());

        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild> children =
                ((ContainerNode) capturedNN.getValue()).body();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 1);
        assertTrue(hasSpecificChild(children, "filter"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(DatabindContext.ofModel(
            effectiveModelContext), true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(
                NetconfMessageTransformUtil.NETCONF_GET_NODEID.getNodeType(), capturedNN.getValue());
        Element getElement =
                getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(), QNAME_BASE, "get");
        assertNotNull(getElement);
        Element filter = getSpecificElementSubtree(getElement, QNAME_BASE, "filter");
        assertNotNull(filter);
        assertNotNull(getSpecificElementSubtree(filter, NetconfState.QNAME, "netconf-state"));
    }

    @Test
    void testBaseServiceGetConfigMock() {
        YangInstanceIdentifier yangInstanceId = YangInstanceIdentifier.builder()
                .node(NetconfState.QNAME)
                .node(Schemas.QNAME)
                .node(Schema.QNAME)
                .nodeWithKey(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1")
                .build();

        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);


        baseService.getConfig(RUNNING_DATASTORE, Optional.of(yangInstanceId));

        ArgumentCaptor<ContainerNode> capturedNN = ArgumentCaptor.forClass(ContainerNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(any(QName.class), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild> children =
                ((ContainerNode) capturedNN.getValue()).body();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 2);
        assertTrue(hasSpecificChild(children, "source"));
        assertTrue(hasSpecificChild(children, "filter"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(DatabindContext.ofMountPoint(
            mountContext), true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(NetconfMessageTransformUtil.NETCONF_GET_CONFIG_NODEID
                        .getNodeType(), capturedNN.getValue());
        Element getElement =
                getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(), QNAME_BASE, "get-config");
        assertNotNull(getElement);
        Element filter = getSpecificElementSubtree(getElement, QNAME_BASE, "filter");
        assertNotNull(filter);
        assertNotNull(getSpecificElementSubtree(filter, NetconfState.QNAME, "netconf-state"));

        Element sourceElement = getSpecificElementSubtree(getElement, QNAME_BASE, "source");
        assertNotNull(sourceElement);
        assertNotNull(getSpecificElementSubtree(sourceElement, QNAME_BASE, "running"));
    }

    @Test
    void testBaseServiceEditConfigMock() {
        YangInstanceIdentifier yangInstanceId = YangInstanceIdentifier.builder()
                .node(NetconfState.QNAME)
                .node(Schemas.QNAME)
                .node(Schema.QNAME)
                .nodeWithKey(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1")
                .build();

        final String editVersionValue = "test_version_x_x_x";

        MapEntryNode schema = ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifierWithPredicates
                        .of(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1"))
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(NodeIdentifier.create(QName.create(Schema.QNAME, "version")))
                        .withValue(editVersionValue)
                        .build())
                .build();

        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);
        baseService.editConfig(RUNNING_DATASTORE, Optional.of(schema), yangInstanceId,
                Optional.of(EffectiveOperation.MERGE), Optional.of(EffectiveOperation.CREATE), true);

        ArgumentCaptor<ContainerNode> capturedNN = ArgumentCaptor.forClass(ContainerNode.class);
        Mockito.verify(domRpcService, times(1)).invokeRpc(
                any(QName.class), capturedNN.capture());

        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild> children =
                ((ContainerNode) capturedNN.getValue()).body();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 4);
        assertTrue(hasSpecificChild(children, "target"));
        assertTrue(hasSpecificChild(children, "default-operation"));
        assertTrue(hasSpecificChild(children, "edit-content"));
        assertTrue(hasSpecificChild(children, "error-option"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(DatabindContext.ofMountPoint(
            mountContext), true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(
                NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_NODEID.getNodeType(), capturedNN.getValue());
        Element editData =
                getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(), QNAME_BASE, "edit-config");
        assertNotNull(editData);
        assertNotNull(getSpecificElementSubtree(editData, QNAME_BASE, "target"));
        assertNotNull(getSpecificElementSubtree(editData, QNAME_BASE, "default-operation"));
        assertNotNull(getSpecificElementSubtree(editData, QNAME_BASE, "error-option"));
        Element config = getSpecificElementSubtree(editData, QNAME_BASE, "config");
        assertNotNull(config);
        final Element netconfStateElement = getSpecificElementSubtree(config, NetconfState.QNAME, "netconf-state");
        assertNotNull(netconfStateElement);
        final Element schemasElement = getSpecificElementSubtree(netconfStateElement, NetconfState.QNAME, "schemas");
        assertNotNull(schemasElement);
        final Element schemaElement = getSpecificElementSubtree(schemasElement, NetconfState.QNAME, "schema");
        assertNotNull(schemaElement);
        final Element versionElement = getSpecificElementSubtree(schemaElement, NetconfState.QNAME, "version");
        assertEquals(versionElement.getTextContent(), editVersionValue);
    }
}
