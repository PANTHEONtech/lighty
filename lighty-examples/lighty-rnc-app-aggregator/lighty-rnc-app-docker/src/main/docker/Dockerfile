FROM openjdk:11-jre-slim

WORKDIR /lighty-rnc
COPY LICENSE ${lighty.app.name} ./

EXPOSE 8888

ENTRYPOINT ["java", \
            "-Dcom.sun.management.jmxremote", \
            "-Dcom.sun.management.jmxremote.authenticate=false", \
            "-Dcom.sun.management.jmxremote.ssl=false", \
            "-Dcom.sun.management.jmxremote.local.only=false", \
            "-Dcom.sun.management.jmxremote.port=1099", \
            "-Dcom.sun.management.jmxremote.rmi.port=1099", \
            "-Djava.rmi.server.hostname=127.0.0.1", \
            "-jar", "${lighty.app.jar}" ]