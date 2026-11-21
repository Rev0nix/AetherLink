#!/usr/bin/env bash
set -e

VERSION="${1:-v2.2.2}"
echo "============================================="
echo " Deploying AetherLink Release: $VERSION"
echo "============================================="

# Clean stale artifacts to ensure fresh release compilation
rm -f 1-AetherLink-Android.apk 2-AetherLink-Windows-Setup.exe 3-AetherLink-Linux.deb target/release/desktop

# 1. Build Android APK
echo "--> Building Android APK..."
(cd apps/android && chmod +x gradlew && ./gradlew assembleRelease || ./gradlew assembleDebug)
if [ -f "apps/android/app/build/outputs/apk/release/app-release.apk" ]; then
    cp apps/android/app/build/outputs/apk/release/app-release.apk ./1-AetherLink-Android.apk
elif [ -f "apps/android/app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp apps/android/app/build/outputs/apk/debug/app-debug.apk ./1-AetherLink-Android.apk
fi

# 2. Build Desktop Binary & Linux Debian Package
echo "--> Building Desktop Release Binary..."
(cd apps/desktop && npm run build)
cargo build --release --bin desktop

echo "--> Packaging Linux Debian (.deb) package..."
(cd apps/desktop && npx tauri build --bundles deb --ignore-version-mismatches || true)
DEB_FILE=$(find target/ -type f -name "*.deb" | head -n 1)
if [ -n "$DEB_FILE" ]; then
    cp "$DEB_FILE" ./3-AetherLink-Linux.deb
fi

# 3. Push Git Tag to Trigger GitHub Actions Release
echo "--> Pushing Git tag $VERSION..."
git tag -f "$VERSION"
git push -f origin "$VERSION"

# 4. Direct Upload to GitHub Releases
python3 - <<EOF
import urllib.request, json, os

token = ''
with open(os.path.expanduser('~/.git-credentials')) as f:
    line = f.read().strip()
    if '@' in line:
        token = line.split('@')[0].split(':', 2)[-1]

headers = {
    'Accept': 'application/vnd.github+json',
    'User-Agent': 'AetherLink-Deployer',
    'Authorization': f'Bearer {token}'
}

tag = "$VERSION"
url = f'https://api.github.com/repos/Rev0nix/AetherLink/releases/tags/{tag}'

try:
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        rel = json.loads(resp.read().decode())
        rel_id = rel['id']
except urllib.error.HTTPError:
    create_url = 'https://api.github.com/repos/Rev0nix/AetherLink/releases'
    payload = json.dumps({
        'tag_name': tag,
        'name': f'AetherLink {tag}',
        'body': '### AetherLink Official Release\n\n1. Android: `1-AetherLink-Android.apk`\n2. Windows: `2-AetherLink-Windows-Setup.exe`\n3. Linux: `3-AetherLink-Linux.deb`',
        'draft': False,
        'prerelease': False
    }).encode('utf-8')
    req = urllib.request.Request(create_url, data=payload, headers=headers, method='POST')
    with urllib.request.urlopen(req) as resp:
        rel = json.loads(resp.read().decode())
        rel_id = rel['id']

files = [
    ('1-AetherLink-Android.apk', 'application/vnd.android.package-archive'),
    ('2-AetherLink-Windows-Setup.exe', 'application/vnd.microsoft.portable-executable'),
    ('3-AetherLink-Linux.deb', 'application/vnd.debian.binary-package')
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
