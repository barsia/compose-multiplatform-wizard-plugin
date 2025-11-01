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
  - `ui/` - **Shared Compose UI** - Single wizard UI built with Compose Desktop that works in both IDEA and AS
  - Template processing and validation
  - Services and utilities
- `idea/` - IntelliJ IDEA-specific wizard integration (New Project Wizard)
- `android/` - Android Studio-specific wizard integration (Native AS wizard + actions)

### Key Innovation: Shared Compose UI

Both IntelliJ IDEA and Android Studio use the **same Compose UI components** for the wizard:
- Built with Compose Desktop
- Integrated via Swing's `ComposePanel`
- Consistent look and feel across platforms
- Single source of truth for wizard UI logic

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

## License

Apache License 2.0

