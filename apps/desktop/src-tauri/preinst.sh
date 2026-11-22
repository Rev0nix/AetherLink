#!/bin/sh
# Terminate old running instances
killall -9 aether-link aetherlink desktop vexyron 2>/dev/null || true
# Clean up obsolete binaries and desktop entry files from old aether-link packages
rm -f /usr/share/applications/aether-link.desktop /usr/share/applications/aetherlink.desktop 2>/dev/null || true
rm -f /usr/bin/desktop /usr/bin/aether-link /usr/bin/aetherlink 2>/dev/null || true
exit 0
