#!/bin/bash

# Fixed startup script for KYC Microservices Stack
# Handles service startup with proper timeouts and logging

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# Configuration
STARTUP_TIMEOUT=120  # seconds to wait for each service to start
EUREKA_WAIT=10       # extra time to wait for Eureka after it starts

echo -e "${GREEN}=== KYC Microservices Stack Startup ===${NC}"
echo "Root directory: $ROOT_DIR"
echo ""

# Kill any existing services on our ports to ensure clean start
cleanup_ports() {
    echo -e "${YELLOW}Cleaning up any existing processes...${NC}"
    for port in 8761 8084 8081 8082 8085 8086 8083 8080; do
        lsof -ti:$port 2>/dev/null | xargs kill -9 2>/dev/null || true
    done
    sleep 2
}

check_port() {
    local port=$1
    lsof -i :$port >/dev/null 2>&1
    return $?
}

wait_for_port() {
    local port=$1
    local service=$2
    local timeout=$3
    local elapsed=0

    echo -e "${YELLOW}  Waiting for $service (port $port)...${NC}"

    while ! check_port $port && [ $elapsed -lt $timeout ]; do
        sleep 2
        elapsed=$((elapsed + 2))
        echo -n "."
    done

    if check_port $port; then
        echo -e "\n${GREEN}  ✓ $service is running on port $port${NC}"
        return 0
    else
        echo -e "\n${RED}  ✗ $service failed to start on port $port${NC}"
        return 1
    fi
}

start_service() {
    local port=$1
    local name=$2
    local module=$3
    local log_file="$ROOT_DIR/logs/${module}.log"

    mkdir -p "$ROOT_DIR/logs"

    if check_port $port; then
        echo -e "${YELLOW}$name already running on port $port${NC}"
        return 0
    fi

    echo -e "\n${GREEN}Starting: $name${NC}"

    # Start service using pre-built JAR file if it exists, otherwise use gradle
    local jar_file="$ROOT_DIR/${module}/build/libs/app.jar"
    if [ ! -f "$jar_file" ]; then
        jar_file="$ROOT_DIR/${module}/build/libs/${module}-0.0.1-SNAPSHOT.jar"
    fi

    if [ -f "$jar_file" ]; then
        echo "  Using pre-built JAR: $jar_file"
        java -jar "$jar_file" > "$log_file" 2>&1 &
    else
        echo "  Building with Gradle..."
        "$ROOT_DIR/gradlew" -p "$ROOT_DIR" :${module}:bootRun > "$log_file" 2>&1 &
    fi
    local pid=$!
    echo "  Process ID: $pid"
    echo "  Log file: $log_file"

    # Wait for service to start
    if wait_for_port $port "$name" $STARTUP_TIMEOUT; then
        return 0
    else
        echo -e "${RED}  Failed to start. Last 20 lines of log:${NC}"
        tail -20 "$log_file"
        return 1
    fi
}

# Main startup sequence
echo -e "${YELLOW}Step 1: Cleaning up old processes${NC}"
cleanup_ports

echo -e "${YELLOW}Step 2: Starting Eureka Service Registry (CRITICAL)${NC}"
start_service 8761 "Service Registry" "service-registry" || exit 1
sleep $EUREKA_WAIT  # Extra wait for Eureka to be fully ready

echo -e "${YELLOW}Step 3: Starting core services${NC}"
start_service 8084 "Auth Service"        "auth-service"      || true
sleep 3
start_service 8081 "Risk Service"        "risk-service"      || true
sleep 3
start_service 8082 "Screening Service"   "screening-service" || true
sleep 3
start_service 8085 "Document Service"    "document-service"  || true
sleep 3
start_service 8086 "KYC Orchestration"   "kyc-orchestration" || true
sleep 3

echo -e "${YELLOW}Step 4: Starting UI backend services${NC}"
start_service 8083 "Viewer Service"      "viewer"            || true
sleep 3
start_service 8080 "API Gateway"         "api-gateway"       || true
sleep 3

# Frontend (optional)
echo -e "${YELLOW}Step 5: Starting Frontend${NC}"
if check_port 5173; then
    echo -e "${YELLOW}Frontend already running on port 5173${NC}"
else
    if ! command -v npm &>/dev/null; then
        export NVM_DIR="$HOME/.nvm"
        [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
    fi

    if ! command -v npm &>/dev/null; then
        echo -e "${RED}WARNING: npm not found — skipping frontend${NC}"
    else
        echo -e "${GREEN}Starting frontend...${NC}"
        mkdir -p "$ROOT_DIR/logs"

        # Install dependencies if needed
        if [ ! -d "$ROOT_DIR/viewer/frontend/node_modules" ]; then
            echo "  Installing dependencies..."
            npm --prefix "$ROOT_DIR/viewer/frontend" install > "$ROOT_DIR/logs/frontend-install.log" 2>&1
        fi

        npm --prefix "$ROOT_DIR/viewer/frontend" run dev > "$ROOT_DIR/logs/frontend.log" 2>&1 &
        sleep 5

        if wait_for_port 5173 "Frontend" $STARTUP_TIMEOUT; then
            echo -e "${GREEN}Frontend started${NC}"
        fi
    fi
fi

# Summary
echo ""
echo -e "${GREEN}=== Startup Complete ===${NC}"
echo ""
echo "📍 Access your application:"
echo "   Frontend:      ${GREEN}http://localhost:5173${NC}"
echo "   API Gateway:   ${GREEN}http://localhost:8080${NC}"
echo "   Eureka:        ${GREEN}http://localhost:8761${NC}"
echo ""
echo "📚 Service Documentation:"
echo "   Viewer:        ${GREEN}http://localhost:8083/swagger-ui.html${NC}"
echo "   Orchestration: ${GREEN}http://localhost:8086/swagger-ui.html${NC}"
echo "   Screening:     ${GREEN}http://localhost:8082/swagger-ui.html${NC}"
echo "   Risk:          ${GREEN}http://localhost:8081/swagger-ui.html${NC}"
echo ""
echo "📋 Service logs available in: ${GREEN}$ROOT_DIR/logs/${NC}"
echo ""
echo -e "${YELLOW}To stop services: killall java${NC}"
echo -e "${YELLOW}To view logs: tail -f logs/*.log${NC}"
echo ""
