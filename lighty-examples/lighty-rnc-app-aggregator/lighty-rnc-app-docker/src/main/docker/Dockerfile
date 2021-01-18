FROM openjdk:11-jre-slim

WORKDIR /lighty-rnc
COPY LICENSE ${lighty.app.name} entrypoint.sh ./

ENV APP_JAR_NAME=${lighty.app.jar}

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh"]