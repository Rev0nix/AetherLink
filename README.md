<div align="center">

# ⚡ AetherLink

**Seamless, High-Performance Cross-Device Continuity Bridge for Android & Desktop**

[![Rust](https://img.shields.io/badge/Rust-Workspace-orange?logo=rust)](https://www.rust-lang.org/)
[![Tauri](https://img.shields.io/badge/Desktop-Tauri%20%2B%20React-24C8DB?logo=tauri)](https://tauri.app/)
[![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-3DDC84?logo=android)](https://developer.android.com/jetpack/compose)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions)](.github/workflows/ci.yml)

*Sync clipboards, explore remote files, mirror notifications and screens, transfer data over local TCP/ADB, and leverage built-in AI workflows across your devices.*

</div>

---

## 📖 Overview

**AetherLink** is a modern, modular ecosystem bridge that unifies your **Android smartphone** and **Desktop workstation** (Linux, Windows, macOS). Designed for low latency, native system integration, and extensibility, AetherLink combines a **Rust + Tauri + React** desktop client with a **Kotlin + Jetpack Compose** Android application, backed by a multi-crate Rust core engine.

Unlike cloud-dependent sync tools, AetherLink prioritizes direct local-network communication (via mDNS/NSD discovery and custom TCP sockets) alongside an **ADB (Android Debug Bridge)** fallback layer for reliable wired or wireless device management.

---

## ✨ Core Features & Capabilities

### 🔍 1. Smart Device Discovery & Secure Pairing
* **Zero-Config Local Discovery:** Uses Android Network Service Discovery (`NsdDiscoveryManage.kt`) and Rust network discovery (`discovery.rs`) to automatically detect AetherLink instances on the same subnet.
* **QR Code & Handshake Pairing:** Instant pairing via camera QR scanning (`QrCodeScannerHelper.kt`, `PairingScreen.kt`) and desktop pairing modals (`PairingModal.tsx`) backed by dedicated pairing payloads (`PairPayloads.kt`, `pairing.rs`).
* **ADB Integration:** Built-in ADB controller (`crates/device/src/adb.rs`) capable of enumerating, bridging, and managing connected Android devices directly from the Rust backend.

### 📋 2. Universal Clipboard Synchronization
* **Bidirectional Real-Time Sync:** Automatically synchronizes copied text and links between Android and Desktop (`Clipboard.tsx`, `ClipboardScreen.kt`).
* **Deep Android OS Hooks:**
  * **Quick Settings Tile (`ClipboardTileService.kt`):** Push or toggle clipboard sync directly from the Android notification shade.
  * **Text Selection Context Menu (`ProcessTextActivity.kt`):** Highlight text in any Android app and tap *"Send to AetherLink"* via Android's `PROCESS_TEXT` intent.
  * **Floating Sync Bubble (`FloatingSyncBubbleService.kt`):** System overlay widget for one-tap cross-device actions while multitasking.

### 📁 3. Remote File Explorer & High-Speed Transfer
* **Full Directory Browsing:** Navigate your Android device's file system from your desktop (`FileExplorer.tsx`) with interactive breadcrumbs (`Breadcrumb.tsx`), toolbars (`FileToolbar.tsx`), and context menus (`FileContextMenu.tsx`).
* **Direct TCP Streaming:** Fast file uploads, downloads, and management handled over local TCP sockets (`ConnectionManager.kt`, `tcp_server.rs`, `file_service.rs`).

### 🔔 4. Real-Time Notification Mirroring
* **Unified Notification Feed:** Forward Android notifications to your desktop workspace (`Notifications.tsx`, `NotificationSyncScreen.kt`) so you never have to pick up your phone while working.

### 🖥️ 5. Low-Latency Screen Mirroring
* **Live Display Streaming:** Stream and view your Android device's screen directly on your desktop (`ScreenMirroringScreen.kt`, `crates/streaming`).

### 🤖 6. Integrated AI Assistant
* **Cross-Device AI Workflows:** Dedicated AI assistant interface (`AiAssistantScreen.kt`) powered by the modular `crates/ai` Rust backend to summarize, process, or act on synced clipboard items and notifications.

---

## 🏗️ Monorepo Architecture

AetherLink is organized as a unified monorepo separating native client applications from reusable Rust core libraries:

```text
AetherLink-main/
├── .github/
│   └── workflows/
│       ├── ci.yml                  # Automated build & test checks
│       └── release.yml             # Multi-platform release packaging pipeline
├── apps/
│   ├── android/                    # Native Android Application (Kotlin + Jetpack Compose)
│   │   └── app/src/main/java/com/aetherlink/android/
│   │       ├── core/               # Custom Aether design system, UI components, constants, QR helper
│   │       ├── domain/             # Clean Architecture models, repositories & use cases
│   │       ├── feature/            # Feature modules (assistant, clipboard, devices, files, home, mirroring, notifications, onboarding, pairing, settings, splash)
│   │       ├── protocol/           # Wire message definitions (DeviceInfo, Message, MessageType, PairPayloads)
│   │       ├── service/            # Foreground services, NSD discovery, TCP client & ConnectionManager
│   │       ├── ui/                 # Navigation graph, BottomBar & app theming
│   │       └── viewmodel/          # State management (HomeViewModel)
│   └── desktop/                    # Cross-Platform Desktop Client (Tauri + React + TypeScript)
│       ├── src/                    # React UI (Dashboard, FileExplorer, Clipboard, Notifications, Devices, Settings)
│       └── src-tauri/              # Tauri Rust Backend
│           └── src/
│               ├── commands/       # Tauri IPC commands (device.rs, features.rs, pairing.rs)
│               └── network/        # Local mDNS discovery & multi-client TCP server (tcp_server.rs)
├── crates/                         # Modular Rust Workspace Libraries
│   ├── ai/                         # AI processing & assistant engine
│   ├── common/                     # Shared types (device, connection, battery, constants, errors)
│   ├── core/                       # App lifecycle, state management, event bus, plugin/device registry
│   ├── crypto/                     # Cryptographic primitives & secure channel helpers
│   ├── database/                   # Local storage & persistence layer
│   ├── device/                     # Device manager, ADB bridge, pairing logic, heartbeat & file services
│   ├── network/                    # Low-level transport & socket abstractions
│   ├── plugin/                     # Extensible plugin system interface
│   ├── protocol/                   # Shared serialization & protocol framing
│   ├── streaming/                  # Screen & media streaming pipeline
│   └── sync/                       # State & clipboard synchronization engine
├── aetherlink.desktop              # Linux XDG Desktop Entry specification
├── Cargo.toml                      # Root Cargo Workspace manifest
├── deploy-release.sh               # Automated build & release deployment script
└── launch-desktop.sh               # Quick-launch helper script for the desktop app
```

---

## 🛠️ Technology Stack

| Layer | Technologies & Libraries |
| :--- | :--- |
| **Desktop Backend** | Rust, Tauri, Tokio (Async Runtime), Custom TCP Server (`tcp_server.rs`) |
| **Desktop Frontend** | React, TypeScript, Vite, Tailwind CSS, Custom Store (`appStore.ts`) |
| **Android Client** | Kotlin, Jetpack Compose, Material 3, Custom `Aether` Design System, Android NSD, Foreground Services |
| **Core Engine** | Rust Cargo Workspace (`11` modular crates), ADB Bridge (`crates/device/src/adb.rs`) |
| **DevOps & Packaging** | GitHub Actions (`ci.yml`, `release.yml`), Gradle (Kotlin DSL), Cargo, Shell Scripts |

---

## 🔌 Network & Protocol Design

AetherLink establishes communication between devices through a multi-stage lifecycle:

1. **Discovery Phase:**
   * Android broadcasts/scans via `NsdDiscoveryManage.kt` and `DiscoveryService.kt`.
   * Desktop listens and advertises via `apps/desktop/src-tauri/src/network/discovery.rs`.
2. **Transport Layer:**
   * Desktop spins up a persistent TCP server (`apps/desktop/src-tauri/src/network/tcp_server.rs`).
   * Android connects as a client via `TcpClient.kt` and manages active sessions in `ConnectionManager.kt`.
3. **Wire Protocol:**
   * Both clients share structured message schemas (`crates/protocol/src/lib.rs` on Rust; `DeviceInfo.kt`, `Message.kt`, `MessageType.kt`, and `PairPayloads.kt` on Android) to route clipboard updates, file transfer chunks, pairing handshakes, and device telemetry.
4. **ADB Fallback:**
   * When local Wi-Fi isolation prevents direct TCP discovery, `crates/device/src/adb.rs` allows device detection and communication over USB or wireless ADB.

---

## 🚀 Getting Started

### Prerequisites

Ensure your development environment has the following installed:
* **Rust Toolchain:** `rustc` and `cargo` (latest stable via [rustup.rs](https://rustup.rs/))
* **Node.js & npm:** Node.js `v18+` and `npm`
* **Android SDK & JDK:** JDK `17+` and Android Studio (with Android SDK API 34+)
* **ADB (Optional):** Android Platform Tools in your system `PATH` for ADB device bridging
* **Linux Tauri Dependencies (Linux only):** `libwebkit2gtk-4.1-dev`, `build-essential`, `curl`, `wget`, `file`, `libxdo-dev`, `libssl-dev`, `libayatana-appindicator3-dev`, `librsvg2-dev`

---

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/AetherLink.git
cd AetherLink
```

### 2. Build the Rust Core Workspace

Verify that all shared crates compile cleanly:

```bash
cargo build --workspace
```

### 3. Run the Desktop Client (Development Mode)

Install frontend dependencies and launch the Tauri desktop window:

```bash
cd apps/desktop
npm install
npm run tauri dev
```

> **Tip:** On Unix-like systems, you can also inspect and run the root convenience script:
> ```bash
> chmod +x launch-desktop.sh
> ./launch-desktop.sh
> ```

### 4. Build & Install the Android App

Build the debug APK using the included Gradle wrapper:

```bash
cd apps/android
./gradlew assembleDebug
```

Deploy the APK to a connected Android device via ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 5. Linux Desktop Integration

To install AetherLink into your Linux application launcher, copy `aetherlink.desktop` to your local applications directory (updating the `Exec` and `Icon` paths inside the file as needed):

```bash
cp aetherlink.desktop ~/.local/share/applications/
```

---

## 🧩 Development & Architecture Notes

* **Modular Crate Scaffolding:** While core networking, pairing, ADB management, and desktop IPC commands are actively implemented inside `apps/desktop/src-tauri/src/` and `crates/device/`, several domain crates (`ai`, `crypto`, `database`, `network`, `plugin`, `streaming`, `sync`) are structured as clean workspace crates ready for expanded feature implementation.
* **Android Permissions:** Because AetherLink supports background sync, local network discovery, overlay bubbles (`FloatingSyncBubbleService`), and notification mirroring, ensure you grant **Notification Access**, **Display Over Other Apps**, and **Nearby Devices / Local Network** permissions on first launch via the `OnboardingScreen`.
* **Release Automation:** Production builds across platforms can be triggered via GitHub Actions (`.github/workflows/release.yml`) or locally via `./deploy-release.sh`.

---

## 🤝 Contributing

Contributions are welcome! To contribute to AetherLink:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Ensure both Rust (`cargo test --workspace`) and Android (`./gradlew test`) checks pass.
4. Commit your changes (`git commit -m 'Add amazing feature'`).
5. Push to the branch (`git push origin feature/amazing-feature`) and open a Pull Request.

---

## 📄 License

This project's licensing terms are defined in the [`LICENSE`](LICENSE) file in the root directory. *(Note: Populate `LICENSE` with your preferred license—such as MIT or Apache-2.0—prior to public distribution.)*