FROM eclipse-temurin:25-jre-ubi10-minimal
WORKDIR /data

ENV API_PORT=8080
ENV UI_PORT=8081

# Copy Files
COPY ./target/local-router.jar /home/container

# Healthcheck
HEALTHCHECK --interval=5s --timeout=5s --retries=6 --start-period=5s \
    CMD curl -f "http://localhost:$API_PORT/_healthcheck" || exit 1

# Entrypoint
CMD ["sh", "-c", "java $JAVA_OPTS -jar /home/container/local-router.jar"]

EXPOSE $API_PORT/tcp
EXPOSE $UI_PORT/tcp