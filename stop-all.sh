#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${RED}Stopping KYC Microservices Stack...${NC}"

get_pid_by_port() {
    local port=$1
    if command -v lsof >/dev/null 2>&1; then
        lsof -t -i :$port 2>/dev/null
    elif command -v netstat >/dev/null 2>&1; then
        # Windows netstat fallback (PID is 5th column)
        netstat -ano 2>/dev/null | grep -i "LISTENING" | grep -E ":$port[[:space:]]" | awk '{print $5}' | head -n 1
    fi
}

kill_process() {
    local pid=$1
    if command -v taskkill >/dev/null 2>&1; then
        # Windows native task kill
        taskkill //F //PID $pid >/dev/null 2>&1 || kill $pid >/dev/null 2>&1
    else
        kill $pid >/dev/null 2>&1
    fi
}

kill_port() {
    port=$1
    name=$2
    pid=$(get_pid_by_port $port)
    if [ -n "$pid" ]; then
        echo "Stopping $name (PID: $pid) on port $port..."
        kill_process $pid
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
kill_port 8086 "Orchestration Service"
kill_port 8083 "Viewer Service"
kill_port 5173 "Frontend"

echo -e "${GREEN}All shutdown commands issued.${NC}"
