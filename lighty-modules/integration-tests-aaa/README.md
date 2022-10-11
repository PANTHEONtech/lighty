# Lighty Integration Tests

This project contains some integration tests for testing OdlController, that uses community RestConf.
Tests are located in this [directory](src/test/java/io/lighty/kit/examples/tests).

Tests will start OdlController with community RestConf and AAA.

Run
-------------
* build the project: ```mvn clean install```
* run tests: ```mvn failsafe:integration-test -Ptest-integration```
* no ALPN needed

Setup Logging
-------------
Default logging configuration may be overwritten by JVM option
```-Dlog4j.configurationFile=path/to/log4j2.xml```

Content of ```log4j2.xml``` is described [here](https://logging.apache.org/log4j/2.x/manual/configuration.html).

