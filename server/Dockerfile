# Gradle with JDK 21 for build
FROM gradle:8.7.0-jdk21 AS build

WORKDIR /app
# TODO: cache dependencies?
COPY . .
# Build fat JAR
RUN gradle bootJar --no-daemon

# Lightweight JDK 21 runtime for run
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
