/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.gnmi.test.gnmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.aaa.encrypt.service.impl.AAAEncryptionServiceImpl;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import io.lighty.gnmi.southbound.lightymodule.GnmiSouthboundModule;
import io.lighty.gnmi.southbound.lightymodule.GnmiSouthboundModuleBuilder;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev240202.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.Keystore;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.KeystoreKey;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ConnectionParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ExtensionsParameters;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ExtensionsParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.extensions.parameters.GnmiParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeState;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.SecurityChoice;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnly;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnlyBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.GnmiYangModels;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.ModuleVersionType;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModel;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModelKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

public class GnmiWithoutRestconfTest {
    private static final String INITIAL_JSON_DATA_PATH = "src/test/resources/json/initData";
    private static final String TEST_SCHEMA_PATH = "src/test/resources/additional/models";
    private static final String SIMULATOR_CONFIG = "/json/simulator_config.json";
    private static final Path CONFIGURATION_PATH = Path.of("src/test/resources/json/app_init_config.json");
    private static final Duration POLL_INTERVAL_DURATION = Duration.ofMillis(1_000L);
    private static final Duration WAIT_TIME_DURATION = Duration.ofMillis(10_000L);
    public static final long TIMEOUT_MILLIS = 30_000;
    private static final String GNMI_NODE_ID = "gnmiNodeId";
    private static final String DEVICE_ADDRESS = "127.0.0.1";
    private static final int DEVICE_PORT = 3333;
    private static final String FIRST_VALUE = "first";
    private static final String SECOND_VALUE = "second";
    private static final String THIRD_VALUE = "third";
    private static final String CERT_ID = "cert_id";
    private static final String CA_VALUE = "CA_VALUE";
    private static final String CLIENT_CERT = "CLIENT_CERT";
    private static final String CLIENT_KEY = "CLIENT_KEY";
    private static final String PASSPHRASE = "PASSPHRASE";
    private static final String YANG_BODY = "YANG_BODY";
    private static final String YANG_NAME = "YANG_NAME";
    private static final String YANG_VERSION = "YANG_VERSION";
    private static final QNameModule INERFACE_QNAME_MODULE
            = QNameModule.of(XMLNamespace.of("http://openconfig.net/yang/interfaces"), Revision.of("2021-04-06"));
    private static final QName INTERFACES_QNAME = QName.create(INERFACE_QNAME_MODULE, "interfaces");
    private static final QNameModule TEST_MODULE_QN_MODULE = QNameModule.of(XMLNamespace.of("test:model"));
    private static final QName TEST_DATA_CONTAINER_QN = QName.create(TEST_MODULE_QN_MODULE, "test-data");
    private static final QName TEST_LEAF_LIST_QN = QName.create(TEST_DATA_CONTAINER_QN, "test-leaf-list");

    private static final QNameModule CERT_STORAGE_QN_MODULE
            = QNameModule.of(XMLNamespace.of("urn:lighty:gnmi:certificate:storage"), Revision.of("2021-05-04"));
    private static final QName ADD_KEYSTORE_RPC_QN = QName.create(CERT_STORAGE_QN_MODULE, "add-keystore-certificate");
    private static final QName ADD_KEYSTORE_INPUT_QN = QName.create(ADD_KEYSTORE_RPC_QN, "input");
    private static final QName KEYSTORE_ID_QN = QName.create(CERT_STORAGE_QN_MODULE, "keystore-id");
    private static final QName CA_CERT_QN = QName.create(CERT_STORAGE_QN_MODULE, "ca-certificate");
    private static final QName CLIENT_KEY_QN = QName.create(CERT_STORAGE_QN_MODULE, "client-key");
    private static final QName PASSPHRASE_QN = QName.create(CERT_STORAGE_QN_MODULE, "passphrase");
    private static final QName CLIENT_CERT_QN = QName.create(CERT_STORAGE_QN_MODULE, "client-cert");

    private static final QNameModule YANG_STORAGE_QN_MODULE
            = QNameModule.of(XMLNamespace.of("urn:lighty:gnmi:yang:storage"), Revision.of("2021-03-31"));
    private static final QName UPLOAD_YANG_RPC_QN = QName.create(YANG_STORAGE_QN_MODULE, "upload-yang-model");
    private static final QName UPLOAD_YANG_INPUT_QN = QName.create(UPLOAD_YANG_RPC_QN, "input");
    private static final QName GNMI_YANG_MODELS_QN = QName.create(YANG_STORAGE_QN_MODULE, "gnmi-yang-models");
    private static final QName GNMI_YANG_MODEL_QN = QName.create(GNMI_YANG_MODELS_QN, "gnmi-yang-model");
    private static final QName YANG_NAME_QN = QName.create(GNMI_YANG_MODEL_QN, "name");
    private static final QName YANG_VERSION_QN = QName.create(GNMI_YANG_MODEL_QN, "version");
    private static final QName YANG_BODY_QN = QName.create(GNMI_YANG_MODEL_QN, "body");

    private static LightyController lightyController;
    private static GnmiSouthboundModule gnmiSouthboundModule;
    private static SimulatedGnmiDevice gnmiDevice;

    @BeforeAll
    public static void startUp() throws ConfigurationException, ExecutionException, InterruptedException, IOException,
            NoSuchAlgorithmException, InvalidKeySpecException, TimeoutException,
            EffectiveModelContextBuilderException {
        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(Files.newInputStream(CONFIGURATION_PATH)))
                .build();
        Boolean controllerStartSuccessfully = lightyController.start().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
        assertTrue(controllerStartSuccessfully);

        gnmiSouthboundModule = new GnmiSouthboundModuleBuilder()
                .withConfig(GnmiConfigUtils.getGnmiConfiguration(Files.newInputStream(CONFIGURATION_PATH)))
                .withLightyServices(lightyController.getServices())
                .withExecutorService(Executors.newCachedThreadPool())
                .withEncryptionService(createEncryptionService())
                .build();
        Boolean gnmiStartSuccessfully = gnmiSouthboundModule.start().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
        assertTrue(gnmiStartSuccessfully);

        gnmiDevice = getUnsecureGnmiDevice(DEVICE_ADDRESS, DEVICE_PORT);
        gnmiDevice.start();
    }

    @AfterAll
    public static void tearDown() {
        gnmiDevice.stop();
        assertTrue(gnmiSouthboundModule.shutdown(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        assertTrue(lightyController.shutdown(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCrudOperation() throws ExecutionException, InterruptedException, TimeoutException {
        final DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBroker();
        //Write device to data-store
        final Node testGnmiNode = createNode(GNMI_NODE_ID, DEVICE_ADDRESS, DEVICE_PORT, getInsecureSecurityChoice());
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<Node> nodeInstanceIdentifier = IdentifierUtils.gnmiNodeIID(testGnmiNode.getNodeId());
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodeInstanceIdentifier, testGnmiNode);
        writeTransaction.commit().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);

        //Verify that device is connected
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    final Optional<Node> node = readOperData(bindingDataBroker, nodeInstanceIdentifier);
                    assertTrue(node.isPresent());
                    final Node foundNode = node.get();
                    final GnmiNode gnmiNode = foundNode.augmentation(GnmiNode.class);
                    assertNotNull(gnmiNode);
                    final NodeState nodeState = gnmiNode.getNodeState();
                    assertNotNull(nodeState);
                    final NodeState.NodeStatus nodeStatus = nodeState.getNodeStatus();
                    assertEquals(NodeState.NodeStatus.READY, nodeStatus);
                });

        //Get gnmi DOMDataBroker
        final DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();
        final Optional<DOMMountPoint> mountPoint
                = domMountPointService.getMountPoint(IdentifierUtils.nodeidToYii(testGnmiNode.getNodeId()));
        assertTrue(mountPoint.isPresent());
        final DOMMountPoint domMountPoint = mountPoint.get();
        final Optional<DOMDataBroker> service = domMountPoint.getService(DOMDataBroker.class);
        assertTrue(service.isPresent());
        final DOMDataBroker domDataBroker = service.get();

        //GET Interfaces
        final YangInstanceIdentifier interfacesYIID = YangInstanceIdentifier.builder().node(INTERFACES_QNAME).build();
        final Optional<NormalizedNode> normalizedNode = readDOMConfigData(domDataBroker, interfacesYIID);
        assertTrue(normalizedNode.isPresent());
        assertEquals(INTERFACES_QNAME, normalizedNode.get().name().getNodeType());

        //SET data
        final YangInstanceIdentifier testLeafListYIID = YangInstanceIdentifier.builder()
                .node(TEST_DATA_CONTAINER_QN).build();
        final ContainerNode testDataContainerNode = getTestDataContainerNode();
        writeDOMConfigData(domDataBroker, testLeafListYIID, testDataContainerNode);

        //GET created data
        final Optional<NormalizedNode> createdContainer = readDOMConfigData(domDataBroker, testLeafListYIID);
        assertTrue(createdContainer.isPresent());
        assertEquals(TEST_DATA_CONTAINER_QN, createdContainer.get().name().getNodeType());

        //UPDATE data
        final ContainerNode updateTestDataContainerNode = getUpdateTestDataContainerNode();
        updateDOMConfigData(domDataBroker, testLeafListYIID, updateTestDataContainerNode);

        //GET updated data
        final Optional<NormalizedNode> updatedContainer = readDOMConfigData(domDataBroker, testLeafListYIID);
        assertTrue(updatedContainer.isPresent());
        assertEquals(TEST_DATA_CONTAINER_QN, updatedContainer.get().name().getNodeType());
        assertTrue(updatedContainer.get() instanceof ContainerNode);
        ContainerNode containerNode = (ContainerNode) updatedContainer.get();
        assertEquals(1, containerNode.body().toArray().length);
        assertTrue(containerNode.body().toArray()[0] instanceof LeafSetNode);
        LeafSetNode<?> leafSetNode = (LeafSetNode) containerNode.body().toArray()[0];
        assertEquals(3, leafSetNode.body().size());
        List<String> list = Arrays.asList(FIRST_VALUE, SECOND_VALUE, THIRD_VALUE);
        for (Object object : leafSetNode.body()) {
            assertTrue(object instanceof LeafSetEntryNode);
            assertTrue(list.contains(((LeafSetEntryNode<String>) object).body()));
        }

        //DELETE created data
        deleteDOMConfigData(domDataBroker, testLeafListYIID);

        //GET deleted data
        final Optional<NormalizedNode> removedLeafListNN = readDOMConfigData(domDataBroker, testLeafListYIID);
        assertFalse(removedLeafListNN.isPresent());

        //Remove device after test
        try {
            deleteConfigData(bindingDataBroker, nodeInstanceIdentifier);
            deleteOperData(bindingDataBroker, nodeInstanceIdentifier);
        } catch (ExecutionException | InterruptedException e) {
            Assertions.fail("Failed to remove device data from gNMI", e);
        }
        //Verify that device is already removed from data store
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    final Optional<Node> node = readOperData(bindingDataBroker, nodeInstanceIdentifier);
                    assertFalse(node.isPresent());
                });
    }

    @Test
    public void testRegisterCertificateToKeystore() throws ExecutionException, InterruptedException, TimeoutException {
        // Invoke RPC for registering certificates
        final ContainerNode certificateInput
                = getCertificateInput(CERT_ID, CA_VALUE, CLIENT_CERT, CLIENT_KEY, PASSPHRASE);
        lightyController.getServices().getDOMRpcService().invokeRpc(ADD_KEYSTORE_RPC_QN,
                        certificateInput)
                .get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);

        //Test if certificates was added
        final DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBroker();
        final InstanceIdentifier<Keystore> keystoreII = InstanceIdentifier
                .builder(Keystore.class, new KeystoreKey(CERT_ID))
                .build();
        final Optional<Keystore> keystore = readOperData(bindingDataBroker, keystoreII);
        assertTrue(keystore.isPresent());
        assertEquals(CA_VALUE, keystore.get().getCaCertificate());
        assertEquals(CLIENT_CERT, keystore.get().getClientCert());
        //Passphrase and client_key are encrypted before storing in data-store. So it shouldn't be same as provided
        assertNotEquals(PASSPHRASE, keystore.get().getPassphrase());
        assertNotEquals(CLIENT_KEY, keystore.get().getClientKey());

        //Remove created keystore after test
        deleteOperData(bindingDataBroker, keystoreII);
    }

    @Test
    public void testUpdatingYangModels() throws ExecutionException, InterruptedException, TimeoutException {
        // Invoke RPC for uploading yang models
        final ContainerNode yangModelInput = getYangModelInput(YANG_NAME, YANG_BODY, YANG_VERSION);
        lightyController.getServices().getDOMRpcService().invokeRpc(UPLOAD_YANG_RPC_QN, yangModelInput)
                .get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);

        // Test if yang models was uploaded
        final DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBroker();
        final InstanceIdentifier<GnmiYangModel> yangModelII = InstanceIdentifier.builder(GnmiYangModels.class)
                .child(GnmiYangModel.class, new GnmiYangModelKey(YANG_NAME, new ModuleVersionType(YANG_VERSION)))
                .build();
        final Optional<GnmiYangModel> gnmiYangModel = readOperData(bindingDataBroker, yangModelII);
        assertTrue(gnmiYangModel.isPresent());
        assertEquals(YANG_BODY, gnmiYangModel.get().getBody());
        assertEquals(YANG_NAME, gnmiYangModel.get().getName());
        assertEquals(YANG_VERSION, gnmiYangModel.get().getVersion().getValue());

        //Remove created YANG model after test
        deleteOperData(bindingDataBroker, yangModelII);
    }

    private ContainerNode getYangModelInput(final String yangName, final String yangBody,
                                                  final String yangVersion) {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(UPLOAD_YANG_INPUT_QN))
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(YANG_NAME_QN))
                        .withValue(yangName)
                        .build())
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(YANG_BODY_QN))
                        .withValue(yangBody)
                        .build())
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(YANG_VERSION_QN))
                        .withValue(yangVersion)
                        .build())
                .build();
    }

    private ContainerNode getCertificateInput(final String certId, final String ca, final String clientCert,
                                                    final String certKey, final String passphrase) {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(ADD_KEYSTORE_INPUT_QN))
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(KEYSTORE_ID_QN))
                        .withValue(certId)
                        .build())
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CA_CERT_QN))
                        .withValue(ca)
                        .build())
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CLIENT_CERT_QN))
                        .withValue(clientCert)
                        .build())
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CLIENT_KEY_QN))
                        .withValue(certKey)
                        .build())
                .withChild(ImmutableNodes.newLeafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(PASSPHRASE_QN))
                        .withValue(passphrase)
                        .build())
                .build();
    }

    private <T extends DataObject> Optional<T> readOperData(final DataBroker dataBroker,
                                                            final InstanceIdentifier<T> path)
            throws ExecutionException, InterruptedException, TimeoutException {
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();) {
            return readTransaction.read(LogicalDatastoreType.OPERATIONAL, path)
                    .get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
        }
    }

    private void deleteOperData(final DataBroker dataBroker, final InstanceIdentifier<?> path)
            throws ExecutionException, InterruptedException, TimeoutException {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.OPERATIONAL, path);
        writeTransaction.commit().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
    }

    private void deleteConfigData(final DataBroker dataBroker, final InstanceIdentifier<?> path)
            throws ExecutionException, InterruptedException, TimeoutException {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        writeTransaction.commit().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
    }

    private Optional<NormalizedNode> readDOMConfigData(final DOMDataBroker domDataBroker,
                                                             final YangInstanceIdentifier path)
            throws ExecutionException, InterruptedException, TimeoutException {
        try (DOMDataTreeReadTransaction readTransaction = domDataBroker.newReadOnlyTransaction();) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, path)
                    .get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
        }
    }

    private void writeDOMConfigData(final DOMDataBroker domDataBroker, final YangInstanceIdentifier path,
                                    final NormalizedNode data)
            throws ExecutionException, InterruptedException,TimeoutException {
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, path, data);
        writeTransaction.commit().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
    }

    private void updateDOMConfigData(final DOMDataBroker domDataBroker, final YangInstanceIdentifier path,
                                     final NormalizedNode data)
            throws ExecutionException, InterruptedException, TimeoutException {
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, path, data);
        writeTransaction.commit().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
    }

    private void deleteDOMConfigData(final DOMDataBroker domDataBroker, final YangInstanceIdentifier path)
            throws ExecutionException, InterruptedException, TimeoutException {
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        writeTransaction.commit().get(TIMEOUT_MILLIS,  TimeUnit.MILLISECONDS);
    }

    private static Node createNode(final String nameOfNode, final String address, final int port,
                                   final SecurityChoice securityChoice) {
        final ConnectionParametersBuilder connectionParametersBuilder = new ConnectionParametersBuilder()
                .setHost(new Host(new IpAddress(Ipv4Address.getDefaultInstance(address))))
                .setPort(new PortNumber(Uint16.valueOf(port)))
                .setSecurityChoice(securityChoice);

        ExtensionsParameters extensionsParameters = new ExtensionsParametersBuilder()
                .setGnmiParameters(new GnmiParametersBuilder()
                        .setUseModelNamePrefix(true)
                        .build())
                .build();

        return new NodeBuilder()
                .setNodeId(new NodeId(nameOfNode))
                .addAugmentation(new GnmiNodeBuilder()
                        .setConnectionParameters(connectionParametersBuilder.build())
                        .setExtensionsParameters(extensionsParameters)
                        .build())
                .build();
    }

    private static ContainerNode getTestDataContainerNode() {
        final var firstEntryValue = ImmutableNodes.newLeafSetEntryBuilder()
                .withValue(FIRST_VALUE)
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<>(TEST_LEAF_LIST_QN, FIRST_VALUE))
                .build();
        final var secondEntryValues = ImmutableNodes.newLeafSetEntryBuilder()
                .withValue(SECOND_VALUE)
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<>(TEST_LEAF_LIST_QN, SECOND_VALUE))
                .build();
        final var leafSetNode = ImmutableNodes.newSystemLeafSetBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_LEAF_LIST_QN))
                .withChild(firstEntryValue)
                .withChild(secondEntryValues)
                .build();

        return ImmutableNodes.newContainerBuilder()
                .withChild(leafSetNode)
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_DATA_CONTAINER_QN))
                .build();
    }

    private static ContainerNode getUpdateTestDataContainerNode() {
        final var thirdEntryValue = ImmutableNodes.newLeafSetEntryBuilder()
                .withValue(THIRD_VALUE)
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<>(TEST_LEAF_LIST_QN, THIRD_VALUE))
                .build();
        final LeafSetNode<Object> leafSetNode = ImmutableNodes.newSystemLeafSetBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_LEAF_LIST_QN))
                .withChild(thirdEntryValue)
                .build();

        return ImmutableNodes.newContainerBuilder()
                .withChild(leafSetNode)
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_DATA_CONTAINER_QN))
                .build();
    }


    private static SecurityChoice getInsecureSecurityChoice() {
        return new InsecureDebugOnlyBuilder()
                .setConnectionType(InsecureDebugOnly.ConnectionType.INSECURE)
                .build();
    }

    private static AAAEncryptionServiceImpl createEncryptionService()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final AaaEncryptServiceConfig encrySrvConfig = getDefaultAaaEncryptServiceConfig();
        final byte[] encryptionKeySalt = Base64.getDecoder().decode(encrySrvConfig.getEncryptSalt());
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encrySrvConfig.getEncryptMethod());
        final KeySpec keySpec = new PBEKeySpec(encrySrvConfig.getEncryptKey().toCharArray(), encryptionKeySalt,
                encrySrvConfig.getEncryptIterationCount(), encrySrvConfig.getEncryptKeyLength());
        final SecretKey key
                = new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(), encrySrvConfig.getEncryptType());
        final GCMParameterSpec ivParameterSpec = new GCMParameterSpec(encrySrvConfig.getAuthTagLength(),
            encryptionKeySalt);
        return new AAAEncryptionServiceImpl(ivParameterSpec, encrySrvConfig.getCipherTransforms(), key);
    }

    private static AaaEncryptServiceConfig getDefaultAaaEncryptServiceConfig() {
        return new AaaEncryptServiceConfigBuilder().setEncryptKey("V1S1ED4OMeEh")
                .setPasswordLength(12).setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
                .setEncryptMethod("PBKDF2WithHmacSHA1").setEncryptType("AES")
                .setEncryptIterationCount(32768).setEncryptKeyLength(128)
                .setAuthTagLength(128).setCipherTransforms("AES/GCM/NoPadding").build();
    }

    private static SimulatedGnmiDevice getUnsecureGnmiDevice(final String host, final int port) {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(GnmiWithoutRestconfTest.class.getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setTargetAddress(host);
        simulatorConfiguration.setTargetPort(port);
        simulatorConfiguration.setYangsPath(TEST_SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INITIAL_JSON_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INITIAL_JSON_DATA_PATH + "/state.json");

        return new SimulatedGnmiDevice(simulatorConfiguration);
    }
}
