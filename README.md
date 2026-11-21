<div align="center">

# ⚡ AetherLink

**High-Performance, Local-First Cross-Device Continuity Ecosystem for Android & Desktop**

[![Rust](https://img.shields.io/badge/Rust-Workspace-orange?logo=rust)](https://www.rust-lang.org/)
[![Tauri](https://img.shields.io/badge/Desktop-Tauri%20%2B%20React-24C8DB?logo=tauri)](https://tauri.app/)
[![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-3DDC84?logo=android)](https://developer.android.com/jetpack/compose)
[![Windows](https://img.shields.io/badge/Windows-Supported-0078D6?logo=windows)](https://github.com/Rev0nix/AetherLink/releases)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions)](.github/workflows/ci.yml)
[![Release](https://img.shields.io/badge/Release-v2.2.0-success?logo=github)](https://github.com/Rev0nix/AetherLink/releases)

*Unify your Android smartphone and Desktop workstation (Windows, Linux, macOS) with zero-config mDNS/NSD discovery, QR handshake pairing, real-time clipboard & notification sync, remote file exploration, screen mirroring, ADB bridging, and built-in AI workflows.*

</div>

---

## 📖 Overview

**AetherLink** is a modern, modular ecosystem bridge built to eliminate friction between your mobile device and desktop computer. Designed with a **local-first, privacy-centric architecture**, AetherLink establishes direct peer-to-peer communication over your local network using custom TCP sockets and multicast DNS (`mDNS` / Android `NSD`), while providing an automated **ADB (Android Debug Bridge)** fallback layer when Wi-Fi client isolation is active.

The platform is engineered as a clean monorepo uniting two native clients around a shared multi-crate Rust core:
* **Desktop Client (`apps/desktop`):** Built with **Rust + Tauri v2 + React + TypeScript + Vite**, delivering ultra-low memory overhead, native OS integration, and a multi-client asynchronous TCP server.
* **Android Client (`apps/android`):** Built with **Kotlin + Jetpack Compose + Material 3**, featuring Clean Architecture (`domain`, `feature`, `service`, `protocol`), foreground background services, Quick Settings tiles, and system overlay widgets.
* **Rust Workspace (`crates/`):** 11 modular domain crates powering protocol framing, device lifecycle management, ADB bridging, pairing cryptography, and extensible plugins.

---

## 🆕 Recent Updates & Cross-Platform Tooling

Based on the latest repository updates:
* **Cross-Platform Quick-Launch Scripts:** Added native desktop launcher scripts for **Windows Batch** (`launch-desktop.bat`), **PowerShell** (`launch-desktop.ps1`), and **Unix/Linux/macOS** (`launch-desktop.sh`).
* **Expanded CI/CD & Release Automation:** Enhanced GitHub Actions pipelines (`.github/workflows/ci.yml` and `.github/workflows/release.yml`) alongside `deploy-release.sh` for streamlined multi-platform builds and artifact packaging.
* **Refined Tauri IPC & TCP Server:** Expanded desktop command handlers (`features.rs`) and socket management (`tcp_server.rs`) for smoother device command execution and file operations.

---

## ✨ Core Features & Deep-Dive Capabilities

### 🔍 1. Zero-Config Discovery & Cryptographic Handshake Pairing
* **Automatic Subnet Discovery:**
  * **Android:** `NsdDiscoveryManage.kt` and `DiscoveryService.kt` broadcast and resolve `_aetherlink._tcp` services via Android's Network Service Discovery API.
  * **Desktop:** `apps/desktop/src-tauri/src/network/discovery.rs` advertises the workstation and listens for mobile peers on the local network.
* **Instant QR Code & Pin Pairing:**
  * **Mobile Scanner:** `QrCodeScannerHelper.kt` and `PairingScreen.kt` parse connection metadata directly from the desktop QR modal (`PairingModal.tsx`).
  * **Structured Pairing Payloads:** Handshakes are validated through `PairPayloads.kt` (Android) and `crates/device/src/pairing.rs` / `commands/pairing.rs` (Rust).
* **ADB Fallback Bridge:**
  * `crates/device/src/adb.rs` interfaces directly with `adb` to detect USB-connected or wireless-debugging Android devices, forward TCP ports, and maintain connectivity even on restrictive corporate or public networks.

### 📋 2. Universal Clipboard Synchronization
* **Bidirectional Real-Time Sync:** Any text copied on Desktop (`Clipboard.tsx`) or Android (`ClipboardScreen.kt`) is immediately framed and transmitted across the active TCP session.
* **Deep Android OS Integrations:**
  * **Quick Settings Tile (`ClipboardTileService.kt`):** Push clipboard contents or toggle sync directly from the Android notification shade without opening the app.
  * **Text Selection Context Menu (`ProcessTextActivity.kt`):** Registers Android's `ACTION_PROCESS_TEXT` intent so you can highlight text in any browser or app and tap *"Send to AetherLink"*.
  * **Background Sync Activity (`ClipboardSyncActivity.kt`):** Handles silent background clipboard capture and dispatch.
  * **Floating Sync Bubble (`FloatingSyncBubbleService.kt`):** An optional overlay widget providing one-tap cross-device clipboard and file actions while multitasking.

### 📁 3. Remote File Explorer & High-Speed TCP Transfer
* **Desktop File Manager (`FileExplorer.tsx`):** Browse your Android device's internal storage from your desktop with full directory traversal, breadcrumb navigation (`Breadcrumb.tsx`), action toolbars (`FileToolbar.tsx`), list views (`FileList.tsx`, `FileItem.tsx`), and right-click context menus (`FileContextMenu.tsx`).
* **Dedicated Transfer Screen (`FileTransferScreen.kt`):** Monitor incoming/outgoing transfers and pick files directly from Android.
* **Chunked Socket Streaming:** Managed by `ConnectionManager.kt` on Android and `tcp_server.rs` / `crates/device/src/file_service.rs` on Rust for reliable binary transfer.

### 🔔 4. Real-Time Notification Mirroring
* **Unified Notification Center:** Android notifications are captured by `NotificationSyncScreen.kt` / `AetherLinkService.kt` and streamed to the desktop `Notifications.tsx` view so you can monitor alerts, messages, and app updates from your workstation.

### 🖥️ 5. Low-Latency Screen Mirroring
* **Live Device Display Stream:** `ScreenMirroringScreen.kt` pairs with `crates/streaming` to cast your Android screen to your desktop window for presentations, testing, or remote control.

### 🤖 6. Integrated AI Assistant
* **Context-Aware Cross-Device AI:** `AiAssistantScreen.kt` connects with `crates/ai` to summarize synced clipboard text, draft replies to mirrored notifications, or automate device actions.

---

## 🏗️ Complete Monorepo Architecture

```text
AetherLink-main/
├── .github/
│   └── workflows/
│       ├── ci.yml                      # Automated Rust, Tauri, and Android CI checks
│       └── release.yml                 # Multi-platform release build & packaging workflow
├── apps/
│   ├── android/                        # Native Android Client (Kotlin + Jetpack Compose)
│   │   ├── build.gradle.kts            # Root Gradle configuration (Kotlin DSL)
│   │   ├── gradle/libs.versions.toml   # Centralized version catalog
│   │   └── app/src/main/
│   │       ├── AndroidManifest.xml     # Foreground services, NSD, overlay & intent filters
│   │       └── java/com/aetherlink/android/
│   │           ├── AetherLinkApp.kt    # Application entry & DI initialization
│   │           ├── MainActivity.kt     # Single-activity Compose host
│   │           ├── core/
│   │           │   ├── components/     # AetherBadge, AetherButton, AetherCard, AetherHeader
│   │           │   ├── constants/      # AppConstants, NetworkConstants
│   │           │   ├── designsystem/   # Custom Color, Theme, and Typography tokens
│   │           │   └── utils/          # QrCodeScannerHelper
│   │           ├── domain/
│   │           │   ├── model/          # Device, DeviceState
│   │           │   ├── repository/     # DiscoveryRepository & DiscoveryRepositoryImpl
│   │           │   └── usecase/        # DiscoverDevicesUseCase
│   │           ├── feature/
│   │           │   ├── assistant/      # AiAssistantScreen
│   │           │   ├── clipboard/      # ClipboardScreen, ClipboardSyncActivity, ProcessTextActivity
│   │           │   ├── devices/        # DevicesScreen
│   │           │   ├── files/          # FileTransferScreen
│   │           │   ├── home/           # HomeScreen dashboard
│   │           │   ├── mirroring/      # ScreenMirroringScreen
│   │           │   ├── notifications/  # NotificationSyncScreen
│   │           │   ├── onboarding/     # Permission onboarding flow
│   │           │   ├── pairing/        # QR & manual PairingScreen
│   │           │   ├── settings/       # SettingsScreen
│   │           │   └── splash/         # SplashScreen
│   │           ├── protocol/           # Wire schemas: DeviceInfo, Message, MessageType, PairPayloads
│   │           ├── service/
│   │           │   ├── AetherLinkService.kt         # Persistent foreground connection service
│   │           │   ├── ClipboardTileService.kt      # Android Quick Settings Tile
│   │           │   ├── DiscoveryService.kt          # Background NSD service
│   │           │   ├── FloatingSyncBubbleService.kt # System overlay bubble
│   │           │   ├── discovery/                   # NsdDiscoveryManage
│   │           │   └── network/                     # ConnectionManager, TcpClient
│   │           ├── ui/
│   │           │   ├── components/     # StatusCard
│   │           │   ├── navigation/     # AppNavigation, BottomBar, Screen routes
│   │           │   └── theme/          # Material 3 theme wrappers
│   │           └── viewmodel/          # HomeViewModel state holder
│   └── desktop/                        # Cross-Platform Desktop Client (Tauri + React)
│       ├── package.json                # Frontend dependencies & scripts
│       ├── vite.config.ts              # Vite bundler config
│       ├── src/                        # React + TypeScript UI
│       │   ├── components/             # DeviceCard, PairingModal, Sidebar, TopBar, device/*, file/*
│       │   ├── layouts/                # MainLayout shell
│       │   ├── pages/                  # Dashboard, FileExplorer, Clipboard, Notifications, Devices, DeviceDetails, Settings
│       │   ├── router/                 # Client-side routing
│       │   ├── services/tauri.ts       # Typed Tauri IPC invoke wrappers
│       │   ├── store/appStore.ts       # Global reactive application state
│       │   └── types/                  # TypeScript interfaces (device.ts, file.ts)
│       └── src-tauri/                  # Tauri Rust Native Backend
│           ├── Cargo.toml              # Tauri backend dependencies
│           ├── tauri.conf.json         # Window, bundle & capability configuration
│           ├── capabilities/           # Security capability permissions (default.json)
│           └── src/
│               ├── main.rs & lib.rs    # Tauri app builder & plugin registration
│               ├── commands/           # IPC handlers: device.rs, features.rs, pairing.rs
│               └── network/            # discovery.rs & multi-client tcp_server.rs
├── crates/                             # Modular Rust Workspace Crates
│   ├── ai/                             # AI processing & prompt orchestration
│   ├── common/                         # Shared domain models (device, connection, battery, constants, error)
│   ├── core/                           # App lifecycle, config, event bus, device/plugin registry, state
│   ├── crypto/                         # Encryption, key exchange & verification primitives
│   ├── database/                       # Local persistence & history storage
│   ├── device/                         # DeviceManager, ADB bridge, pairing logic, heartbeat & file_service
│   ├── network/                        # Low-level transport & socket traits
│   ├── plugin/                         # Extensible plugin architecture
│   ├── protocol/                       # Cross-platform binary/JSON message framing
│   ├── streaming/                      # Screen & media frame streaming
│   └── sync/                           # State & clipboard synchronization engine
├── aetherlink.desktop                  # Linux XDG Desktop Entry
├── Cargo.toml                          # Root Cargo Workspace definition
├── deploy-release.sh                   # Automated multi-target release build script
├── launch-desktop.bat                  # Windows Command Prompt quick-launcher
├── launch-desktop.ps1                  # Windows PowerShell quick-launcher
└── launch-desktop.sh                   # Linux/macOS Bash quick-launcher
```

---

## 🛠️ Technology Stack

| Subsystem | Technologies & Frameworks |
| :--- | :--- |
| **Desktop Shell & Backend** | Rust (2021 Edition), Tauri, Tokio Async Runtime, Custom TCP Server (`tcp_server.rs`) |
| **Desktop Frontend** | React, TypeScript, Vite, Tailwind CSS, Custom Store (`appStore.ts`), Typed IPC (`tauri.ts`) |
| **Android Application** | Kotlin, Jetpack Compose, Material 3, Custom `Aether` Design System, Android NSD, Foreground Services |
| **Core Engine & Protocol** | Cargo Workspace (`11` modular crates), Custom Wire Protocol (`crates/protocol`), ADB CLI Bridge (`adb.rs`) |
| **CI/CD & Distribution** | GitHub Actions (`ci.yml`, `release.yml`), Gradle Kotlin DSL (`libs.versions.toml`), Cross-platform launch scripts |

---

## 🔌 Communication & Protocol Lifecycle

AetherLink operates over a deterministic 4-stage connection pipeline:

1. **Peer Discovery (`mDNS` / `NSD`):**
   * Desktop (`network/discovery.rs`) and Android (`NsdDiscoveryManage.kt`) advertise their device metadata (`DeviceInfo`) on the local subnet.
2. **Session Establishment (`TCP`):**
   * Desktop runs a persistent async TCP listener (`network/tcp_server.rs`).
   * Android initiates connections via `TcpClient.kt` and manages reconnection/session state inside `ConnectionManager.kt` (~28 KB core networking engine).
3. **Pairing & Verification:**
   * Users scan the QR code rendered by `PairingModal.tsx` using `QrCodeScannerHelper.kt`, exchanging `PairPayloads` validated by `crates/device/src/pairing.rs`.
4. **Multiplexed Feature Framing:**
   * All clipboard updates (`MessageType`), file directory queries, binary file transfers, and notification payloads are serialized via `crates/protocol/src/lib.rs` and dispatched to Tauri IPC commands (`commands/features.rs`).
   * **ADB Fallback:** If local Wi-Fi blocks peer-to-peer traffic, `crates/device/src/adb.rs` detects USB/wireless ADB devices and tunnels traffic seamlessly.

---

## 🚀 Getting Started

### Prerequisites

* **Rust Toolchain:** Latest stable `rustc` & `cargo` via [rustup.rs](https://rustup.rs/)
* **Node.js & npm:** Node.js `v18+` and `npm`
* **Android Studio / SDK:** JDK `17+` and Android SDK (API 34+)
* **ADB (Recommended):** Android Platform Tools installed and available in your system `PATH`
* **Linux System Libraries (Linux only):**
  ```bash
  sudo apt update && sudo apt install -y libwebkit2gtk-4.1-dev build-essential \
    curl wget file libxdo-dev libssl-dev libayatana-appindicator3-dev librsvg2-dev
  ```

---

### 1. Clone & Verify the Rust Workspace

```bash
git clone https://github.com/your-username/AetherLink.git
cd AetherLink

# Build all 11 workspace crates
cargo build --workspace
```

---

### 2. Launch the Desktop Client

#### Option A: Running from Release Bundles

* **Windows:**
  If you downloaded `AetherLink-windows-x64.zip`:
  1. Extract the zip folder.
  2. Double-click `launch-desktop.bat` (or right-click `launch-desktop.ps1` -> Run with PowerShell).
     > **💡 Windows Smart App Control / SmartScreen Note:** Windows 11 Smart App Control blocks unsigned `.exe` files downloaded from web browsers via the NTFS "Mark of the Web" (`Zone.Identifier`). `launch-desktop.bat` automatically unblocks extracted files and launches `AetherLink.exe` safely.
     >
     > Alternatively, right-click `AetherLink.exe` (or the `.zip` archive before extraction) -> **Properties** -> check **Unblock** at the bottom -> click **Apply**, or run PowerShell:
     > ```powershell
     > Unblock-File -Path .\AetherLink.exe
     > ```
* **Linux:**
  If you downloaded `AetherLink-linux-x86_64.tar.gz`:
  1. Extract the tarball:
     ```bash
     tar -xzvf AetherLink-linux-x86_64.tar.gz
     cd aetherlink-desktop
     ```
  2. Launch directly:
     ```bash
     ./launch-desktop.sh
     ```
  3. (Optional) Install the desktop shortcut into your application menu:
     ```bash
     ./install-desktop.sh
     ```

#### Option B: Running from Source / Development Mode

You can use the included cross-platform quick-launch scripts from the repository root:

* **Windows (Command Prompt):**
  ```cmd
  launch-desktop.bat
  ```
* **Windows (PowerShell):**
  ```powershell
  .\launch-desktop.ps1
  ```
* **Linux / macOS:**
  ```bash
  chmod +x launch-desktop.sh
  ./launch-desktop.sh
  ```

Or run manually via `npm`:
```bash
cd apps/desktop
npm install
npm run tauri dev
```

---

### 3. Build & Install the Android App

```bash
cd apps/android

# Build the debug APK
./gradlew assembleDebug

# Install onto a connected Android device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### 4. Linux Desktop Launcher Integration

To register AetherLink in your Linux desktop environment's application menu:
```bash
cp aetherlink.desktop ~/.local/share/applications/
# Edit ~/.local/share/applications/aetherlink.desktop to point Exec= and Icon= to your binary location
```

---

## 🧩 Developer & Architecture Notes

* **Crate Modularity:** Core desktop networking (`tcp_server.rs`, `features.rs`), ADB device management (`crates/device/src/adb.rs`), pairing (`pairing.rs`), and protocol framing (`crates/protocol`) are actively implemented, while auxiliary crates (`ai`, `crypto`, `database`, `network`, `plugin`, `streaming`, `sync`) provide clean workspace boundaries for expanding features without coupling to UI layers.
* **Required Android Permissions:** On first launch, use the built-in `OnboardingScreen` to grant:
  * **Notification Access** (for Notification Mirroring)
  * **Display Over Other Apps** (for `FloatingSyncBubbleService`)
  * **Nearby Devices / Local Network & Camera** (for NSD Discovery and QR Pairing)
* **Building Production Releases:** Use `./deploy-release.sh` locally or push a version tag to trigger `.github/workflows/release.yml` for automated multi-OS packaging.

---

## 🤝 Contributing

1. **Fork** the repository and create a feature branch (`git checkout -b feature/your-feature`).
2. **Verify Rust & Desktop checks:** Run `cargo test --workspace` and `npm run build` inside `apps/desktop`.
3. **Verify Android checks:** Run `./gradlew test` inside `apps/android`.
4. **Commit & Push** your changes and open a Pull Request.

---

## 📄 License

This project is governed by the terms in the root [`LICENSE`](LICENSE) file. *(Reminder: Add your chosen open-source license text, such as MIT or Apache-2.0, to `LICENSE` before publishing.)*