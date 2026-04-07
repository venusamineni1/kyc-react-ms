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

COPY document-service/src document-service/src/

RUN chmod +x gradlew && ./gradlew :document-service:build -x test --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /workspace/document-service/build/libs/app.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
