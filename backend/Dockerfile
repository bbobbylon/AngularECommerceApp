# syntax=docker/dockerfile:1
# --- Build stage: compile + package the Spring Boot jar (tests run against H2) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# --- Run stage: slim JRE image ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Run as a non-root user (defense in depth: a compromised process isn't root in the container).
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
# The app reads server.port (8585) and datasource from env in the cloud.
EXPOSE 8585
# Container-aware heap sizing (the JVM respects cgroup limits); extend/override via JAVA_OPTS.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
# `exec` so the JVM becomes PID 1 and receives SIGTERM on `docker stop` / pod eviction — which is
# what triggers the app's graceful shutdown (server.shutdown=graceful). Without exec, sh would be
# PID 1 and swallow the signal. Orchestrators should health-check the actuator probes:
#   /actuator/health/liveness  and  /actuator/health/readiness
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
