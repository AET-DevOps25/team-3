# Build stage
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src src/

# Fix line endings, make executable, and build
RUN sed -i 's/\r$//' gradlew && \
    chmod +x gradlew && \
    ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app


# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar


# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"] 