# =============================================================================
# Smart Parking System - Multi-stage Dockerfile
# =============================================================================
# Build Stage: Maven compilation and packaging
# Runtime Stage: Lightweight JRE with optimized JVM settings
# =============================================================================

# ---------------------
# BUILD STAGE
# ---------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy Maven configuration files first for better caching
COPY pom.xml .

# Download dependencies (cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ---------------------
# RUNTIME STAGE
# ---------------------
FROM eclipse-temurin:21-jre

# Set labels for metadata
LABEL maintainer="Smart Parking System Team"
LABEL version="1.0.0"
LABEL description="Smart Parking Slot Reservation System"

# Create application user (non-root for security)
RUN groupadd -r parking && useradd -r -g parking parking

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chown -R parking:parking /app/logs

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set timezone to Asia/Kolkata
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Set JVM container optimization flags
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+UseStringDeduplication -Xms512m -Xmx1024m"

# Set encoding
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8


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
