@echo off
echo Restarting all services...

call stop-all.bat
timeout /t 5 /nobreak >nul
call start-all.bat
