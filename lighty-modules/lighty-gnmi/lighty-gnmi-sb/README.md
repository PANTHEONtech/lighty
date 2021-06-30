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

