@echo off
setlocal enabledelayedexpansion

echo Starting build for all services...

call :BuildService "service-registry"
if %errorlevel% neq 0 exit /b %errorlevel%

call :BuildService "api-gateway"
if %errorlevel% neq 0 exit /b %errorlevel%

call :BuildService "auth-service"
if %errorlevel% neq 0 exit /b %errorlevel%

call :BuildService "risk-service"
if %errorlevel% neq 0 exit /b %errorlevel%

call :BuildService "screening-service"
if %errorlevel% neq 0 exit /b %errorlevel%

call :BuildService "document-service"
if %errorlevel% neq 0 exit /b %errorlevel%

call :BuildService "viewer"
if %errorlevel% neq 0 exit /b %errorlevel%

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
goto :eof

:BuildService
set "SERVICE=%~1"
echo.
echo Building %SERVICE%...
cd %SERVICE%
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo Failed to build %SERVICE%
    cd ..
    exit /b 1
)
echo %SERVICE% built successfully!
cd ..
exit /b 0
