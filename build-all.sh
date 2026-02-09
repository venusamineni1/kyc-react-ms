#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

build_service() {
    service=$1
    echo -e "${GREEN}Building $service...${NC}"
    cd $service
    if ./mvnw clean package -DskipTests; then
        echo -e "${GREEN}$service built successfully!${NC}"
        cd ..
    else
        echo -e "${RED}Failed to build $service${NC}"
        cd ..
        exit 1
    fi
}

echo "Starting build for all services..."

# Build Order (Registry first is good practice, though not strictly required for build)
build_service "service-registry"
build_service "api-gateway"
build_service "auth-service"
build_service "risk-service"
build_service "screening-service"
build_service "document-service"
build_service "viewer"

echo -e "${GREEN}All backend services built successfully!${NC}"

# Frontend Build (Optional, depending on deployment needs)
echo -e "${GREEN}Building Frontend...${NC}"
cd viewer/frontend
if npm install && npm run build; then
    echo -e "${GREEN}Frontend built successfully!${NC}"
else
    echo -e "${RED}Failed to build Frontend${NC}"
    exit 1
fi
cd ../..

echo -e "${GREEN}Full stack build complete!${NC}"
