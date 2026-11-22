#!/bin/bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$HOME/.local/share/applications"
mkdir -p "$HOME/.local/share/icons/hicolor/512x512/apps"

ICON_SRC=""
if [ -f "$DIR/icon.png" ]; then
    ICON_SRC="$DIR/icon.png"
elif [ -f "$DIR/vexyron-app-icon.png" ]; then
    ICON_SRC="$DIR/vexyron-app-icon.png"
elif [ -f "$DIR/apps/desktop/src-tauri/icons/icon.png" ]; then
    ICON_SRC="$DIR/apps/desktop/src-tauri/icons/icon.png"
fi

if [ -n "$ICON_SRC" ]; then
    cp "$ICON_SRC" "$HOME/.local/share/icons/hicolor/512x512/apps/vexyron.png"
fi

cat > "$HOME/.local/share/applications/vexyron.desktop" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=VEXYRON
GenericName=Device Companion & Sync
Comment=Seamless device companion & clipboard/file sync between Laptop and Phone
Exec=$DIR/launch-desktop.sh %U
Icon=$HOME/.local/share/icons/hicolor/512x512/apps/vexyron.png
Terminal=false
Categories=Network;FileTransfer;Utility;
StartupNotify=true
StartupWMClass=vexyron
Keywords=sync;clipboard;vexyron;phone;file;transfer;
EOF

chmod +x "$HOME/.local/share/applications/vexyron.desktop"

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
fi

echo "=========================================================="
echo " VEXYRON desktop shortcut successfully installed!"
echo " You can now launch VEXYRON from your application menu."
echo "=========================================================="
