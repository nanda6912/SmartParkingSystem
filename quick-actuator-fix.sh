#!/bin/bash

# =============================================================================
# Smart Parking System - Quick Actuator Fix
# =============================================================================
# Simple script to test and fix Actuator endpoint issues
# =============================================================================

set -e

echo "=========================================="
echo "Smart Parking System - Quick Actuator Fix"
echo "=========================================="
echo

echo "[INFO] Testing Actuator endpoints inside container..."

# Test 1: Direct health endpoint access
echo "[INFO] Testing direct health endpoint..."
if docker exec smart-parking-backend curl -f -s http://localhost:8081/actuator/health 2>/dev/null; then
    echo "[SUCCESS] Direct /actuator/health works inside container"
    echo "[INFO] Response: $(docker exec smart-parking-backend curl -s http://localhost:8081/actuator/health 2>/dev/null)"
else
    echo "[ERROR] Direct /actuator/health failed inside container"
fi

echo

# Test 2: Health without actuator path
echo "[INFO] Testing /health endpoint..."
if docker exec smart-parking-backend curl -f -s http://localhost:8081/health 2>/dev/null; then
    echo "[SUCCESS] Direct /health works inside container"
else
    echo "[ERROR] Direct /health failed inside container"
fi

echo

# Test 3: Main application
echo "[INFO] Testing main application..."
if docker exec smart-parking-backend curl -f -s http://localhost:8081/ 2>/dev/null; then
    echo "[SUCCESS] Main application works inside container"
else
    echo "[ERROR] Main application failed inside container"
fi

echo

# Test 4: Check Spring Boot logs
echo "[INFO] Checking Spring Boot startup logs..."
echo "--- Last 20 lines of Spring Boot logs ---"
docker compose logs --tail=20 smart-parking-backend 2>/dev/null || echo "No logs available"
echo "----------------------------------------"

echo

echo "=========================================="
echo "DIAGNOSIS COMPLETE"
echo "=========================================="
echo

echo "[INFO] If endpoints work inside container but not externally:"
echo "1. Check if management.endpoints.web.base-path=/actuator is causing issues"
echo "2. Try removing base-path and let Spring Boot use defaults"
echo "3. Verify SPRING_PROFILES_ACTIVE=prod is set in docker-compose.yml"
echo "4. Check if actuator dependency is properly included in build"
echo

echo "[INFO] If endpoints don't work inside container:"
echo "1. Spring Boot may not be recognizing actuator starter"
echo "2. Check pom.xml for spring-boot-starter-actuator dependency"
echo "3. Verify application-prod.properties is being loaded"
echo "4. Check for any startup errors in logs"

echo
echo "[SUCCESS] Quick diagnosis complete. Use results to determine next steps."
