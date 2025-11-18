package io.github.heisiar.composewizard.composer

import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ModularResourceCopier
import io.github.heisiar.composewizard.shared.services.VersionComparison
import java.io.File

class BuildFileComposer(
    private val config: ProjectConfig,
    private val modules: List<PlatformModule>,
    private val features: List<ProjectFeature>,
    private val resourceCopier: ModularResourceCopier
) {
    
    fun composeRootBuildFile(targetPath: String) {
        val template = resourceCopier.readResourceFile("templates/modular/base/build.gradle.kts.template")
        
        val rootPlugins = mutableListOf<String>()
        
        // Add module-specific root plugins first
        modules.forEach { module ->
            rootPlugins.addAll(module.getRootPlugins(config))
        }
        
        // Add feature-specific root plugins before base plugins
        features.forEach { feature ->
            rootPlugins.addAll(feature.getRootPlugins(config))
        }
        
        // Add base plugins last
        rootPlugins.add("alias(libs.plugins.composeMultiplatform)")
        rootPlugins.add("alias(libs.plugins.composeCompiler)")
        rootPlugins.add("alias(libs.plugins.kotlinMultiplatform)")
        
        val pluginsContent = rootPlugins.distinct().joinToString("\n") { plugin ->
            if (plugin.contains("apply false")) {
                "    $plugin"
            } else {
                "    $plugin apply false"
            }
        }
        
        val content = template.replace("{{PLUGINS}}", pluginsContent)
        
        File(targetPath, "build.gradle.kts").writeText(content)
    }
    
    fun composeAppBuildFile(targetPath: String, isMultiplatform: Boolean) {
        val imports = collectImports()
        val plugins = collectPlugins()
        val targets = collectTargets()
        val sourceSets = collectSourceSets(isMultiplatform)
        val configs = collectConfigs()
        val dependencies = collectDependencies()
        
        val content = buildString {
            if (imports.isNotEmpty()) {
                append(imports.joinToString("\n"))
                appendLine()
                appendLine()
            }
            
            appendLine("plugins {")
            plugins.forEach { appendLine("    $it") }
            appendLine("}")
            appendLine()
            
            appendLine("kotlin {")
            targets.forEach { append(it) }
            appendLine("    sourceSets {")
            sourceSets.forEach { append(it) }
            appendLine("    }")
            appendLine("}")
            
            // Android config goes first
            val androidConfig = configs.find { it.trimStart().startsWith("android {") }
            if (androidConfig != null) {
                appendLine()
                appendLine(androidConfig)
            }
            
            // Then dependencies
            if (dependencies.isNotEmpty()) {
                appendLine()
                val firstDep = dependencies.first().trim()
                if (firstDep.startsWith("dependencies {")) {
                    dependencies.forEach { appendLine(it) }
                } else {
                    appendLine("dependencies {")
                    dependencies.forEach { appendLine("    $it") }
                    appendLine("}")
                }
            }
            
            // Then other configs (desktop, etc.)
            val otherConfigs = configs.filter { !it.trimStart().startsWith("android {") }
            if (otherConfigs.isNotEmpty()) {
                appendLine()
                otherConfigs.forEach { 
                    appendLine(it)
                }
            }
        }
        
        File(targetPath, "composeApp/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(content.trimEnd() + "\n")
        }
    }
    
    fun composeSettingsFile(targetPath: String) {
        val template = resourceCopier.readResourceFile("templates/modular/base/settings.gradle.kts")
        
        val repositoriesFragment = if (config.enableDevVersions) {
            resourceCopier.readResourceFile("templates/modular/base/settings-repositories-dev.fragment")
        } else {
            resourceCopier.readResourceFile("templates/modular/base/settings-repositories-release.fragment")
        }
        
        val content = template.replace("{{REPOSITORIES}}", repositoriesFragment)
        
        File(targetPath, "settings.gradle.kts").writeText(content)
    }
    
    fun composeLibsVersions(targetPath: String) {
        val template = resourceCopier.readResourceFile("templates/modular/base/gradle/libs.versions.toml.template")
        
        var content = template
        content = content.replace("{{COMPOSE_VERSION}}", config.composeVersion)
        content = content.replace("{{KOTLIN_VERSION}}", config.kotlinVersion)
        content = content.replace("{{LIFECYCLE_VERSION}}", config.lifecycleVersion ?: "2.9.5")
        
        // Android blocks
        content = if (config.targetAndroid) {
            val androidVersions = """agp = "8.11.2"
android-compileSdk = "36"
android-minSdk = "24"
android-targetSdk = "36"
androidx-activity = "1.11.0"
androidx-appcompat = "1.7.1"
androidx-core = "1.17.0""""
            val androidLibraries = """androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidx-core" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidx-appcompat" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }"""
            val androidPlugins = """androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }"""
            
            content.replace("{{ANDROID_VERSIONS_BLOCK}}", "\n$androidVersions")
                .replace("{{ANDROID_LIBRARIES_BLOCK}}", "\n$androidLibraries")
                .replace("{{ANDROID_PLUGINS_BLOCK}}", "\n$androidPlugins")
        } else {
            content.replace("{{ANDROID_VERSIONS_BLOCK}}", "")
                .replace("{{ANDROID_LIBRARIES_BLOCK}}", "")
                .replace("{{ANDROID_PLUGINS_BLOCK}}", "")
        }
        
        // Desktop blocks
        content = if (config.targetDesktop) {
            val desktopVersions = "\nkotlinx-coroutines = \"1.10.2\""
            val desktopLibraries = "\nkotlinx-coroutinesSwing = { module = \"org.jetbrains.kotlinx:kotlinx-coroutines-swing\", version.ref = \"kotlinx-coroutines\" }"
            
            content.replace("{{DESKTOP_VERSIONS_BLOCK}}", desktopVersions)
                .replace("{{DESKTOP_LIBRARIES_BLOCK}}", desktopLibraries)
        } else {
            content.replace("{{DESKTOP_VERSIONS_BLOCK}}", "")
                .replace("{{DESKTOP_LIBRARIES_BLOCK}}", "")
        }
        
        // Test blocks
        val isMultiplatform = listOf(config.targetAndroid, config.targetDesktop, config.targetIOS, config.targetWeb).count { it } > 1
        content = if (config.includeTests) {
            // For single-platform Android projects, include espresso/testExt
            // For multiplatform projects, exclude them (use only Kotlin common tests)
            val testVersions = if (config.targetAndroid && !isMultiplatform) {
                "\nandroidx-espresso = \"3.7.0\"\nandroidx-testExt = \"1.3.0\""
            } else {
                ""
            }
            val testJunitVersion = "\njunit = \"4.13.2\""
            val testLibraries = if (config.targetAndroid && !isMultiplatform) {
                """
androidx-testExt-junit = { module = "androidx.test.ext:junit", version.ref = "androidx-testExt" }
androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "androidx-espresso" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlin-testJunit = { module = "org.jetbrains.kotlin:kotlin-test-junit", version.ref = "kotlin" }
junit = { module = "junit:junit", version.ref = "junit" }"""
            } else {
                """
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlin-testJunit = { module = "org.jetbrains.kotlin:kotlin-test-junit", version.ref = "kotlin" }
junit = { module = "junit:junit", version.ref = "junit" }"""
            }
            
            content.replace("{{TEST_VERSIONS_BLOCK}}", testVersions)
                .replace("{{TEST_JUNIT_VERSION_BLOCK}}", testJunitVersion)
                .replace("{{TEST_LIBRARIES_BLOCK}}", "$testLibraries")
        } else {
            content.replace("{{TEST_VERSIONS_BLOCK}}", "")
                .replace("{{TEST_JUNIT_VERSION_BLOCK}}", "")
                .replace("{{TEST_LIBRARIES_BLOCK}}", "")
        }
        
        content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_VERSION_BLOCK", 
                                                     config.includeMaterial3, config.material3Version, "compose-material3")
        content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_ADAPTIVE_VERSION_BLOCK",
                                                     config.includeMaterial3Adaptive, config.material3AdaptiveVersion, "compose-material3-adaptive")
        content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_VERSION_BLOCK",
                                                     config.includeNavigation, config.navigationVersion, "androidx-navigation")
        content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION3_VERSION_BLOCK",
                                                     config.includeNavigation3, config.navigation3Version, "compose-navigation3-ui")
        content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_EVENT_VERSION_BLOCK",
                                                     config.includeNavigationEvent, config.navigationEventVersion, "compose-navigationevent")
        content = replaceOptionalLibraryVersionBlock(content, "SAVED_STATE_VERSION_BLOCK",
                                                     config.includeSavedState, config.savedStateVersion, "androidx-savedstate")
        content = replaceOptionalLibraryVersionBlock(content, "WINDOW_VERSION_BLOCK",
                                                     config.includeWindow, config.windowVersion, "androidx-window-core")
        
        content = replaceOptionalLibraryLibrariesBlock(content, "MATERIAL3_LIBRARIES_BLOCK",
                                                       config.includeMaterial3, "androidx-material3")
        content = replaceOptionalLibraryLibrariesBlock(content, "MATERIAL3_ADAPTIVE_LIBRARIES_BLOCK",
                                                       config.includeMaterial3Adaptive, "androidx-material3-adaptive")
        content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION_LIBRARIES_BLOCK",
                                                       config.includeNavigation, "androidx-navigation")
        content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION3_LIBRARIES_BLOCK",
                                                       config.includeNavigation3, "androidx-navigation3")
        content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION_EVENT_LIBRARIES_BLOCK",
                                                       config.includeNavigationEvent, "androidx-navigation-event")
        content = replaceOptionalLibraryLibrariesBlock(content, "SAVED_STATE_LIBRARIES_BLOCK",
                                                       config.includeSavedState, "androidx-savedstate")
        content = replaceOptionalLibraryLibrariesBlock(content, "WINDOW_LIBRARIES_BLOCK",
                                                       config.includeWindow, "androidx-window")
        
        // Hot Reload blocks
        // Logic:
        // 1. For Compose < 1.10.0-beta01: Optional, user controls via checkbox
        // 2. For Compose >= 1.10.0-beta01: Bundled, add ONLY if user overrides default version
        val shouldIncludeHotReload = when {
            // Compose < 1.10.0-beta01: Optional library
            VersionComparison.isComposeVersionLessThan(config.composeVersion, "1.10.0-beta01") -> 
                config.includeHotReload && config.hotReloadVersion != null
            
            // Compose >= 1.10.0-beta01: Bundled, add only if user overrides
            else -> 
                config.hotReloadVersion != null && 
                config.hotReloadVersion != config.bundledHotReloadVersion
        }
        
        content = if (shouldIncludeHotReload) {
            content.replace("{{HOT_RELOAD_VERSION_BLOCK}}", "\ncomposeHotReload = \"${config.hotReloadVersion}\"")
                .replace("{{HOT_RELOAD_PLUGIN_BLOCK}}", "\ncomposeHotReload = { id = \"org.jetbrains.compose.hot-reload\", version.ref = \"composeHotReload\" }")
        } else {
            content.replace("{{HOT_RELOAD_VERSION_BLOCK}}", "")
                .replace("{{HOT_RELOAD_PLUGIN_BLOCK}}", "")
        }
        
        // Clean up multiple empty lines
        content = content.replace(Regex("\n{3,}"), "\n\n")
        
        File(targetPath, "gradle/libs.versions.toml").apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
    
    private fun collectImports(): List<String> {
        val imports = mutableListOf<String>()
        
        val order = listOf("desktop", "web", "android", "ios")
        order.forEach { platformId ->
            modules.find { it.platformId == platformId }?.let { module ->
                imports.addAll(module.getImports(config))
            }
        }
        
        return imports
    }
    
    private fun collectPlugins(): List<String> {
        val plugins = mutableListOf<String>()
        
        modules.forEach { module ->
            plugins.addAll(module.getPlugins(config))
        }
        
        features.forEach { feature ->
            plugins.addAll(feature.getPlugins(config))
        }
        
        return plugins.distinct()
    }
    
    private fun collectTargets(): List<String> {
        return modules.map { it.getTargetFragment(config) }
    }
    
    private fun collectSourceSets(isMultiplatform: Boolean): List<String> {
        val sourceSets = mutableListOf<String>()
        
        if (isMultiplatform) {
            val androidModule = modules.find { it.platformId == "android" }
            if (androidModule != null) {
                sourceSets.add(androidModule.getSourceSetFragment(config))
            }
            
            sourceSets.add(getCommonMainSourceSet())
            
            // Add feature-specific source sets (like commonTest) right after commonMain
            features.forEach { feature ->
                sourceSets.addAll(feature.getSourceSetFragments(config))
            }
            
            modules.filter { it.platformId != "android" }.forEach { module ->
                sourceSets.add(module.getSourceSetFragment(config))
            }
        } else {
            val module = modules.first()
            when (module.platformId) {
                "android" -> {
                    sourceSets.add(module.getSourceSetFragment(config))
                    sourceSets.add(getCommonMainSourceSet())
                }
                "desktop" -> {
                    sourceSets.add(getCommonMainSourceSet())
                    sourceSets.add(module.getSourceSetFragment(config))
                }
                else -> {
                    sourceSets.add(getCommonMainSourceSet())
                }
            }
            
            // Add feature-specific source sets (like commonTest) for single platform too
            features.forEach { feature ->
                sourceSets.addAll(feature.getSourceSetFragments(config))
            }
        }
        
        return sourceSets
    }
    
    private fun getCommonMainSourceSet(): String {
        val fragment = resourceCopier.readResourceFile("templates/modular/base/commonMain.sourceset.fragment")
        val material3Dependency = if (config.includeMaterial3 && config.material3Version != null) {
            ""
        } else {
            "\n            implementation(compose.material3)"
        }
        val optionalLibrariesDependencies = generateOptionalLibrariesDependencies()
        val result = fragment
            .replace("{{MATERIAL3_DEPENDENCY}}", material3Dependency)
            .replace("{{OPTIONAL_LIBRARIES_DEPENDENCIES}}", optionalLibrariesDependencies)
        return result
    }
    
    private fun generateOptionalLibrariesDependencies(): String {
        val dependencies = buildList {
            if (config.includeMaterial3 && config.material3Version != null) {
                add("            implementation(libs.compose.material3)")
            }
            if (config.includeMaterial3Adaptive && config.material3AdaptiveVersion != null) {
                add("            implementation(libs.compose.material3.adaptive)")
                add("            implementation(libs.compose.material3.adaptive.layout)")
                add("            implementation(libs.compose.material3.adaptive.navigation)")
            }
            if (config.includeNavigation && config.navigationVersion != null) {
                add("            implementation(libs.androidx.navigation)")
            }
            if (config.includeNavigation3 && config.navigation3Version != null) {
                add("            implementation(libs.compose.navigation3.ui)")
                add("            implementation(libs.compose.material3.adaptive.nav3)")
            }
            if (config.includeNavigationEvent && config.navigationEventVersion != null) {
                add("            implementation(libs.compose.navigationevent)")
            }
            if (config.includeSavedState && config.savedStateVersion != null) {
                add("            implementation(libs.androidx.savedstate)")
            }
            if (config.includeWindow && config.windowVersion != null) {
                add("            implementation(libs.androidx.window.core)")
            }
        }
        
        return if (dependencies.isNotEmpty()) {
            "\n" + dependencies.joinToString("\n")
        } else {
            ""
        }
    }
    
    private fun collectConfigs(): List<String> {
        return modules.mapNotNull { module ->
            val config = module.getConfigFragment(config)
            if (config.isNotBlank()) config else null
        }
    }
    
    private fun collectDependencies(): List<String> {
        val dependencies = mutableListOf<String>()
        modules.forEach { module ->
            dependencies.addAll(module.getDependencies(config))
        }
        return dependencies
    }
    
    private fun replaceOptionalLibraryVersionBlock(content: String, placeholder: String, include: Boolean, version: String?, versionKey: String): String {
        val replacement = if (include && version != null) {
            "$versionKey = \"$version\""
        } else {
            ""
        }
        return content.replace("{{$placeholder}}", replacement)
    }
    
    private fun replaceOptionalLibraryLibrariesBlock(content: String, placeholder: String, include: Boolean, libraryPrefix: String): String {
        val replacement = if (include) {
            when (libraryPrefix) {
                "androidx-material3" -> """compose-material3 = { group = "org.jetbrains.compose.material3", name = "material3", version.ref = "compose-material3" }"""
                "androidx-material3-adaptive" -> """compose-material3-adaptive = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive", version.ref = "compose-material3-adaptive" }
compose-material3-adaptive-layout = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive-layout", version.ref = "compose-material3-adaptive" }
compose-material3-adaptive-navigation = { group = "org.jetbrains.compose.material3", name = "material3-adaptive-navigation-suite", version.ref = "compose-material3" }
compose-material3-adaptive-nav3 = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive-navigation3", version.ref = "compose-material3-adaptive" }"""
                "androidx-navigation" -> """androidx-navigation = { group = "org.jetbrains.androidx.navigation", name = "navigation-compose", version.ref = "androidx-navigation" }"""
                "androidx-navigation3" -> """compose-navigation3-ui = { group = "org.jetbrains.androidx.navigation3", name = "navigation3-ui", version.ref = "compose-navigation3-ui" }"""
                "androidx-navigation-event" -> """compose-navigationevent = { group = "org.jetbrains.androidx.navigationevent", name = "navigationevent-compose", version.ref = "compose-navigationevent" }"""
                "androidx-savedstate" -> """androidx-savedstate = { group = "org.jetbrains.androidx.savedstate", name = "savedstate", version.ref = "androidx-savedstate" }"""
                "androidx-window" -> """androidx-window-core = { group = "org.jetbrains.androidx.window", name = "window-core", version.ref = "androidx-window-core" }"""
                else -> ""
            }
        } else {
            ""
        }
        return content.replace("{{$placeholder}}", replacement)
    }
}

