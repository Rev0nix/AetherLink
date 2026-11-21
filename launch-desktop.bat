@echo off
setlocal
set "DIR=%~dp0"

if exist "%DIR%target\release\desktop.exe" (
    start "" "%DIR%target\release\desktop.exe" %*
    exit /b 0
)

if exist "%DIR%target\debug\desktop.exe" (
    start "" "%DIR%target\debug\desktop.exe" %*
    exit /b 0
)

cd /d "%DIR%apps\desktop"
if not exist "node_modules" (
    echo Installing frontend dependencies...
    call npm install
)

echo Starting AetherLink in development mode...
call npm run tauri dev
