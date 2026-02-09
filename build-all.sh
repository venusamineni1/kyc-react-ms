#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building all services via Root POM...${NC}"

# Use the root maven wrapper to build everything defined in pom.xml
if ./mvnw clean package -DskipTests; then
    echo -e "${GREEN}All backend services built successfully!${NC}"
else
    echo -e "${RED}Backend build failed${NC}"
    exit 1
fi

# Frontend Build (Still manual as it's not a maven module)
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
