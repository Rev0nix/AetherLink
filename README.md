AetherLink

AetherLink is a modern, cross-platform ecosystem bridge designed to seamlessly connect your Android devices with your desktop environment. Built with performance and modularity in mind, it leverages Rust, Tauri, React, and Kotlin (Jetpack Compose) to deliver real-time synchronization, file transfers, screen mirroring, and AI-powered assistance across devices.

🏗️ Architecture & Monorepo Overview

AetherLink is structured as a multi-language monorepo containing native mobile/desktop applications and a modular Rust workspace:

AetherLink/
├── .github/workflows/      # CI/CD pipelines (ci.yml, release.yml)
├── apps/
│   ├── android/            # Native Android application (Kotlin + Jetpack Compose)
│   └── desktop/            # Cross-platform Desktop app (Tauri + React + TypeScript)
├── crates/                 # Shared Rust workspace libraries & core engine
│   ├── ai/                 # AI assistant integration & processing
│   ├── common/             # Shared models, constants, connection & device types
│   ├── core/               # Application state, event bus, registry & core services
│   ├── crypto/             # Encryption & secure pairing utilities
│   ├── database/           # Local persistence & storage management
│   ├── device/             # Device discovery, ADB bridge, pairing & lifecycle
│   ├── network/            # Networking primitives & transport layers
│   ├── plugin/             # Extensibility & plugin architecture
│   ├── protocol/           # Wire protocol definitions & serialization
│   ├── streaming/          # Screen mirroring & media streaming engine
│   └── sync/               # Clipboard & notification synchronization logic
├── aetherlink.desktop      # Linux desktop entry file
├── Cargo.toml              # Root Rust workspace configuration
├── deploy-release.sh       # Automated release deployment script
└── launch-desktop.sh       # Helper script to launch the desktop client


✨ Key Features

Based on the application modules across Android and Desktop, AetherLink provides:

🔍 Automatic Device Discovery & Pairing:

Local network discovery using NSD (Network Service Discovery / mDNS) and TCP sockets.

Quick QR Code scanning (QrCodeScannerHelper) and secure handshake pairing (PairingModal, PairingScreen).

Built-in ADB (Android Debug Bridge) integration (crates/device/src/adb.rs) for wired/wireless fallback and device management.

📋 Universal Clipboard Sync:

Two-way real-time clipboard synchronization across devices.

Android Quick Settings Tile (ClipboardTileService), text selection context action (ProcessTextActivity), and floating sync bubble (FloatingSyncBubbleService).

📁 Cross-Device File Explorer & Transfer:

Browse Android storage directly from your desktop (FileExplorer.tsx) with breadcrumbs, context menus, and fast TCP file transfers.

🔔 Notification Mirroring:

Receive and manage Android notifications right on your desktop (Notifications.tsx, NotificationSyncScreen.kt).

🖥️ Screen Mirroring:

Low-latency device screen streaming (ScreenMirroringScreen.kt, crates/streaming).

🤖 Built-in AI Assistant:

Integrated AI assistant screen (AiAssistantScreen.kt, crates/ai) for smart cross-device workflows.

🛠️ Tech Stack

Desktop Client (apps/desktop)

Framework: Tauri (Rust backend + Webview frontend)

Frontend: React, TypeScript, Vite, Tailwind CSS

Networking: Custom Rust TCP Server (tcp_server.rs) & mDNS/NSD discovery

Android Client (apps/android)

Language: Kotlin

UI Toolkit: Jetpack Compose (Material 3 / Custom Aether Design System)

Architecture: Clean Architecture (domain, feature, service, protocol, viewmodel)

Background Services: Foreground AetherLinkService, DiscoveryService, FloatingSyncBubbleService, and ClipboardTileService

Core Backend (crates/)

Language: Rust (Cargo Workspace)

Modules: Modular crate architecture separating protocol, cryptography, ADB handling, device registry, and streaming.

🚀 Getting Started

Prerequisites

Rust & Cargo: Latest stable toolchain (rustup)

Node.js & npm: Node.js v18+ (for the Desktop frontend)

Android Studio / JDK 17+: For building and running the Android APK

Tauri System Dependencies: Ensure platform-specific WebView/build libraries are installed (see Tauri Prerequisites).

1. Running the Desktop Application

Navigate to the desktop app directory, install dependencies, and start the Tauri development environment:

cd apps/desktop
npm install
npm run tauri dev


(Alternatively, on Linux/macOS, you can inspect and run the root helper script ./launch-desktop.sh.)

2. Building the Android Application

Open apps/android in Android Studio, or build via Gradle from the terminal:

cd apps/android
./gradlew assembleDebug


Install the generated APK onto your Android device:

adb install app/build/outputs/apk/debug/app-debug.apk


3. Building Rust Core Crates

To compile and verify all underlying Rust workspace crates from the root directory:

cargo build --workspace


🔍 Architectural Observations & Notes

During analysis of the repository structure, a few notable details stand out for contributors:

Modular Stubs Ready for Expansion: Several crates in crates/ (such as ai, crypto, database, network, plugin, streaming, and sync) are currently structured as clean scaffolding (lib.rs stubs), while heavy lifting for TCP communication, pairing, and ADB is actively implemented in apps/desktop/src-tauri/src/ and crates/device/.

Rich Android System Integration: The Android client goes beyond a standard app by integrating OS-level entry points (PROCESS_TEXT menu, Quick Settings Tile, and overlay FloatingSyncBubbleService).

CI/CD Ready: Pre-configured GitHub Actions workflows (.github/workflows/ci.yml and release.yml) and deploy-release.sh are included for automated builds and packaging.

License: The root LICENSE file is currently empty (0 bytes)—be sure to populate it with your chosen open-source or proprietary license before public distribution.

📄 License

Distributed under the terms specified in the LICENSE file in the root directory.
