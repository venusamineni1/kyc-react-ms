@echo off
setlocal enabledelayedexpansion

if "%~1"=="" (
    echo Usage: restart-service.bat ^<service-name^>
    echo Available services: registry, auth, risk, screening, viewer, gateway, document
    exit /b 1
)

set SERVICE=%~1
set PORT=
set DIR=
set LOG=
set NAME=

if "%SERVICE%"=="registry" (
    set DIR=service-registry
    set PORT=8761
    set LOG=registry.log
    set NAME=Service Registry
)
if "%SERVICE%"=="auth" (
    set DIR=auth-service
    set PORT=8084
    set LOG=auth.log
    set NAME=Auth Service
)
if "%SERVICE%"=="risk" (
    set DIR=risk-service
    set PORT=8081
    set LOG=risk.log
    set NAME=Risk Service
)
if "%SERVICE%"=="screening" (
    set DIR=screening-service
    set PORT=8082
    set LOG=screening.log
    set NAME=Screening Service
)
if "%SERVICE%"=="document" (
    set DIR=document-service
    set PORT=8085
    set LOG=document.log
    set NAME=Document Service
)
if "%SERVICE%"=="viewer" (
    set DIR=viewer
    set PORT=8083
    set LOG=viewer.log
    set NAME=Viewer Service
)
if "%SERVICE%"=="gateway" (
    set DIR=api-gateway
    set PORT=8080
    set LOG=gateway.log
    set NAME=API Gateway
)

if "%PORT%"=="" (
    echo Unknown service: %SERVICE%
    echo Available services: registry, auth, risk, screening, viewer, gateway, document
    exit /b 1
)

echo Restarting %NAME%...

REM 1. Stop the service
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    set PID=%%a
)

if defined PID (
    echo Stopping existing process (PID %PID%) on port %PORT%...
    taskkill /F /PID %PID% >nul 2>&1
    
    :WaitStop
    timeout /t 1 /nobreak >nul
    netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
    if !errorlevel!==0 goto WaitStop
    echo Process stopped.
) else (
    echo No running process found on port %PORT%.
)

REM 2. Start the service
echo Starting %NAME%...
cd %DIR%
start "%NAME%" /MIN cmd /c "mvnw.cmd spring-boot:run > ..\%LOG% 2>&1"
cd ..

REM 3. Wait for startup
echo Waiting for %NAME% to be ready on port %PORT%...
:WaitStart
timeout /t 2 /nobreak >nul
netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
if !errorlevel! neq 0 goto WaitStart

echo %NAME% restarted successfully!
echo Logs available in %LOG%
