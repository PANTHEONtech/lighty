Lighty ovsdb-sb with lighty-restconf-nb-community
====================================

This application contains and will start these modules:
* LightyController
* Lighty Community RestConf
* Ovsdb-sb plugin


Build and Run
-------------
Build the project using maven command: ```mvn clean install``` from
lighty-community-restconf-ovsdb-app directory. This will create *.zip* archive
in target directory. Extract this archive. Start application from command line from unzipped
directory with:
```
java -jar lighty-community-restconf-ovsdb-app-9.2.1-SNAPSHOT.jar sampleConfigSingleNode.json
```
To start with script, go into unzipped directory. Than run ```./start-ovsdb.sh```
You can change sampleConfigSingleNode.json as needed, or create new configuration file.

How to use Ovsdb example application
------------------------------------
In order to try ovsdb application talking mode, follow these steps:
- Import postman collection from application resources. (You can also rewrite the commands
into curl.)
- Start application with commands in Build and Run section.
- Check if ovsdb is initialized with ```ovsdb:1``` request.
- Check whether your application is running with ```controller``` request.
- You need to have openvswitch-switch installed. (tested with version 2.9.0)
```
sudo apt install openvswitch-switch
```
- Start OpenVSwitch with ```sudo ovs-vsctl set-manager ptcp:64400```
this will start it, and it will listen on port 64400 (you can use any
unused port.)
- Configure ovsdb application to request updates from openvswitch with
 ```setup ovsdb to talk``` request.
- Check topology with ```ovsdb:1``` or ```network-topology``` requests.
- To remove configuration use ``delete configuration`` request
- Reset OpenVSwitch with ```sudo ovs-vsctl emer-reset```

In order to try ovsdb application listening mode, follow these steps:
- Start application with commands in Build and Run section.
- Application is already configured to listen on port 6640.
- Start OpenVSwitch with ```sudo ovs-vsctl set-manager tcp:127.0.0.1:6640```
- As application is already listening simply check topology status with
```ovsdb:1``` or ```network-topology``` requests.

If is not delete ovsdb configuration before turning off application. Ovsdb will be looking for this configuration in next launch. 


Building and running Docker Image
---------------------------------
- Go into app main directory > lighty-community-restconf-ovsdb-app
- Make sure your app is built beforehand.
- Run ```docker build -t ovsdb .``` to build image under 'ovsdb' name.
- Start container with ```docker run -it --rm --name ovsdb_container ovsdb```
- Find out container ip with 
 ```
 docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" ovsdb_container
 ```
 - Use the IP for your restconf calls.

Setup logging
-------------
Default logging configuration may be overwritten by JVM option
```-Dlog4j.configuration=/path/to/log4j.properties```

Content of ```log4j.properties``` is described [here](https://logging.apache.org/log4j/2.x/manual/configuration.html).

Further reading
---------------
[Opendaylight documentation](https://docs.opendaylight.org/en/stable-fluorine/user-guide/ovsdb-user-guide.html).