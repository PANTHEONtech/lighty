# lighty.io gNMI south-bound connector

This module provides tools to manage and communicate with gNMI devices.
Details about gNMI can be found in [official specification](https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md).

Notable classes are:
* [GnmiSessionManager](src/main/java/io/lighty/modules/gnmi/connector/session/api/SessionManager.java) - creates and manages
  sessions to gNMI devices. Instance can be created by
  [GnmiSessionManagerFactory](src/main/java/io/lighty/modules/gnmi/connector/session/SessionManagerFactory.java)

* [GnmiSession](src/main/java/io/lighty/modules/gnmi/connector/gnmi/session/api/GnmiSession.java) - provides
  get/set/capabilities/subscribe operations to communicate with gNMI devices.
  Instance can be created by
  [GnmiSessionFactory](src/main/java/io/lighty/modules/gnmi/connector/gnmi/session/impl/GnmiSessionFactory.java)

### gNMI certificates
For proper gNMI functionality, the valid SSl certificates are necessary. The certificates used for testing purposes are
valid for 3650 days (approx 10 years). The script [generate_certs.sh](src/main/scripts/generate_certs.sh)
can help with generating new certificates, in case the expiration passed.
The generated certificates then can be used for clients & servers in this project.

Expiration dates of certificates can be checked with command
```
openssl x509 -in <PATH_TO_CERTIFICATE> -dates -noout
```
where output will look similar to
```
$ openssl x509 -in certs/server.crt -dates -noout
notBefore=Apr  7 09:15:26 2021 GMT
notAfter=Apr  7 09:15:26 2022 GMT
```
