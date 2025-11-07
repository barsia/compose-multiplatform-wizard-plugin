package io.github.heisiar.composewizard.generator

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
    val initGit: Boolean = false
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
}

enum class Platform {
    DESKTOP,
    ANDROID,
    IOS,
    WEB
}

