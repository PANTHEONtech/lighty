Lighty Spring.io Dependency Injection Extension
===============================================
This project provides Lighty Core module for
[Spring.io](https://spring.io/)
Dependency Injection Extension. This extension is available for Lighty projects
application projects using [Spring.io](https://spring.io/) as runtime environment.

```
io.lighty.core.controller.spring.LightyCoreSpringConfiguration
```

How to use it
-------------
1. Add dependency into your project
```
   <dependency>
      <groupId>io.lighty.core</groupId>
      <artifactId>lighty-controller-spring-di</artifactId>
   </dependency>
```

2. Extend ```LightyCoreSpringConfiguration``` class to apply lighty.io services bean configuration and 
initialize ```LightyController``` as spring bean with whichever custom configuration you want
```
@Configuration
public class LightyConfiguration extends LightyCoreSpringConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyConfiguration.class);

    @Override
    protected LightyController initLightyController() throws LightyLaunchException, InterruptedException {
        try {
            LOG.info("Building LightyController Core");
            final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
            final Set<YangModuleInfo> mavenModelPaths = new HashSet<>();
            mavenModelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
            final LightyController lightyController = lightyControllerBuilder
                    .from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(mavenModelPaths))
                    .build();
            LOG.info("Starting LightyController (waiting 10s after start)");
            final ListenableFuture<Boolean> started = lightyController.start();
            started.get();
            LOG.info("LightyController Core started");

            return lightyController;
        } catch (ConfigurationException | ExecutionException e) {
            throw new LightyLaunchException("Could not init LightyController", e);
        }
    }

    @Override
    protected void shutdownLightyController(@Nonnull LightyController lightyController) throws LightyLaunchException {
        try {
            LOG.info("Shutting down LightyController ...");
            lightyController.shutdown();
        } catch (Exception e) {
            throw new LightyLaunchException("Could not shutdown LightyController", e);
        }
    }
    ...
}
```

3. Inject & use ODL services in your code
```
@RestController
@RequestMapping(path = "netconf")
public class NetconfDeviceRestService {

    private static final InstanceIdentifier<Topology> NETCONF_TOPOLOGY_IID = InstanceIdentifier
        .create(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId("topology-netconf")));
    private static final long TIMEOUT = 1;
    private static final InstanceIdentifier<Toaster> TOASTER_IID = InstanceIdentifier.create(Toaster.class);

    @Autowired
    @Qualifier("BindingDataBroker")
    private DataBroker dataBroker;

    @Autowired
    private MountPointService mountPointService;

    ...
}
```
