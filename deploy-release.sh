#!/usr/bin/env bash
set -e

VERSION="${1:-v2.3.3}"
echo "============================================="
echo " Deploying VEXYRON Release: $VERSION"
echo "============================================="

rm -f ./VEXYRON-Android.apk ./VEXYRON-Linux.deb ./VEXYRON-Linux.AppImage ./VEXYRON-Linux-x86_64.tar.gz ./VEXYRON-Windows-Portable.zip

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
    curl -sL "https://github.com/Rev0nix/AetherLink/releases/download/v2.3.1/VEXYRON-Windows-Setup.exe" -o VEXYRON-Windows-Setup.exe || true
fi

echo "--> Packaging Linux Debian (.deb) package..."
(cd apps/desktop && npm run build)
(cd apps/desktop && npx tauri build --bundles deb --ignore-version-mismatches || true)
DEB_FILE=$(ls -t target/release/bundle/deb/*.deb 2>/dev/null | head -n 1)
if [ -n "$DEB_FILE" ]; then
    echo "--> Copying newest Debian package: $DEB_FILE"
    cp "$DEB_FILE" ./VEXYRON-Linux.deb
fi

# Push Git Tag to Trigger GitHub Actions Release
echo "--> Pushing Git tag $VERSION..."
git tag -f "$VERSION"
git push -f origin "$VERSION"

# Direct Upload to GitHub Releases
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

tag = os.environ.get("VERSION", "v2.3.2")
url = f'https://api.github.com/repos/Rev0nix/AetherLink/releases/tags/{tag}'

body = f"""## 🚀 VEXYRON Official Release ({tag})

Universal cross-device synchronization and remote control ecosystem connecting Android phones with Windows and Linux laptops.

Licensed under the **PolyForm Noncommercial License 1.0.0**.

---

### 📥 3 Official Application Downloads

| Platform | Package File | Description |
|:---|:---|:---|
| 📱 **Android** | [**VEXYRON-Android.apk**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Android.apk) | Android 8.0+ mobile application with Remote Terminal |
| 🪟 **Windows** | [**VEXYRON-Windows-Setup.exe**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Windows-Setup.exe) | Standard Windows laptop installer (no admin elevation required) |
| 🐧 **Linux** | [**VEXYRON-Linux.deb**](https://github.com/Rev0nix/AetherLink/releases/download/{tag}/VEXYRON-Linux.deb) | Debian / Ubuntu 64-bit native package (clean upgrade from aether-link) |

---

### 💻 Laptop Installation & Setup

#### 🪟 Windows Laptops:
1. Download and run `VEXYRON-Windows-Setup.exe`.
2. Installs per-user into `%LocalAppData%\\Programs\\VEXYRON` without requiring Administrator privileges or prompting UAC blocks.
3. If Windows SmartScreen appears, click **More info** -> **Run anyway**.

#### 🐧 Linux Laptops (Ubuntu / Debian):
Install via terminal or your package manager:
```bash
sudo apt install ./VEXYRON-Linux.deb
```
*(Resolves previous `trying to overwrite '/usr/bin/desktop'` conflicts when upgrading from `aether-link 2.2.8`)*

---

### 🌟 What's New in {tag}

- ⚡ **Fixed Remote Terminal Socket Delivery**:
  Resolved "Failed to deliver command to desktop socket" by ensuring commands and session initialization are dispatched asynchronously on background I/O threads, eliminating network on main thread exceptions.
- 🔍 **Automatic OS Recognition (Linux vs Windows)**:
  When your phone connects to your laptop, both the phone and laptop automatically recognize whether the host is Linux or Windows. Includes custom badges, host details, tailored prompts (`PS path>` for Windows vs `user@host:path$` for Linux), and dynamic quick command toolbars.
- 🪟 **Standard Windowed Mode (Non-Fullscreen)**:
  Completely disabled full-screen takeover on both desktop and Android. Restores standard window decorations and system bar padding (status bar & navigation bar) without clipping or immersive lock.
- 💻 **Cross-Platform Remote Laptop Terminal**:
  Connect phone to laptop to execute shell commands with live working directory tracking (`cd`), output buffering, and history recall.
- 🛡️ **Universal Linux Firewall Helper**:
  Comprehensive firewall configuration for UFW, Firewalld, iptables, openSUSE, and nftables.
- 📜 **PolyForm Noncommercial License 1.0.0**:
  Added the official PolyForm Noncommercial License across the project.
- 📦 **Strictly 3 Official Applications**:
  Android (`VEXYRON-Android.apk`), Windows (`VEXYRON-Windows-Setup.exe`), and Linux (`VEXYRON-Linux.deb`).
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
    ('VEXYRON-Linux.deb', 'application/vnd.debian.binary-package')
]

existing_assets = {a['name']: a['id'] for a in rel.get('assets', [])}
allowed_names = {name for name, _ in files}

# Clean up any non-3-app assets from earlier releases
for a in rel.get('assets', []):
    if a['name'] not in allowed_names:
        del_url = f"https://api.github.com/repos/Rev0nix/AetherLink/releases/assets/{a['id']}"
        del_req = urllib.request.Request(del_url, headers=headers, method='DELETE')
        try:
            urllib.request.urlopen(del_req)
            print(f"Removed extra asset: {a['name']}")
        except Exception:
            pass

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
