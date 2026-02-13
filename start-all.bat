@echo off
setlocal enabledelayedexpansion

echo Starting KYC Microservices Stack...

REM 1. Service Registry
call :StartService 8761 "Service Registry" service-registry registry.log
REM 2. Auth Service
call :StartService 8084 "Auth Service" auth-service auth.log
REM 3. Risk Service
call :StartService 8081 "Risk Service" risk-service risk.log
REM 4. Screening Service
call :StartService 8082 "Screening Service" screening-service screening.log
REM 5. Document Service
call :StartService 8085 "Document Service" document-service document.log
REM 6. Viewer Service
call :StartService 8083 "Viewer Service" viewer viewer.log
REM 7. API Gateway
call :StartService 8080 "API Gateway" api-gateway gateway.log

REM 8. Frontend
echo Starting Frontend...
cd viewer/frontend
start "Frontend" /MIN cmd /c "npm run dev > ../../frontend.log 2>&1"
cd ../..

echo.
echo All systems operational! Logs are in the root directory.
echo Frontend: http://localhost:5173
echo Gateway:  http://localhost:8080
echo Registry: http://localhost:8761

goto :eof

:StartService
set "PORT=%~1"
set "NAME=%~2"
set "DIR=%~3"
set "LOG=%~4"

echo Checking %NAME% on port %PORT%...
netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
if %errorlevel%==0 (
    echo %NAME% is already running on port %PORT%.
) else (
    echo Starting %NAME%...
    cd %DIR%
    REM Start in a new minimized window. Use cmd /c to run the command and redirect output.
    REM Using "start /B" keeps it in the same window, but logging redirection might interfere.
    REM Using separate window is safer for visibility.
    start "%NAME%" /MIN cmd /c "..\mvnw.cmd spring-boot:run > ..\%LOG% 2>&1"
    cd ..
    
    echo Waiting for %NAME% to start on port %PORT%...
    :WaitLoop
    timeout /t 2 /nobreak >nul
    netstat -ano | findstr ":%PORT%" | findstr "LISTENING" >nul
    if !errorlevel! neq 0 goto WaitLoop
    echo %NAME% is UP!
)
exit /b 0
