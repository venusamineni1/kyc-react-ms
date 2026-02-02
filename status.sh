#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

check_service() {
    name=$1
    port=$2
    if lsof -i :$port > /dev/null; then
        pid=$(lsof -t -i :$port)
        echo -e "${GREEN}● $name is RUNNING on port $port (PID: $pid)${NC}"
    else
        echo -e "${RED}○ $name is STOPPED (Port $port free)${NC}"
    fi
}

echo "=== KYC Microservices Status ==="
check_service "Service Registry" 8761
check_service "API Gateway     " 8080
check_service "Auth Service    " 8084
check_service "Viewer Service  " 8083
check_service "Risk Service    " 8081
check_service "Screening Service" 8082
check_service "Frontend        " 5173
echo "================================"
