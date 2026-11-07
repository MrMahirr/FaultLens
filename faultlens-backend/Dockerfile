# syntax=docker/dockerfile:1
# Stage 1: build. Dependencies are resolved in their own layer (only the POMs are copied first),
# so a source-only change rebuilds in seconds instead of downloading everything again.
FROM maven:3.9-eclipse-temurin-26 AS builder
WORKDIR /app

COPY pom.xml ./
COPY common/pom.xml common/
COPY collector/pom.xml collector/
COPY processor/pom.xml processor/
COPY analyzer/pom.xml analyzer/
COPY api/pom.xml api/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DexcludeGroupIds=com.faultlens || true

COPY . .
# Tests run in CI; skipUTs/skipITs because plain -DskipTests is overridden by the POM.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipUTs -DskipITs

# Stage 2: runtime (JRE only, non-root)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S faultlens && adduser -S -G faultlens -h /app faultlens
COPY --from=builder --chown=faultlens:faultlens /app/api/target/*.jar app.jar
USER faultlens

EXPOSE 3500

# Container memory limits drive the heap; Netty loads its native transport via
# System.loadLibrary, which Java 24+ only allows with explicit native access.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# /readyz stays on the main port even when actuator runs on MANAGEMENT_PORT.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:3500/readyz || exit 1

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
