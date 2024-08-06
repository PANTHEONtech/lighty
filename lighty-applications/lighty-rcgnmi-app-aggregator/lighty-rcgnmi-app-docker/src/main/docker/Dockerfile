FROM eclipse-temurin:21-jre-alpine

WORKDIR /lighty-rcgnmi

# Create new user 'rcgnmi' with UID 1000
# https://wiki.alpinelinux.org/wiki/Setting_up_a_new_user
# https://github.com/nodejs/docker-node/blob/main/docs/BestPractices.md
RUN addgroup --system rcgnmi && adduser --system --ingroup rcgnmi --uid 1000 rcgnmi
# Move ownership of /lighty-rcgnmi folder to rcgnmi user
RUN chown -R rcgnmi:rcgnmi /lighty-rcgnmi
# Switch to rcgnmi user
USER rcgnmi

COPY LICENSE ${lighty.app.name} entrypoint.sh ./

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh", "${lighty.app.jar}"]