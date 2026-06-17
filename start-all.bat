@echo off
setlocal enabledelayedexpansion

:: KYC Microservices Stack Startup - Windows
:: Requires Java on PATH. Run from the repo root as Administrator if port cleanup fails.

set ROOT_DIR=%~dp0
set ROOT_DIR=%ROOT_DIR:~0,-1%
set STARTUP_TIMEOUT=120
set EUREKA_WAIT=10
set LOG_DIR=%ROOT_DIR%\logs

echo === KYC Microservices Stack Startup ===
echo Root directory: %ROOT_DIR%
echo.

goto :main

:: ── :check_port port ─────────────────────────────────────────────────────────
:: Sets CHECK_PORT_RESULT=1 if port is LISTENING, 0 otherwise.
:check_port
    set CHECK_PORT_RESULT=0
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /r ":%1 " ^| findstr "LISTENING" 2^>nul') do (
        set CHECK_PORT_RESULT=1
    )
    exit /b 0

:: ── :wait_for_port port name ─────────────────────────────────────────────────
:wait_for_port
    set /a _elapsed=0
    echo   Waiting for %~2 (port %~1)...
    :_wfp_loop
        call :check_port %~1
        if "!CHECK_PORT_RESULT!"=="1" (
            echo   OK  %~2 is running on port %~1
            exit /b 0
        )
        if !_elapsed! GEQ %STARTUP_TIMEOUT% (
            echo   FAIL  %~2 did not start within %STARTUP_TIMEOUT%s on port %~1
            exit /b 1
        )
        timeout /t 2 /nobreak >nul
        set /a _elapsed+=2
        goto :_wfp_loop

:: ── :start_service port name module ─────────────────────────────────────────
:start_service
    set _PORT=%~1
    set _NAME=%~2
    set _MODULE=%~3
    set _LOG=%LOG_DIR%\%_MODULE%.log
    set _JAR=%ROOT_DIR%\%_MODULE%\build\libs\app.jar

    if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

    call :check_port %_PORT%
    if "!CHECK_PORT_RESULT!"=="1" (
        echo %_NAME% already running on port %_PORT%
        exit /b 0
    )

    echo.
    echo Starting: %_NAME%

    if exist "%_JAR%" (
        echo   Using pre-built JAR: %_JAR%
        start "%_NAME%" /MIN cmd /c "java -jar ""%_JAR%"" > ""%_LOG%"" 2>&1"
    ) else (
        echo   No pre-built JAR found, building with Gradle...
        start "%_NAME%" /MIN cmd /c """%ROOT_DIR%\gradlew.bat"" -p ""%ROOT_DIR%"" :%_MODULE%:bootRun > ""%_LOG%"" 2>&1"
    )

    call :wait_for_port %_PORT% %_NAME%
    if errorlevel 1 (
        echo   Last 20 lines of %_LOG%:
        powershell -NoProfile -Command "Get-Content '%_LOG%' -Tail 20 -ErrorAction SilentlyContinue"
    )
    exit /b 0

:: ── Main ─────────────────────────────────────────────────────────────────────
:main

echo Step 1: Cleaning up existing processes on KYC ports
for %%p in (8761 8084 8081 8082 8085 8086 8083 8080 5173) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /r ":%%p " ^| findstr "LISTENING" 2^>nul') do (
        taskkill /PID %%a /F >nul 2>&1
    )
)
timeout /t 2 /nobreak >nul

echo.
echo Step 2: Starting Eureka Service Registry ^(required by all other services^)
call :start_service 8761 "Service Registry" "service-registry"
if errorlevel 1 (
    echo FATAL: Eureka failed to start. Aborting.
    exit /b 1
)
echo Waiting %EUREKA_WAIT%s for Eureka to be fully ready...
timeout /t %EUREKA_WAIT% /nobreak >nul

echo.
echo Step 3: Starting core services
call :start_service 8084 "Auth Service"      "auth-service"
timeout /t 3 /nobreak >nul
call :start_service 8081 "Risk Service"      "risk-service"
timeout /t 3 /nobreak >nul
call :start_service 8082 "Screening Service" "screening-service"
timeout /t 3 /nobreak >nul
call :start_service 8085 "Document Service"  "document-service"
timeout /t 3 /nobreak >nul
call :start_service 8086 "KYC Orchestration" "kyc-orchestration"
timeout /t 3 /nobreak >nul

echo.
echo Step 4: Starting UI backend
call :start_service 8083 "Viewer Service"    "viewer"
timeout /t 3 /nobreak >nul
call :start_service 8080 "API Gateway"       "api-gateway"
timeout /t 3 /nobreak >nul

echo.
echo Step 5: Starting Frontend
call :check_port 5173
if "!CHECK_PORT_RESULT!"=="1" (
    echo Frontend already running on port 5173
) else (
    where npm >nul 2>&1
    if errorlevel 1 (
        echo WARNING: npm not found - skipping frontend
        echo          Install Node.js from https://nodejs.org and re-run.
    ) else (
        echo Starting frontend...
        if not exist "%ROOT_DIR%\viewer\frontend\node_modules" (
            echo   Installing npm dependencies...
            npm --prefix "%ROOT_DIR%\viewer\frontend" install > "%LOG_DIR%\frontend-install.log" 2>&1
        )
        start "Frontend" /MIN cmd /c "npm --prefix ""%ROOT_DIR%\viewer\frontend"" run dev > ""%LOG_DIR%\frontend.log"" 2>&1"
        call :wait_for_port 5173 "Frontend"
    )
)

:: ── Summary ───────────────────────────────────────────────────────────────────
echo.
echo === Startup Complete ===
echo.
echo Access your application:
echo   Frontend:      http://localhost:5173
echo   API Gateway:   http://localhost:8080
echo   Eureka:        http://localhost:8761
echo.
echo Service Swagger docs:
echo   Viewer:        http://localhost:8083/swagger-ui.html
echo   Orchestration: http://localhost:8086/swagger-ui.html
echo   Screening:     http://localhost:8082/swagger-ui.html
echo   Risk:          http://localhost:8081/swagger-ui.html
echo.
echo Logs: %LOG_DIR%
echo.
echo To stop all Java services:  taskkill /IM java.exe /F
echo To view a log:              type "%LOG_DIR%\viewer.log"
echo.
endlocal
