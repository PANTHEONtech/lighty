# gNMI south-bound module
is south-bound lighty module which manages connection with gNMI target.
This module implements functionality to make CRUD operations on gNMI target.

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

## Supported encodingsGnmiCapabilitiesService.java
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
