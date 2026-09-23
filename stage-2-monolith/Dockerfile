# syntax=docker/dockerfile:1
# ============================================================================
# SECUREBANK BACKEND IMAGE — multi-stage build (Week 8: Containers).
# ============================================================================
# THE MULTI-STAGE IDEA: compile with a full JDK + Maven (hundreds of MB of
# tools), then ship ONLY the jar on a slim JRE. The build stage is thrown
# away — the final image never contains source, Maven, or the JDK. Smaller
# image = faster pulls, smaller attack surface (Week 9 hardening).
#
# LAYER CACHING (the ordering below is deliberate):
# Docker caches each layer until its inputs change. Dependencies (pom.xml)
# change rarely; source changes constantly. Copy pom first + go-offline, and
# rebuilds after code edits reuse the cached dependency layer — seconds
# instead of minutes. COPY src LAST.
# ============================================================================

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# 1a) Maven wrapper + POM only -> dependency cache layer.
#     (We use the project's mvnw so the Maven version is pinned by the repo,
#      not by whatever base image tag we picked — reproducible builds.)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# 1b) Source layer. Tests are SKIPPED here on purpose: CI runs them (see
#     .github/workflows/ci.yml); the image build should not duplicate that.
COPY src ./src
RUN ./mvnw -B -q -DskipTests package

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime

# curl is for the HEALTHCHECK below (and for humans debugging inside the
# container). --no-install-recommends + list cleanup keep the layer small.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# SECURITY: run as an unprivileged user. If the JVM is ever compromised, the
# attacker lands as uid 1001 with no write access to /app — not as root.
RUN useradd --system --uid 1001 --shell /usr/sbin/nologin securebank
WORKDIR /app

# The executable fat jar from the build stage (Spring Boot's repackaged jar —
# java -jar is all it needs; embedded Tomcat included).
COPY --from=build /build/target/*.jar app.jar
RUN chown securebank:securebank app.jar
USER securebank

EXPOSE 8080

# HEALTHCHECK: Docker (and compose's service_healthy condition) polls this.
# It hits the actuator probe the stage-2 README mentions — the same endpoint
# Kubernetes will use in infra/k8s (livenessProbe). One health contract,
# three consumers. start-period gives Spring ~30s to boot before counting
# failures.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=10 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# exec form (JSON array) so java is PID 1 and receives SIGTERM directly —
# graceful shutdown works; the shell form would swallow the signal.
ENTRYPOINT ["java", "-jar", "app.jar"]
