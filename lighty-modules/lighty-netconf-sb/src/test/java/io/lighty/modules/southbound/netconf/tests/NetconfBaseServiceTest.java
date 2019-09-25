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
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

public class NetconfBaseServiceTest extends NetconfBaseServiceBaseTest {

    private static final QName QNAME_BASE =
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl.getInstance().getName();
    private static final QName RUNNING_DATASTORE = org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.get.config.input.source.config.source.Running.QNAME;

    @Test
    public void testBaseServiceGetMock() {
        YangInstanceIdentifier YII = YangInstanceIdentifier.builder()
                .node(NetconfState.QNAME)
                .node(Schemas.QNAME)
                .node(Schema.QNAME)
                .nodeWithKey(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1")
                .build();

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true);
        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService, schemaContext);

        baseService.get(Optional.of(YII));

        ArgumentCaptor<SchemaPath> capturedSchemaPath = ArgumentCaptor.forClass(SchemaPath.class);
        ArgumentCaptor<NormalizedNode> capturedNN = ArgumentCaptor.forClass(NormalizedNode.class);
        Mockito.verify(domRpcService, times(1)).invokeRpc(capturedSchemaPath.capture(), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                ((ContainerNode) capturedNN.getValue()).getValue();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 1);
        assertTrue(hasSpecificChild(children, "filter"));

        NetconfMessage netconfMessage = transformer.toRpcRequest(capturedSchemaPath.getValue(), capturedNN.getValue());
        Element getElement = getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(),
                QNAME_BASE.getNamespace().toString(), "get");
        assertNotNull(getElement);
        Element filter = getSpecificElementSubtree(getElement, QNAME_BASE.getNamespace().toString(), "filter");
        assertNotNull(filter);
        assertNotNull(getSpecificElementSubtree(filter, NetconfState.QNAME.getNamespace().toString(), "netconf-state"));
    }

    @Test
    public void testBaseServiceGetConfigMock() {
        YangInstanceIdentifier YII = YangInstanceIdentifier.builder()
                .node(NetconfState.QNAME)
                .node(Schemas.QNAME)
                .node(Schema.QNAME)
                .nodeWithKey(Schema.QNAME, QName.create(Schema.QNAME, "identifier"), "listkeyvalue1")
                .build();

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true);
        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService, schemaContext);


        baseService.getConfig(RUNNING_DATASTORE, Optional.of(YII));

        ArgumentCaptor<SchemaPath> capturedSchemaPath = ArgumentCaptor.forClass(SchemaPath.class);
        ArgumentCaptor<NormalizedNode> capturedNN = ArgumentCaptor.forClass(NormalizedNode.class);
        Mockito.verify(domRpcService, times(1)).invokeRpc(capturedSchemaPath.capture(), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                ((ContainerNode) capturedNN.getValue()).getValue();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 2);
        assertTrue(hasSpecificChild(children, "source"));
        assertTrue(hasSpecificChild(children, "filter"));

        NetconfMessage netconfMessage = transformer.toRpcRequest(capturedSchemaPath.getValue(), capturedNN.getValue());
        Element getElement = getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(),
                QNAME_BASE.getNamespace().toString(), "get-config");
        assertNotNull(getElement);
        Element filter = getSpecificElementSubtree(getElement, QNAME_BASE.getNamespace().toString(), "filter");
        assertNotNull(filter);
        assertNotNull(getSpecificElementSubtree(filter, NetconfState.QNAME.getNamespace().toString(), "netconf-state"));

        Element sourceElement = getSpecificElementSubtree(getElement, QNAME_BASE.getNamespace().toString(), "source");
        assertNotNull(sourceElement);
        assertNotNull(getSpecificElementSubtree(sourceElement, QNAME_BASE.getNamespace().toString(), "running"));
    }

    @Test
    public void testBaseServiceEditConfigMock() {
        YangInstanceIdentifier YII = YangInstanceIdentifier.builder()
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

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true);
        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfBaseService baseService = new NetconfBaseServiceImpl(new NodeId("node1"), domRpcService, schemaContext);
        baseService.editConfig(RUNNING_DATASTORE, Optional.of(schema), YII, Optional.of(ModifyAction.MERGE),
                Optional.of(ModifyAction.CREATE), true);

        ArgumentCaptor<SchemaPath> capturedSchemaPath = ArgumentCaptor.forClass(SchemaPath.class);
        ArgumentCaptor<NormalizedNode> capturedNN = ArgumentCaptor.forClass(NormalizedNode.class);
        Mockito.verify(domRpcService, times(1)).invokeRpc(capturedSchemaPath.capture(), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                ((ContainerNode) capturedNN.getValue()).getValue();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 4);
        assertTrue(hasSpecificChild(children, "target"));
        assertTrue(hasSpecificChild(children, "default-operation"));
        assertTrue(hasSpecificChild(children, "edit-content"));
        assertTrue(hasSpecificChild(children, "error-option"));
        NetconfMessage netconfMessage = transformer.toRpcRequest(capturedSchemaPath.getValue(), capturedNN.getValue());
        Element editData = getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(),
                QNAME_BASE.getNamespace().toString(), "edit-config");
        assertNotNull(editData);
        assertNotNull(getSpecificElementSubtree(editData, QNAME_BASE.getNamespace().toString(), "target"));
        assertNotNull(getSpecificElementSubtree(editData, QNAME_BASE.getNamespace().toString(), "default-operation"));
        assertNotNull(getSpecificElementSubtree(editData, QNAME_BASE.getNamespace().toString(), "error-option"));
        Element config = getSpecificElementSubtree(editData, QNAME_BASE.getNamespace().toString(), "config");
        assertNotNull(config);
        final Element netconfStateElement = getSpecificElementSubtree(config, NetconfState.QNAME.getNamespace().toString(), "netconf-state");
        assertNotNull(netconfStateElement);
        final Element schemasElement = getSpecificElementSubtree(netconfStateElement, NetconfState.QNAME.getNamespace().toString(), "schemas");
        assertNotNull(schemasElement);
        final Element schemaElement = getSpecificElementSubtree(schemasElement, NetconfState.QNAME.getNamespace().toString(), "schema");
        assertNotNull(schemaElement);
        final Element versionElement = getSpecificElementSubtree(schemaElement, NetconfState.QNAME.getNamespace().toString(), "version");
        assertEquals(versionElement.getTextContent(), editVersionValue);
    }
}
