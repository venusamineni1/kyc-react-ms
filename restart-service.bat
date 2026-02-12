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
netstat -aon | findstr ":%PORT%" | findstr "LISTENING" > "%TEMP%\%NAME%.pid"
if %errorlevel% neq 0 (
    REM Not found is fine.
)

set PID=
for /f "tokens=5" %%a in ('type "%TEMP%\%NAME%.pid" 2^>nul') do set PID=%%a
if exist "%TEMP%\%NAME%.pid" del "%TEMP%\%NAME%.pid"

if defined PID (
    echo Stopping existing process (PID %PID%) on port %PORT%...
    taskkill /F /PID %PID% >nul 2>&1
    if !errorlevel! neq 0 (
        echo [ERROR] Line 74: Failed to kill process %PID%
        exit /b 1
    )
    
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

if not exist "%DIR%" (
   echo [ERROR] Line 88: Directory %DIR% not found.
   exit /b 1
)

cd %DIR% || (
   echo [ERROR] Line 92: Failed to change directory to %DIR%
   exit /b 1
)

if not exist "mvnw.cmd" (
    if not exist "..\mvnw.cmd" (
         echo [ERROR] Line 97: mvnw.cmd not found in %DIR% or parent.
         exit /b 1
    )
)

start "%NAME%" /MIN cmd /c "mvnw.cmd spring-boot:run > ..\%LOG% 2>&1"
REM start does not usually return errorlevel unless binary missing.

cd ..

REM 3. Wait for startup
echo Waiting for %NAME% to be ready on port %PORT%...
:WaitStart
timeout /t 2 /nobreak >nul
netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
if !errorlevel! neq 0 goto WaitStart

echo %NAME% restarted successfully!
echo Logs available in %LOG%
exit /b 0
