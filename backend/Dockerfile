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
COPY --from=build /app/target/*.jar app.jar
# The app reads server.port (8585) and datasource from env in the cloud.
EXPOSE 8585
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
