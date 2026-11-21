$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Unblock extracted files to bypass Windows Smart App Control and SmartScreen Mark-of-the-Web blocking
Get-ChildItem -Path $ScriptDir -Recurse | Unblock-File -ErrorAction SilentlyContinue

# Priority 1: Standalone executable in same directory (extracted release package)
if (Test-Path "$ScriptDir\AetherLink.exe") {
    Start-Process "$ScriptDir\AetherLink.exe" -ArgumentList $args
    exit 0
}
if (Test-Path "$ScriptDir\desktop.exe") {
    Start-Process "$ScriptDir\desktop.exe" -ArgumentList $args
    exit 0
}

# Priority 2: Built binary in repository target/
if (Test-Path "$ScriptDir\target\release\desktop.exe") {
    Start-Process "$ScriptDir\target\release\desktop.exe" -ArgumentList $args
    exit 0
}
if (Test-Path "$ScriptDir\target\debug\desktop.exe") {
    Start-Process "$ScriptDir\target\debug\desktop.exe" -ArgumentList $args
    exit 0
}

# Priority 3: Fall back to development mode if within source repository
if (Test-Path "$ScriptDir\apps\desktop") {
    Set-Location "$ScriptDir\apps\desktop"
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing frontend dependencies..."
        npm install
    }
    Write-Host "Starting AetherLink in development mode..."
    npm run tauri dev
    exit 0
}

Write-Error "Could not locate AetherLink.exe in $ScriptDir"
