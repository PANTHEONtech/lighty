FROM openjdk:11-jre-slim

WORKDIR /lighty-rnc
COPY LICENSE ${lighty.app.name} ./

EXPOSE 8888

ENTRYPOINT [ "sh", "-c", "java $JXM_REMOTE_OPTS -jar ${lighty.app.jar}" ]