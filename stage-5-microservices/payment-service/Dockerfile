# syntax=docker/dockerfile:1
# ============================================================================
# Multi-stage build — SAME pattern as stage-2-monolith/Dockerfile (see the
# long comments there: layer caching, non-root user, healthcheck, exec form).
# Kept short here; the teaching copy lives in stage 2.
# ============================================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 1001 --shell /usr/sbin/nologin securebank
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown securebank:securebank app.jar
USER securebank
EXPOSE 8083
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=10 \
    CMD curl -fsS http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]