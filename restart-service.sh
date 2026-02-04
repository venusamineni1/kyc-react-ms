#!/bin/bash

# Usage: ./restart-service.sh <service-name>
# Example: ./restart-service.sh auth-service

SERVICE=$1
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "$SERVICE" ]; then
    echo "Usage: $0 <service-name>"
    echo "Available services: registry, auth, risk, screening, viewer, gateway"
    exit 1
fi

# Configuration Mapping
case $SERVICE in
    "registry"|"service-registry")
        DIR="service-registry"
        PORT=8761
        LOG="registry.log"
        NAME="Service Registry"
        ;;
    "auth"|"auth-service")
        DIR="auth-service"
        PORT=8084
        LOG="auth.log"
        NAME="Auth Service"
        ;;
    "risk"|"risk-service")
        DIR="risk-service"
        PORT=8081
        LOG="risk.log"
        NAME="Risk Service"
        ;;
    "screening"|"screening-service")
        DIR="screening-service"
        PORT=8082
        LOG="screening.log"
        NAME="Screening Service"
        ;;
    "viewer"|"viewer-service")
        DIR="viewer"
        PORT=8083
        LOG="viewer.log"
        NAME="Viewer Service"
        ;;
    "gateway"|"api-gateway")
        DIR="api-gateway"
        PORT=8080
        LOG="gateway.log"
        NAME="API Gateway"
        ;;
    *)
        echo -e "${RED}Unknown service: $SERVICE${NC}"
        echo "Available services: registry, auth, risk, screening, viewer, gateway"
        exit 1
        ;;
esac

echo -e "Restarting ${GREEN}$NAME${NC}..."

# 1. Stop the service
PID=$(lsof -t -i:$PORT -sTCP:LISTEN)
if [ -n "$PID" ]; then
    echo "Stopping existing process (PID $PID) on port $PORT..."
    kill $PID
    
    # Wait for it to die
    while lsof -i:$PORT > /dev/null; do
        sleep 1
    done
    echo "Process stopped."
else
    echo "No running process found on port $PORT."
fi

# 2. Start the service
echo "Starting $NAME..."
cd $DIR
nohup ./mvnw spring-boot:run > ../$LOG 2>&1 &
cd ..

# 3. Wait for startup
echo "Waiting for $NAME to be ready on port $PORT..."
while ! nc -z localhost $PORT; do   
  sleep 1
done

echo -e "${GREEN}$NAME restarted successfully!${NC}"
echo "Logs available in $LOG"
