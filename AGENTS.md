# Compose Multiplatform Wizard Plugin

IntelliJ Platform plugin providing a project creation wizard for Compose Multiplatform.
Works in IntelliJ IDEA (2025.3+) and Android Studio (Panda 2+).

## Commands

- `./gradlew compileKotlin` — compile (default target: local AS or IDEA)
- `./gradlew compileKotlin -PrunIntellijIdea=true` — compile against IntelliJ IDEA
- `./gradlew test` — run all tests
- `./gradlew test --tests "*.ClassName"` — run a specific test class
- `./gradlew runIde` — launch Android Studio with the plugin
- `./gradlew runIde -PrunIntellijIdea=true` — launch IntelliJ IDEA with the plugin
- `./gradlew buildPlugin` — build single distributable ZIP (works in both IDEs)

## Build Verification

Both IDE targets must compile before release:

```
./gradlew compileKotlin
./gradlew compileKotlin -PrunIntellijIdea=true
```

## Coding Standards

- All files strictly under 300 lines (excluding imports).
- `WizardDefaults` is the single source of truth for all default values — never duplicate them.
- Platform-specific behavior uses `PlatformDetector.isAndroidStudio` / `isIntellijIdea`.
- UI spacing is tighter for IDEA (fits without scrolling) vs AS (more padding).
- Compose and Jewel dependencies are provided by the platform (`compileOnly`) — never bundle them.

## State Flow

`WizardState` (Compose mutableStateOf) -> `ComposeWizardStep.syncStateFromUI()` -> `updateDataModel()` -> `ComposeMultiplatformModuleBuilder` -> `ProjectCreator` -> `ProjectComposer`

## Adding a New Wizard Option

1. Add default to `WizardDefaults`
2. Add state to `WizardState`
3. Add field to `ComposeMultiplatformModuleBuilder`
4. Add field to `ProjectConfig` + update `ProjectConfig.from()`
5. Sync in `ComposeWizardStep.syncStateFromUI()` + `updateDataModel()`
6. Add UI in `WizardPlatformsAndOptions.kt` or `LibrariesSection.kt`
7. Pass initial value through `WizardUIRoot`
8. If it generates files: create a `ProjectFeature` and wire in `ProjectCreator`

## Adding a New Platform Module

1. Implement `PlatformModule` in `composer/modules/`
2. Add templates in `src/main/resources/templates/modular/modules/<platform>/`
3. Add conditional `composer.addModule()` in `ProjectCreator`
4. Add build file fragments via module's methods

## Pitfalls

- Never call blocking operations on EDT — use `executeOnPooledThread` for long work (see `ASWizardIntegration`).
- Don't use `GitRepositoryInitializer` API — it pollutes `.gitignore` with unwanted exclude paths.
- `setupRootModel()` temporarily disables `initGit` because Git is initialized later by platform-specific code.
- Generated projects use version catalog (`libs.versions.toml`), not hardcoded dependency strings.

## Release Notes

- `CHANGELOG.md` must contain the full release history, including the current version.
- Marketplace `What's new` in `build.gradle.kts` must contain only the current version.
