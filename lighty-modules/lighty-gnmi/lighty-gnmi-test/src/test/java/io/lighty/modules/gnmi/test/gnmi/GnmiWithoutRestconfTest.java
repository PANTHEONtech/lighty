package io.lighty.modules.gnmi.test.gnmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.gnmi.southbound.identifier.IdentifierUtils;
import io.lighty.gnmi.southbound.lightymodule.GnmiSouthboundModule;
import io.lighty.gnmi.southbound.lightymodule.GnmiSouthboundModuleBuilder;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import io.lighty.modules.southbound.netconf.impl.AAAEncryptionServiceImpl;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
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
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfig;
import org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.AaaEncryptServiceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNode;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ConnectionParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.node.state.NodeState;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.SecurityChoice;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnly;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnlyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;

public class GnmiWithoutRestconfTest {
    private static final String INITIAL_JSON_DATA_PATH = "src/test/resources/json/initData";
    private static final String TEST_SCHEMA_PATH = "src/test/resources/simulator_models";
    private static final Path CONFIGURATION_PATH =  Path.of("src/test/resources/json/app_init_config.json");
    private static final Duration POLL_INTERVAL_DURATION = Duration.ofMillis(1_000L);
    private static final Duration WAIT_TIME_DURATION = Duration.ofMillis(10_000L);
    private static final String GNMI_NODE_ID = "gnmiNodeId";
    private static final String DEVICE_ADDRESS = "127.0.0.1";
    private static final int DEVICE_PORT = 3333;
    private static final String FIRST_VALUE = "first";
    private static final String SECOND_VALUE = "second";
    private static final String THIRD_VALUE = "third";
    private static final QNameModule INERFACE_QNAME_MODULE
            = QNameModule.create(URI.create("http://openconfig.net/yang/interfaces"), Revision.of("2019-11-19"));
    private static final QName INTERFACES_QNAME = QName.create(INERFACE_QNAME_MODULE, "interfaces");
    private static final QNameModule TEST_MODULE_QN_MODULE = QNameModule.create(URI.create("test:model"));
    private static final QName TEST_DATA_CONTAINER_QN = QName.create(TEST_MODULE_QN_MODULE, "test-data");
    private static final QName TEST_LEAF_LIST_QN = QName.create(TEST_DATA_CONTAINER_QN, "test-leaf-list");

    private static LightyController lightyController;
    private static GnmiSouthboundModule gnmiSouthboundModule;
    private static SimulatedGnmiDevice gnmiDevice;


    @BeforeAll
    public static void startUp() throws ConfigurationException, ExecutionException, InterruptedException, IOException,
            InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {

        lightyController = new LightyControllerBuilder()
                .from(ControllerConfigUtils.getConfiguration(Files.newInputStream(CONFIGURATION_PATH)))
                .build();
        lightyController.start().get();

        gnmiSouthboundModule = new GnmiSouthboundModuleBuilder()
                .withConfig(GnmiConfigUtils.getGnmiConfiguration(Files.newInputStream(CONFIGURATION_PATH)))
                .withLightyServices(lightyController.getServices())
                .withExecutorService(Executors.newCachedThreadPool())
                .withEncryptionService(createEncryptionService())
                .build();
        gnmiSouthboundModule.start().get();

        gnmiDevice = getUnsecureGnmiDevice(DEVICE_ADDRESS, DEVICE_PORT);
        gnmiDevice.start();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        gnmiDevice.stop();
        gnmiSouthboundModule.shutdown();
        lightyController.shutdown();
    }

    @Test
    public void testCrudOperation() throws ExecutionException, InterruptedException {
        final DataBroker bindingDataBroker = lightyController.getServices().getBindingDataBroker();
        //Write device to data-store
        final Node testGnmiNode = createNode(GNMI_NODE_ID, DEVICE_ADDRESS, DEVICE_PORT, getInsecureSecurityChoice());
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<Node> nodeInstanceIdentifier = IdentifierUtils.gnmiNodeIID(testGnmiNode.getNodeId());
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodeInstanceIdentifier, testGnmiNode);
        writeTransaction.commit().get();

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
        final Optional<NormalizedNode<?, ?>> normalizedNode = readDOMConfigData(domDataBroker, interfacesYIID);
        assertTrue(normalizedNode.isPresent());
        assertEquals(INTERFACES_QNAME, normalizedNode.get().getIdentifier().getNodeType());

        //SET data
        final YangInstanceIdentifier testLeafListYIID = YangInstanceIdentifier.builder()
                .node(TEST_DATA_CONTAINER_QN).build();
        final ContainerNode testDataContainerNode = getTestDataContainerNode();
        writeDOMConfigData(domDataBroker, testLeafListYIID, testDataContainerNode);

        //GET created data
        final Optional<NormalizedNode<?, ?>> createdContainer = readDOMConfigData(domDataBroker, testLeafListYIID);
        assertTrue(createdContainer.isPresent());
        assertEquals(TEST_DATA_CONTAINER_QN, createdContainer.get().getIdentifier().getNodeType());

        //UPDATE data
        final ContainerNode updateTestDataContainerNode = getUpdateTestDataContainerNode();
        updateDOMConfigData(domDataBroker, testLeafListYIID, updateTestDataContainerNode);

        //GET updated data
        final Optional<NormalizedNode<?, ?>> updatedContainer = readDOMConfigData(domDataBroker, testLeafListYIID);
        assertTrue(updatedContainer.isPresent());
        assertEquals(TEST_DATA_CONTAINER_QN, updatedContainer.get().getIdentifier().getNodeType());
        assertTrue(updatedContainer.get() instanceof ContainerNode);
        ContainerNode containerNode = (ContainerNode) updatedContainer.get();
        assertEquals(1, containerNode.getValue().toArray().length);
        assertTrue(containerNode.getValue().toArray()[0] instanceof LeafSetNode);
        LeafSetNode<?> leafSetNode = (LeafSetNode) containerNode.getValue().toArray()[0];
        assertTrue(leafSetNode.getValue().size() == 3);
        List<String> list = Arrays.asList(FIRST_VALUE, SECOND_VALUE, THIRD_VALUE);
        for (Object object : leafSetNode.getValue()) {
            assertTrue(object instanceof LeafSetEntryNode);
            assertTrue(list.contains(((LeafSetEntryNode<String>) object).getValue()));
        }

        //DELETE created data
        deleteDOMConfigData(domDataBroker, testLeafListYIID);

        //GET deleted data
        final Optional<NormalizedNode<?, ?>> removedLeafListNN = readDOMConfigData(domDataBroker, testLeafListYIID);
        assertFalse(removedLeafListNN.isPresent());
    }

    private <T extends DataObject> Optional<T> readOperData(final DataBroker dataBroker,
                                                            final InstanceIdentifier<T> path)
            throws ExecutionException, InterruptedException {
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();) {
            return readTransaction.read(LogicalDatastoreType.OPERATIONAL, path).get();
        }
    }

    private Optional<NormalizedNode<?, ?>> readDOMConfigData(final DOMDataBroker domDataBroker,
                                                             final YangInstanceIdentifier path)
            throws ExecutionException, InterruptedException {
        try (DOMDataTreeReadTransaction readTransaction = domDataBroker.newReadOnlyTransaction();) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, path).get();
        }
    }

    private void writeDOMConfigData(final DOMDataBroker domDataBroker, final YangInstanceIdentifier path,
                                     final NormalizedNode<?,?> data) throws ExecutionException, InterruptedException {
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, path, data);
        writeTransaction.commit().get();
    }

    private void updateDOMConfigData(final DOMDataBroker domDataBroker, final YangInstanceIdentifier path,
                                    final NormalizedNode<?,?> data) throws ExecutionException, InterruptedException {
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, path, data);
        writeTransaction.commit().get();
    }

    private void deleteDOMConfigData(final DOMDataBroker domDataBroker, final YangInstanceIdentifier path)
            throws ExecutionException, InterruptedException {
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, path);
        writeTransaction.commit().get();
    }

    private static Node createNode(final String nameOfNode, final String address, final int port,
                                   final SecurityChoice securityChoice) {
        final ConnectionParametersBuilder connectionParametersBuilder = new ConnectionParametersBuilder()
                .setHost(new Host(IpAddressBuilder.getDefaultInstance(address)))
                .setPort(new PortNumber(Uint16.valueOf(port)))
                .setSecurityChoice(securityChoice);

        return new NodeBuilder()
                .setNodeId(new NodeId(nameOfNode))
                .addAugmentation(new GnmiNodeBuilder()
                        .setConnectionParameters(connectionParametersBuilder.build())
                        .build())
                .build();
    }

    private static ContainerNode getTestDataContainerNode() {
        final LeafSetEntryNode<Object> firstEntryValue = ImmutableLeafSetEntryNodeBuilder.create()
                .withValue(FIRST_VALUE)
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue(TEST_LEAF_LIST_QN, FIRST_VALUE))
                .build();
        final LeafSetEntryNode<Object> secondEntryValues = ImmutableLeafSetEntryNodeBuilder.create()
                .withValue(SECOND_VALUE)
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue(TEST_LEAF_LIST_QN, SECOND_VALUE))
                .build();
        final LeafSetNode<Object> leafSetNode = ImmutableLeafSetNodeBuilder
                .create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_LEAF_LIST_QN))
                .withChild(firstEntryValue)
                .withChild(secondEntryValues)
                .build();

        return ImmutableContainerNodeBuilder.create()
                .withChild(leafSetNode)
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_DATA_CONTAINER_QN))
                .build();
    }

    private static ContainerNode getUpdateTestDataContainerNode() {
        final LeafSetEntryNode<Object> thirdEntryValue = ImmutableLeafSetEntryNodeBuilder.create()
                .withValue(THIRD_VALUE)
                .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue(TEST_LEAF_LIST_QN, THIRD_VALUE))
                .build();
        final LeafSetNode<Object> leafSetNode = ImmutableLeafSetNodeBuilder
                .create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_LEAF_LIST_QN))
                .withChild(thirdEntryValue)
                .build();

        return ImmutableContainerNodeBuilder.create()
                .withChild(leafSetNode)
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(TEST_DATA_CONTAINER_QN))
                .build();
    }


    private static SecurityChoice getInsecureSecurityChoice() {
        return new InsecureDebugOnlyBuilder()
                .setConnectionType(InsecureDebugOnly.ConnectionType.INSECURE)
                .build();
    }

    private static AAAEncryptionServiceImpl createEncryptionService() throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        final AaaEncryptServiceConfig encrySrvConfig = getDefaultAaaEncryptServiceConfig();
        final byte[] encryptionKeySalt = Base64.getDecoder().decode(encrySrvConfig.getEncryptSalt());
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encrySrvConfig.getEncryptMethod());
        final KeySpec keySpec = new PBEKeySpec(encrySrvConfig.getEncryptKey().toCharArray(), encryptionKeySalt,
                encrySrvConfig.getEncryptIterationCount(), encrySrvConfig.getEncryptKeyLength());
        final SecretKey key
                = new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(), encrySrvConfig.getEncryptType());
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(encryptionKeySalt);

        final Cipher encryptCipher = Cipher.getInstance(encrySrvConfig.getCipherTransforms());
        encryptCipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

        final Cipher decryptCipher = Cipher.getInstance(encrySrvConfig.getCipherTransforms());
        decryptCipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

        return new AAAEncryptionServiceImpl(encryptCipher, decryptCipher);
    }

    private static AaaEncryptServiceConfig getDefaultAaaEncryptServiceConfig() {
        return new AaaEncryptServiceConfigBuilder().setEncryptKey("V1S1ED4OMeEh")
                .setPasswordLength(12).setEncryptSalt("TdtWeHbch/7xP52/rp3Usw==")
                .setEncryptMethod("PBKDF2WithHmacSHA1").setEncryptType("AES")
                .setEncryptIterationCount(32768).setEncryptKeyLength(128)
                .setCipherTransforms("AES/CBC/PKCS5Padding").build();
    }

    private static SimulatedGnmiDevice getUnsecureGnmiDevice(final String host, final int port) {
        return new SimulatedGnmiDeviceBuilder().setHost(host).setPort(port)
                .setInitialConfigDataPath(INITIAL_JSON_DATA_PATH + "/config.json")
                .setInitialStateDataPath(INITIAL_JSON_DATA_PATH + "/state.json")
                .setYangsPath(TEST_SCHEMA_PATH)
                .build();
    }
}
