# Changelog

All notable changes to this project will be documented in this file.

## 0.1.2

- Compatibility: IntelliJ Platform 253.30387.90+ (IDEA 2025.3+, Android Studio Panda 2+).
- Added "Add AGENTS.md / CLAUDE.md blueprint" option for AI coding assistants.
- Fixed library version resolution for dev Compose versions (e.g., Material3 Adaptive returned 1.2.0 instead of 1.3.0-alpha).
- Fixed Refresh button not invalidating library version cache.
- Updated dev Maven repository URL.
- Fixed Dev Compose versions loading after JetBrains moved the dev Maven repository.
- Hardened remote XML parsing against XXE in version resolution code paths.
- Removed plugin analytics and consent flow.

## 0.1.1

- Added the required Gradle plugin dependency declaration for IntelliJ IDEA compatibility.
- Updated Marketplace metadata, links, and public repository references.
- Relicensed the project under Apache License 2.0.

## 0.1.0

- Initial release of Compose Multiplatform Wizard.
- Added project generation for Compose Multiplatform targets.
- Added version resolution for Compose and related libraries.
