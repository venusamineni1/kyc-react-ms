#!/bin/bash

# Colors
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting KYC Microservices Stack...${NC}"

# Function to check if a port is in use
check_port() {
    lsof -i :$1 > /dev/null
    return $?
}

# Function to wait for a service to be ready
wait_for_port() {
    port=$1
    service=$2
    echo "Waiting for $service to start on port $port..."
    while ! nc -z localhost $port; do   
      sleep 1
    done
    echo -e "${GREEN}$service is UP!${NC}"
}

# 1. Service Registry (Eureka)
if check_port 8761; then
    echo "Service Registry is already running on port 8761."
else
    echo "Starting Service Registry..."
    cd service-registry
    nohup ./mvnw spring-boot:run > ../registry.log 2>&1 &
    cd ..
    wait_for_port 8761 "Service Registry"
fi

# 2. Auth Service
if check_port 8084; then
    echo "Auth Service is already running on port 8084."
else
    echo "Starting Auth Service..."
    cd auth-service
    nohup ./mvnw spring-boot:run > ../auth.log 2>&1 &
    cd ..
    wait_for_port 8084 "Auth Service"
fi

# 3. Risk Service
if check_port 8081; then
    echo "Risk Service is already running on port 8081."
else
    echo "Starting Risk Service..."
    cd risk-service
    nohup ./mvnw spring-boot:run > ../risk.log 2>&1 &
    cd ..
    wait_for_port 8081 "Risk Service"
fi

# 4. Screening Service
if check_port 8082; then
    echo "Screening Service is already running on port 8082."
else
    echo "Starting Screening Service..."
    cd screening-service
    nohup ./mvnw spring-boot:run > ../screening.log 2>&1 &
    cd ..
    wait_for_port 8082 "Screening Service"
fi

# 5. Viewer Service (Core)
if check_port 8083; then
    echo "Viewer Service is already running on port 8083."
else
    echo "Starting Viewer Service..."
    cd viewer
    nohup ./mvnw spring-boot:run > ../viewer.log 2>&1 &
    cd ..
    wait_for_port 8083 "Viewer Service"
fi

# 6. API Gateway
if check_port 8080; then
    echo "API Gateway is already running on port 8080."
else
    echo "Starting API Gateway..."
    cd api-gateway
    nohup ./mvnw spring-boot:run > ../gateway.log 2>&1 &
    cd ..
    wait_for_port 8080 "API Gateway"
fi

# 7. Frontend
echo "Starting Frontend..."
cd viewer/frontend
nohup npm run dev > ../../frontend.log 2>&1 &
cd ../..

echo -e "${GREEN}All systems operational! Logs are in the root directory.${NC}"
echo "Frontend: http://localhost:5173"
echo "Gateway:  http://localhost:8080"
echo "Registry: http://localhost:8761"
