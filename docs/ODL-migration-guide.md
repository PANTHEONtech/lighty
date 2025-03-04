# How-To migrate from ODL to lighty.io
This guide describes migration procedure of SDN application from [OpenDaylight](https://www.opendaylight.org/)/[Karaf](https://karaf.apache.org/) to [lighty.io](https://github.com/PANTHEONtech/lighty).
It contains summary of practical experiences based on real-life ODL project migrations.

### 1. ODL application review
Each ODL/Karaf application consists of [OSGi bundles](https://www.osgi.org/developer/architecture/) and [Karaf features](https://karaf.apache.org/manual/latest/#_provisioning). This step is designed to identify
key parts of application. We need the list of karaf features and OSGi bundles 
activated for every feature.

### 2. Application bundle review
Each OSGi bundle must be activated during feature install. Based on previous step, for each bundle, we ned to check:
- __bundle activators__ - classes implementing ```org.osgi.framework.BundleActivator```
- __configuration loading__ - application code and blueprints dealing with configuration files in Karaf's ```etc/``` directory.
- __blueprints used__ - some OSGi bundles may use [Blueprint deployers](https://karaf.apache.org/manual/latest/#_blueprint_deployer)

### 3. Create lighty.io module
Bundle activation code will not work in lighty.io nor will work blueprint subsystem. Based on previous steps, it is necessary to implement ```io.lighty.core.controller.api.LightyModule``` 
for each logical component of application. Each implementation of LightyModule will start and stop (init and shutdown)
services from your application. LightyModule does the job of bundle activators and blueprints. 
lighty.io offers also ```io.lighty.core.controller.api.AbstractLightyModule``` implementation which makes implementation of LightyModule even easier.
Example below activates MyService bean and uses LightyServices for dependency injection of DataBroker service.
```
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;

public class MyModule extends AbstractLightyModule {

    private LightyServices lightyServices;
    private MyService myService;

    public MyModule(LightyServices lightyServices) {
        this.lightyServices = lightyServices;
        this.myService = new MyService(lightyServices.getBindingDataBroker());
    }

    @Override
    protected boolean initProcedure() {
        myService.init();
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        myService.shutdown();
        return true;
    }
    
}
```

### 4. Create lighty.io application
Once LightyModule(s) are implemented, application as whole may be started. LightyControllerImpl is started as first LightyModule.
Other LightyModule(s) follow and it is up to user to create and manage start sequence as well as shutdown sequence. Code snippet below is just example of application 
using MyModule instance.

```
public static void main(String[] args) {
    ...
    //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Pekko)
    LightyController lightyController = new LightyControllerBuilder()
        .from(controllerConfiguration)
        .build();
    lightyController.start().get();
    
    //2. start MyModule instance
    MyModule myModule = new MyModule(lightyController.getServices());
    myModule.start().get();
    ...
}
```
