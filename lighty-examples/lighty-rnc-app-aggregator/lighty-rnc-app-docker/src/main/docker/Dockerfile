FROM openjdk:11-jre-slim

WORKDIR /lighty-rnc
COPY LICENSE ${lighty.app.name} ./
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "${lighty.app.jar}"]