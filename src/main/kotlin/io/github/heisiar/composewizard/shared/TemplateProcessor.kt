package io.github.heisiar.composewizard.shared

class TemplateProcessor(
    private val projectName: String,
    private val projectId: String,
    private val composeVersion: String,
    private val kotlinVersion: String,
    private val lifecycleVersion: String?,
    private val material3Version: String?,
    private val material3AdaptiveVersion: String?,
    private val navigationVersion: String?,
    private val navigationEventVersion: String?,
    private val savedStateVersion: String?,
    private val windowVersion: String?,
    private val hotReloadVersion: String?,
    private val includeTests: Boolean,
    private val targetDesktop: Boolean,
    private val targetAndroid: Boolean,
    private val targetIOS: Boolean,
    private val targetWeb: Boolean,
    private val enableDevVersions: Boolean = false,
    private val includeMaterial3: Boolean = false,
    private val includeMaterial3Adaptive: Boolean = false,
    private val includeNavigation: Boolean = false,
    private val includeNavigationEvent: Boolean = false,
    private val includeSavedState: Boolean = false,
    private val includeWindow: Boolean = false,
    private val includeHotReload: Boolean = false
) {
    
    private val projectIdPath = projectId.replace('.', '/')
    
    fun determineTemplate(): String {
        val selectedCount = listOf(targetDesktop, targetAndroid, targetIOS, targetWeb).count { it }
        
        return when {
            selectedCount == 4 -> "All"
            selectedCount == 1 && targetAndroid -> "Android"
            selectedCount == 1 && targetIOS -> "iOS"
            selectedCount == 1 && targetDesktop -> "Desktop"
            selectedCount == 1 && targetWeb -> "Web"
            else -> "All"
        }
    }
    
    fun copyTemplateToProject(targetPath: String) {
        val modularProcessor = ModularTemplateProcessor(
            projectName = projectName,
            projectId = projectId,
            composeVersion = composeVersion,
            kotlinVersion = kotlinVersion,
            lifecycleVersion = lifecycleVersion,
            material3Version = material3Version,
            material3AdaptiveVersion = material3AdaptiveVersion,
            navigationVersion = navigationVersion,
            navigationEventVersion = navigationEventVersion,
            savedStateVersion = savedStateVersion,
            windowVersion = windowVersion,
            hotReloadVersion = hotReloadVersion,
            includeTests = includeTests,
            targetDesktop = targetDesktop,
            targetAndroid = targetAndroid,
            targetIOS = targetIOS,
            targetWeb = targetWeb,
            enableDevVersions = enableDevVersions,
            includeMaterial3 = includeMaterial3,
            includeMaterial3Adaptive = includeMaterial3Adaptive,
            includeNavigation = includeNavigation,
            includeNavigationEvent = includeNavigationEvent,
            includeSavedState = includeSavedState,
            includeWindow = includeWindow,
            includeHotReload = includeHotReload
        )
        
        modularProcessor.copyTemplateToProject(targetPath)
    }
    
}

