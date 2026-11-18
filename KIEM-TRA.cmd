@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0game-code\Run-DreamHouse-Checks.ps1" %*
exit /b %errorlevel%
