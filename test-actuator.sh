#!/bin/bash

echo "Testing Actuator endpoints inside container..."

echo "1. Testing /actuator/health..."
docker exec smart-parking-backend curl -s http://localhost:8081/actuator/health
echo ""

echo "2. Testing /health..."
docker exec smart-parking-backend curl -s http://localhost:8081/health
echo ""

echo "3. Testing main application..."
docker exec smart-parking-backend curl -s -w "Status: %{http_code}" http://localhost:8081/ 2>/dev/null
echo ""

echo "4. Checking Spring Boot logs..."
docker compose logs --tail=10 smart-parking-backend
echo ""

echo "5. Container environment..."
docker exec smart-parking-backend env | grep -E "(SPRING|MANAGEMENT|SERVER)"
