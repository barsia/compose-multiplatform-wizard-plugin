package io.github.heisiar.composewizard.composer

import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ModularResourceCopier
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
        val template = resourceCopier.readResourceFile("templates/modular/base/gradle/libs.versions.toml")
        
        var content = template
        content = content.replace("{{COMPOSE_VERSION}}", config.composeVersion)
        content = content.replace("{{KOTLIN_VERSION}}", config.kotlinVersion)
        content = content.replace("{{LIFECYCLE_VERSION}}", config.lifecycleVersion ?: "2.9.5")
        
        content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_VERSION_BLOCK", 
                                                     config.includeMaterial3, config.material3Version, "androidx-material3")
        content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_ADAPTIVE_VERSION_BLOCK",
                                                     config.includeMaterial3Adaptive, config.material3AdaptiveVersion, "androidx-material3-adaptive")
        content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_VERSION_BLOCK",
                                                     config.includeNavigation, config.navigationVersion, "androidx-navigation")
        content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION3_VERSION_BLOCK",
                                                     config.includeNavigation3, config.navigation3Version, "androidx-navigation3")
        content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_EVENT_VERSION_BLOCK",
                                                     config.includeNavigationEvent, config.navigationEventVersion, "androidx-navigation-event")
        content = replaceOptionalLibraryVersionBlock(content, "SAVED_STATE_VERSION_BLOCK",
                                                     config.includeSavedState, config.savedStateVersion, "androidx-savedstate")
        content = replaceOptionalLibraryVersionBlock(content, "WINDOW_VERSION_BLOCK",
                                                     config.includeWindow, config.windowVersion, "androidx-window")
        
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
        
        if (!config.includeTests) {
            content = content.lines()
                .filterNot { line ->
                    line.contains("junit") ||
                    line.contains("testExt") ||
                    line.contains("espresso") ||
                    line.contains("kotlin-test")
                }
                .joinToString("\n")
        }
        
        if (!config.targetAndroid) {
            content = content.lines()
                .filterNot { line ->
                    line.contains("agp =") ||
                    line.contains("android-compileSdk") ||
                    line.contains("android-minSdk") ||
                    line.contains("android-targetSdk") ||
                    line.contains("androidx-activity") ||
                    line.contains("androidx-appcompat") ||
                    line.contains("androidx-core") ||
                    line.contains("androidApplication") ||
                    line.contains("androidLibrary")
                }
                .joinToString("\n")
        }
        
        if (!config.targetDesktop) {
            content = content.lines()
                .filterNot { line ->
                    line.contains("kotlinx-coroutines") ||
                    line.contains("kotlinx-coroutinesSwing")
                }
                .joinToString("\n")
        }
        
        if (!config.includeHotReload) {
            content = content.lines()
                .filterNot { line ->
                    line.contains("composeHotReload") && line.contains("=")
                }
                .joinToString("\n")
        }
        
        // For multiplatform projects, remove Android-specific test libraries (espresso, testExt)
        // They are only needed for Android single-platform projects with instrumented tests
        val isMultiplatform = listOf(config.targetAndroid, config.targetDesktop, config.targetIOS, config.targetWeb).count { it } > 1
        if (isMultiplatform) {
            content = content.lines()
                .filterNot { line ->
                    line.contains("androidx-espresso") ||
                    line.contains("androidx-testExt")
                }
                .joinToString("\n")
        }
        
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
        val optionalLibrariesDependencies = generateOptionalLibrariesDependencies()
        return fragment.replace("{{OPTIONAL_LIBRARIES_DEPENDENCIES}}", optionalLibrariesDependencies)
    }
    
    private fun generateOptionalLibrariesDependencies(): String {
        val dependencies = buildList {
            if (config.includeMaterial3 && config.material3Version != null) {
                add("            implementation(libs.androidx.material3)")
            }
            if (config.includeMaterial3Adaptive && config.material3AdaptiveVersion != null) {
                add("            implementation(libs.androidx.material3.adaptive)")
                add("            implementation(libs.androidx.material3.adaptive.layout)")
                add("            implementation(libs.androidx.material3.adaptive.navigation)")
            }
            if (config.includeNavigation && config.navigationVersion != null) {
                add("            implementation(libs.androidx.navigation)")
            }
            if (config.includeNavigation3 && config.navigation3Version != null) {
                add("            implementation(libs.androidx.navigation3)")
            }
            if (config.includeNavigationEvent && config.navigationEventVersion != null) {
                add("            implementation(libs.androidx.navigation.event)")
            }
            if (config.includeSavedState && config.savedStateVersion != null) {
                add("            implementation(libs.androidx.savedstate)")
            }
            if (config.includeWindow && config.windowVersion != null) {
                add("            implementation(libs.androidx.window)")
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
                "androidx-material3" -> """androidx-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "androidx-material3" }"""
                "androidx-material3-adaptive" -> """androidx-material3-adaptive = { module = "org.jetbrains.compose.material3.adaptive:adaptive", version.ref = "androidx-material3-adaptive" }
androidx-material3-adaptive-layout = { module = "org.jetbrains.compose.material3.adaptive:adaptive-layout", version.ref = "androidx-material3-adaptive" }
androidx-material3-adaptive-navigation = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation", version.ref = "androidx-material3-adaptive" }"""
                "androidx-navigation" -> """androidx-navigation = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "androidx-navigation" }"""
                "androidx-navigation3" -> """androidx-navigation3 = { module = "org.jetbrains.androidx.navigation3:navigation3-compose", version.ref = "androidx-navigation3" }"""
                "androidx-navigation-event" -> """androidx-navigation-event = { module = "org.jetbrains.androidx.navigationevent:navigationevent-compose", version.ref = "androidx-navigation-event" }"""
                "androidx-savedstate" -> """androidx-savedstate = { module = "org.jetbrains.androidx.savedstate:savedstate", version.ref = "androidx-savedstate" }"""
                "androidx-window" -> """androidx-window = { module = "org.jetbrains.androidx.window:window-core", version.ref = "androidx-window" }"""
                else -> ""
            }
        } else {
            ""
        }
        return content.replace("{{$placeholder}}", replacement)
    }
}

