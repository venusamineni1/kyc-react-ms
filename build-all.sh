#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building all services via Gradle...${NC}"

# Gradle 8.12 cannot run on newer JDKs (e.g. "Unsupported class file major version 69").
# Use the bundled JDK 21 if present so this script works regardless of the system default JDK.
BUNDLED_JDK="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/jdk-21.0.2+13/Contents/Home"
if [ -d "$BUNDLED_JDK" ]; then
    export JAVA_HOME="$BUNDLED_JDK"
    echo -e "${GREEN}Using bundled JDK 21 at ${JAVA_HOME}${NC}"
fi

# Use the root gradle wrapper to build everything
if ./gradlew clean build -x test; then
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
