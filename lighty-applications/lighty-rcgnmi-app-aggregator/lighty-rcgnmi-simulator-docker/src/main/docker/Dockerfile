FROM openjdk:11-jre-slim

WORKDIR /lighty-rcgnmi-simulator
COPY LICENSE ${lighty.simulator.name} entrypoint.sh ./

EXPOSE 3333

ENTRYPOINT ["./entrypoint.sh", "${lighty.simulator.jar}", "./"]