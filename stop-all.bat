@echo off
setlocal enabledelayedexpansion

echo === KYC Microservices Stack Shutdown ===
echo.

:: Stop in reverse startup order (frontend first, registry last)
call :stop_service 5173 "Frontend"
call :stop_service 8080 "API Gateway"
call :stop_service 8083 "Viewer Service"
call :stop_service 8086 "KYC Orchestration"
call :stop_service 8085 "Document Service"
call :stop_service 8082 "Screening Service"
call :stop_service 8081 "Risk Service"
call :stop_service 8084 "Auth Service"
call :stop_service 8761 "Service Registry"

echo.
echo === Shutdown Complete ===
echo.
goto :eof

:: ── :stop_service port name ───────────────────────────────────────────────────
:stop_service
    set _PORT=%~1
    set _NAME=%~2
    set _PID=

    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /r ":%_PORT% " ^| findstr "LISTENING" 2^>nul') do (
        set _PID=%%a
    )

    if defined _PID (
        echo Stopping %_NAME% on port %_PORT% ^(PID: %_PID%^)...
        taskkill /F /PID %_PID% >nul 2>&1
        if !errorlevel! equ 0 (
            echo   OK  Stopped.
        ) else (
            echo   WARN  taskkill returned non-zero for PID %_PID% -- may already be gone.
        )
    ) else (
        echo %_NAME% is not running on port %_PORT%.
    )
    exit /b 0
