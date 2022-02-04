FROM alpine:latest

RUN apk add openjdk11-jre

WORKDIR /lighty-rcgnmi

# Create new user 'rcgnmi-user' with uid 1000 and group 2000
RUN addgroup -S 2000
RUN adduser -S -D -h /lighty-rcgnmi rcgnmi-user 2000 --uid 1000
# Move ownership of /lighty-rcgnmi folder to rcgnmi-user
RUN chown -R rcgnmi-user:2000 /lighty-rcgnmi
# Switch to rcgnmi-user
USER rcgnmi-user

COPY LICENSE ${lighty.app.name} entrypoint.sh ./

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh", "${lighty.app.jar}"]