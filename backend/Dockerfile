# PrintForge API image. Build from the backend/ directory: docker build -t printforge-api backend

# --- Build -----------------------------------------------------------------------------------
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
# Dependencies first, so source changes do not re-download them.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp package -DskipTests && cp target/printforge-api-*.jar app.jar

# --- Runtime ---------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Run as an unprivileged user; it owns only the upload directories.
RUN addgroup -S printforge && adduser -S printforge -G printforge \
 && mkdir -p /app/uploads/custom-prints /app/uploads/products \
 && chown -R printforge:printforge /app/uploads
COPY --from=build /build/app.jar /app/app.jar
USER printforge

# Uploaded models and product photos live here; mount a volume to keep them.
ENV UPLOAD_DIR=/app/uploads/custom-prints \
    PRODUCT_IMAGE_DIR=/app/uploads/products
VOLUME ["/app/uploads"]
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
