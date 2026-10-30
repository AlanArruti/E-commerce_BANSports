# syntax=docker/dockerfile:1.7

# =============================================================================
# Imagen del backend. Build multi-stage: el JDK y el repo Maven se quedan en la
# etapa de build, la imagen final solo lleva un JRE y la app.
# =============================================================================

# ── Stage 1: dependencias ───────────────────────────────────────────────────
# Se resuelven en su propia capa: mientras el pom.xml no cambie, Docker la
# reutiliza y no vuelve a bajar nada aunque cambie todo el código.
FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /app

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline

# ── Stage 2: build ──────────────────────────────────────────────────────────
FROM deps AS build
WORKDIR /app

COPY src src

# -DskipTests: los tests corren en la máquina de desarrollo y en CI, no en cada
# build de imagen. Para incluirlos: docker build --build-arg SKIP_TESTS=false
ARG SKIP_TESTS=true
RUN ./mvnw -B -q package -DskipTests=${SKIP_TESTS} \
    && cp target/*-SNAPSHOT.jar app.jar

# Spring Boot sabe partir su propio jar en capas (dependencias primero, código
# de la app último). Así un cambio de código solo reescribe la última capa.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ── Stage 3: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# wget lo usa el healthcheck del compose. curl pesa más y no hace falta.
RUN apk add --no-cache wget \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup

# El orden importa: de lo que menos cambia a lo que más.
COPY --from=build --chown=appuser:appgroup /app/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appgroup /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appgroup /app/extracted/application/ ./

USER appuser

EXPOSE 8080

# La JVM lee los límites del contenedor por su cuenta (Java 21). Solo le decimos
# qué porcentaje de la memoria disponible puede usar para el heap.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
