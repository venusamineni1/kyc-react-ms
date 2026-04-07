@echo off
setlocal enabledelayedexpansion

set ROOT_DIR=%~dp0

echo Starting KYC Microservices Stack...

call :StartService 8761 "Service Registry"  service-registry  registry.log
call :StartService 8084 "Auth Service"      auth-service      auth.log
call :StartService 8081 "Risk Service"      risk-service      risk.log
call :StartService 8082 "Screening Service" screening-service screening.log
call :StartService 8085 "Document Service"  document-service  document.log
call :StartService 8083 "Viewer Service"    viewer            viewer.log
call :StartService 8080 "API Gateway"       api-gateway       gateway.log

echo Starting Frontend...
cd /d "%ROOT_DIR%viewer\frontend"
start "Frontend" /MIN cmd /c "npm run dev > ..\..\frontend.log 2>&1"
cd /d "%ROOT_DIR%"

echo.
echo All systems operational! Logs are in the root directory.
echo Frontend: http://localhost:5173
echo Gateway:  http://localhost:8080
echo Registry: http://localhost:8761
goto :eof

:StartService
set "PORT=%~1"
set "NAME=%~2"
set "MODULE=%~3"
set "LOG=%~4"

echo Checking %NAME% on port %PORT%...
netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
if %errorlevel%==0 (
    echo %NAME% is already running on port %PORT%.
) else (
    echo Starting %NAME%...
    start "%NAME%" /MIN cmd /c ""%ROOT_DIR%gradlew.bat" -p "%ROOT_DIR%" :%MODULE%:bootRun > "%ROOT_DIR%%LOG%" 2>&1"
    echo Waiting for %NAME% to start on port %PORT%...
    :WaitLoop_%PORT%
    timeout /t 2 /nobreak >nul
    netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
    if !errorlevel! neq 0 goto WaitLoop_%PORT%
    echo %NAME% is UP!
)
exit /b 0
