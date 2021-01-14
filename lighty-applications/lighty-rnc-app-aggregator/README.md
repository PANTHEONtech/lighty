# Lighty RNC application
Lighty Restconf Netconf application allows to easily initialize, start and use most used OpenDaylight services and optionally add custom business logic.

Most important lighty.io components used are:
- [lighty.io controller](https://github.com/PANTHEONtech/lighty/tree/13.1.x/lighty-core/lighty-controller)
  provides core ODL services (like MDSAL, yangtools, global schema context,...) that are required
  for other services or plugins.
- [RESTCONF northbound plugin](https://github.com/PANTHEONtech/lighty/tree/13.1.x/lighty-modules/lighty-restconf-nb-community)
  provides the RESTCONF interface that is used to communicate with the application using the RESTCONF protocol over the HTTP.
- [NETCONF southbound plugin](https://github.com/PANTHEONtech/lighty/tree/13.1.x/lighty-modules/lighty-netconf-sb)
  enables application to connect to the NETCONF devices using the NETCONF protocol and read/write configuration
  from them or execute RPCs.
- [AAA module](https://github.com/PANTHEONtech/lighty/tree/13.1.x/lighty-modules/lighty-aaa) provides authorization,
  authentication and accounting which for example enables to use Basic Authentication for RESTCONF northbound interface.
  This module is optional and can be turned ON/OFF using application configuration.

![RNC lighty.io architecture](docs/lighty-architecture.png)

## Prerequisites
In order to build and start the RNC lighty.io application locally, you need:
* Java 11 or later 
* maven 3.5.4 or later
* optionally docker for creating and running docker images

## Build and start
To build and start the RNC lighty.io application in the local environment follow these steps:

1. Build the application using this maven command:  
   `mvn clean install -pl :lighty-rnc-app -am`

2. Unpack the application ZIP distribution created in the _lighty-rnc-app/target_ called
   `lighty-rnc-app-<version>-bin.zip`
   
3. Start the application by running it's _.jar_ file:  
   `java -jar lighty-rnc-app-<version>.jar`
   
4. To start the application with custom lighty configuration, use arg -c and for custom initial log4j configuration use argument -l:  
   `java -jar lighty-rnc-app-<version>.jar -c /path/to/config-file -l /path/to/log4j-config-file`  
   
   Example configuration files are located on following path:  
   `lighty-rnc-app/src/main/resources/*`

5. If the application was started successfully, then a log similar should be present in the console:  
   `INFO [main] (Main.java:80) - RNC lighty.io application started in 5989.108ms`

6. Test the RNC lighty.io application. Default RESTCONF port is `8888`

7. The default credential for http requests is login:`admin`, password: `admin`. 
To manipulate user's credentials [idmtool](https://docs.opendaylight.org/projects/aaa/en/stable-aluminium/user-guide.html#idmtool) can be used:
    - create new `etc` directory and create there `org.ops4j.pax.web.cfg` file
    - add this two lines in the `org.ops4j.pax.web.cfg` file: 
       
       `org.osgi.service.http.port.secure = 8888`
       
       `org.osgi.service.http.secure.enabled = true`
        
        where `8888` - default RESTCONF port. It can be different according to your configuration.
    - download `idmtool.py` script from AAA project github repository and place downloaded file into `etc` directory
        
        `curl -O https://raw.githubusercontent.com/opendaylight/aaa/stable/aluminium/aaa-shiro/impl/src/main/resources/idmtool.py`
    - run script (latest python version must be installed)
    
        `python etc/idmtool.py -h`
        
        This will show you the list of possible commands that can be used.
        If you are using self generated ssl sertificate, you should use `-k` argument.
        For example, to show the list of all users:
      
        `python etc/idmtool.py -k admin list-users`
      
        where `admin` - user name (the default name); `list-users` command to show all users.
        It will ask to write password for `admin` user (the default one is `admin`)    
    
    Also, it is possible to configure user's credentials via [REST](https://docs.opendaylight.org/projects/aaa/en/stable-aluminium/user-guide.html#configuration-using-the-restful-web-service)

8. For using ssl connection the correct certificate must be used.
By default, a test certificate is used (`lighty-rnc-module/src/main/resources/keystore/lightyio.jks`).
For generating a JKS file `keytool` utility can be used. Example of command to regenerate keystore with self-signed certificate:

```
keytool -genkey -alias lightyio -keyalg RSA -keystore lightyio.jks -keysize 2048 -ext san=dns:lighty.io,dns:lightyio,dns:restconf.lighty.io,dns:management.lighty.io,dns:localhost,ip:127.0.0.1,ip:0.0.0.0 -validity 2000
```
which creates new `lightyio.jks` keystore file with hostnames: `lighty.io, lightyio, restconf.lighty.io, management.lighty.io, localhost`

The path to correct certificate can be placed in `configuration.json file` (`restconf.keyStoreFile`)
Also you need to provide file type for this certificate (`restconf.keyStoreType`) and password (`restconf.keyStorePassword`)

## Build and start with the docker
To build and start the RNC lighty.io application using docker in the local environment follow these steps:

1. Build the application using this maven command:  
   `mvn clean install -P docker`

2. Start the application using following docker command.   
   `docker run -it --name lighty-rnc --network host --rm lighty-rnc`

3. To start the application with custom lighty configuration( -c ) and custom initial log4j config file( -l ) use command:
   `docker run -it --name lighty-rnc --network host -v /absolute_path/to/config-file/conf.json:/lighty-rnc/conf.json -v /absolute_path/to/config-file/logger.properties:/lighty-rnc/logger.properties --rm lighty-rnc -c conf.json -l logger.properties`
   
   Example configuration files are located on following path:  
   `lighty-rnc-app/src/main/resources/*`

4. If the application was started successfully, then a log similar should be present in the console:  
   `INFO [main] (Main.java:81) - RNC lighty.io application started in 5989.108ms`

5. Test the RNC lighty.io application. Default RESTCONF port is `8888`
## JMX debugging
Java Management Extensions is a tool enabled by default which makes it easy to change runtime
configuration of the application. Among other options, we expose the option to change logging behaviour during runtime
via JMX client which can be connected to running lighty instance.
1. Start the application (see previous sections)
2. Connect the JXM client  
  We recommend using `jconsole` because it is part of the standard java JRE.  
  The command for connecting jconsole to JMX server is:  
    `jconsole <ip-of-running-lighty>:<JMX-port>`, the default JMX-port is 1099.
      
This approach works only if the application is running locally.  
  
If you want to connect the JMX client to application running remotely or containerized (k8s deployment or/and docker),
you need to start the application using following JAVA_OPTS:  
`JAVA_OPTS = -Dcom.sun.management.jmxremote
             -Dcom.sun.management.jmxremote.authenticate=false
             -Dcom.sun.management.jmxremote.ssl=false
             -Dcom.sun.management.jmxremote.local.only=false
             -Dcom.sun.management.jmxremote.port=<JMX_PORT>
             -Dcom.sun.management.jmxremote.rmi.port=<JMX_PORT>
             -Djava.rmi.server.hostname=127.0.0.1`
             
Then run `java $JAVA_OPTS -jar lighty-rnc-app-<version> ...`  
## Connecting JMX client to application running in docker
1. As we said, if we want to be able to connect the JMX, we need to start the app with JAVA_OPTS described in
 previous chapter.  
 In docker the most convenient way to do this is to create env.file and run the docker run with `--env-file env.file` argument
 The env.file must contain the definition of the described JAVA_OPTS environment variable.  
 We also need to publish the container JMX_PORT to host, this is done via `-p <JMX_PORT>:<JMX_PORT>` argument.
 So the docker run command becomes:  
  `docker run -it --name lighty-rnc --env-file env.file -p <JMX_PORT>:<JMX_PORT> ...`
 The rest of the command stays the same as explained in previous chapters.
 2. Connect the JMX client via command `jconsole <ip-of-container>:<JMX_PORT>`.
 ## Connecting JMX client to application deployed in kubernetes
Once you have deployed the application via our provided helm chart in which you enabled jmxRemoting,
you just need to forward the JMX port of the pod in which the instance of the application you want to debug is running.
In kubernetes this is done via `kubectl port-forward` command.
1. Forward the pod's JMX port, run `kubectl port-forward <name-of-the-pod> <JMX_PORT>`  
2. Connect JMX client, run `jconsole <pod-ip>:<JMX-port>`




