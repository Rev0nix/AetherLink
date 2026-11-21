#!/bin/bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# If another desktop instance is not running, launch release or debug binary
if [ -f "$DIR/target/release/desktop" ]; then
    exec "$DIR/target/release/desktop" "$@"
elif [ -f "$DIR/target/debug/desktop" ]; then
    exec "$DIR/target/debug/desktop" "$@"
else
    cd "$DIR/apps/desktop"
    exec npm run tauri dev
fi
