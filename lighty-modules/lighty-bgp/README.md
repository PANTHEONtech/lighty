# BGP south-bound plugin
Lighty BGP plugin provides Opendaylight's [BGPCEP](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/index.html)
services.

## How to use

To use lighty BGP plugin in your project:

* Add the following dependency:
```
  <dependency>
    <groupId>io.lighty.modules</groupId>
    <artifactId>lighty-bgp</artifactId>
    <version>20.4.0-SNAPSHOT</version>
  </dependency>
```
* Initialize and start BgpModule instance:
```
  final BgpModule bgpModule = new BgpModule(lightyServices);
  bgpModule.start();
```

## Initial configuration
Opendaylight's BGPCEP default [RIB policies configuration](https://docs.opendaylight.org/projects/bgpcep/en/latest/bgp/bgp-user-guide-rib-config-policies.html)
are loaded on startup i.e `default-odl-export-policy` and `default-odl-import-policy`. The configuration file
that contains these policies is loaded directly from [BGPCEP project](https://github.com/opendaylight/bgpcep/blob/master/bgp/config-example/src/main/resources/initial/routing-policy-config.xml).
