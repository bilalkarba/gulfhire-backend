# =============================================================
# GulfHire Backend — Spring Boot 3.5 / Java 21
# Multi-stage build: Maven builder -> slim JRE runtime (non-root)
# =============================================================

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first — this layer is only invalidated when pom.xml changes.
# go-offline can miss plugin-time dependencies; `|| true` lets the build continue
# and `package` downloads whatever is missing.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user (production best practice — the app writes nothing
# to disk; uploads go to Cloudinary).
RUN groupadd -r gulfhire && useradd -r -g gulfhire gulfhire \
    && chown -R gulfhire:gulfhire /app
USER gulfhire

# Wildcard so the Dockerfile survives pom version bumps.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage lets the JVM size its heap from the container memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
