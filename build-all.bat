@echo off
setlocal enabledelayedexpansion

echo Building all services via Root POM...

REM Use the root maven wrapper to build everything defined in pom.xml
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo Backend build failed
    exit /b 1
)

echo.
echo All backend services built successfully!

echo.
echo Building Frontend...
cd viewer/frontend
call npm install
if %errorlevel% neq 0 (
    echo Failed to install frontend dependencies
    cd ../..
    exit /b 1
)
call npm run build
if %errorlevel% neq 0 (
    echo Failed to build frontend
    cd ../..
    exit /b 1
)
cd ../..

echo.
echo Full stack build complete!
