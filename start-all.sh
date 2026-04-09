#!/bin/bash

# Colors
GREEN='\033[0;32m'
NC='\033[0m' # No Color

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${GREEN}Starting KYC Microservices Stack...${NC}"

check_port() {
    local port=$1
    # 1. Try lsof (macOS/Linux)
    if command -v lsof >/dev/null 2>&1; then
        lsof -i :$port >/dev/null 2>&1
        return $?
    # 2. Try nc (netcat)
    elif command -v nc >/dev/null 2>&1; then
        nc -z localhost $port >/dev/null 2>&1
        return $?
    # 3. Fallback to bash built-in (Windows Git Bash)
    else
        (echo > /dev/tcp/localhost/$port) >/dev/null 2>&1
        return $?
    fi
}

wait_for_port() {
    port=$1
    service=$2
    echo "Waiting for $service to start on port $port..."
    while ! check_port $port; do
      sleep 1
    done
    echo -e "${GREEN}$service is UP!${NC}"
}

start_service() {
    local port=$1
    local name=$2
    local module=$3
    local log=$4

    if check_port $port; then
        echo "$name is already running on port $port."
    else
        echo "Starting $name..."
        nohup "$ROOT_DIR/gradlew" -p "$ROOT_DIR" :${module}:bootRun > "$ROOT_DIR/$log" 2>&1 &
        wait_for_port $port "$name"
    fi
}

# Start in dependency order
start_service 8761 "Service Registry" service-registry  registry.log
start_service 8084 "Auth Service"     auth-service      auth.log
start_service 8081 "Risk Service"     risk-service      risk.log
start_service 8082 "Screening Service" screening-service screening.log
start_service 8085 "Document Service" document-service  document.log
start_service 8083 "Viewer Service"   viewer            viewer.log
start_service 8080 "API Gateway"      api-gateway       gateway.log

# Frontend
if check_port 5173; then
    echo "Frontend is already running on port 5173."
else
    echo "Starting Frontend..."

    # Resolve npm — works whether node is in PATH directly or via NVM
    if ! command -v npm &>/dev/null; then
        export NVM_DIR="$HOME/.nvm"
        [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
    fi

    if ! command -v npm &>/dev/null; then
        echo "WARNING: npm not found — skipping frontend. Install Node.js or NVM first."
    else
        # Install dependencies if node_modules is missing
        if [ ! -d "$ROOT_DIR/viewer/frontend/node_modules" ]; then
            echo "Installing frontend dependencies..."
            npm --prefix "$ROOT_DIR/viewer/frontend" install
        fi

        nohup npm --prefix "$ROOT_DIR/viewer/frontend" run dev \
            > "$ROOT_DIR/frontend.log" 2>&1 &

        wait_for_port 5173 "Frontend"
    fi
fi

echo -e "${GREEN}All systems operational! Logs are in the root directory.${NC}"
echo "Frontend: http://localhost:5173"
echo "Gateway:  http://localhost:8080"
echo "Registry: http://localhost:8761"
