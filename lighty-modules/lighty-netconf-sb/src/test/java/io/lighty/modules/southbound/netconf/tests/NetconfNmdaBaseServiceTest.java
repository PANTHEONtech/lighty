/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import static io.lighty.modules.southbound.netconf.impl.NetconfNmdaBaseServiceImpl.NETCONF_EDIT_DATA_QNAME;
import static io.lighty.modules.southbound.netconf.impl.NetconfNmdaBaseServiceImpl.NETCONF_GET_DATA_QNAME;
import static io.lighty.modules.southbound.netconf.impl.NetconfNmdaBaseServiceImpl.NETCONF_NMDA_EXTENSION_QNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.lighty.modules.southbound.netconf.impl.NetconfNmdaBaseServiceImpl;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.netconf.api.EffectiveOperation;
import org.opendaylight.netconf.api.messages.NetconfMessage;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastores.rev180214.Operational;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastores.rev180214.Running;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.origin.rev180214.Dynamic;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.origin.rev180214.Learned;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

public class NetconfNmdaBaseServiceTest extends NetconfBaseServiceBaseTest {
    private static final Map<QName, Object> TEST_SCHEMA_KEYS = Map.of(
            QName.create(Schema.QNAME, "identifier"), "listkeyvalue1",
            QName.create(Schema.QNAME, "version"), "versionValue",
            QName.create(Schema.QNAME, "format"), "formatValue");
    private static final YangInstanceIdentifier TEST_SCHEMA_YIID = YangInstanceIdentifier.builder()
            .node(NetconfState.QNAME)
            .node(Schemas.QNAME)
            .node(Schema.QNAME)
            .nodeWithKey(Schema.QNAME, TEST_SCHEMA_KEYS)
            .build();

    @Test
    public void testBaseServiceGetDataMock() {
        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfNmdaBaseServiceImpl baseService = new NetconfNmdaBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);

        baseService.getData(Operational.QNAME, Optional.of(TEST_SCHEMA_YIID), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        ArgumentCaptor<ContainerNode> capturedNN = ArgumentCaptor.forClass(ContainerNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(any(QName.class), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild> children =
                ((ContainerNode) capturedNN.getValue()).body();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 2);
        assertTrue(hasSpecificChild(children, "datastore"));
        assertTrue(hasSpecificChild(children, "filter-spec"));

        Optional<DataContainerChild> filter = children.stream()
                .filter(child -> child.name().getNodeType().getLocalName().equals("filter-spec")).findAny();
        assertTrue(filter.isPresent());

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(NETCONF_GET_DATA_QNAME, capturedNN.getValue());
        Element getData = getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(),
                NETCONF_GET_DATA_QNAME, NETCONF_GET_DATA_QNAME.getLocalName());
        assertNotNull(getData);
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "datastore"));
        Element subtreeFilter = getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "subtree-filter");
        assertNotNull(subtreeFilter);
        assertNotNull(getSpecificElementSubtree(subtreeFilter, NetconfState.QNAME, "netconf-state"));
    }

    @Test
    public void testBaseServiceGetDataFullMock() {
        DOMRpcService domRpcService = mock(DOMRpcService.class);

        Set<QName> originFilter = new HashSet<>();
        originFilter.add(Learned.QNAME);
        originFilter.add(Dynamic.QNAME);

        NetconfNmdaBaseServiceImpl baseService = new NetconfNmdaBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);
        baseService.getData(Operational.QNAME, Optional.of(TEST_SCHEMA_YIID), Optional.of(true), Optional.of(42),
                Optional.of(originFilter), Optional.empty(), Optional.of(true));

        ArgumentCaptor<ContainerNode> capturedNN = ArgumentCaptor.forClass(ContainerNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(any(QName.class), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild> children =
                ((ContainerNode) capturedNN.getValue()).body();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 6);
        assertTrue(hasSpecificChild(children, "datastore"));
        assertTrue(hasSpecificChild(children, "filter-spec"));
        assertTrue(hasSpecificChild(children, "with-origin"));
        assertTrue(hasSpecificChild(children, "max-depth"));
        assertTrue(hasSpecificChild(children, "origin-filters"));
        assertTrue(hasSpecificChild(children, "config-filter"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(NETCONF_GET_DATA_QNAME, capturedNN.getValue());
        Element getData = getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(),
                NETCONF_GET_DATA_QNAME, NETCONF_GET_DATA_QNAME.getLocalName());
        assertNotNull(getData);
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "datastore"));
        Element subtreeFilter = getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "subtree-filter");
        assertNotNull(subtreeFilter);
        assertNotNull(getSpecificElementSubtree(subtreeFilter, NetconfState.QNAME, "netconf-state"));
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "config-filter"));
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "origin-filter"));
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "origin-filter", 1));
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "max-depth"));
        assertNotNull(getSpecificElementSubtree(getData, NETCONF_NMDA_EXTENSION_QNAME, "with-origin"));
    }

    @Test
    public void testBaseServiceEditDataMock() {
        MapEntryNode schema = Builders.mapEntryBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifierWithPredicates
                        .of(Schema.QNAME, TEST_SCHEMA_KEYS))
                .build();

        DOMRpcService domRpcService = mock(DOMRpcService.class);

        NetconfNmdaBaseServiceImpl baseService = new NetconfNmdaBaseServiceImpl(new NodeId("node1"), domRpcService,
                effectiveModelContext);
        baseService.editData(Running.QNAME, Optional.of(schema), TEST_SCHEMA_YIID,
                Optional.of(EffectiveOperation.MERGE),
                Optional.of(EffectiveOperation.CREATE));

        ArgumentCaptor<ContainerNode> capturedNN = ArgumentCaptor.forClass(ContainerNode.class);
        Mockito.verify(domRpcService, times(1))
                .invokeRpc(any(QName.class), capturedNN.capture());
        assertTrue(capturedNN.getValue() instanceof ContainerNode);
        Collection<DataContainerChild> children =
                ((ContainerNode) capturedNN.getValue()).body();
        assertFalse(children.isEmpty());
        assertEquals(children.size(), 3);
        assertTrue(hasSpecificChild(children, "datastore"));
        assertTrue(hasSpecificChild(children, "default-operation"));
        assertTrue(hasSpecificChild(children, "edit-content"));

        NetconfMessageTransformer transformer = new NetconfMessageTransformer(mountContext, true, baseSchema);
        NetconfMessage netconfMessage = transformer.toRpcRequest(NETCONF_EDIT_DATA_QNAME, capturedNN.getValue());
        Element editData = getSpecificElementSubtree(netconfMessage.getDocument().getDocumentElement(),
                NETCONF_EDIT_DATA_QNAME, NETCONF_EDIT_DATA_QNAME.getLocalName());
        assertNotNull(editData);
        assertNotNull(getSpecificElementSubtree(editData, NETCONF_NMDA_EXTENSION_QNAME, "datastore"));
        assertNotNull(getSpecificElementSubtree(editData, NETCONF_NMDA_EXTENSION_QNAME, "default-operation"));
        Element config = getSpecificElementSubtree(editData, NETCONF_NMDA_EXTENSION_QNAME, "config");
        assertNotNull(config);
        assertNotNull(getSpecificElementSubtree(config, NetconfState.QNAME, "netconf-state"));
    }
}
