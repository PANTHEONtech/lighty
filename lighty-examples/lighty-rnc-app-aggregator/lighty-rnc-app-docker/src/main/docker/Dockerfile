FROM openjdk:11-jre-slim

WORKDIR /lighty-rnc
COPY LICENSE ${lighty.app.name} ./
COPY entrypoint.sh ${lighty.app.name} ./

ENV APP_JAR_NAME=${lighty.app.jar}

EXPOSE 8888

RUN chmod +x ./entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]