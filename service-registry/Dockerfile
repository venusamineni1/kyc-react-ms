# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# All module build files needed for multi-module resolution
COPY service-registry/build.gradle   service-registry/
COPY api-gateway/build.gradle        api-gateway/
COPY auth-service/build.gradle       auth-service/
COPY risk-service/build.gradle       risk-service/
COPY screening-service/build.gradle  screening-service/
COPY document-service/build.gradle   document-service/
COPY viewer/build.gradle             viewer/

# Source for this module only
COPY service-registry/src service-registry/src/

RUN chmod +x gradlew && ./gradlew :service-registry:build -x test --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /workspace/service-registry/build/libs/app.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
