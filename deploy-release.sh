#!/usr/bin/env bash
set -e

VERSION="${1:-v1.0.0}"
echo "============================================="
echo " Deploying AetherLink Release: $VERSION"
echo "============================================="

# 1. Build Android APK if not already built
if [ ! -f "apps/android/app/build/outputs/apk/debug/app-debug.apk" ] && [ ! -f "AetherLink.apk" ]; then
    echo "--> Building Android APK..."
    (cd apps/android && chmod +x gradlew && ./gradlew assembleDebug)
    cp apps/android/app/build/outputs/apk/debug/app-debug.apk ./AetherLink.apk
fi

# 2. Build Desktop Binary if not already built
if [ ! -f "target/release/desktop" ]; then
    echo "--> Building Desktop Release Binary..."
    (cd apps/desktop && npm run build)
    cargo build --release --bin desktop
fi

# 3. Create Desktop Tarball
echo "--> Packaging Desktop tarball..."
rm -rf /tmp/aetherlink-desktop
mkdir -p /tmp/aetherlink-desktop
cp target/release/desktop /tmp/aetherlink-desktop/aetherlink
cp launch-desktop.sh /tmp/aetherlink-desktop/
cp aetherlink.desktop /tmp/aetherlink-desktop/
cp apps/desktop/src-tauri/icons/icon.png /tmp/aetherlink-desktop/
chmod +x /tmp/aetherlink-desktop/aetherlink /tmp/aetherlink-desktop/launch-desktop.sh
tar -czvf AetherLink-linux-x86_64.tar.gz -C /tmp aetherlink-desktop

# 4. Push Git Tag to Trigger GitHub Actions Release
echo "--> Pushing Git tag $VERSION..."
git tag -f "$VERSION"
git push -f origin "$VERSION"

# 5. Direct Upload to GitHub Releases
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
        'body': '### AetherLink Official Release\n\n- Android Companion APK\n- Linux Desktop Standalone Bundle',
        'draft': False,
        'prerelease': False
    }).encode('utf-8')
    req = urllib.request.Request(create_url, data=payload, headers=headers, method='POST')
    with urllib.request.urlopen(req) as resp:
        rel = json.loads(resp.read().decode())
        rel_id = rel['id']

files = [
    ('AetherLink.apk', 'application/vnd.android.package-archive'),
    ('AetherLink-linux-x86_64.tar.gz', 'application/gzip')
]

for name, mime in files:
    if not os.path.exists(name): continue
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
