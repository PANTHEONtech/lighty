/*
 * Copyright (c) 2019 Pantheon.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

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
import org.opendaylight.netconf.api.ModifyAction;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.testng.annotations.Test;
import org.w3c.dom.Element;


public class NetconfBaseServiceTest extends NetconfBaseServiceBaseTest {

    private static final QName QNAME_BASE =
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601
                    .$YangModuleInfoImpl.getInstance().getName();
    private static final QName RUNNING_DATASTORE =
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601
                    .get.config.input.source.config.source.Running.QNAME;

    @Test
    public void testBaseServiceGetMock() {
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

        ArgumentCaptor<QName> capturedQname = ArgumentCaptor.forClass(QName.class);
        ArgumentCaptor<NormalizedNode> capturedNN = ArgumentCaptor.forClass(NormalizedNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(capturedQname.capture(), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                ((ContainerNode) capturedNN.getValue()).getValue();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 1);
        assertTrue(hasSpecificChild(children, "filter"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(capturedQname.getValue(), capturedNN.getValue());
        Element getElement =
                getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(), QNAME_BASE, "get");
        assertNotNull(getElement);
        Element filter = getSpecificElementSubtree(getElement, QNAME_BASE, "filter");
        assertNotNull(filter);
        assertNotNull(getSpecificElementSubtree(filter, NetconfState.QNAME, "netconf-state"));
    }

    @Test
    public void testBaseServiceGetConfigMock() {
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

        ArgumentCaptor<QName> capturedQname = ArgumentCaptor.forClass(QName.class);
        ArgumentCaptor<NormalizedNode> capturedNN = ArgumentCaptor.forClass(NormalizedNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(capturedQname.capture(), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                ((ContainerNode) capturedNN.getValue()).getValue();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 2);
        assertTrue(hasSpecificChild(children, "source"));
        assertTrue(hasSpecificChild(children, "filter"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(capturedQname.getValue(), capturedNN.getValue());
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
    public void testBaseServiceEditConfigMock() {
        YangInstanceIdentifier yangInstanceId = YangInstanceIdentifier.builder()
                .node(NetconfState.QNAME)
                .node(Schemas.QNAME)
                .node(Schema.QNAME)
                .nodeWithKey(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1")
                .build();

        final String editVersionValue = "test_version_x_x_x";

        MapEntryNode schema = Builders.mapEntryBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifierWithPredicates
                        .of(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1"))
                .withChild(ImmutableLeafNodeBuilder.create()
                        .withNodeIdentifier(NodeIdentifier.create(QName.create(Schema.QNAME, "version")))
                        .withValue(editVersionValue)
                        .build())
                .build();

        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);
        baseService.editConfig(RUNNING_DATASTORE, Optional.of(schema), yangInstanceId, Optional.of(ModifyAction.MERGE),
                Optional.of(ModifyAction.CREATE), true);

        ArgumentCaptor<QName> capturedQname = ArgumentCaptor.forClass(QName.class);
        ArgumentCaptor<NormalizedNode> capturedNN = ArgumentCaptor.forClass(NormalizedNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(capturedQname.capture(), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                ((ContainerNode) capturedNN.getValue()).getValue();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 4);
        assertTrue(hasSpecificChild(children, "target"));
        assertTrue(hasSpecificChild(children, "default-operation"));
        assertTrue(hasSpecificChild(children, "edit-content"));
        assertTrue(hasSpecificChild(children, "error-option"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(capturedQname.getValue(), capturedNN.getValue());
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
