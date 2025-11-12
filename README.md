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

