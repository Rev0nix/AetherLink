# Fix Gradle Sync Error: "Cannot add extension with name 'kotlin'"

The user is experiencing a Gradle sync error where the `kotlin` extension is being registered multiple times. This typically occurs when multiple Kotlin plugins are applied to the same project or when there is a version mismatch/classloader conflict.

## Proposed Changes

### 1. Update Versions in Version Catalog
Update `libs.versions.toml` to use the versions mentioned in the error message and project context. This ensures consistency.
- Update `kotlin` to `2.2.10`
- Update `agp` to `9.3.0`

### 2. Reorder Plugins in App Module
In `app/build.gradle.kts`, reorder the plugins to apply the Kotlin Android plugin before the Android Application plugin. This can sometimes resolve extension registration conflicts by ensuring the Kotlin extension is created first and other plugins can then contribute to it.

#### [MODIFY] [libs.versions.toml](file:///home/revanth/AetherLink/apps/android/gradle/libs.versions.toml)
#### [MODIFY] [app/build.gradle.kts](file:///home/revanth/AetherLink/apps/android/app/build.gradle.kts)

## Verification Plan

### Automated Verification
- Run `./gradlew help` to verify that the configuration phase succeeds.
- Run `./gradlew :app:assembleDebug` to verify the build.

### Manual Verification
- Sync the project in Android Studio.
