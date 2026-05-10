#!/bin/bash

# =============================================================================
# Smart Parking System - Unhealthy Container Fix Script
# =============================================================================
# Diagnoses and fixes backend container unhealthy status
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to check current container status
check_container_status() {
    print_status "Checking current container status..."
    echo "--- Current Container Status ---"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep smart-parking
    echo "--------------------------------"
}

# Function to test Actuator endpoints inside container
test_inside_container() {
    print_status "Testing Actuator endpoints from inside container..."
    
    # Test /actuator/health from inside container
    echo "Testing /actuator/health inside container..."
    if docker compose exec parking-backend curl -f -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
        print_success "Internal /actuator/health works"
    else
        print_error "Internal /actuator/health failed"
        return 1
    fi
    
    # Test /health directly (without base-path)
    echo "Testing /health directly inside container..."
    if docker compose exec parking-backend curl -f -s http://localhost:8081/health > /dev/null 2>&1; then
        print_success "Internal /health works"
    else
        print_warning "Internal /health not accessible"
    fi
    
    # Test main application
    echo "Testing main application inside container..."
    if docker compose exec parking-backend curl -f -s http://localhost:8081/ > /dev/null 2>&1; then
        print_success "Main application works internally"
    else
        print_warning "Main application not accessible internally"
    fi
}

# Function to check Spring Boot logs for Actuator issues
check_spring_logs() {
    print_status "Checking Spring Boot logs for Actuator initialization..."
    echo "--- Spring Boot Logs (Last 30 lines) ---"
    docker compose logs --tail=30 parking-backend 2>/dev/null || echo "No logs available"
    echo "----------------------------------------"
}

# Function to check environment variables inside container
check_container_env() {
    print_status "Checking container environment variables..."
    echo "--- Container Environment ---"
    docker compose exec parking-backend env | grep -E "(SPRING|MANAGEMENT|SERVER)" || echo "No relevant env vars found"
    echo "-----------------------------"
}

# Function to test Actuator with different configurations
test_actuator_configs() {
    print_status "Testing different Actuator configurations..."
    
    echo "1. Testing with current configuration..."
    docker compose exec parking-backend curl -s http://localhost:8081/actuator/health 2>/dev/null || echo "FAILED"
    
    echo "2. Testing without base-path (let Spring Boot use default)..."
    # Temporarily remove base-path to test
    echo "Testing if removing base-path helps..."
    docker compose exec parking-backend bash -c 'curl -s http://localhost:8081/actuator/health' 2>/dev/null || echo "FAILED"
}

# Function to provide diagnosis and fix recommendations
provide_diagnosis() {
    echo
    echo "=========================================="
    echo "UNHEALTHY CONTAINER DIAGNOSIS"
    echo "=========================================="
    echo
    
    print_status "Based on container being unhealthy, here are possible causes:"
    echo
    echo "1. ACTUATOR ENDPOINT NOT ACCESSIBLE:"
    echo "   - /actuator/health returning 404 inside container"
    echo "   - Spring Boot not recognizing Actuator configuration"
    echo
    echo "2. CONFIGURATION NOT LOADING:"
    echo "   - management.endpoints.web.exposure.include not being applied"
    echo "   - management.endpoints.web.base-path causing issues"
    echo
    echo "3. SPRING PROFILE ISSUES:"
    echo "   - SPRING_PROFILES_ACTIVE may not be set to 'prod'"
    echo "   - application-prod.properties not being loaded"
    echo
    echo "4. DOCKER HEALTHCHECK MISMATCH:"
    echo "   - Healthcheck using wrong path or port"
    echo "   - Container health failing despite app running"
    echo
    echo "IMMEDIATE FIXES TO TRY:"
    echo "   1) Remove management.endpoints.web.base-path=/actuator"
    echo "   2) Use management.endpoints.web.exposure.include=*"
    echo "   3) Rebuild container: docker compose down && docker compose up --build -d"
    echo "   4) Test with curl inside container"
    echo "   5) Check Spring Boot startup logs for errors"
    echo
}

# Function to apply minimal Actuator config
apply_minimal_config() {
    print_status "Applying minimal Actuator configuration..."
    
    # Create temporary minimal config
    cat > /tmp/minimal-actuator.properties << 'EOF'
# Minimal Actuator configuration for testing
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
server.port=8081
EOF
    
    print_success "Minimal configuration created at /tmp/minimal-actuator.properties"
    print_status "To test: docker compose exec parking-backend bash -c 'java -Dspring.config.location=file:/tmp/minimal-actuator.properties -jar app.jar'"
}

# Function to restart container with new config
restart_with_config() {
    print_status "Restarting container with new configuration..."
    
    # Stop container
    docker compose stop parking-backend
    sleep 5
    
    # Start with minimal config for testing
    docker compose up -d parking-backend
    sleep 30
    
    # Restart with original config
    docker compose stop parking-backend
    sleep 5
    docker compose up -d parking-backend
}

# Main execution
main() {
    echo "=========================================="
    echo "Smart Parking System - Unhealthy Container Fix"
    echo "=========================================="
    echo
    
    check_container_status
    echo
    check_spring_logs
    echo
    check_container_env
    echo
    test_inside_container
    echo
    test_actuator_configs
    echo
    provide_diagnosis
    echo
    
    echo "=========================================="
    echo "AVAILABLE ACTIONS"
    echo "=========================================="
    echo
    echo "Choose an action:"
    echo "1) Apply minimal Actuator configuration and restart"
    echo "2) Test Actuator endpoints with different paths"
    echo "3) Check Spring Boot logs in detail"
    echo "4) Restart container normally"
    echo "5) Show container environment"
    echo "6) Exit"
    echo
    read -p "Enter choice [1-6]: " choice
    
    case $choice in
        1)
            apply_minimal_config
            restart_with_config
            ;;
        2)
            test_inside_container
            test_actuator_configs
            ;;
        3)
            check_spring_logs
            ;;
        4)
            print_status "Restarting container..."
            docker compose restart parking-backend
            sleep 30
            check_container_status
            ;;
        5)
            check_container_env
            ;;
        6)
            print_status "Exiting..."
            exit 0
            ;;
        *)
            print_error "Invalid choice"
            exit 1
            ;;
    esac
}

# Handle command line arguments
case "${1:-}" in
    --status-only)
        check_container_status
        ;;
    --logs-only)
        check_spring_logs
        ;;
    --test-only)
        test_inside_container
        test_actuator_configs
        ;;
    --env-only)
        check_container_env
        ;;
    --help)
        echo "Usage: $0 [OPTION]"
        echo "Options:"
        echo "  --status-only   Check container status only"
        echo "  --logs-only    Check Spring Boot logs only"
        echo "  --test-only    Test Actuator endpoints only"
        echo "  --env-only     Check container environment only"
        echo "  --help        Show this help"
        echo
        echo "Default: Interactive diagnosis and fix menu"
        ;;
    *)
        main
        ;;
esac
