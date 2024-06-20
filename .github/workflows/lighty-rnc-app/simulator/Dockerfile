ARG SIMULATOR_VERSION="20.1.0"

FROM alpine:3.19.1 as clone

ARG SIMULATOR_VERSION
RUN apk add git
WORKDIR /netconf-simulator
RUN git clone https://github.com/PANTHEONtech/lighty-netconf-simulator.git -b $SIMULATOR_VERSION

FROM maven:3.9-eclipse-temurin-17-alpine as build
ARG SIMULATOR_VERSION
WORKDIR /lighty-netconf-simulator
COPY --from=clone /netconf-simulator/lighty-netconf-simulator /lighty-netconf-simulator
RUN mvn -B install -DskipTests

FROM eclipse-temurin:17-jre-alpine
ARG SIMULATOR_VERSION
WORKDIR /lighty-netconf-simulator
COPY --from=build /lighty-netconf-simulator/examples/devices/lighty-network-topology-device/target/ /lighty-netconf-simulator/target

EXPOSE 17380

ENV APP_PATH="/lighty-netconf-simulator/target/lighty-network-topology-device-$SIMULATOR_VERSION.jar"
ENTRYPOINT java -jar $APP_PATH
