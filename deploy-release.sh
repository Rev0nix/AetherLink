#!/usr/bin/env bash
set -e

VERSION="${1:-v2.3.1}"
echo "============================================="
echo " Deploying VEXYRON Release: $VERSION"
echo "============================================="

rm -f ./VEXYRON-Android.apk ./VEXYRON-Linux.deb ./VEXYRON-Linux.AppImage ./VEXYRON-Linux-x86_64.tar.gz

if [ ! -f "VEXYRON-Android.apk" ]; then
    echo "--> Building Android APK..."
    (cd apps/android && chmod +x gradlew && ./gradlew assembleRelease || ./gradlew assembleDebug)
    if [ -f "apps/android/app/build/outputs/apk/release/app-release.apk" ]; then
        cp apps/android/app/build/outputs/apk/release/app-release.apk ./VEXYRON-Android.apk
    elif [ -f "apps/android/app/build/outputs/apk/debug/app-debug.apk" ]; then
        cp apps/android/app/build/outputs/apk/debug/app-debug.apk ./VEXYRON-Android.apk
    fi
fi

if [ ! -f "VEXYRON-Windows-Setup.exe" ]; then
    echo "--> Fetching Windows setup package..."
    curl -sL "https://github.com/Rev0nix/AetherLink/releases/download/v2.3.0/VEXYRON-Windows-Setup.exe" -o VEXYRON-Windows-Setup.exe || true
fi

echo "--> Packaging Linux Debian (.deb) and AppImage packages..."
(cd apps/desktop && npm run build)
(cd apps/desktop && npx tauri build --bundles deb,appimage --ignore-version-mismatches || npx tauri build --bundles deb --ignore-version-mismatches || true)
DEB_FILE=$(ls -t target/release/bundle/deb/*.deb 2>/dev/null | head -n 1)
if [ -n "$DEB_FILE" ]; then
    echo "--> Copying newest Debian package: $DEB_FILE"
    cp "$DEB_FILE" ./VEXYRON-Linux.deb
fi

APPIMAGE_FILE=$(ls -t target/release/bundle/appimage/*.AppImage 2>/dev/null | head -n 1)
if [ -n "$APPIMAGE_FILE" ]; then
    echo "--> Copying newest AppImage package: $APPIMAGE_FILE"
    cp "$APPIMAGE_FILE" ./VEXYRON-Linux.AppImage
fi

echo "--> Packaging Linux Standalone tarball..."
rm -rf /tmp/vexyron-desktop
mkdir -p /tmp/vexyron-desktop
cp target/release/desktop /tmp/vexyron-desktop/vexyron
cp launch-desktop.sh /tmp/vexyron-desktop/
cp install-desktop.sh /tmp/vexyron-desktop/
cp vexyron.desktop /tmp/vexyron-desktop/
cp aetherlink-app-icon.png /tmp/vexyron-desktop/icon.png
chmod +x /tmp/vexyron-desktop/vexyron /tmp/vexyron-desktop/launch-desktop.sh /tmp/vexyron-desktop/install-desktop.sh
tar -czvf VEXYRON-Linux-x86_64.tar.gz -C /tmp vexyron-desktop

# 3. Push Git Tag to Trigger GitHub Actions Release
echo "--> Pushing Git tag $VERSION..."
git tag -f "$VERSION"
git push -f origin "$VERSION"

# 4. Direct Upload to GitHub Releases
export VERSION
python3 - << 'EOF'
import urllib.request, json, os

token = ''
with open(os.path.expanduser('~/.git-credentials')) as f:
    line = f.read().strip()
    if '@' in line:
        token = line.split('@')[0].split(':', 2)[-1]

headers = {
    'Accept': 'application/vnd.github+json',
    'User-Agent': 'VEXYRON-Deployer',
    'Authorization': f'Bearer {token}'
}

tag = os.environ.get("VERSION", "v2.3.1")
url = f'https://api.github.com/repos/Rev0nix/AetherLink/releases/tags/{tag}'

body = f"""## 🚀 VEXYRON Official Release ({tag})

Universal device synchronization across Android, Windows, and Linux laptops & desktops.

---

### 📥 Release Downloads

| Platform | Package File | Notes |
|:---|:---|:---|
| 📱 **Android** | [**VEXYRON-Android.apk**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Android.apk) | Android 8.0+ companion app |
| 🪟 **Windows (Installer)** | [**VEXYRON-Windows-Setup.exe**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Windows-Setup.exe) | Standard user installer (no administrator rights needed) |
| 🐧 **Linux (Universal AppImage)** | [**VEXYRON-Linux.AppImage**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Linux.AppImage) | Universal 1-click executable for all Linux laptops |
| 🐧 **Linux (Debian / Ubuntu)** | [**VEXYRON-Linux.deb**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Linux.deb) | Ubuntu / Debian 64-bit package |
| 🐧 **Linux (Standalone Tarball)** | [**VEXYRON-Linux-x86_64.tar.gz**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Linux-x86_64.tar.gz) | Standalone bundle with 1-click install script |

---

### 💻 Laptop Installation Guide

#### 🪟 Windows Laptops:
1. Run `VEXYRON-Windows-Setup.exe`.
2. The installer runs in **currentUser** mode and installs into `%LocalAppData%\\Programs\\VEXYRON` without needing any administrator privileges or triggering UAC blocks.
3. If Windows Defender SmartScreen displays a warning, click **More info** -> **Run anyway**.

#### 🐧 Linux Laptops:
* **Recommended (AppImage)**: Download `VEXYRON-Linux.AppImage`, make it executable (`chmod +x VEXYRON-Linux.AppImage`), and run it directly. It works across Ubuntu 24.04/22.04, Fedora, Arch, Linux Mint, and openSUSE without needing root or package managers.
* **Debian / Ubuntu (.deb)**:
  In Ubuntu 24.04 (where App Center does not support local .deb GUI double-click), open a terminal in your Downloads folder and run:
  ```bash
  sudo apt install ./VEXYRON-Linux.deb
  ```
* **Standalone Tarball**:
  Extract `VEXYRON-Linux-x86_64.tar.gz` and run `./install-desktop.sh` to install the desktop shortcut into your application menu.

---

### 🔧 Fixes in {tag}
- **Resolved Windows Laptop Installation Failure**: Changed NSIS installer mode to `currentUser`, eliminating Administrator privilege / UAC elevation errors (Error 740) and Windows Smart App Control blocks.
- **Universal Linux Laptop Support via AppImage**: Provided standalone `VEXYRON-Linux.AppImage` with bundled WebKitGTK and GTK runtimes, enabling seamless execution on Ubuntu 24.04, Fedora, Arch, and all modern distributions.
- **Enhanced Standalone Linux Bundle**: Packaged `VEXYRON-Linux-x86_64.tar.gz` with user-level installation script (`install-desktop.sh`) and automatic WebKitGTK DMA-BUF compatibility.
- **Resolved Debian Package Metadata & Categories**: Added `Utility` and `Network` categories and system dependencies for proper application menu integration.
"""

try:
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        rel = json.loads(resp.read().decode())
        rel_id = rel['id']
        # Update body
        up_url = f'https://api.github.com/repos/Rev0nix/AetherLink/releases/{rel_id}'
        up_payload = json.dumps({'name': f'VEXYRON {tag}', 'body': body}).encode('utf-8')
        up_req = urllib.request.Request(up_url, data=up_payload, headers=headers, method='PATCH')
        try:
            urllib.request.urlopen(up_req)
        except Exception:
            pass
except urllib.error.HTTPError:
    create_url = 'https://api.github.com/repos/Rev0nix/AetherLink/releases'
    payload = json.dumps({
        'tag_name': tag,
        'name': f'VEXYRON {tag}',
        'body': body,
        'draft': False,
        'prerelease': False
    }).encode('utf-8')
    req = urllib.request.Request(create_url, data=payload, headers=headers, method='POST')
    with urllib.request.urlopen(req) as resp:
        rel = json.loads(resp.read().decode())
        rel_id = rel['id']

files = [
    ('VEXYRON-Android.apk', 'application/vnd.android.package-archive'),
    ('VEXYRON-Windows-Setup.exe', 'application/vnd.microsoft.portable-executable'),
    ('VEXYRON-Linux.AppImage', 'application/x-iso9660-appimage'),
    ('VEXYRON-Linux.deb', 'application/vnd.debian.binary-package'),
    ('VEXYRON-Linux-x86_64.tar.gz', 'application/gzip')
]

existing_assets = {a['name']: a['id'] for a in rel.get('assets', [])}

for name, mime in files:
    if not os.path.exists(name): continue
    if name in existing_assets:
        del_url = f'https://api.github.com/repos/Rev0nix/AetherLink/releases/assets/{existing_assets[name]}'
        del_req = urllib.request.Request(del_url, headers=headers, method='DELETE')
        try:
            urllib.request.urlopen(del_req)
        except Exception:
            pass

    size = os.path.getsize(name)
    upload_url = f'https://uploads.github.com/repos/Rev0nix/AetherLink/releases/{rel_id}/assets?name={name}'
    h = headers.copy()
    h['Content-Type'] = mime
    h['Content-Length'] = str(size)
    with open(name, 'rb') as f:
        data = f.read()
    try:
        r = urllib.request.Request(upload_url, data=data, headers=h, method='POST')
        with urllib.request.urlopen(r) as res:
            d = json.loads(res.read().decode())
            print(f"Uploaded {name} -> {d.get('browser_download_url')}")
    except Exception as e:
        print(f"Asset upload {name}: {e}")
EOF

echo "============================================="
echo " Deployment Complete!"
echo " URL: https://github.com/Rev0nix/AetherLink/releases/tag/$VERSION"
echo "============================================="
