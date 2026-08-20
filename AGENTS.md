# AGENTS.md

Guidance for AI coding agents working in this lighty.io repository.

## Project overview

lighty.io is a Java SDK/runtime built on OpenDaylight (ODL) core components, packaged to run in a plain Java SE environment (no Karaf/OSGi required). It provides:

- **lighty-core** - ODL core services: MD-SAL, Controller, yangtools, Clustering. Includes DI extensions for Google Guice and Spring.
- **lighty-modules** - ODL NorthBound (RESTCONF, OpenApi) and SouthBound (NETCONF, gNMI, AAA, Jetty Server) plugin initializers.
- **lighty-examples** - Example controller applications (RESTCONF/NETCONF app, gNMI/RESTCONF app, Spring Boot integration).
- **lighty-applications** - Packaged aggregator apps (RESTCONF-NETCONF app, gNMI RESTCONF app).
- **lighty-models** - Example YANG models and artifacts.
- **lighty-resources** - Resource artifacts.
- **lighty-tests-report** - Test reporting.

Current branch tracks OpenDaylight 2026-09 "Manganese" release compatibility.

## Build & test

Requirements:
- JDK 21
- Maven 3.9.5+
- Proper `settings.xml` in `~/.m2` (see the upstream odlparent settings.xml)

Build and install locally:
```
mvn clean install -DskipTests
```

Run unit and integration tests:
```
mvn clean install
```

Before running IT tests, ensure ports **8080, 8888, 8185, 2550** are free on localhost - a full ODL/lighty.io controller with north/south-bound plugins is often started for these tests, so stale processes on these ports will cause failures.

## Working with the controller

- Two controller deployment styles exist: **Standalone** (own JVM as a microservice) and **Embedded** (runs inside another application's JVM). Know which one a change targets.
- `LightyController` is the core runtime component; it's built via `LightyControllerBuilder` and exposes services through `LightyController.getServices()`.
- Startup config lives in `lightyControllerConfig.json` (main config, references other config files) plus `akka.conf` for the actor system. Initial YANG-modeled config data can be loaded on startup via an `initialConfigData` block pointing at a json/xml file.
- Controller startup is a 5-step sequence (step 4 optional) - see `docs/lighty.io-controller-startup-sequence.svg` before altering init order.
- Architecture reference: `docs/lighty.io-controller-architecture.svg`.

## Conventions

- Dependency versions are managed via `lighty-bom` (Maven BOM import) - add new module dependencies there rather than hardcoding versions in individual POMs.
- New south-bound/north-bound plugins follow the existing pattern in `lighty-modules` (an initializer wrapping the underlying ODL plugin).
- Tests are being migrated to JUnit 5 (TestNG is being removed) - write new tests in JUnit 5.
- Logging uses log4j2 with JMX enabled by default for runtime log-level changes (default JMX port 1099); don't remove this without reason, and be aware `-Dlog4j2.disable.jmx=true` is the documented opt-out.

## Migration context

If working on OpenDaylight→lighty.io migration tasks, see `docs/ODL-migration-guide.md` - it documents practical experience from real migrations and should be treated as the source of truth over ad hoc assumptions.

## Where to look first

- Module-level READMEs (e.g. `lighty-core/lighty-controller/README.md`, `lighty-modules/*/README.md`, `lighty-examples/README.md`) contain usage examples and dependency snippets - check the relevant one before writing new integration code.
- `SECURITY.md` for vulnerability reporting process.
- `CONTRIBUTIONS` for contribution guidelines.

## Support channels

- Community: GitHub Issues on this repo.
- Enterprise: PANTHEON.tech Enterprise Support (https://pantheon.tech).
