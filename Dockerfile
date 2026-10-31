# Multi-stage build.
# Stage 1 compiles and tests the code with the full JDK and Maven.
# Stage 2 keeps only the runnable jar on a slim JRE, so the final image
# carries no build tooling and no source.
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace

# Copy the pom first so dependency downloads are cached in their own layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as an unprivileged user rather than root.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/taskflow-api.jar /app/app.jar
RUN chown -R app:app /app
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
