# Compose Multiplatform Wizard Plugin

A unified IntelliJ Platform plugin that provides project creation wizard for Compose Multiplatform projects.

## Features

- **Universal Support**: Works in both IntelliJ IDEA and Android Studio
- **Multi-platform Templates**: Create projects for Desktop, Android, iOS, and Web
- **Smart Configuration**: Platform-specific integration that adapts to the IDE
- **Shared Core**: Common template processing logic across both IDEs

## Compatibility

- IntelliJ IDEA 2024.2+
- Android Studio 2024.2+

## Architecture

This plugin uses a unified codebase with platform-specific integrations:

- `shared/` - Common logic shared across both IDEs:
  - `ui/` - **Shared Compose UI** - Single wizard UI built with Jetpack Compose that works in both IDEA and AS
    - **Main Wizard** (164 lines):
      - `ComposeWizardStep.kt` - Main wizard step implementation using `ModuleWizardStep`
      - `WizardMainContent.kt` - Main UI layout and composition (orchestration layer)
    - **Project Fields** (135 lines):
      - `WizardProjectFields.kt` - Platform-specific field layouts (AS/IDEA)
      - `WizardInputFields.kt` - Input components (ProjectNameField, PackageNameField, ProjectLocationField)
    - **Version Selection** (255 lines):
      - `WizardVersionField.kt` - Compose version selection with Dev/Stable toggle
    - **Libraries** (state + UI components):
      - `LibrariesState.kt` - State manager for library versions and loading (73 lines)
      - `LibraryVersionDropdown.kt` - Dropdown component for library version selection (190 lines)
      - `LibrariesSection.kt` - Full libraries section UI (278 lines)
    - **Platforms & Options** (213 lines):
      - `WizardPlatformsAndOptions.kt` - Platform selection and project options (Git, Tests)
    - **UI Components**:
      - `WizardUIComponents.kt` - Reusable UI components (CompactSwitch, ProjectPathHint, PlatformCheckbox)
      - `WizardIcons.kt` - Icon definitions (175 lines)
      - `WizardValidationComponents.kt` - Validation popups (196 lines)
      - `WizardLibraryComponents.kt` - Library-specific UI (SkeletonText, indicators, copy icon) (179 lines)
      - `WizardFooter.kt` - Footer with version info and error display (79 lines)
    - **Utilities**:
      - `WizardVersionUtils.kt` - Version comparison utilities
      - `WizardLayoutUtils.kt` - Layout constants and modifiers
      - `WizardStateManager.kt` - State management and validation logic
  - `models/` - Data models and builders (`ComposeMultiplatformModuleBuilder`)
  - `services/` - Version caching and other services
  - Template processing and validation
- `idea/` - IntelliJ IDEA-specific wizard integration (New Project Wizard)
- `androidstudio/` - Android Studio-specific wizard integration (Native AS wizard + actions)

### Key Innovation: Shared Compose UI

Both IntelliJ IDEA and Android Studio use the **same Compose UI components** for the wizard:
- Built with Jetpack Compose Desktop and Jewel UI components
- Integrated via Swing's `ComposePanel` in `ComposeWizardStep`
- Consistent look and feel across platforms using Jewel theme bridge
- Single source of truth for wizard UI logic
- Reactive state management with Compose runtime

### Architecture Highlights

**State Management Pattern:**
- `LibrariesState` - Dedicated state manager for library version loading and caching
- Separation of concerns: UI components (presentation) vs. state logic (business logic)
- Reactive updates via Compose state and coroutines

**Component Hierarchy:**
```
WizardMainContent (orchestration)
├── ProjectFields (platform-specific layouts)
│   ├── AndroidStudioProjectFields
│   └── IntellijIdeaProjectFields
├── PlatformsSection (platform toggles)
├── LibrariesSection (library configuration)
│   └── LibraryVersionDropdown (per-library UI)
├── OptionsSection (Git, Tests)
└── WizardFooter (version info, errors)
```

**File Size Discipline:**
- All files strictly under 300 lines (excluding imports)
- Clear responsibility separation
- Easy navigation and maintenance

## Library Version Resolution Logic

### Overview

The wizard automatically resolves compatible library versions for each selected Compose Multiplatform version. The resolution logic varies by library type and Compose version.

### Standard Libraries (Lifecycle, Material3, Navigation, etc.)

**Resolution Strategy (in order of priority):**

1. **Web UI Scraping** - Primary source
   - Fetch from `https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html`
   - Parses official JetBrains compatibility table
   - Most accurate for official Compose releases

2. **Bundled Versions** - Secondary fallback
   - If Web UI fetch succeeds but specific library is missing → use `LIBRARY_BUNDLES` (hardcoded)
   - Pre-validated version bundles embedded in plugin code

3. **Semantic Fallback** - Tertiary fallback
   - Try previous Compose versions (e.g., `1.10.0-beta01` → `1.10.0-alpha03` → `1.9.3`)
   - Check Web UI for each fallback version
   - Use first successful match

4. **Direct Maven Fallback** - Final fallback
   - Query Maven Central directly
   - Parse `maven-metadata.xml` for available versions
   - Use first available version

**Version Filtering for Dropdowns:**

For each library, the dropdown shows:
- **Current version** (first in list)
- **Bundled version** (if different from current)
- **Up to 3 newer published versions** from Maven Central (filtered by semantic version > current)
- Total: max 5 versions

### Hot Reload - Special Logic

Hot Reload has **different behavior** based on Compose version:

#### For Compose < 1.10.0-beta01 (Optional Library)

**Version Resolution:**
- Fixed version: `1.0.0-rc02` (hardcoded)
- No GitHub fetch
- No bundled indicator

**Checkbox:**
- `enabled = true` (can be disabled)

**Dropdown Filtering:**
- Shows only versions `<= 1.0.0-rc02` from Maven
- Filters using semantic version comparison

**Icon:**
- No lock icon (optional library)

#### For Compose >= 1.10.0-beta01 (Bundled Library)

**Version Resolution (in order):**

1. **GitHub Fetch** - Primary source
   ```
   https://raw.githubusercontent.com/JetBrains/compose-multiplatform/
   v${composeVersion}/gradle-plugins/gradle/libs.versions.toml
   ```
   - Parses `plugin-hot-reload = { prefer = "X.Y.Z" }`
   - This is the "aligned" version bundled with Compose
   - Cached separately in `hotReloadGithubVersions`

2. **Maven Fallback** - If GitHub 404
   - Query Maven Central for Hot Reload plugin
   - Use first available version (latest)

3. **Hardcoded Fallback** - If Maven fails
   - Use `1.0.0-rc02` as last resort

**Checkbox:**
- `enabled = false` (bundled, cannot be disabled)
- Tooltip: "Included in the base template and cannot be disabled"

**Dropdown Filtering:**
- Shows ALL versions from Maven (no upper limit)
- Can manually select any version

**Lock Icon Logic:**
```kotlin
showLock = (selectedVersion == githubVersion)
```

- 🔒 **Lock shown** when selected version matches GitHub version
- **Lock hidden** when user selects different version from dropdown
- 🔒 **Lock reappears** when user returns to GitHub version

### Caching Strategy

**Cache Layers:**

1. **In-Memory Cache** (`ComposeVersionCacheState`)
   - Compose versions (stable/dev)
   - Library versions per Compose version
   - Available Maven versions per library
   - Hot Reload GitHub versions
   - TTL: 1 hour for remote data

2. **Per-Version Cache** (LinkedHashMap)
   - Key: Compose version string
   - Value: Resolved library version
   - Max size: 200 entries (LRU eviction)

3. **Fallback Indicators** (`isFromFallback`)
   - Tracks if version came from fallback strategy
   - Shows pin icon (📌) in UI for non-primary sources

**Cache Invalidation:**
- Manual: "Refresh versions" button
- Automatic: After 1 hour
- Scope: All libraries + Compose versions

### Version Comparison

**Semantic Version Parsing:**
```
1.10.0-beta01+dev3245
│  │  │   │      │
│  │  │   │      └─ devNum (build number)
│  │  │   └──────── suffix + suffixNum (beta01)
│  │  └──────────── patch (0)
│  └─────────────── minor (10)
└────────────────── major (1)
```

**Comparison Order:**
1. Major → Minor → Patch (numeric)
2. Suffix (lexicographic): `stable` (zzz) > `rc` > `dev` > `beta` > `alpha`
3. Suffix number (numeric): `beta02` > `beta01`
4. Dev number (numeric): `+dev3245` > `+dev3194`

**Example Ordering (newest → oldest):**
```
1.10.0          (stable, suffix="zzz")
1.10.0-rc02
1.10.0-rc01
1.10.0-beta02+dev3245
1.10.0-beta02+dev3194
1.10.0-beta02
1.10.0-beta01
1.10.0-alpha03
```

### Error Handling

**Network Errors:**
- Web UI unreachable → fallback to bundled versions
- Maven unreachable → fallback to hardcoded versions
- GitHub unreachable (Hot Reload) → fallback to Maven

**Rate Limiting:**
- Detected via HTTP 429 or specific error patterns
- Skip remaining fallback attempts for same library
- Show cached version or empty state

**Invalid Versions:**
- Parse errors → version treated as "0.0.0-{original}"
- Comparison still works, but version ranks lowest
- User sees original string in UI

### UI Indicators

**Icons:**
- 🔒 **Lock** (`BundledLibraryIndicator`) - Hot Reload aligned with Compose (GitHub version)
- 📌 **Pin** (`PinnedVersionIndicator`) - Version from fallback strategy (non-primary source)

**States:**
- **Loading** - Skeleton shimmer animation
- **Loaded** - Normal dropdown with version
- **Error** - Empty or fallback version, no visual error indicator

## Development

### Requirements

- JDK 21
- Gradle 8.5+

### Building

```bash
./gradlew buildPlugin
```

### Testing

**Option 1: Using helper scripts (recommended):**
```bash
# Test in Android Studio (default)
./run-android-studio.sh

# Test in IntelliJ IDEA  
./run-idea.sh
```

**Option 2: Using Gradle directly:**
```bash
# Test in Android Studio (default - compiles against AS)
./gradlew runIde

# Test in IntelliJ IDEA
./gradlew runIde -PrunIntellijIdea=true
```

**Important**: The plugin **compiles against Android Studio** by default to have access to AS wizard API. This allows the wizard to appear in the native "Phone and Tablet" section of AS New Project wizard.

The plugin will automatically:
- Download the appropriate IDE (AS 2025.2.1.7 or IDEA 2025.2.4)
- Install the plugin
- Launch the IDE with the plugin enabled
- Show which platform is running in the console

#### Dual Compilation Verification

**During development, you MUST verify compilation for both platforms:**

```bash
# 1. Verify Android Studio compilation (includes androidstudio/ code)
./gradlew compileKotlin

# 2. Verify IntelliJ IDEA compilation (excludes androidstudio/ code)
./gradlew compileKotlin -PrunIntellijIdea=true
```

**Why?** The `androidstudio/` package uses Android Studio-specific APIs (`com.android.tools.*`) that are not available in IntelliJ IDEA. The build system conditionally excludes this code when compiling for IDEA. Both compilations must succeed to ensure the plugin works correctly in both IDEs.

## Publishing to Marketplace

### Build for Android Studio Target

**Always build the Marketplace distribution using the Android Studio target:**

```bash
./gradlew buildPlugin
```

This creates a plugin JAR that includes the `androidstudio/` code. The resulting plugin will:

✅ **Work in both IntelliJ IDEA and Android Studio** from a single distribution  
✅ **Provide full functionality in Android Studio** (native wizard + actions)  
✅ **Provide IDEA wizard in IntelliJ IDEA** (AS-specific code won't load due to optional dependencies)  
✅ **Appear as ONE plugin in JetBrains Marketplace**

### How It Works

The plugin uses **optional dependencies** in `plugin.xml`:

```xml
<depends optional="true" config-file="plugin-android.xml">org.jetbrains.android</depends>
```

- **In Android Studio**: Both `plugin.xml` and `plugin-android.xml` load → full functionality
- **In IntelliJ IDEA**: Only `plugin.xml` loads → IDEA wizard works, AS code stays dormant

This architecture ensures **one plugin distribution works universally** across both IDEs without runtime errors.

## License

Apache License 2.0

