ARG SIMULATOR_VERSION="21.3.0"

FROM alpine:3.23.3 AS clone

ARG SIMULATOR_VERSION
RUN apk add git
WORKDIR /netconf-simulator
RUN git clone https://github.com/PANTHEONtech/lighty-netconf-simulator.git -b $SIMULATOR_VERSION

FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG SIMULATOR_VERSION
WORKDIR /lighty-netconf-simulator
COPY --from=CLONE /netconf-simulator/lighty-netconf-simulator /lighty-netconf-simulator
RUN mvn -B install -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG SIMULATOR_VERSION
WORKDIR /lighty-netconf-simulator
COPY --from=BUILD /lighty-netconf-simulator/examples/devices/lighty-network-topology-device/target/ /lighty-netconf-simulator/target

EXPOSE 17380

ENV APP_PATH="/lighty-netconf-simulator/target/lighty-network-topology-device-$SIMULATOR_VERSION.jar"
ENTRYPOINT ["sh", "-c", "java -jar $APP_PATH"]
