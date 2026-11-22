#!/bin/bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Linux WebKitGTK acceleration compatibility fix
export WEBKIT_DISABLE_DMABUF_RENDERER=1

# Cleanly terminate any stale instances to free TCP port 45876
if command -v fuser >/dev/null 2>&1; then
    fuser -k 45876/tcp 2>/dev/null || true
fi
pkill -x vexyron 2>/dev/null || true
pkill -x aetherlink 2>/dev/null || true
pkill -x desktop 2>/dev/null || true
sleep 0.2

# Priority 1: Standalone binary in same directory (extracted release package)
if [ -f "$DIR/vexyron" ]; then
    chmod +x "$DIR/vexyron"
    exec "$DIR/vexyron" "$@"
elif [ -f "$DIR/aetherlink" ]; then
    chmod +x "$DIR/aetherlink"
    exec "$DIR/aetherlink" "$@"
elif [ -f "$DIR/desktop" ]; then
    chmod +x "$DIR/desktop"
    exec "$DIR/desktop" "$@"
# Priority 2: Built binary in workspace target/ directory
elif [ -f "$DIR/target/release/vexyron" ]; then
    chmod +x "$DIR/target/release/vexyron"
    exec "$DIR/target/release/vexyron" "$@"
elif [ -f "$DIR/target/release/desktop" ]; then
    chmod +x "$DIR/target/release/desktop"
    exec "$DIR/target/release/desktop" "$@"
elif [ -f "$DIR/target/debug/vexyron" ]; then
    chmod +x "$DIR/target/debug/vexyron"
    exec "$DIR/target/debug/vexyron" "$@"
elif [ -f "$DIR/target/debug/desktop" ]; then
    chmod +x "$DIR/target/debug/desktop"
    exec "$DIR/target/debug/desktop" "$@"
# Priority 3: Fall back to development mode if within source repository
elif [ -d "$DIR/apps/desktop" ]; then
    cd "$DIR/apps/desktop"
    if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        npm install
    fi
    exec npm run tauri dev
else
    echo "Error: Could not locate VEXYRON executable in $DIR" >&2
    exit 1
fi
