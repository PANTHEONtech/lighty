# Example application - ODL RESTCONF to REST

This application starts:
* Lighty Controller
* OpenDaylight RESTCONF plugin


## Prerequisites
The following are the prerequisites for app creation:
1. __Install JDK__ - make sure [JDK 11](https://jdk.java.net/11/) is installed
2. __Install maven__ - make sure you have maven 3.6.3 or later installed
3. __Setup maven__ - make sure you have the proper [settings.xml](https://github.com/opendaylight/odlparent/blob/master/settings.xml) in your ```~/.m2``` directory

## Setup controller project
1. Create project using Maven.
2. Set lighty.io core as Maven parent project
```xml
<parent>
    <groupId>io.lighty.core</groupId>
    <artifactId>lighty-parent</artifactId>
    <version>15.1.1-SNAPSHOT</version>
</parent>    
```
3. Add dependency on RESTCONF north-bound plugin.
```xml
<dependency>
    <groupId>io.lighty.modules</groupId>
    <artifactId>lighty-restconf-nb-community</artifactId>
    <version>15.1.1-SNAPSHOT</version>
  </dependency>
```

## Add YANG model
1. Create dictionary ```src/main/yang```
2. Create YANG module. 
For example simple hello-world RPC hello.yang:
```YANG
module hello {
  yang-version 1.1;
  namespace "urn:opendaylight:params:xml:ns:yang:hello";
  prefix "hello";

  revision "2021-03-21" {
    description "Initial revision of hello model";
  }

  rpc hello-world {
    input {
      leaf name {
        type string;
      }
    }
    output {
      leaf greeting {
        type string;
      }
    }
  }
}
```
## OpenDaylight – YANG – Maven
The ODL yang-maven-plugin is used to generate Java classes and interfaces from the Yang mode.
1. Add this plugin to Maven build in ```pom.xml```
```xml
<build>
    <plugins>
        <!-- Yangtools, generate yang -->
        <plugin>
            <groupId>org.opendaylight.yangtools</groupId>
            <artifactId>yang-maven-plugin</artifactId>
            <version>7.0.9</version>
            <dependencies>
                <dependency>
                    <groupId>org.opendaylight.mdsal</groupId>
                    <artifactId>mdsal-binding-java-api-generator</artifactId>
                    <version>8.0.7</version>
                </dependency>
            </dependencies>
            <executions>
                <execution>
                    <id>binding</id>
                    <goals>
                        <goal>generate-sources</goal>
                    </goals>
                    <configuration>
                        <inspectDependencies>true</inspectDependencies>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
2. Add dependency for annotations used in generated classes
```xml
<dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-annotations</artifactId>
    <version>3.1.12</version>
</dependency>
```
3. Run ```mvn clean install``` in project dictionary to generate Java classes from the Yang model to ```target/generated-sources/BindingJavaFileGenerator```
4. Add YANG model to default controller configuration:
```JAVA
Set<YangModuleInfo> moduleInfos = new HashSet<>();
ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
    moduleInfos.add(yangModelBindingProvider.getModuleInfo());
}
singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(moduleInfos);
```
 or in custom config file  [example](src/main/resources/sampleConfigSingleNode.json).

## Implement the HelloWorld RPC API
In ```target/generated-sources/BindingJavaFileGenerator``` an interface called ```HelloService``` is generated, which defines the RPC as a Java method with the input container as argument and output container as a return value.
1. Provide an implementation for this generated interface 
```JAVA
public class HelloProvider implements HelloService {
    @Override
    public ListenableFuture<RpcResult<HelloWorldOutput>> helloWorld(HelloWorldInput input) {
        HelloWorldOutputBuilder helloBuilder = new HelloWorldOutputBuilder();
        String greeting;
        // TODO: generate greeting, for example REST call to external system 
        helloBuilder.setGreeting(greeting)
        return RpcResultBuilder.success(helloBuilder.build()).buildFuture();
    }
}
```
2. Create registration util class for provided implementation
```JAVA
public class HelloProviderRegistration {
    private HelloProviderRegistration() {
        // Utility class
    }

    public static ObjectRegistration<HelloService> registerRpc(final LightyController lightyController) {
        final var actionProviderService = lightyController.getServices().getRpcProviderService();
        return actionProviderService.registerRpcImplementation(HelloService.class, new HelloProvider());
    }
}
```
3. After starting LightyController register RPC
```
HelloProviderRegistration.registerRpc(lightyController);
```

## Build and Run
build the root project: ```mvn clean install```

### Start this demo example
* build the project using ```mvn clean install```
* go to target directory ```cd lighty-examples/lighty-community-restconf-rest-app/target```
* unzip example application bundle ```unzip  lighty-community-restconf-rest-app-15.1.1-SNAPSHOT-bin.zip```
* go to unzipped application directory ```cd lighty-community-restconf-rest-app-15.1.1-SNAPSHOT```
* start controller example controller application ```java -jar lighty-community-restconf-rest-app-15.1.1-SNAPSHOT.jar```

### Test example application
Once example application has been started using command ```java -jar lighty-community-restconf-dom-actions-app-<VERSION>.jar```
RESTCONF web interface is available at URL ```http://localhost:8888/restconf/*```

* __POST__ ```http://localhost:8888/restconf/operations/hello:hello-world```
1. Use the payload like the following:
```json
{
  "input": {
    "name": "Your Name"
  }
}
```
2. You will receive output like the following:
```json
{
  "hello:output": {
    "greeting": "Hello Your Name"
  }
}
```