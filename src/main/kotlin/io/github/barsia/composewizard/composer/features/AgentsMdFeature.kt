package io.github.barsia.composewizard.composer.features

import io.github.barsia.composewizard.composer.PlatformModule
import io.github.barsia.composewizard.composer.ProjectFeature
import io.github.barsia.composewizard.generator.ProjectConfig
import java.io.File
import java.nio.file.Files

class AgentsMdFeature : ProjectFeature {

    override val featureId: String = "agentsMd"
    override val displayName: String = "AGENTS.md"
    override val defaultEnabled: Boolean = false

    override fun apply(targetPath: String, config: ProjectConfig, modules: List<PlatformModule>) {
        val content = generateContent(config)
        File(targetPath, "AGENTS.md").writeText(content)

        try {
            val symlinkPath = File(targetPath, "CLAUDE.md").toPath()
            Files.createSymbolicLink(symlinkPath, java.nio.file.Paths.get("AGENTS.md"))
        } catch (_: Exception) {
            // Symlink creation can fail on Windows without Developer Mode / admin privileges.
            // In that case we skip CLAUDE.md rather than creating a detached copy.
        }
    }

    private fun generateContent(config: ProjectConfig): String {
        return buildString {
            appendLine("# ${config.projectName}")
            appendLine()
            appendLine("Kotlin Multiplatform project with Compose Multiplatform UI.")
            appendLine()

            // Commands
            appendLine("## Commands")
            appendLine()
            appendLine("- `./gradlew :composeApp:build` — build all targets")
            if (config.targetAndroid) {
                appendLine("- `./gradlew :composeApp:assembleDebug` — build Android APK")
            }
            if (config.targetDesktop) {
                appendLine("- `./gradlew :composeApp:run` — run Desktop app")
            }
            if (config.targetWeb) {
                appendLine("- `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` — run Web app in browser")
            }
            if (config.includeTests) {
                appendLine("- `./gradlew :composeApp:allTests` — run tests on all platforms")
                if (config.targetDesktop) {
                    appendLine("- `./gradlew :composeApp:desktopTest` — run Desktop tests only (fastest)")
                }
            }
            appendLine()

            // Coding standards
            appendLine("## Coding Standards")
            appendLine()
            appendLine("- Shared code goes in `commonMain`. Platform-specific code uses `expect`/`actual` declarations.")
            appendLine("- Add dependencies via `gradle/libs.versions.toml`, then reference in `composeApp/build.gradle.kts` sourceSets block.")
            appendLine("- Use Compose Multiplatform resources (`composeApp/src/commonMain/composeResources/`), not platform-specific resource systems.")
            if (config.includeTests) {
                appendLine("- Write tests in `commonTest` using `kotlin.test`. Platform-specific test code goes in the corresponding test source set.")
            }
            appendLine()

            // Pitfalls
            appendLine("## Pitfalls")
            appendLine()
            appendLine("- Don't add platform-specific dependencies (Android, JVM, etc.) to `commonMain` — they won't compile on other targets.")
            if (config.targetIOS) {
                appendLine("- Don't edit files inside `iosApp/*.xcodeproj` manually — use Xcode to modify project settings.")
            }
            if (config.targetWeb) {
                appendLine("- Not all Kotlin libraries support Wasm — verify compatibility before adding to `wasmJsMain`.")
            }
            if (config.targetIOS && config.targetAndroid) {
                appendLine("- `actual` implementations must exist for every target that has a corresponding `expect` declaration — missing one breaks compilation on that target.")
            }
        }
    }
}
