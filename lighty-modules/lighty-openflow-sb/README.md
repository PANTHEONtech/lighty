# Openflow South-Bound Plugin

Lighty-openflow (```io.lighty.modules```)
is Lighty's version of openflow plugin.

## How to build and run openflow plugin

1. Add maven dependency in your pom.xml file.
```
<dependency>
    <groupId>io.lighty.modules</groupId>
    <artifactId>lighty-openflow-sb</artifactId>
    <version>12.3.1-SNAPSHOT</version>
</dependency>
```

2. Initialize and start LightyController. We have to add additional model paths
to LightyController configuration that are used by other modules we are going to start.
We will use Community-Restconf and Lighty-OFP in this example,
so we need to add their model paths to controller configuration.

```
// initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
ArrayList<String> models = new ArrayList<>();
models.addAll(RestConfConfigUtils.MAVEN_MODEL_PATHS);
models.addAll(OpenflowConfigUtils.OFP_MAVEN_MODEL_PATHS);
ControllerConfiguration singleNodeConfiguration =
        ControllerConfigUtils.getDefaultSingleNodeConfiguration(models);
LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
LightyController lightyController = lightyControllerBuilder.from(singleNodeConfiguration).build();
lightyController.start();
```
3. Initialize and start Openflow SouthBound plugin using services from LightyController.

```
LOG.info("Starting openflow...");
OpenflowSouthboundPluginBuilder opfBuilder = new OpenflowSouthboundPluginBuilder();
opfBuilder.from(
        new OpenflowpluginConfiguration(),
        lightyController.getServices()
);
ofplugin = opfBuilder.build();
ListenableFuture<Boolean> ofpStarted = ofplugin.start();
ofpStarted.get();
LOG.info("Openflow started");
```

4. After executing code from previous steps all should be set and running.
Openflow SouthBound plugin is now ready to use.

## Example switches connect
If openflow-plugin is running you can connect example switches with mininet.

```
sudo mn --controller=remote,ip=<IP_OF_RUNNING_LIGHTY> --topo=tree,1 --switch ovsk,protocols=OpenFlow13
``` 

## Clustering
Clustering depends on configuration of underlying akka actor system.
Openflowplugin itself does not have independent settings for clustering.

## Example Openflow plugin application
Example application can be found under lighty-examples project.
