FROM eclipse-temurin:21-jre-alpine

WORKDIR /lighty-rnc

# Create new user 'rnc' with UID 1000
# https://wiki.alpinelinux.org/wiki/Setting_up_a_new_user
# https://github.com/nodejs/docker-node/blob/main/docs/BestPractices.md
RUN addgroup --system rnc && adduser --system --ingroup rnc --uid 1000 rnc
# Move ownership of /lighty-rnc folder to rnc user
RUN chown -R rnc:rnc /lighty-rnc
# Switch to rnc user
USER rnc

COPY LICENSE ${lighty.app.name} entrypoint.sh ./

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh", "${lighty.app.jar}"]