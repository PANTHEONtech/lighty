FROM alpine:latest

RUN apk add openjdk11-jre

WORKDIR /lighty-rcgnmi
COPY LICENSE ${lighty.app.name} entrypoint.sh ./

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh", "${lighty.app.jar}"]