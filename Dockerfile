# =============================================================================
# STAGE 1: "dependency-cache"
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS dependency-cache

RUN apk add --no-cache maven

WORKDIR /build

COPY pom.xml .
COPY domain/pom.xml         domain/pom.xml
COPY application/pom.xml    application/pom.xml
COPY infrastructure/pom.xml infrastructure/pom.xml

RUN mvn dependency:go-offline -B -e


# =============================================================================
# STAGE 2: "builder"
# =============================================================================
FROM dependency-cache AS builder

# Copiar el código fuente completo.
COPY domain/src         domain/src
COPY application/src    application/src
COPY infrastructure/src infrastructure/src

# Compilar y empaquetar.
# -pl infrastructure = compilar solo el módulo que genera el JAR ejecutable.
# -am -> compila también los módulos de los que depende
#       (domain y application).
RUN mvn package -pl infrastructure -am -B -e -DskipTests


# =============================================================================
# STAGE 3: "runtime"
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder \
    /build/infrastructure/target/infrastructure-1.0-SNAPSHOT.jar \
    app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# ── JVM tuning para containers ───────────────────────────────────────────────
#
# UseContainerSupport  -> La JVM lee los cgroups del container para saber
#                         cuántos CPUs y cuánta RAM realmente tiene.
# MaxRAMPercentage=75  -> Usa el 75% de la RAM del container para el heap.
#                         El 25% restante cubre: stack de threads, metaspace,
#                         off-heap del GC, NIO buffers, etc.
# java.security.egd   -> Acelera la inicialización de Tomcat en environments
#                         sin buena fuente de entropía (común en VMs y containers).
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
