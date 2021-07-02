# gNMI south-bound module
is south-bound lighty module which manages connection with gNMI targets. This module implements functionality to make
CRUD operations on multiple gNMI targets. Which make it easy to read and manipulate with data in gNMI devices.

Lighty gNMI augment ODL network-topology model with [gnmi-topology](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang)
model. This allows to configure and register a specific gNMI device as a NETCONF node.
When device is successfully registered, Lighty gNMI creates specific [DataBroker](src/main/java/io/lighty/gnmi/southbound/mountpoint/broker/GnmiDataBroker.java)
for each device. DataBroker provides functionality for writing and reading from gNMI device.
GnmiDataBroker also contains schemaContext created from capabilities received from the device.
All YANG models which gNMI device will use, should be provided in [GnmiConfiguration](src/main/java/io/lighty/gnmi/southbound/lightymodule/config/GnmiConfiguration.java)
as path to folder with required YANG models.

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

##lighty.io gNMI south-bound with lighty.io Controller example
This example will show how to programmatically connect lighty gNMI-sb with gNMI device. First we start lighty gGNMI,
lighty controller and gNMI device. For device in this example will be used gNMI device simulator. Creating device 
connection and All CRUD operation will be performed with writing data directly to MD-SAL data store
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

2. Connect gNMI device
```java
        Node testGnmiNode = createNode(GNMI_NODE_ID, DEVICE_ADDRESS, DEVICE_PORT, getInsecureSecurityChoice());
        WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Node> nodeInstanceIdentifier = IdentifierUtils.gnmiNodeIID(testGnmiNode.getNodeId());
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodeInstanceIdentifier, testGnmiNode);
        writeTransaction.commit().get();
```

3. Wait until gNMI device will be successfully connected to gNMI-sb
```java
        Awaitility.waitAtMost(WAIT_TIME_DURATION)
                .pollInterval(POLL_INTERVAL_DURATION)
                .untilAsserted(() -> {
                    Optional<Node> node = readOperData(bindingDataBroker, nodeInstanceIdentifier);
                    assertTrue(node.isPresent());
                    Node foundNode = node.get();
                    GnmiNode gnmiNode = foundNode.augmentation(GnmiNode.class);
                    assertNotNull(gnmiNode);
                    NodeState nodeState = gnmiNode.getNodeState();
                    assertNotNull(nodeState);
                    NodeState.NodeStatus nodeStatus = nodeState.getNodeStatus();
                    assertEquals(NodeState.NodeStatus.READY, nodeStatus);
                });
```

4. Get DOM GnmiDataBroker registered for specific gNMI device. For each successfully registered gNMI device 
   is created a new GnmiDataBroker with device specific schema context.
```java
        DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();
        Optional<DOMMountPoint> mountPoint
                = domMountPointService.getMountPoint(IdentifierUtils.nodeidToYii(testGnmiNode.getNodeId()));
        assertTrue(mountPoint.isPresent());
        DOMMountPoint domMountPoint = mountPoint.get();
        Optional<DOMDataBroker> service = domMountPoint.getService(DOMDataBroker.class);
        assertTrue(service.isPresent());
        DOMDataBroker domDataBroker = service.get();
```

5. Get openconfig interfaces data from gNMI device.
```java
        YangInstanceIdentifier interfacesYIID = YangInstanceIdentifier.builder().node(INTERFACES_QNAME).build();
        Optional<NormalizedNode<?, ?>> normalizedNode
        try(DOMDataTreeReadTransaction domDataTreeReadTransaction = domDataBroker.newReadOnlyTransaction()) {
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