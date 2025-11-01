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

- `shared/` - Common template processing and validation logic
- `idea/` - IntelliJ IDEA-specific wizard integration
- `android/` - Android Studio-specific wizard integration

The plugin automatically detects the host IDE and activates the appropriate integration.

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

