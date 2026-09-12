FROM gradle:8.7-jdk21 AS build
WORKDIR /app

ARG JVM_OPTS=""
ENV JAVA_TOOL_OPTIONS=${JVM_OPTS}

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG JVM_OPTS=""
ENV JAVA_TOOL_OPTIONS=${JVM_OPTS}


RUN apk add --no-cache wget && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup


USER appuser:appgroup


COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]