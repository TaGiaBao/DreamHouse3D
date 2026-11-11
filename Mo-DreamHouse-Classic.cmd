@echo off
setlocal
cd /d "%~dp0game-code"
call "%~dp0game-code\Mo-DreamHouse-Classic.cmd"
exit /b %errorlevel%
