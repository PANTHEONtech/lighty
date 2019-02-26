FROM openjdk:8u191-jre-alpine

COPY ./target/lighty-community-restconf-ovsdb-app-9.2.1-SNAPSHOT-bin.zip /

RUN unzip /lighty-community-restconf-ovsdb-app-9.2.1-SNAPSHOT-bin.zip && rm /lighty-community-restconf-ovsdb-app-9.2.1-SNAPSHOT-bin.zip

##libstdc++ is required by leveldbjni-1.8 (Akka dispatcher)
#Uncaught error from thread [opendaylight-cluster-data-akka.persistence.dispatchers.default-plugin-dispatcher-22]: Could not load library. Reasons: [no leveldbjni64-1.8 in java.library.path, no leveldbjni-1.8 in java.library.path, no leveldbjni in java.library.path, /tmp/libleveldbjni-64-1-3166161234556196376.8: Error loading shared library libstdc++.so.6: No such file or directory (needed by /tmp/libleveldbjni-64-1-3166161234556196376.8)], shutting down JVM since 'akka.jvm-exit-on-fatal-error' is enabled for ActorSystem[opendaylight-cluster-data]
RUN apk add --update libstdc++ curl && \
    rm -rf /var/cache/apk/*

WORKDIR /lighty-community-restconf-ovsdb-app-9.2.1-SNAPSHOT

EXPOSE 8185
EXPOSE 8888
EXPOSE 6640
EXPOSE 2550

CMD java -jar lighty-community-restconf-ovsdb-app-9.2.1-SNAPSHOT.jar sampleConfigSingleNode.json
