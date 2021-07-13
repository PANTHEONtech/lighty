# gNMI south-bound module
is south-bound lighty module which manages connection with gNMI targets. This module implements functionality to make
CRUD operations on multiple gNMI targets. Which make it easy to read and manipulate with data in gNMI devices.

Lighty gNMI augment ODL network-topology model with [gnmi-topology](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang)
model. This allows us to connect the gNMI device by adding new node with desired [parameters](https://github.com/PANTHEONtech/lighty/blob/master/lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang#L67) to gnmi-topology
in datastore (as ODL NETCONF SBP does). Once new node is added, gNMI-south-bound established connection to the gNMI
device and creates mount point containing [GnmiDataBroker](src/main/java/io/lighty/gnmi/southbound/mountpoint/broker/GnmiDataBroker.java)
which is used for communicating with gNMI device via transactions. GnmiDataBroker also contains [schemaContext](https://javadocs.opendaylight.org/org.opendaylight.yangtools/master/org/opendaylight/yangtools/yang/model/api/SchemaContext.html)
created from capabilities received from the device. All YANG models which gNMI device will use, should be provided
in [GnmiConfiguration](src/main/java/io/lighty/gnmi/southbound/lightymodule/config/GnmiConfiguration.java)
as path to folder with required YANG models or added via [upload-yang-model](https://github.com/PANTHEONtech/lighty/blob/master/lighty-models/lighty-gnmi-models/lighty-gnmi-yang-storage-model/src/main/yang/gnmi-yang-storage.yang#L57)
RPC.

## How to use it
1. Add dependency to your pom.xml file.

```
    <dependency>
        <groupId>io.lighty.modules.gnmi.southbound</groupId>
        <artifactId>lighty-gnmi-sb</artifactId>
    </dependency>
```

2. Initialize GnmiSouthboundModule with required parameters.

```
    GnmiSouthboundModule gnmiSouthboundModule
        = new GnmiSouthboundModuleBuilder()
            .withLightyServices(services)
            .withExecutorService(gnmiExecService)
            .withEncryptionService(encryptionService)
            .build();
    gnmiSouthboundModule.initProcedure();
```

3. Stop GnmiSouthboundModule.

```
    gnmiSouthboundModule.stopProcedure();
```

## Configuration
1. **withLightyServices(LightyServices)** - (Required field) Add lighty services to gnmi-module.

2. **withExecutorService(ExecutorService)** - (Required field) Add Executor service required for managing
   asynchronous tasks.

3. **withEncryptionService(AAAEncryptionService)** - (Required field) Add encryption service for encrypting sensitive
   data in data-store.

4. **withReactor(CrossSourceStatementReactor)** - (Default SchemaConstants.DEFAULT_YANG_REACTOR)  Add reactor used for
   parsing yang modules.

5. **withConfig(GnmiConfiguration)** - (Default empty) Configuration of gNMI south-bound.

## Supported encodings
Since we are operating solely with yang modeled data, which, as stated in [gNMI spec](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#231-json-and-json_ietf),
 should be encoded in RFC7951 JSON format, only JSON_IETF encoding is supported for structured data types. That means each gNMI SetRequest sent by gNMI-module targeted to structured data
 (yang container,list ...) is encoded in JSON_IETF and each gNMI GetRequest has encoding set to JSON_IETF.
This encoding is also expected encoding for gNMI GetResponse of structured data.
This encoding enforcement does not apply to SetRequest/GetResponse targeting scalar types (single yang leaf), in this case,
 [gNMI specification](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#223-node-values) applies.
### Encodings in gNMI CapabilityResponse
As stated above, only JSON_IETF is supported for encoding structured data types. This means that device MUST declare it's
 support of JSON_IETF encoding in [CapabilityResponse](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#322-the-capabilityresponse-message)
  supported_encodings field. If JSON_IETF is not present in supported_encoding field, connection of gNMI device is closed.

## lighty.io gNMI south-bound with lighty.io Controller CRUD example
This example will show how to programmatically connect lighty gNMI-sb with gNMI device. First we start lighty gNMI,
lighty controller and gNMI device. For device in this example will be used [gNMI device simulator](../lighty-gnmi-device-simulator/README.md).
Creating device connection and All CRUD operation will be performed with writing data directly to MD-SAL data store
without using RESTCONF. Full example can be found inside test [GnmiWithoutRestconfTest](../lighty-gnmi-test/src/test/java/io/lighty/modules/gnmi/test/gnmi/GnmiWithoutRestconfTest.java).

1. Initialize lighty Controller lighty gNMI.
```java
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
```

2. Connect gNMI device. More about configuring security or extension parameter on gNMI device can be found in
[RCgNMI Documentation](https://github.com/PANTHEONtech/lighty/blob/master/lighty-applications/lighty-rcgnmi-app-aggregator/README.md#how-to-use-rcgnmi-example-app)
and in [gnmi-topology.yang](https://github.com/PANTHEONtech/lighty/blob/master/lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang)
YANG model.
```java
        final Node testGnmiNode = createNode(GNMI_NODE_ID, DEVICE_ADDRESS, DEVICE_PORT, getInsecureSecurityChoice());
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<Node> nodeInstanceIdentifier = IdentifierUtils.gnmiNodeIID(testGnmiNode.getNodeId());
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodeInstanceIdentifier, testGnmiNode);
        writeTransaction.commit().get();
```

3. Wait until gNMI device will be successfully connected to gNMI-sb
```java
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
```

4. Get DOM GnmiDataBroker registered for specific gNMI device. For each successfully registered gNMI device
   is created a new GnmiDataBroker with device specific schema context.
```java
        final DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();
        final Optional<DOMMountPoint> mountPoint
                = domMountPointService.getMountPoint(IdentifierUtils.nodeidToYii(testGnmiNode.getNodeId()));
        assertTrue(mountPoint.isPresent());
        final DOMMountPoint domMountPoint = mountPoint.get();
        final Optional<DOMDataBroker> service = domMountPoint.getService(DOMDataBroker.class);
        assertTrue(service.isPresent());
        final DOMDataBroker domDataBroker = service.get();
```

5. Get openconfig interfaces data from gNMI device.
```java
        final YangInstanceIdentifier interfacesYIID = YangInstanceIdentifier.builder().node(INTERFACES_QNAME).build();
        final Optional<NormalizedNode<?, ?>> normalizedNode;
        try (DOMDataTreeReadTransaction domDataTreeReadTransaction = domDataBroker.newReadOnlyTransaction()) {
            normalizedNode = domDataTreeReadTransaction.read(LogicalDatastoreType.CONFIGURATION, interfacesYIID).get();
        }
        assertTrue(normalizedNode.isPresent());
        assertEquals(INTERFACES_QNAME, normalizedNode.get().getIdentifier().getNodeType());
```

6. Set data modeled by [gnmi-test-model](../lighty-gnmi-test/src/test/resources/models/plugin_models/gnmi-test-model.yang) to gNMI device
```java
        final YangInstanceIdentifier testLeafListYIID = YangInstanceIdentifier.builder().node(TEST_DATA_CONTAINER_QN).build();
        final ContainerNode testDataContainerNode = getTestDataContainerNode();
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, testLeafListYIID, testDataContainerNode);
        writeTransaction.commit().get();
```

7. Update data in gNMI device
```java
        final YangInstanceIdentifier testLeafListYIID = YangInstanceIdentifier.builder()
                .node(TEST_DATA_CONTAINER_QN).build();
        final ContainerNode updateTestDataContainerNode = getUpdateTestDataContainerNode();
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, testLeafListYIID, updateTestDataContainerNode);
        writeTransaction.commit().get();
```

8. Delete data from gNMI device
```java
        final YangInstanceIdentifier testLeafListYIID = YangInstanceIdentifier.builder().node(TEST_DATA_CONTAINER_QN).build();
        final DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, testLeafListYIID);
        writeTransaction.commit().get();
```

## lighty.io gNMI south-bound with lighty.io Controller register certificates
This example will show how to programmatically add certificates for lighty gNMI. Full example can be found inside test
[GnmiWithoutRestconfTest](../lighty-gnmi-test/src/test/java/io/lighty/modules/gnmi/test/gnmi/GnmiWithoutRestconfTest.java).

1. Adding certificates to keystore doesn't require device. So this example use only lighty Controller and lighty gNMI modules.
    Certificates could be assigned to device, with [keystore-id](https://github.com/PANTHEONtech/lighty/blob/master/lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang#L45)
    parameter when is creating device mountpoint.
```java
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
```

2. Invoke RPC for adding certificates to lighty gNMI. YANG RPC and certificates keystore is modeled by
   [gnmi-certificate-storage.yang](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-certificates-storage-model/src/main/yang/gnmi-certificate-storage.yang)
```java
        final NormalizedNode<?, ?> certificateInput
                = getCertificateInput(CERT_ID, CA_VALUE, CLIENT_CERT, CLIENT_KEY, PASSPHRASE);
        lightyController.getServices().getDOMRpcService().invokeRpc(ADD_KEYSTORE_RPC_QN, certificateInput);
```

## lighty.io gNMI south-bound with lighty.io Controller updating YANG models in runtime
This example will show how to programmatically add YANG models to lighty gNMI. When device will require some yang models
which are not included in provided [GnmiConfiguration](src/main/java/io/lighty/gnmi/southbound/lightymodule/config/GnmiConfiguration.java)
, than it could be added by RPC in runtime. Full example can be found inside test
[GnmiWithoutRestconfTest](../lighty-gnmi-test/src/test/java/io/lighty/modules/gnmi/test/gnmi/GnmiWithoutRestconfTest.java).

1. Updating YANG models doesn't require started device. So this example use only lighty Controller and lighty gNMI modules.
```java
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
```

2. Invoke RPC for Updating YANG models to lighty gNMI. Yang storage and RPC for updating YANG models is modeled by
   [gnmi-yang-storage.yang](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-yang-storage-model/src/main/yang/gnmi-yang-storage.yang).
```java
        final NormalizedNode<?, ?> yangModelInput = getYangModelInput(YANG_NAME, YANG_BODY, YANG_VERSION);
        lightyController.getServices().getDOMRpcService().invokeRpc(UPLOAD_YANG_RPC_QN, yangModelInput);
```
