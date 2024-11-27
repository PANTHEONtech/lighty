# YANG models in lighty.io
[YANG 1.0](https://tools.ietf.org/html/rfc6020) and [YANG 1.1](https://tools.ietf.org/html/rfc7950) models are essential part of programming for [OpenDaylight](https://www.opendaylight.org/) __model-driven architecture__.
Standardized yang models are already available as maven artifacts as part of OpenDaylight project.
To mention just some:

* __MD-SAL__ [pre-packaged YANG models](https://nexus.opendaylight.org/content/repositories/opendaylight.release/org/opendaylight/mdsal/model/)
* __Controller__ [pre-packaged YANG models](https://nexus.opendaylight.org/content/repositories/opendaylight.release/org/opendaylight/controller/model/) 

In case your project requires special Yang model or needs to alter existing one, follow procedures below or check links below for YANG repositories.

### Create and deploy own YANG model
This guide explains in detail how to create your own Yang model, 
use it in lighty.io project and add it into LightyController global schema context.

#### 1. Write your own YANG model
It is as simple as creating own text file ``my-model.yang`` with content:
```
module my-model.yang {

  yang-version 1.1;
  namespace "urn:example:my-model";
  prefix "mymodel";
  revision 2018-09-14;

  container device {
    list server {
      key name;
      leaf name {
        type string;
      }
    }
  }

}
```

#### 2. Create maven project for your model
Bare yang file can not be used without generating java bindings for it. OepnDaylighty provides for this purpose 
maven plugin which makes this step really easy. Generated java binding code is used in your projects, so you can
access model nad DataStore in type safe way.

* Create maven project structure:
```
my-model/src/main/yang/my-model.yang
my-model/pom.xml
```

* Content of pom.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.lighty.core</groupId>
        <artifactId>lighty-binding-parent</artifactId>
        <version>20.3.0</version>
        <relativePath/>
    </parent>

    <groupId>org.mygroup</groupId>
    <artifactId>my-model</artifactId>
    <version>1.0.1-SNAPSHOT</version>

</project>
```

#### 3. Add your model into global schema context
On LightyController startup, it is necessary to register all YANG models with dependencies. This is how you do it:

* Add dependency on model artifact into your controller project pom.xml
```
  <dependecy>
    <groupId>org.mygroup</groupId>
    <artifactId>my-model</artifactId>
    <version>1.0.1-SNAPSHOT</version>
  </dependecy>
```

* Use generated java bindings in LightyController initialization.
```
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public class Main {

    private static final Set<YangModuleInfo> MY_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.urn.example.my.model.rev180914.YangModuleInfoImpl.getInstance()
    );

    public static void main(String[] args) {
        ControllerConfiguration controllerConfiguration = ControllerConfigUtils
                .getDefaultSingleNodeConfiguration(MY_MODELS);
        LightyController lightyController = new LightyControllerBuilder()
                .from(controllerConfiguration)
                .build();
        lightyController
                .start()
                .get();
    }

}
```

### More models 
If you need standardized [IETF](https://www.ietf.org/) models, or include existing ones:

* [YANG catalog](https://yangcatalog.org/)
* [OpenConfig YANG models](https://github.com/openconfig/public)
* [OpenConfig standard IETF models](https://github.com/openconfig/yang)

### More examples
This repository contains [example projects](../lighty-examples/README.md) where you can see how YANG models are used.
See also __example-data-center__, __lighty-test-models__ and __lighty-toaster__ as model maven artifacts examples in this project.
