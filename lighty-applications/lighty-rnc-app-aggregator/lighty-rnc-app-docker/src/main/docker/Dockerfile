FROM alpine:latest

RUN apk add openjdk11-jre

WORKDIR /lighty-rnc

# Create new user 'rnc-user' with uid 1000 and group 2000
RUN addgroup -S 2000
RUN adduser -S -D -h /lighty-rnc rnc-user 2000 --uid 1000
# Move ownership of /lighty-rnc folder to rnc-user
RUN chown -R rnc-user:2000 /lighty-rnc
# Switch to rnc-user
USER rnc-user

COPY LICENSE ${lighty.app.name} entrypoint.sh ./

EXPOSE 8888

ENTRYPOINT ["./entrypoint.sh", "${lighty.app.jar}"]