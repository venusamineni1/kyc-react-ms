@echo off
setlocal enabledelayedexpansion

:: KYC Microservices Stack Startup - Windows
:: Requires Java on PATH. Run from the repo root.

set "ROOT=%~dp0"
set STARTUP_TIMEOUT=120
set EUREKA_WAIT=10
set "LOGS=%ROOT%logs"

if not exist "%LOGS%" mkdir "%LOGS%"

echo === KYC Microservices Stack Startup ===
echo Root: %ROOT%
echo.

:: ── Step 1: Kill anything already on our ports ────────────────────────────────
echo Cleaning up existing processes...
for %%P in (8761 8084 8081 8082 8085 8086 8083 8080 5173) do call :kill_port %%P
timeout /t 2 /nobreak >nul

:: ── Step 2: Eureka (must come first) ─────────────────────────────────────────
echo.
echo [1/8] Service Registry  ^(port 8761^) - required by all other services
call :svc 8761 service-registry
if errorlevel 1 ( echo FATAL: Eureka did not start. Aborting. & exit /b 1 )
echo Waiting %EUREKA_WAIT%s for Eureka to be fully ready...
timeout /t %EUREKA_WAIT% /nobreak >nul

:: ── Step 3: Core services ────────────────────────────────────────────────────
echo.
echo [2/8] Auth Service  ^(port 8084^)
call :svc 8084 auth-service
timeout /t 3 /nobreak >nul

echo.
echo [3/8] Risk Service  ^(port 8081^)
call :svc 8081 risk-service
timeout /t 3 /nobreak >nul

echo.
echo [4/8] Screening Service  ^(port 8082^)
call :svc 8082 screening-service
timeout /t 3 /nobreak >nul

echo.
echo [5/8] Document Service  ^(port 8085^)
call :svc 8085 document-service
timeout /t 3 /nobreak >nul

echo.
echo [6/8] KYC Orchestration  ^(port 8086^)
call :svc 8086 kyc-orchestration
timeout /t 3 /nobreak >nul

:: ── Step 4: UI backend ────────────────────────────────────────────────────────
echo.
echo [7/8] Viewer Service  ^(port 8083^)
call :svc 8083 viewer
timeout /t 3 /nobreak >nul

echo.
echo [8/8] API Gateway  ^(port 8080^)
call :svc 8080 api-gateway
timeout /t 3 /nobreak >nul

:: ── Step 5: Frontend ─────────────────────────────────────────────────────────
echo.
echo Frontend  ^(port 5173^)
netstat -ano 2>nul | findstr ":5173 " | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo   Already running on port 5173
) else (
    where npm >nul 2>&1
    if errorlevel 1 (
        echo   WARNING: npm not found - skipping frontend
        echo            Install Node.js from https://nodejs.org and re-run.
    ) else (
        if not exist "%ROOT%viewer\frontend\node_modules" (
            echo   Installing npm dependencies...
            npm --prefix "%ROOT%viewer\frontend" install >"%LOGS%\frontend-install.log" 2>&1
        )
        echo   Launching...
        start "frontend" /MIN cmd /c "npm --prefix ""%ROOT%viewer\frontend"" run dev 1>""%LOGS%\frontend.log"" 2>&1"
        call :wait 5173 frontend
    )
)

:: ── Summary ───────────────────────────────────────────────────────────────────
echo.
echo === Startup Complete ===
echo.
echo   Frontend:      http://localhost:5173
echo   API Gateway:   http://localhost:8080
echo   Eureka:        http://localhost:8761
echo.
echo   Viewer API:    http://localhost:8083/swagger-ui.html
echo   Orchestration: http://localhost:8086/swagger-ui.html
echo   Screening:     http://localhost:8082/swagger-ui.html
echo   Risk:          http://localhost:8081/swagger-ui.html
echo.
echo   Logs: %LOGS%
echo.
echo   Stop all:   taskkill /IM java.exe /F
echo   View log:   type "%LOGS%\viewer.log"
echo.
endlocal
goto :eof

:: =============================================================================
:: :svc PORT MODULE
::   Launch a Spring Boot service and wait for it to bind its port.
:: =============================================================================
:svc
  set "P=%~1"
  set "M=%~2"
  set "JAR=%ROOT%%M%\build\libs\app.jar"
  set "LOG=%LOGS%\%M%.log"

  netstat -ano 2>nul | findstr ":%P% " | findstr "LISTENING" >nul
  if not errorlevel 1 (
    echo   Already running on port %P%
    exit /b 0
  )

  if exist "%JAR%" (
    echo   Launching from JAR...
    start "%M%" /MIN cmd /c "java -jar ""%JAR%"" 1>""%LOG%"" 2>&1"
  ) else (
    echo   JAR not found - building with Gradle...
    start "%M%" /MIN cmd /c """%ROOT%gradlew.bat"" -p ""%ROOT%"" :%M%:bootRun 1>""%LOG%"" 2>&1"
  )

  call :wait %P% %M%
  exit /b %errorlevel%

:: =============================================================================
:: :wait PORT NAME
::   Poll netstat until PORT is LISTENING or STARTUP_TIMEOUT is reached.
:: =============================================================================
:wait
  set /a _T=0
  echo   Waiting for %~2 on port %~1...
  :_wait_loop
    netstat -ano 2>nul | findstr ":%~1 " | findstr "LISTENING" >nul
    if not errorlevel 1 (
      echo   OK  %~2 is up on port %~1
      exit /b 0
    )
    if !_T! GEQ %STARTUP_TIMEOUT% (
      echo   TIMEOUT  %~2 did not start within %STARTUP_TIMEOUT%s
      powershell -NoProfile -Command "Get-Content '%LOGS%\%~2.log' -Tail 20 -ErrorAction SilentlyContinue"
      exit /b 1
    )
    timeout /t 2 /nobreak >nul
    set /a _T+=2
    goto :_wait_loop

:: =============================================================================
:: :kill_port PORT
::   Kill the process listening on PORT, if any.
:: =============================================================================
:kill_port
  for /f "tokens=5" %%A in ('netstat -ano 2^>nul ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
    taskkill /PID %%A /F >nul 2>&1
  )
  exit /b 0
