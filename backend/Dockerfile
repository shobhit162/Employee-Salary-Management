# --- build -------------------------------------------------------------------
# Dependencies are resolved in their own layer so a source-only change does not
# re-download the world on every build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- run ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run the application as root.
RUN addgroup -S app && adduser -S -G app app
USER app

COPY --from=build --chown=app:app /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
