#!/bin/bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$HOME/.local/share/applications"
mkdir -p "$HOME/.local/share/icons/hicolor/512x512/apps"

if [ -f "$DIR/icon.png" ]; then
    cp "$DIR/icon.png" "$HOME/.local/share/icons/hicolor/512x512/apps/aetherlink.png"
elif [ -f "$DIR/apps/desktop/src-tauri/icons/icon.png" ]; then
    cp "$DIR/apps/desktop/src-tauri/icons/icon.png" "$HOME/.local/share/icons/hicolor/512x512/apps/aetherlink.png"
fi

cat > "$HOME/.local/share/applications/aetherlink.desktop" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=AetherLink
GenericName=Device Companion & Sync
Comment=Seamless device companion & clipboard/file sync between Laptop and Phone
Exec=$DIR/launch-desktop.sh %U
Icon=$HOME/.local/share/icons/hicolor/512x512/apps/aetherlink.png
Terminal=false
Categories=Network;FileTransfer;Utility;
StartupNotify=true
StartupWMClass=aetherlink
Keywords=sync;clipboard;aetherlink;phone;file;transfer;
EOF

chmod +x "$HOME/.local/share/applications/aetherlink.desktop"
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
fi

echo "=========================================================="
echo " AetherLink desktop shortcut successfully installed!"
echo " You can now launch AetherLink from your application menu."
echo "=========================================================="
