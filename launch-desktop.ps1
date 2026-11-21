$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if (Test-Path "$ScriptDir\target\release\desktop.exe") {
    Start-Process "$ScriptDir\target\release\desktop.exe" -ArgumentList $args
    exit 0
}

if (Test-Path "$ScriptDir\target\debug\desktop.exe") {
    Start-Process "$ScriptDir\target\debug\desktop.exe" -ArgumentList $args
    exit 0
}

Set-Location "$ScriptDir\apps\desktop"
if (-not (Test-Path "node_modules")) {
    Write-Host "Installing frontend dependencies..."
    npm install
}

Write-Host "Starting AetherLink in development mode..."
npm run tauri dev
