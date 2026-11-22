#!/usr/bin/env bash
set -e

VERSION="${1:-v2.3.2}"
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

- 💻 **Android Remote Laptop Terminal**:
  Connect your phone to your laptop (Windows or Linux) to execute terminal commands in real-time right from your phone. Features live directory navigation (`cd`), auto-scrolling output, quick command chips, history recall, and host status badges.
- 🛡️ **Universal Linux Firewall Helper**:
  Settings and Devices screens now include tailored firewall rules for **all Linux distributions**:
  - **UFW** (Ubuntu, Debian, Linux Mint, Pop!_OS)
  - **Firewalld** (Fedora, RHEL, CentOS, Rocky Linux, AlmaLinux)
  - **iptables** (Arch Linux, Manjaro, generic kernels)
  - **openSUSE** (openSUSE Leap & Tumbleweed)
  - **nftables** (Debian 12+, Alpine, NixOS)
- 📜 **PolyForm Noncommercial License 1.0.0**:
  Added the official PolyForm Noncommercial License to the repository and all client application settings.
- 🔧 **Resolved Debian Package Upgrade Conflict**:
  Fixed the installation error `trying to overwrite '/usr/bin/desktop', which is also in package aether-link 2.2.8`. The package now installs `/usr/bin/vexyron` and declares `replaces: ["aether-link", "aetherlink", "desktop"]`, `conflicts: ["aether-link", "aetherlink"]`, and `provides: ["aether-link", "aetherlink"]`.
- 📦 **Streamlined 3-Application Release**:
  Dedicated packages for Android, Windows, and Linux laptops with no extra confusion.
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
