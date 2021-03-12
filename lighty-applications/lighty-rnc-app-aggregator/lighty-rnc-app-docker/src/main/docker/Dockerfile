FROM openjdk:11-jre-slim

WORKDIR /lighty-rnc
COPY LICENSE ${lighty.app.name} entrypoint.sh ./

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh", "${lighty.app.jar}"]