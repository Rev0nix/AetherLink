# Implementation Plan - Fix build.gradle.kts Warnings and Errors

This plan addresses the errors and warnings in `app/build.gradle.kts` and `libs.versions.toml`.

## User Review Required

> [!IMPORTANT]
> I am proposing to rename plugin keys in `libs.versions.toml` from `kotlin-android` style to `kotlinAndroid` style. This avoids potential collisions with the `kotlin` keyword/extension in Gradle Kotlin DSL, which seems to be causing the "Unresolved reference" and "Type mismatch" errors.

> [!IMPORTANT]
> I am downgrading `compileSdk` and `targetSdk` from 36 (Baklava preview) to 35 (Android 15 stable) to address the "Not targeting the latest versions" warning and simplify the SDK configuration.

## Proposed Changes

### [gradle/libs.versions.toml](file:///home/revanth/AetherLink/apps/android/gradle/libs.versions.toml)

- Rename `[plugins]` keys to use camelCase (e.g., `kotlinAndroid` instead of `kotlin-android`).
- Add `kotlinx-serialization-json` version and library.
- Update `kotlinx-serialization-json` to the latest stable version `1.11.0`.

### [app/build.gradle.kts](file:///home/revanth/AetherLink/apps/android/app/build.gradle.kts)

- Update `plugins` block to use the renamed version catalog accessors (e.g., `alias(libs.plugins.kotlinAndroid)`).
- Change `compileSdk` to 35 and `targetSdk` to 35.
- Update `dependencies` block:
    - Replace hardcoded `kotlinx-serialization-json` string with `libs.kotlinx.serialization.json`.
    - Remove redundant `platform(libs.androidx.compose.bom)` from `androidTestImplementation` as it is already included via `implementation`.

### [build.gradle.kts](file:///home/revanth/AetherLink/apps/android/build.gradle.kts) (Root)

- Update `plugins` block to use the renamed version catalog accessors.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the project builds without errors.
- Run `./gradlew :app:help` to verify the Gradle configuration is valid.

### Manual Verification
- Verify that the IDE no longer shows red squiggles or warnings in `app/build.gradle.kts`.
