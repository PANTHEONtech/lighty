Lighty Google Guice Dependency Injection Extension
==================================================
This project provides Lighty Core module for
[Google Guice](https://github.com/google/guice)
Dependency Injection Extension. This extension is available for Lighty projects
Application projects [using](https://github.com/google/guice/wiki/Motivation) Google Guice as DI.

```
io.lighty.core.controller.guice.LightyControllerModule
```

How to use it
-------------
1. Add dependency into your project
```
   <dependency>
      <groupId>io.lighty.core</groupId>
      <artifactId>lighty-controller-guice-di</artifactId>
   </dependency>
```

2. Use ```LightyControllerModule``` to initialize Guice Injector
```
  //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Pekko)
  LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
  lightyController = lightyControllerBuilder
                .from(defaultSingleNodeConfiguration)
                .build();
  lightyController.start().get();

  //2. initialize Lighty Controller Module custom application beans
  LightyControllerModule lightyModule = new LightyControllerModule(lightyController.getServices());
  MyApplicationModule myApplicationModule = new MyApplicationModule();
  Injector injector = Guice.createInjector(lightyModule, myApplicationModule);
  ...
```

3. Inject & use ODL services in your code
```
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DataStoreServiceImpl implements DataStoreService {

    @Inject
    @Named("BindingDataBroker")
    private DataBroker bindingDataBroker;

    ...
}
```
