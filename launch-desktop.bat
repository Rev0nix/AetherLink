@echo off
setlocal enabledelayedexpansion
set "DIR=%~dp0"

:: Unblock extracted files to prevent Windows Smart App Control and SmartScreen Mark-of-the-Web blocking
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path '%DIR%' -Recurse | Unblock-File" >nul 2>&1

:: Cleanly terminate any stale instances to free TCP port 45876
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":45876"') do (
    taskkill /F /PID %%a >nul 2>&1
)
taskkill /F /IM VEXYRON.exe >nul 2>&1
taskkill /F /IM vexyron.exe >nul 2>&1
taskkill /F /IM AetherLink.exe >nul 2>&1
taskkill /F /IM desktop.exe >nul 2>&1

:: Priority 1: Standalone executable in same directory (extracted release package)
if exist "%DIR%VEXYRON.exe" (
    start "" "%DIR%VEXYRON.exe" %*
    exit /b 0
)
if exist "%DIR%vexyron.exe" (
    start "" "%DIR%vexyron.exe" %*
    exit /b 0
)
if exist "%DIR%AetherLink.exe" (
    start "" "%DIR%AetherLink.exe" %*
    exit /b 0
)
if exist "%DIR%desktop.exe" (
    start "" "%DIR%desktop.exe" %*
    exit /b 0
)

:: Priority 2: Built binary in repository target/
if exist "%DIR%target\release\desktop.exe" (
    start "" "%DIR%target\release\desktop.exe" %*
    exit /b 0
)
if exist "%DIR%target\debug\desktop.exe" (
    start "" "%DIR%target\debug\desktop.exe" %*
    exit /b 0
)

:: Priority 3: Fall back to development mode if within source repository
if exist "%DIR%apps\desktop" (
    cd /d "%DIR%apps\desktop"
    if not exist "node_modules" (
        echo Installing frontend dependencies...
        call npm install
    )
    echo Starting VEXYRON in development mode...
    call npm run tauri dev
    exit /b 0
)

echo Error: Could not locate VEXYRON.exe in %DIR%
pause
exit /b 1
