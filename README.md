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

```bash
# Test in IntelliJ IDEA
./gradlew runIde

# Test in Android Studio
./gradlew runIde -Pplatform=androidStudio
```

## License

Apache License 2.0

