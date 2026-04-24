# Build stage (name must be unique across all FROM lines)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=builder /src/target/*.jar /app/app.jar
EXPOSE 8080
# Ensure Spring Boot logs to stderr at INFO (many hosts default LOGGING_LEVEL_ROOT to WARN).
ENV LOGGING_LEVEL_ROOT=INFO \
    LOGGING_LEVEL_COM_BANK_CEBOS=INFO \
    LOGGING_LEVEL_COM_BANK_CEBOS_LOGGING=INFO \
    LOGGING_LEVEL_COM_BANK_CEBOS_BBS=INFO
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
