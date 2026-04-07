@echo off
setlocal enabledelayedexpansion

echo Building all services via Gradle...

REM Use the root gradle wrapper to build everything
call gradlew.bat clean build -x test
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
