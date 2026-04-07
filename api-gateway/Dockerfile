# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

COPY service-registry/build.gradle   service-registry/
COPY api-gateway/build.gradle        api-gateway/
COPY auth-service/build.gradle       auth-service/
COPY risk-service/build.gradle       risk-service/
COPY screening-service/build.gradle  screening-service/
COPY document-service/build.gradle   document-service/
COPY viewer/build.gradle             viewer/

COPY api-gateway/src api-gateway/src/

RUN chmod +x gradlew && ./gradlew :api-gateway:build -x test --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /workspace/api-gateway/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
