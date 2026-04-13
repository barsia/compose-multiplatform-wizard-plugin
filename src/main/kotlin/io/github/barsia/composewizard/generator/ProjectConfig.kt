package io.github.barsia.composewizard.generator

import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder

data class ProjectConfig(
    val projectName: String,
    val projectId: String,
    val composeVersion: String,
    val kotlinVersion: String,
    val targetDesktop: Boolean = false,
    val targetAndroid: Boolean = false,
    val targetIOS: Boolean = false,
    val targetWeb: Boolean = false,
    val includeTests: Boolean = false,
    val initGit: Boolean = false,
    val enableDevVersions: Boolean = false,
    
    val lifecycleVersion: String? = null,
    val material3Version: String? = null,
    val material3AdaptiveVersion: String? = null,
    val navigationVersion: String? = null,
    val navigation3Version: String? = null,
    val navigationEventVersion: String? = null,
    val savedStateVersion: String? = null,
    val windowVersion: String? = null,
    val hotReloadVersion: String? = null,
    val bundledHotReloadVersion: String? = null,  // GitHub version for Compose >= 1.10.0-beta01
    
    val includeMaterial3: Boolean = false,
    val includeMaterial3Adaptive: Boolean = false,
    val includeNavigation: Boolean = false,
    val includeNavigation3: Boolean = false,
    val includeNavigationEvent: Boolean = false,
    val includeSavedState: Boolean = false,
    val includeWindow: Boolean = false,
    val includeHotReload: Boolean = false
) {
    val selectedPlatforms: Set<Platform>
        get() = buildSet {
            if (targetDesktop) add(Platform.DESKTOP)
            if (targetAndroid) add(Platform.ANDROID)
            if (targetIOS) add(Platform.IOS)
            if (targetWeb) add(Platform.WEB)
        }
    
    val projectIdPath: String
        get() = projectId.replace('.', '/')
    
    companion object {
        fun from(builder: ComposeMultiplatformModuleBuilder): ProjectConfig {
            return ProjectConfig(
                projectName = builder.projectName,
                projectId = builder.projectId,
                composeVersion = builder.composeVersion,
                kotlinVersion = builder.kotlinVersion,
                targetDesktop = builder.targetDesktop,
                targetAndroid = builder.targetAndroid,
                targetIOS = builder.targetIOS,
                targetWeb = builder.targetWeb,
                includeTests = builder.includeTests,
                initGit = builder.initGit,
                enableDevVersions = builder.enableDevVersions,
                
                lifecycleVersion = builder.lifecycleVersion,
                material3Version = builder.material3Version,
                material3AdaptiveVersion = builder.material3AdaptiveVersion,
                navigationVersion = builder.navigationVersion,
                navigation3Version = builder.navigation3Version,
                navigationEventVersion = builder.navigationEventVersion,
                savedStateVersion = builder.savedStateVersion,
                windowVersion = builder.windowVersion,
                hotReloadVersion = builder.hotReloadVersion,
                bundledHotReloadVersion = builder.bundledHotReloadVersion,
                
                includeMaterial3 = builder.includeMaterial3,
                includeMaterial3Adaptive = builder.includeMaterial3Adaptive,
                includeNavigation = builder.includeNavigation,
                includeNavigation3 = builder.includeNavigation3,
                includeNavigationEvent = builder.includeNavigationEvent,
                includeSavedState = builder.includeSavedState,
                includeWindow = builder.includeWindow,
                includeHotReload = builder.includeHotReload
            )
        }
    }
}

enum class Platform {
    DESKTOP,
    ANDROID,
    IOS,
    WEB
}

