If RNC app build fails in Lift environmnet due to missing dependency:


1. To get and validate RNC app module, run maven command:
   ```
   mvn -Dmaven.repo.local=/home/mibanik/.m2/repo-test validate -pl :lighty-rnc-app -am
   ```
   and gather the list of needed modules.
   
   e.g.
   ```
   [INFO] Reactor Build Order:
   [INFO] 
   [INFO] io.lighty.core:lighty-minimal-parent                               [pom]
   [INFO] io.lighty.core:lighty-parent                                       [pom]
   [INFO] io.lighty.resources:log4j-properties                               [jar]
   [INFO] io.lighty.core:lighty-common                                       [jar]
   [INFO] io.lighty.core:lighty-clustering                                   [jar]
   [INFO] io.lighty.core:lighty-binding-parent                               [pom]
   [INFO] io.lighty.models.test:lighty-test-models                           [jar]
   [INFO] io.lighty.models.test:lighty-toaster                               [jar]
   [INFO] io.lighty.core:lighty-codecs-util                                  [jar]
   [INFO] io.lighty.resources:singlenode-configuration                       [jar]
   [INFO] io.lighty.core:lighty-controller                                   [jar]
   [INFO] lighty-aaa-encryption-service                                      [jar]
   [INFO] io.lighty.modules:lighty-netconf-sb                                [jar]
   [INFO] io.lighty.modules:lighty-jetty-server                              [jar]
   [INFO] io.lighty.modules:lighty-restconf-nb-community                     [jar]
   [INFO] io.lighty.modules:lighty-aaa                                       [jar]
   [INFO] lighty-rnc-module                                                  [jar]
   [INFO] io.lighty.resources:start-script                                   [jar]
   [INFO] io.lighty.resources:controller-application-assembly                [jar]
   [INFO] io.lighty.core:lighty-app-parent                                   [pom]
   [INFO] lighty-rnc-app                                                     [jar]
   ```

2. extract module names

   e.g.
   ```
   lighty-minimal-parent
   lighty-parent
   log4j-properties
   lighty-common
   lighty-clustering
   lighty-binding-parent
   lighty-test-models
   lighty-toaster
   lighty-codecs-util
   singlenode-configuration
   lighty-controller
   lighty-aaa-encryption-service
   lighty-netconf-sb
   lighty-jetty-server
   lighty-restconf-nb-community
   lighty-aaa
   lighty-rnc-module
   start-script
   controller-application-assembly
   lighty-app-parent
   lighty-rnc-app
   ```

   Add also `dependency-versions` & `lighty-bom` to the list, because those are used for dependency management and `-am`
   parameter did not include them into build reactor.


3. update project/modules in `.lift/top.level.pom.xml`

   1. put nodule names inside `<module>` tag
   ```
   <module>lighty-minimal-parent</module>
   <module>lighty-parent</module>
   <module>log4j-properties</module>
   <module>lighty-common</module>
   <module>lighty-clustering</module>
   <module>lighty-binding-parent</module>
   <module>lighty-test-models</module>
   <module>lighty-toaster</module>
   <module>lighty-codecs-util</module>
   <module>singlenode-configuration</module>
   <module>lighty-controller</module>
   <module>lighty-aaa-encryption-service</module>
   <module>lighty-netconf-sb</module>
   <module>lighty-jetty-server</module>
   <module>lighty-restconf-nb-community</module>
   <module>lighty-aaa</module>
   <module>lighty-rnc-module</module>
   <module>start-script</module>
   <module>controller-application-assembly</module>
   <module>lighty-app-parent</module>
   <module>lighty-rnc-app</module>
   ```
   2. place temporarily modules into pom.xml
   3. Intellij Idea can help you to complete/add location for module
   ```
       <modules>
           <module>lighty-core/lighty-minimal-parent</module>
           <module>lighty-core/lighty-parent</module>
           <module>lighty-resources/log4j-properties</module>
           <module>lighty-core/lighty-common</module>
           <module>lighty-core/lighty-clustering</module>
           <module>lighty-core/lighty-binding-parent</module>
           <module>lighty-models/test/lighty-test-models</module>
           <module>lighty-models/test/lighty-toaster</module>
           <module>lighty-core/lighty-codecs-util</module>
           <module>lighty-resources/singlenode-configuration</module>
           <module>lighty-core/lighty-controller</module>
           <module>lighty-modules/lighty-aaa-aggregator/lighty-aaa-encryption-service</module>
           <module>lighty-modules/lighty-netconf-sb</module>
           <module>lighty-modules/lighty-jetty-server</module>
           <module>lighty-modules/lighty-restconf-nb-community</module>
           <module>lighty-modules/lighty-aaa-aggregator/lighty-aaa</module>
           <module>lighty-applications/lighty-rnc-app-aggregator/lighty-rnc-module</module>
           <module>lighty-resources/start-script</module>
           <module>lighty-resources/controller-application-assembly</module>
           <module>lighty-core/lighty-app-parent</module>
           <module>lighty-applications/lighty-rnc-app-aggregator/lighty-rnc-app</module>
   
           <module>lighty-core/dependency-versions</module>
           <module>lighty-core/lighty-bom</module>
       </modules>
   ```
   4. update modules in `.lift/top.level.pom.xml`

      (and revert modules in pom.xml where you temporarily put the modules)

4. Lift now can proceed analysis on updated RNC app
   1. Lift, thanks to setup.sh, will use `.lift/top.level.pom.xml` and proceed build with this pom.xml used as top-level pom.xml