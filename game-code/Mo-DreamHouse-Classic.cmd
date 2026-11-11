@echo off
setlocal
set "DREAMHOUSE_JDK="
for /f "delims=" %%J in ('powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ". '%~dp0Find-Java.ps1'; Find-DreamHouseJava" 2^>nul') do set "DREAMHOUSE_JDK=%%J\bin"
set "DREAMHOUSE_FX=%~dp0classic\runtime"
cd /d "%~dp0classic"
if not exist "%DREAMHOUSE_FX%\fx\javafx-controls.jar" goto maven
if not exist "%DREAMHOUSE_JDK%\javac.exe" goto maven
if not exist target\classes mkdir target\classes
"%DREAMHOUSE_JDK%\javac.exe" --release 21 -encoding UTF-8 --module-path "%DREAMHOUSE_FX%\fx" --add-modules javafx.controls -d target\classes src\main\java\vn\dreamhouse\*.java
if errorlevel 1 goto failed
copy /Y "src\main\resources\vn\dreamhouse\studio.css" "target\classes\vn\dreamhouse\studio.css" >nul
if errorlevel 1 goto failed
"%DREAMHOUSE_JDK%\java.exe" --enable-native-access=javafx.graphics "-Djavafx.cachedir=%DREAMHOUSE_FX%\fx-cache" --module-path "%DREAMHOUSE_FX%\fx" --add-modules javafx.controls -cp target\classes vn.dreamhouse.DreamHouseApp
if errorlevel 1 goto failed
exit /b 0

:maven
call mvn javafx:run
if errorlevel 1 goto failed
exit /b 0

:failed
echo DreamHouse Classic could not start. See the error above.
pause
exit /b 1
