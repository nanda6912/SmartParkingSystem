# =============================================================================
# Smart Parking System - Multi-Stage Docker Build
# =============================================================================
# Optimized for production with Java 21, UTF-8 encoding, and Asia/Kolkata timezone
# =============================================================================

# Stage 1: Maven Build
FROM maven:3.9.4-openjdk-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:21-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r parking && useradd -r -g parking parking

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Set JVM container optimization flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+UseStringDeduplication -Xms512m -Xmx1024m"

# Set encoding
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

# Set timezone
ENV TZ=Asia/Kolkata

# Expose application ports
EXPOSE 8081

# Change ownership to non-root user
RUN chown -R parking:parking /app

# Switch to non-root user
USER parking

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
