package io.github.heisiar.composewizard.shared

import java.io.File

class ModularBuildGenerator(
    private val resourceCopier: ModularResourceCopier,
    private val selectedPlatforms: List<String>,
    private val targetAndroid: Boolean,
    private val targetDesktop: Boolean,
    private val includeTests: Boolean,
    private val includeHotReload: Boolean,
    private val includeMaterial3: Boolean,
    private val includeMaterial3Adaptive: Boolean,
    private val includeNavigation: Boolean,
    private val includeNavigation3: Boolean,
    private val includeNavigationEvent: Boolean,
    private val includeSavedState: Boolean,
    private val includeWindow: Boolean,
    private val material3Version: String?,
    private val material3AdaptiveVersion: String?,
    private val navigationVersion: String?,
    private val navigation3Version: String?,
    private val navigationEventVersion: String?,
    private val savedStateVersion: String?,
    private val windowVersion: String?
) {
    
    fun generateBuildFiles(targetPath: String) {
        generateRootBuildFile(targetPath)
        generateComposeAppBuildFile(targetPath)
    }
    
    private fun generateRootBuildFile(targetPath: String) {
        val template = resourceCopier.readResourceFile("templates/modular/base/build.gradle.kts.template")
        
        val rootPlugins = buildString {
            for (platform in selectedPlatforms) {
                val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/$platform/root-plugin.fragment")
                if (fragment.isNotBlank()) {
                    appendLine(fragment)
                }
            }
        }
        
        val content = template.replace("{{ROOT_PLUGINS}}", rootPlugins)
        
        File(targetPath, "build.gradle.kts").writeText(content)
    }
    
    private fun generateComposeAppBuildFile(targetPath: String) {
        val template = resourceCopier.readResourceFile("templates/modular/base/composeApp/build.gradle.kts.template")
        
        val imports = collectFragments("import")
        val plugins = collectFragments("plugin")
        val targets = collectFragments("target")
        
        val optionalLibrariesDependencies = generateOptionalLibrariesDependencies()
        
        val sourceSets = buildString {
            var commonMainFragment = resourceCopier.readResourceFile("templates/modular/base/commonMain.sourceset.fragment")
            commonMainFragment = commonMainFragment.replace("{{OPTIONAL_LIBRARIES_DEPENDENCIES}}", optionalLibrariesDependencies)
            appendLine(commonMainFragment)
            appendLine(collectFragments("sourceset"))
            if (includeTests) {
                appendLine(resourceCopier.readResourceFile("templates/modular/features/tests/sourceset.fragment"))
            }
        }
        val androidConfig = if (targetAndroid) collectFragments("config", listOf("android")) else ""
        val desktopConfig = if (targetDesktop) collectFragments("config", listOf("desktop")) else ""
        val dependencies = collectFragments("dependency")
        
        val hotReloadPlugin = if (includeHotReload) "    alias(libs.plugins.composeHotReload)" else ""
        
        var content = template
            .replace("{{IMPORTS}}", imports)
            .replace("{{PLUGINS}}", plugins)
            .replace("{{HOT_RELOAD_PLUGIN}}", hotReloadPlugin)
            .replace("{{TARGETS}}", targets)
            .replace("{{SOURCE_SETS}}", sourceSets)
            .replace("{{ANDROID_CONFIG}}", androidConfig)
            .replace("{{DESKTOP_CONFIG}}", desktopConfig)
            .replace("{{DEPENDENCIES}}", dependencies)
        
        content = content.lines()
            .filter { it.isNotBlank() || it.trim().isEmpty() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
        
        File(targetPath, "composeApp/build.gradle.kts").writeText(content)
    }
    
    private fun collectFragments(fragmentType: String, platforms: List<String>? = null): String {
        val platformsToUse = platforms ?: selectedPlatforms
        return buildString {
            for (platform in platformsToUse) {
                val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/$platform/$fragmentType.fragment")
                if (fragment.isNotBlank()) {
                    appendLine(fragment)
                }
            }
        }
    }
    
    private fun generateOptionalLibrariesDependencies(): String {
        val dependencies = buildList {
            if (includeMaterial3 && material3Version != null) {
                add("            implementation(libs.androidx.material3)")
            }
            if (includeMaterial3Adaptive && material3AdaptiveVersion != null) {
                add("            implementation(libs.androidx.material3.adaptive)")
                add("            implementation(libs.androidx.material3.adaptive.layout)")
                add("            implementation(libs.androidx.material3.adaptive.navigation)")
            }
            if (includeNavigation && navigationVersion != null) {
                add("            implementation(libs.androidx.navigation)")
            }
            if (includeNavigation3 && navigation3Version != null) {
                add("            implementation(libs.androidx.navigation3)")
            }
            if (includeNavigationEvent && navigationEventVersion != null) {
                add("            implementation(libs.androidx.navigation.event)")
            }
            if (includeSavedState && savedStateVersion != null) {
                add("            implementation(libs.androidx.savedstate)")
            }
            if (includeWindow && windowVersion != null) {
                add("            implementation(libs.androidx.window)")
            }
        }
        
        return if (dependencies.isNotEmpty()) {
            "\n" + dependencies.joinToString("\n")
        } else {
            ""
        }
    }
    
    fun updateLibraryVersions(targetPath: String, composeVersion: String, kotlinVersion: String, lifecycleVersion: String?) {
        val libsVersionsFile = File(targetPath, "gradle/libs.versions.toml")
        if (!libsVersionsFile.exists()) {
            return
        }
        
        try {
            var content = libsVersionsFile.readText()
            content = content.replace("{{COMPOSE_VERSION}}", composeVersion)
            content = content.replace("{{KOTLIN_VERSION}}", kotlinVersion)
            content = content.replace("{{LIFECYCLE_VERSION}}", lifecycleVersion ?: "2.9.5")
            
            content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_VERSION_BLOCK", 
                                                         includeMaterial3, material3Version, "androidx-material3")
            content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_ADAPTIVE_VERSION_BLOCK",
                                                         includeMaterial3Adaptive, material3AdaptiveVersion, "androidx-material3-adaptive")
            content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_VERSION_BLOCK",
                                                         includeNavigation, navigationVersion, "androidx-navigation")
            content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION3_VERSION_BLOCK",
                                                         includeNavigation3, navigation3Version, "androidx-navigation3")
            content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_EVENT_VERSION_BLOCK",
                                                         includeNavigationEvent, navigationEventVersion, "androidx-navigation-event")
            content = replaceOptionalLibraryVersionBlock(content, "SAVED_STATE_VERSION_BLOCK",
                                                         includeSavedState, savedStateVersion, "androidx-savedstate")
            content = replaceOptionalLibraryVersionBlock(content, "WINDOW_VERSION_BLOCK",
                                                         includeWindow, windowVersion, "androidx-window")
            
            content = replaceOptionalLibraryLibrariesBlock(content, "MATERIAL3_LIBRARIES_BLOCK",
                                                           includeMaterial3, "androidx-material3")
            content = replaceOptionalLibraryLibrariesBlock(content, "MATERIAL3_ADAPTIVE_LIBRARIES_BLOCK",
                                                           includeMaterial3Adaptive, "androidx-material3-adaptive")
            content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION_LIBRARIES_BLOCK",
                                                           includeNavigation, "androidx-navigation")
            content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION3_LIBRARIES_BLOCK",
                                                           includeNavigation3, "androidx-navigation3")
            content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION_EVENT_LIBRARIES_BLOCK",
                                                           includeNavigationEvent, "androidx-navigation-event")
            content = replaceOptionalLibraryLibrariesBlock(content, "SAVED_STATE_LIBRARIES_BLOCK",
                                                           includeSavedState, "androidx-savedstate")
            content = replaceOptionalLibraryLibrariesBlock(content, "WINDOW_LIBRARIES_BLOCK",
                                                           includeWindow, "androidx-window")
            
            libsVersionsFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                "androidx-navigation3" -> """androidx-navigation3 = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "androidx-navigation3" }"""
                "androidx-navigation-event" -> """androidx-navigation-event = { module = "org.jetbrains.androidx.navigationevent:navigationevent", version.ref = "androidx-navigation-event" }"""
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
