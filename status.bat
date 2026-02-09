@echo off
setlocal enabledelayedexpansion

echo === KYC Microservices Status ===

call :CheckService "Service Registry " 8761
call :CheckService "API Gateway      " 8080
call :CheckService "Auth Service     " 8084
call :CheckService "Viewer Service   " 8083
call :CheckService "Risk Service     " 8081
call :CheckService "Screening Service" 8082
call :CheckService "Document Service " 8085
call :CheckService "Frontend         " 5173

echo ================================
goto :eof

:CheckService
set "NAME=%~1"
set "PORT=%~2"
set "PID="

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    set PID=%%a
)

if defined PID (
    echo [RUNNING] %NAME% on port %PORT% (PID: %PID%)
) else (
    echo [STOPPED] %NAME% (Port %PORT% free)
)
exit /b 0
