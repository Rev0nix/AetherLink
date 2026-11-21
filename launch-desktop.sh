#!/bin/bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Linux WebKitGTK acceleration compatibility fix
export WEBKIT_DISABLE_DMABUF_RENDERER=1

# Priority 1: Standalone binary in same directory (extracted release package)
if [ -x "$DIR/aetherlink" ]; then
    exec "$DIR/aetherlink" "$@"
elif [ -f "$DIR/aetherlink" ]; then
    chmod +x "$DIR/aetherlink"
    exec "$DIR/aetherlink" "$@"
elif [ -x "$DIR/desktop" ]; then
    exec "$DIR/desktop" "$@"
elif [ -f "$DIR/desktop" ]; then
    chmod +x "$DIR/desktop"
    exec "$DIR/desktop" "$@"
# Priority 2: Built binary in workspace target/ directory
elif [ -x "$DIR/target/release/desktop" ]; then
    exec "$DIR/target/release/desktop" "$@"
elif [ -x "$DIR/target/debug/desktop" ]; then
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
    echo "Error: Could not locate AetherLink executable in $DIR" >&2
    exit 1
fi
