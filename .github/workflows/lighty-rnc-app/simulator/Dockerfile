FROM alpine:3.18.2 as clone
RUN apk add git
WORKDIR /netconf-simulator
RUN git clone https://github.com/PANTHEONtech/lighty-netconf-simulator.git -b 17.3.0

FROM maven:3.8-eclipse-temurin-17-alpine as build
WORKDIR /lighty-netconf-simulator
COPY --from=clone /netconf-simulator/lighty-netconf-simulator /lighty-netconf-simulator
RUN mvn -B install -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /lighty-netconf-simulator
COPY --from=build /lighty-netconf-simulator/examples/devices/lighty-network-topology-device/target/ /lighty-netconf-simulator/target

EXPOSE 17380

ENTRYPOINT ["java", "-jar", "/lighty-netconf-simulator/target/lighty-network-topology-device-17.3.0.jar"]
