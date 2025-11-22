package io.github.heisiar.composewizard.testutils

import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder

/**
 * Test fixtures for project generation tests.
 * Provides pre-configured ComposeMultiplatformModuleBuilder instances.
 */
object ProjectFixtures {
    
    const val TEST_PROJECT_NAME = "KotlinProject"
    const val TEST_PACKAGE_NAME = "org.example.project"
    
    val DESKTOP_ONLY_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = TEST_PROJECT_NAME,
            projectId = TEST_PACKAGE_NAME,
            composeVersion = "1.9.3",
            targetDesktop = true
        ).apply {
            lifecycleVersion = "2.9.6"
        }
    
    val WEB_ONLY_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "WebProject",
            projectId = "org.example.web",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetWeb = true
        )

    val IOS_ONLY_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "ProjectIos",
            projectId = "org.example.ios",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetIOS = true
        )

    val ANDROID_ONLY_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "ProjectAndroid",
            projectId = "org.example.android",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetAndroid = true
        )
    
    fun desktopOnlyConfig(
        projectName: String = TEST_PROJECT_NAME,
        packageName: String = TEST_PACKAGE_NAME,
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = true
    )
    
    fun webOnlyConfig(
        projectName: String = "WebProject",
        packageName: String = "org.example.web",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetWeb = true
    )
    
    fun androidConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "ProjectAndroid",
        packageName: String = "org.example.android",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetAndroid = true,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun desktopConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = TEST_PROJECT_NAME,
        packageName: String = TEST_PACKAGE_NAME,
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = true,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun iosConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "ProjectIos",
        packageName: String = "org.example.ios",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetIOS = true,
        includeTests = includeTests,
        initGit = initGit
    )
    
    fun webConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "WebProject",
        packageName: String = "org.example.web",
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetWeb = true,
        includeTests = includeTests,
        initGit = initGit
    )
    
    val ANDROID_DESKTOP_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "ProjectAndroidDesktop",
            projectId = "org.example.android.desktop",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetDesktop = true,
            targetAndroid = true
        ).apply {
            lifecycleVersion = "2.9.6"
            includeHotReload = true
            hotReloadVersion = "1.0.0-rc02"
        }
    
    val ANDROID_IOS_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "ProjectAndroidIos",
            projectId = "org.example.android.ios",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetAndroid = true,
            targetIOS = true
        ).apply {
            lifecycleVersion = "2.9.6"
        }
    
    val DESKTOP_WEB_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "ProjectDesktopWeb",
            projectId = "org.example.desktop.web",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetDesktop = true,
            targetWeb = true
        ).apply {
            lifecycleVersion = "2.9.6"
            includeHotReload = true
            hotReloadVersion = "1.0.0-rc02"
        }
    
    val ALL_PLATFORMS_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "ProjectAllPlatforms",
            projectId = "org.example.all.platforms",
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            targetDesktop = true,
            targetAndroid = true,
            targetIOS = true,
            targetWeb = true
        ).apply {
            lifecycleVersion = "2.9.6"
            includeHotReload = true
            hotReloadVersion = "1.0.0-rc02"
        }
    
    fun androidDesktopIosConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "AndroidDesktopIosTests",
        packageName: String = "org.example.project.android.desktop.ios.tests",
        composeVersion: String = "1.9.3",
        kotlinVersion: String = "2.2.21"
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetDesktop = true,
        targetAndroid = true,
        targetIOS = true,
        includeTests = includeTests,
        initGit = initGit
    ).apply {
        lifecycleVersion = "2.9.6"
        includeHotReload = true
        hotReloadVersion = "1.0.0-rc02"
    }
    
    fun androidWebIosConfig(
        includeTests: Boolean = false,
        initGit: Boolean = false,
        projectName: String = "AndroidWebIosGit",
        packageName: String = "org.example.android.web.ios.git",
        composeVersion: String = "1.9.3",
        kotlinVersion: String = "2.2.21"
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = composeVersion,
        kotlinVersion = kotlinVersion,
        targetAndroid = true,
        targetIOS = true,
        targetWeb = true,
        includeTests = includeTests,
        initGit = initGit
    ).apply {
        lifecycleVersion = "2.9.6"
    }

    fun androidDesktopIosAllLibrariesConfig(
        includeTests: Boolean = true,
        includeMaterial3: Boolean = true,
        includeMaterial3Adaptive: Boolean = true,
        includeNavigation3: Boolean = true,
        includeNavigationEvent: Boolean = true,
        includeSavedState: Boolean = true,
        includeWindow: Boolean = true,
        projectName: String = "AndroidDesktopIosAllLibraries",
        packageName: String = "org.example.project"
    ) = createBuilder(
        projectName = projectName,
        projectId = packageName,
        composeVersion = "1.10.0-beta01",
        kotlinVersion = "2.2.21",
        targetAndroid = true,
        targetDesktop = true,
        targetIOS = true,
        includeTests = includeTests,
        initGit = false
    ).apply {
        lifecycleVersion = "2.10.0-alpha04"
        material3Version = "1.10.0-alpha04"
        material3AdaptiveVersion = "1.3.0-alpha01"
        navigation3Version = "1.0.0-alpha04"
        navigationEventVersion = "1.0.0-beta01"
        savedStateVersion = "1.4.0-beta01"
        windowVersion = "1.5.0-rc01"

        this.includeMaterial3 = includeMaterial3
        this.includeMaterial3Adaptive = includeMaterial3Adaptive
        this.includeNavigation3 = includeNavigation3
        this.includeNavigationEvent = includeNavigationEvent
        this.includeSavedState = includeSavedState
        this.includeWindow = includeWindow
        this.includeHotReload = false
        this.enableDevVersions = true
    }

    val IOS_DESKTOP_LIBRARIES_TESTS_CONFIG: ComposeMultiplatformModuleBuilder
        get() = createBuilder(
            projectName = "IosDesktopLibrariesTests",
            projectId = "org.ios.desktop.libraries.tests",
            composeVersion = "1.10.0-beta02",
            targetIOS = true,
            targetDesktop = true,
            includeTests = true
        ).apply {
            lifecycleVersion = "2.10.0-alpha05"
            material3Version = "1.10.0-alpha05"
            material3AdaptiveVersion = "1.3.0-alpha02"
            navigationEventVersion = "1.0.0-beta02"
            navigation3Version = "1.0.0-alpha05"
            windowVersion = "1.5.0"
            savedStateVersion = "1.4.0-rc01"
            hotReloadVersion = "1.0.0-rc03"
            
            includeMaterial3 = true
            includeMaterial3Adaptive = true
            includeNavigation3 = true
            includeNavigationEvent = true
            includeSavedState = false
            includeWindow = false
            includeHotReload = false
            enableDevVersions = true
        }
    
    /**
     * Creates a ComposeMultiplatformModuleBuilder with the specified configuration.
     * Uses library versions from LIBRARY_BUNDLES for the specified Compose version.
     */
    private fun createBuilder(
        projectName: String,
        projectId: String,
        composeVersion: String = ComposeVersions.DEFAULT_VERSION,
        kotlinVersion: String = ComposeVersions.KOTLIN_VERSION_WIZARD,
        targetDesktop: Boolean = false,
        targetAndroid: Boolean = false,
        targetIOS: Boolean = false,
        targetWeb: Boolean = false,
        includeTests: Boolean = false,
        initGit: Boolean = false
    ): ComposeMultiplatformModuleBuilder {
        val bundle = ComposeVersions.getLibraryBundle(composeVersion)
            ?: ComposeVersions.getLibraryBundle(ComposeVersions.DEFAULT_VERSION)!!
        
        return ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            this.projectId = projectId
            this.composeVersion = composeVersion
            this.kotlinVersion = kotlinVersion
            this.targetDesktop = targetDesktop
            this.targetAndroid = targetAndroid
            this.targetIOS = targetIOS
            this.targetWeb = targetWeb
            this.includeTests = includeTests
            this.initGit = initGit
            this.enableDevVersions = false
            
            // Library versions from bundle
            this.lifecycleVersion = bundle.lifecycleVersion
            this.material3Version = bundle.material3Version
            this.material3AdaptiveVersion = bundle.material3AdaptiveVersion
            this.navigationVersion = bundle.navigationVersion
            this.navigation3Version = bundle.navigation3Version
            this.navigationEventVersion = bundle.navigationEventVersion
            this.savedStateVersion = bundle.savedStateVersion
            this.windowVersion = bundle.windowVersion
            
            // Library inclusion flags (tests don't use libraries by default)
            this.includeMaterial3 = false
            this.includeMaterial3Adaptive = false
            this.includeNavigation = false
            this.includeNavigation3 = false
            this.includeNavigationEvent = false
            this.includeSavedState = false
            this.includeWindow = false
            this.includeHotReload = false
        }
    }
    
    /**
     * Converts ComposeMultiplatformModuleBuilder to ProjectConfig.
     * Used for XcodeProjectGenerator tests.
     */
    fun toProjectConfig(builder: ComposeMultiplatformModuleBuilder): ProjectConfig {
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
            initGit = builder.initGit
        )
    }
}
