#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${RED}Stopping KYC Microservices Stack...${NC}"

kill_port() {
    port=$1
    name=$2
    pid=$(lsof -t -i :$port)
    if [ -n "$pid" ]; then
        echo "Stopping $name (PID: $pid) on port $port..."
        kill $pid
    else
        echo "$name is not running on port $port."
    fi
}

kill_port 8761 "Service Registry"
kill_port 8080 "API Gateway"
kill_port 8084 "Auth Service"
kill_port 8081 "Risk Service"
kill_port 8082 "Screening Service"
kill_port 8085 "Document Service"
kill_port 8083 "Viewer Service"

# Frontend might be on 5173 or another port if 5173 was taken
# Usually 5173.
pid_front=$(lsof -t -i :5173)
if [ -n "$pid_front" ]; then
    echo "Stopping Frontend (PID: $pid_front) on port 5173..."
    kill $pid_front
else
    echo "Frontend not found on port 5173."
    # Fallback: kill based on 'vite' in ps? 
    # Optional, maybe too aggressive.
fi

echo -e "${GREEN}All shutdown commands issued.${NC}"
