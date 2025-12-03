# gNMI Southbound Module

A southbound [lighty.io](https://lighty.io/) module, which manages connections with gNMI targets.
This module implements functionality to make **CRUD operations on multiple gNMI targets**.
This make it easy to read and manipulate data in gNMI devices.

lighty.io gNMI augments the OpenDaylight network-topology model with the
[gnmi-topology](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang)
model. This provides the possibility to connect a gNMI device, by adding a new node with the desired
[parameters](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang#L67)
to the gnmi-topology in data store (as OpenDaylight NETCONF SBP does).

Once a new node is added, gNMI-south-bound established a connection to the gNMI device and creates a mount point
containing [GnmiDataBroker](src/main/java/io/lighty/gnmi/southbound/mountpoint/broker/GnmiDataBroker.java), which is
used for communicating with gNMI device via transactions. GnmiDataBroker also contains
[schemaContext](https://javadocs.opendaylight.org/org.opendaylight.yangtools/master/org/opendaylight/yangtools/yang/model/api/SchemaContext.html)
created from capabilities received from the device. All YANG models which the gNMI device will use, should be provided
in the [GnmiConfiguration](src/main/java/io/lighty/gnmi/southbound/lightymodule/config/GnmiConfiguration.java).
YANG models could be added to gNMI Southbound module with 3 ways:
 1) Add path to folder containing YANG modules in GnmiConfiguration with property `initialYangsPaths` to json configuration.
    ```
      "gnmi": {
         "initialYangsPaths" : [
            PATH_TO_FOLDER_WITH_YANG_MODELS
         ]
      }
    ```
 2) Add models from classpath as a set of `YangModuleInfo` (org.opendaylight.yangtools.yang.binding.YangModuleInfo) to json configuration.
    ```
     "gnmi": {
        "initialYangModels": [
            { "nameSpace":"MODEL_NAMESPACE","name":"MODEL_NAME","revision":"MODEL_REVISION"}
            ...
        ]
    }
    ```
 3) Add models in runtime with RPC [upload-yang-model](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-yang-storage-model/src/main/yang/gnmi-yang-storage.yang#L61)

Yang models can be added with any combination of the mentioned methods.

## How To Use

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
1. **withLightyServices(LightyServices)** - (Required field) Add lighty.io services to gnmi-module.

2. **withExecutorService(ExecutorService)** - (Required field) Add _Executor_ service, required for managing asynchronous tasks.

3. **withEncryptionService(AAAEncryptionService)** - (Required field) Add encryption service for encrypting sensitive data in data-store.

4. **withReactor(CrossSourceStatementReactor)** - (Default SchemaConstants.DEFAULT_YANG_REACTOR)  Add reactor used for parsing YANG modules.

5. **withConfig(GnmiConfiguration)** - (Default empty) Configuration of gNMI southbound

## Supported Encodings

Since we are operating solely with YANG modeled data, which, as stated in [gNMI spec](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#231-json-and-json_ietf), should be encoded in the *RFC7951 JSON format*, **only JSON_IETF encoding is supported** for structured data types. 

That means each gNMI SetRequest sent by gNMI-module targeted towards structured data (yang container, list, etc.) is encoded in JSON_IETF, while each gNMI GetRequest has encoding set to JSON_IETF.

This encoding is also expected for gNMI GetResponse of structured data.

This encoding enforcement **does not apply to SetRequest/GetResponse targeting scalar types (single yang leaf)**. In this case, [gNMI specification](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#223-node-values) applies.

### Encodings in gNMI CapabilityResponse
As stated above, only JSON_IETF is supported for encoding structured data types. This means that device MUST declare it's support of JSON_IETF encoding in [CapabilityResponse](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md#322-the-capabilityresponse-message) *supported_encodings* field. 

If JSON_IETF is not present in the *supported_encoding* field, the gNMI device connection is closed.

## CRUD Example: lighty.io gNMI Southbound w/ lighty.io Controller

This example shows how to programmatically connect lighty gNMI-sb with a gNMI device. First we start lighty.io gNMI, then lighty.io controller and the gNMI device. For the device in this example, [lighty.io gNMI Device Simulator](../lighty-gnmi-device-simulator/README.md) is used.

Creating a device connection and CRUD operation, is performed by writing data directly to the MD-SAL data store **without using RESTCONF**. The full example can be found inside the test - [GnmiWithoutRestconfTest](../lighty-gnmi-test/src/test/java/io/lighty/modules/gnmi/test/gnmi/GnmiWithoutRestconfTest.java).

1. Initialize lighty.io Controller and lighty.io gNMI:

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

2. Connect gNMI device. More about configuring security or extension parameter on gNMI device can be found in the [RCgNMI Documentation](../../../lighty-applications/lighty-rcgnmi-app-aggregator/README.md#how-to-use-rcgnmi-example-app) and in the [gnmi-topology.yang](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang) YANG model.

```java
        final Node testGnmiNode = createNode(GNMI_NODE_ID, DEVICE_ADDRESS, DEVICE_PORT, getInsecureSecurityChoice());
        final WriteTransaction writeTransaction = bindingDataBroker.newWriteOnlyTransaction();
        final InstanceIdentifier<Node> nodeInstanceIdentifier = IdentifierUtils.gnmiNodeIID(testGnmiNode.getNodeId());
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, nodeInstanceIdentifier, testGnmiNode);
        writeTransaction.commit().get();
```

3. Wait until the gNMI device is successfully connected to gNMI-sb. The device is successfully connected, when the status is `NodeState.NodeStatus.READY`.

Device [node-status](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang#L140) is available in operational memory, inside data store.

4. Get DOM GnmiDataBroker registered for specific gNMI device. For each successfully registered gNMI device, a new GnmiDataBroker with a device specific schema context is created.

```java
        final DOMMountPointService domMountPointService = lightyController.getServices().getDOMMountPointService();
        final Optional<DOMMountPoint> mountPoint
                = domMountPointService.getMountPoint(IdentifierUtils.nodeidToYii(testGnmiNode.getNodeId()));
        final DOMMountPoint domMountPoint = mountPoint.orElseThrow();
        final Optional<DOMDataBroker> service = domMountPoint.getService(DOMDataBroker.class);
        final DOMDataBroker domDataBroker = service.orElseThrow();
```

5. Get openconfig interfaces data from gNMI device.
```java
        final YangInstanceIdentifier interfacesYIID = YangInstanceIdentifier.builder().node(INTERFACES_QNAME).build();
        final DOMDataTreeReadTransaction domDataTreeReadTransaction = domDataBroker.newReadOnlyTransaction();
        final Optional<NormalizedNode> normalizedNode
                = domDataTreeReadTransaction.read(LogicalDatastoreType.CONFIGURATION, interfacesYIID).get();
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

## Register Client Certificates
This example shows how to programmatically add certificates for lighty.io gNMI. Certificates keystore should be created before connecting the actual gNMI device. Certificates could be assigned to device, with [keystore-id](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-topology-model/src/main/yang/gnmi-topology.yang#L45) parameter when the mountpoint is being created. YANG RPC and certificates keystore is modeled by [gnmi-certificate-storage.yang](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-certificates-storage-model/src/main/yang/gnmi-certificate-storage.yang)

- Invoke RPC for adding certificates to lighty.io gNMI:

```java
        final NormalizedNode certificateInput
                = getCertificateInput(CERT_ID, CA_VALUE, CLIENT_CERT, CLIENT_KEY, PASSPHRASE);
        lightyController.getServices().getDOMRpcService().invokeRpc(ADD_KEYSTORE_RPC_QN, certificateInput).get();
```
The full example can be found inside the [GnmiWithoutRestconfTest](../lighty-gnmi-test/src/test/java/io/lighty/modules/gnmi/test/gnmi/GnmiWithoutRestconfTest.java).

## Update YANG Models in Runtime
When the device requires some YANG models, which are not included in the provided [GnmiConfiguration](src/main/java/io/lighty/gnmi/southbound/lightymodule/config/GnmiConfiguration.java), then those could be added by RPC in runtime. 

YANG models should be added **before connecting the gNMI device**. YANG storage and RPC for updating YANG models is modeled by [gnmi-yang-storage.yang](../../../lighty-models/lighty-gnmi-models/lighty-gnmi-yang-storage-model/src/main/yang/gnmi-yang-storage.yang).

- Invoke RPC for Updating YANG models to lighty.io gNMI.
```java
        final NormalizedNode yangModelInput = getYangModelInput(YANG_NAME, YANG_BODY, YANG_VERSION);
        lightyController.getServices().getDOMRpcService().invokeRpc(UPLOAD_YANG_RPC_QN, yangModelInput).get();
```
Thefull example can be found inside the [GnmiWithoutRestconfTest](../lighty-gnmi-test/src/test/java/io/lighty/modules/gnmi/test/gnmi/GnmiWithoutRestconfTest.java).

# Contact Us!
In case of any commercial support requests, [contact us here!](https://pantheon.tech/contact-us/)
