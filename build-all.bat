@echo off
setlocal enabledelayedexpansion

echo Building all services via Gradle...

REM Gradle 8.12 cannot run on newer JDKs (e.g. "Unsupported class file major version 69").
REM Use a bundled JDK 21 if one is present alongside this script (look for jdk-21*\bin\java.exe).
for /d %%D in ("%~dp0jdk-21*") do (
    if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
)
if defined JAVA_HOME echo Using bundled JDK 21 at %JAVA_HOME%

REM Use the root gradle wrapper to build everything
call gradlew.bat clean build -x test
if %errorlevel% neq 0 (
    echo Backend build failed
    exit /b 1
)

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
