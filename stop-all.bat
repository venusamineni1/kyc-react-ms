@echo off
setlocal enabledelayedexpansion

echo Stopping KYC Microservices Stack...

call :StopService 8761 "Service Registry"
call :StopService 8080 "API Gateway"
call :StopService 8084 "Auth Service"
call :StopService 8081 "Risk Service"
call :StopService 8082 "Screening Service"
call :StopService 8085 "Document Service"
call :StopService 8083 "Viewer Service"

REM Frontend (Port 5173)
call :StopService 5173 "Frontend"

echo All shutdown commands issued.
goto :eof

:StopService
set "PORT=%~1"
set "NAME=%~2"

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    set PID=%%a
)

if defined PID (
    echo Stopping %NAME% (PID: %PID%) on port %PORT%...
    taskkill /F /PID %PID% >nul 2>&1
    set "PID="
) else (
    echo %NAME% is not running on port %PORT%.
)
exit /b 0
