package io.github.heisiar.composewizard.testutils

import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ComposeVersions

object ProjectFixtures {
    
    const val TEST_PROJECT_NAME = "KotlinProject"
    const val TEST_PACKAGE_NAME = "org.example.project"
    
    val DESKTOP_ONLY_CONFIG = ProjectConfig(
        projectName = TEST_PROJECT_NAME,
        projectId = TEST_PACKAGE_NAME,
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = true,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = false,
        includeTests = false,
        initGit = false
    )
    
    val WEB_ONLY_CONFIG = ProjectConfig(
        projectName = "WebProject",
        projectId = "org.example.web",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = false,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = true,
        includeTests = false,
        initGit = false
    )

    val IOS_ONLY_CONFIG = ProjectConfig(
        projectName = "ProjectIos",
        projectId = "org.example.ios",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = false,
        targetAndroid = false,
        targetIOS = true,
        targetWeb = false,
        includeTests = false,
        initGit = false
    )

    val ANDROID_ONLY_CONFIG = ProjectConfig(
        projectName = "ProjectAndroid",
        projectId = "org.example.android",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = false,
        targetAndroid = true,
        targetIOS = false,
        targetWeb = false,
        includeTests = false,
        initGit = false
    )
    
    fun desktopOnlyConfig(
        projectName: String = TEST_PROJECT_NAME,
        packageName: String = TEST_PACKAGE_NAME,
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = true,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = false,
        includeTests = false,
        initGit = false
    )
    
    fun webOnlyConfig(
        projectName: String = "WebProject",
        packageName: String = "org.example.web",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = false,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = true,
        includeTests = false,
        initGit = false
    )
    
    fun androidConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "ProjectAndroid",
        packageName: String = "org.example.android",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = false,
        targetAndroid = true,
        targetIOS = false,
        targetWeb = false,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun desktopConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = TEST_PROJECT_NAME,
        packageName: String = TEST_PACKAGE_NAME,
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = true,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = false,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun iosConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "ProjectIos",
        packageName: String = "org.example.ios",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = false,
        targetAndroid = false,
        targetIOS = true,
        targetWeb = false,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun webConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "WebProject",
        packageName: String = "org.example.web",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = false,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = true,
        includeTests = includeTests,
        initGit = initGit
    )
    
    val ANDROID_DESKTOP_CONFIG = ProjectConfig(
        projectName = "ProjectAndroidDesktop",
        projectId = "org.example.android.desktop",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = true,
        targetAndroid = true,
        targetIOS = false,
        targetWeb = false,
        includeTests = false,
        initGit = false
    )
    
    val ANDROID_IOS_CONFIG = ProjectConfig(
        projectName = "ProjectAndroidIos",
        projectId = "org.example.android.ios",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = false,
        targetAndroid = true,
        targetIOS = true,
        targetWeb = false,
        includeTests = false,
        initGit = false
    )
    
    val DESKTOP_WEB_CONFIG = ProjectConfig(
        projectName = "ProjectDesktopWeb",
        projectId = "org.example.desktop.web",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = true,
        targetAndroid = false,
        targetIOS = false,
        targetWeb = true,
        includeTests = false,
        initGit = false
    )
    
    val ALL_PLATFORMS_CONFIG = ProjectConfig(
        projectName = "ProjectAllPlatforms",
        projectId = "org.example.all.platforms",
        composeVersion = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion = ComposeVersions.DEFAULT_KOTLIN_VERSION,
        targetDesktop = true,
        targetAndroid = true,
        targetIOS = true,
        targetWeb = true,
        includeTests = false,
        initGit = false
    )
    
    fun androidDesktopIosConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "AndroidDesktopIosTests",
        packageName: String = "org.example.project.android.desktop.ios.tests",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = true,
        targetAndroid = true,
        targetIOS = true,
        targetWeb = false,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun androidWebIosConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "AndroidWebIosGit",
        packageName: String = "org.example.android.web.ios.git",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.DEFAULT_KOTLIN_VERSION
    ) = ProjectConfig(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = false,
        targetAndroid = true,
        targetIOS = true,
        targetWeb = true,
        includeTests = includeTests,
        initGit = initGit
    )
}

