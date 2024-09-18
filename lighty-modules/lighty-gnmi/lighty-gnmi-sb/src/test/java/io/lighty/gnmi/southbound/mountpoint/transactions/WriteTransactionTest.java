/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.transactions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import com.google.gson.Gson;
import gnmi.Gnmi;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.device.connection.DeviceConnection;
import io.lighty.gnmi.southbound.device.session.listener.GnmiConnectionStatusListener;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.gnmi.southbound.mountpoint.broker.GnmiDataBroker;
import io.lighty.gnmi.southbound.mountpoint.codecs.YangInstanceIdentifierToPathCodec;
import io.lighty.gnmi.southbound.mountpoint.codecs.YangInstanceNormToGnmiUpdateCodec;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiGet;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiSet;
import io.lighty.gnmi.southbound.mountpoint.requests.GnmiSetRequestFactoryImpl;
import io.lighty.gnmi.southbound.schema.SchemaContextHolder;
import io.lighty.gnmi.southbound.schema.TestYangDataStoreService;
import io.lighty.gnmi.southbound.schema.impl.SchemaContextHolderImpl;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.impl.ByClassPathYangLoaderService;
import io.lighty.modules.gnmi.connector.gnmi.session.api.GnmiSession;
import io.lighty.modules.gnmi.connector.session.api.SessionProvider;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.skyscreamer.jsonassert.JSONAssert;

public class WriteTransactionTest {
    private static final QNameModule INTERFACES_MODULE_QN_MODULE = QNameModule.of(
            XMLNamespace.of("http://openconfig.net/yang/interfaces"), Revision.of("2021-04-06"));
    private static final String OPENCONFIG_GNMI_CONFIG = "/lightyconfigs/openconfig_gnmi_config.json";
    private static final QName INTERFACES_CONTAINER_QN = QName.create(INTERFACES_MODULE_QN_MODULE, "interfaces");
    private static final YangInstanceIdentifier TEST_PREPARE_DATASTORE_IID = YangInstanceIdentifier.builder()
            .node(INTERFACES_CONTAINER_QN)
            .build();
    private static final QName INTERFACE_LIST_QN = QName.create(INTERFACES_CONTAINER_QN, "interface");
    private static final QName NAME_QN = QName.create(INTERFACE_LIST_QN, "name");
    private static final QName CONFIG_CONTAINER_QN = QName.create(INTERFACE_LIST_QN, "config");
    private static final QName CONFIG_NAME_QN = QName.create(CONFIG_CONTAINER_QN, "name");
    private static final QName CONFIG_LOOPBACK_QN = QName.create(CONFIG_CONTAINER_QN, "loopback-mode");
    private static final long TIMEOUT_MILLIS = 30_000;
    private static final String NAME_KEY_VALUE = "NAME";
    private static final HashMap<QName, Object> INTERFACE_NAME_KEY = new HashMap<>() {{
            put(NAME_QN, NAME_KEY_VALUE);
        }};
    private static final YangInstanceIdentifier TEST_CONFIG_IID = YangInstanceIdentifier.builder()
            .node(INTERFACES_CONTAINER_QN)
            .node(INTERFACE_LIST_QN)
            .nodeWithKey(INTERFACE_LIST_QN, INTERFACE_NAME_KEY)
            .node(CONFIG_CONTAINER_QN)
            .build();
    private static final String EXPECTED_IETF_VALUE = "{\"name\":\"NAME\",\"loopback-mode\":true}";

    private GnmiDataBroker gnmiDataBroker;
    private GnmiSession gnmiSession;


    @BeforeEach
    public void startUp() throws YangLoadException, SchemaException, ConfigurationException {
        MockitoAnnotations.initMocks(this);
        this.gnmiSession = Mockito.mock(GnmiSession.class);
        Mockito.when(this.gnmiSession.set(any(Gnmi.SetRequest.class)))
                .thenReturn(FluentFutures.immediateFluentFuture(Gnmi.SetResponse.newBuilder().build()));
        final SessionProvider sessionProvider = Mockito.mock(SessionProvider.class);
        Mockito.when(sessionProvider.getGnmiSession()).thenReturn(gnmiSession);
        final Node node = new NodeBuilder().setNodeId(new NodeId("node")).build();
        final DeviceConnection deviceConnection = new DeviceConnection(sessionProvider,
                Mockito.mock(GnmiConnectionStatusListener.class), node);

        final GnmiConfiguration gnmiConfiguration = GnmiConfigUtils.getGnmiConfiguration(
                this.getClass().getResourceAsStream(OPENCONFIG_GNMI_CONFIG));
        Assertions.assertNotNull(gnmiConfiguration.getYangModulesInfo());
        final TestYangDataStoreService dataStoreService = new TestYangDataStoreService();
        final List<GnmiDeviceCapability> completeCapabilities
                = new ByClassPathYangLoaderService(gnmiConfiguration.getYangModulesInfo()).load(dataStoreService);

        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(dataStoreService, null);
        final EffectiveModelContext schemaContext = schemaContextHolder.getSchemaContext(completeCapabilities);
        deviceConnection.setSchemaContext(schemaContext);
        final YangInstanceIdentifierToPathCodec yiiToPathCodec
                = new YangInstanceIdentifierToPathCodec(deviceConnection, true);
        final GnmiSet gnmiSet = new GnmiSet(deviceConnection,
                new GnmiSetRequestFactoryImpl(yiiToPathCodec, new YangInstanceNormToGnmiUpdateCodec(deviceConnection,
                        yiiToPathCodec, new Gson())),
                deviceConnection.getIdentifier());
        this.gnmiDataBroker = new GnmiDataBroker(Mockito.mock(GnmiGet.class), gnmiSet);
    }

    @Test
    public void removePrepareReqFromListUpdateTest() throws ExecutionException, InterruptedException, TimeoutException,
            JSONException {
        final DOMDataTreeWriteTransaction writeTransaction = gnmiDataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, TEST_CONFIG_IID, getTestDataContainerNode());
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, TEST_PREPARE_DATASTORE_IID, getPrepareListNode());
        writeTransaction.commit().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        final ArgumentCaptor<Gnmi.SetRequest> setRequestArgumentCaptor = ArgumentCaptor.forClass(Gnmi.SetRequest.class);
        verify(gnmiSession).set(setRequestArgumentCaptor.capture());
        assertEquals(1, setRequestArgumentCaptor.getAllValues().size());
        Gnmi.SetRequest setRequest = setRequestArgumentCaptor.getValue();
        assertEquals(1, setRequest.getUpdateCount());
        Gnmi.Update update = setRequest.getUpdate(0);
        assertNotNull(update);
        final String jetfValue = update.getVal().getJsonIetfVal().toStringUtf8();
        assertFalse(jetfValue.isEmpty());
        JSONAssert.assertEquals(EXPECTED_IETF_VALUE, jetfValue, true);
    }

    @Test
    public void removePrepareReqFromAddListTest() throws ExecutionException, InterruptedException, TimeoutException,
            JSONException {
        final DOMDataTreeWriteTransaction writeTransaction = gnmiDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION,  TEST_CONFIG_IID, getTestDataContainerNode());
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, TEST_PREPARE_DATASTORE_IID, getPrepareListNode());
        writeTransaction.commit().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        final ArgumentCaptor<Gnmi.SetRequest> setRequestArgumentCaptor = ArgumentCaptor.forClass(Gnmi.SetRequest.class);
        verify(gnmiSession).set(setRequestArgumentCaptor.capture());
        assertEquals(1, setRequestArgumentCaptor.getAllValues().size());
        Gnmi.SetRequest setRequest = setRequestArgumentCaptor.getValue();
        assertEquals(0, setRequest.getUpdateCount());
        Gnmi.Update replace = setRequest.getReplace(0);
        assertNotNull(replace);
        final String jetfValue = replace.getVal().getJsonIetfVal().toStringUtf8();
        assertFalse(jetfValue.isEmpty());
        JSONAssert.assertEquals(EXPECTED_IETF_VALUE, jetfValue, true);
    }

    @Test
    public void addMultipleMergeRequest() throws ExecutionException, InterruptedException, TimeoutException {
        final DOMDataTreeWriteTransaction writeTransaction = gnmiDataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION,  TEST_CONFIG_IID, getTestDataContainerNode());
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, TEST_CONFIG_IID, getTestDataContainerNode());
        writeTransaction.commit().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        final ArgumentCaptor<Gnmi.SetRequest> setRequestArgumentCaptor = ArgumentCaptor.forClass(Gnmi.SetRequest.class);
        verify(gnmiSession).set(setRequestArgumentCaptor.capture());
        assertEquals(1, setRequestArgumentCaptor.getAllValues().size());
        Gnmi.SetRequest setRequest = setRequestArgumentCaptor.getValue();
        assertEquals(2, setRequest.getUpdateCount());
    }

    @Test
    public void removeEmptyPrepareReqFromUpdateListTest() throws ExecutionException, InterruptedException,
            TimeoutException, JSONException {
        final DOMDataTreeWriteTransaction writeTransaction = gnmiDataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, TEST_CONFIG_IID, getTestDataContainerNode());
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, TEST_PREPARE_DATASTORE_IID,
                getEmptyPrepareListNode());
        writeTransaction.commit().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        final ArgumentCaptor<Gnmi.SetRequest> setRequestArgumentCaptor = ArgumentCaptor.forClass(Gnmi.SetRequest.class);
        verify(gnmiSession).set(setRequestArgumentCaptor.capture());
        assertEquals(1, setRequestArgumentCaptor.getAllValues().size());
        Gnmi.SetRequest setRequest = setRequestArgumentCaptor.getValue();
        assertEquals(1, setRequest.getUpdateCount());
        Gnmi.Update update = setRequest.getUpdate(0);
        assertNotNull(update);
        final String jetfValue = update.getVal().getJsonIetfVal().toStringUtf8();
        assertFalse(jetfValue.isEmpty());
        JSONAssert.assertEquals(EXPECTED_IETF_VALUE, jetfValue, true);
    }


    private static ContainerNode getTestDataContainerNode() {
        final LeafNode<String> configName = ImmutableNodes.leafNode(
                YangInstanceIdentifier.NodeIdentifier.create(CONFIG_NAME_QN), NAME_KEY_VALUE);
        final LeafNode<Boolean> loopbackNode = ImmutableNodes.leafNode(
                YangInstanceIdentifier.NodeIdentifier.create(CONFIG_LOOPBACK_QN), true);
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(CONFIG_CONTAINER_QN))
                .withChild(configName)
                .withChild(loopbackNode)
                .build();
    }

    private static ContainerNode getPrepareListNode() {
        final LeafNode<String> node = ImmutableNodes.leafNode(
                YangInstanceIdentifier.NodeIdentifier.create(NAME_QN), NAME_KEY_VALUE);
        final MapEntryNode name = ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifierWithPredicates
                        .of(INTERFACE_LIST_QN, NAME_QN, NAME_KEY_VALUE))
                .withChild(node)
                .build();
        final MapNode mapNode = ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(INTERFACE_LIST_QN))
                .withChild(name)
                .build();
        return ImmutableNodes.newContainerBuilder()
                .withChild(mapNode)
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(INTERFACES_CONTAINER_QN))
                .build();
    }

    private static ContainerNode getEmptyPrepareListNode() {
        final MapNode build = ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(INTERFACE_LIST_QN))
                .build();
        return ImmutableNodes.newContainerBuilder()
                .withChild(build)
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(INTERFACES_CONTAINER_QN))
                .build();
    }
}
