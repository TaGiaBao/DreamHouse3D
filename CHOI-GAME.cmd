@echo off
setlocal
cd /d "%~dp0game-code"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0game-code\Start-DreamHouse.ps1"
exit /b %errorlevel%
