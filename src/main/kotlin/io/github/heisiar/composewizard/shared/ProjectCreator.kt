package io.github.heisiar.composewizard.shared

import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import java.io.File

/**
 * Unified project creation logic shared between IntelliJ IDEA and Android Studio.
 * 
 * This ensures consistent project structure generation regardless of which IDE is used.
 */
object ProjectCreator {
    
    /**
     * Creates the project structure at the specified path.
     * 
     * @param projectPath Full path where the project should be created
     * @param projectName Name of the project
     * @param builder Module builder containing all project configuration
     * @return true if project was created successfully, false otherwise
     */
    fun createProjectStructure(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    ): Boolean {
        return createProjectStructure(projectPath, projectName, builder, useCache = true)
    }
    
    /**
     * Creates the project structure at the specified path.
     * 
     * This is an internal method that allows controlling whether to use ComposeVersionCache.
     * When useCache=false, it's suitable for integration tests that don't have IntelliJ Application context.
     * 
     * @param projectPath Full path where the project should be created
     * @param projectName Name of the project
     * @param builder Module builder containing all project configuration
     * @param useCache Whether to use ComposeVersionCache (requires IntelliJ Application context)
     * @return true if project was created successfully, false otherwise
     */
    internal fun createProjectStructure(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder,
        useCache: Boolean
    ): Boolean {
        return try {
            
            // Get library versions from LIBRARY_BUNDLES (for fallback)
            val libraryVersions = ComposeVersions.getLibraryBundle(builder.composeVersion)
                ?: ComposeVersions.getLibraryBundle(ComposeVersions.DEFAULT_VERSION)!!
            
            // Use versions from builder if set, otherwise determine based on useCache flag
            val lifecycleVersion: String
            val material3Version: String?
            val material3AdaptiveVersion: String?
            val navigationVersion: String?
            val navigation3Version: String?
            val navigationEventVersion: String?
            val savedStateVersion: String?
            val windowVersion: String?
            
            if (builder.lifecycleVersion != null) {
                // Use versions from builder (already set by UI or tests)
                lifecycleVersion = builder.lifecycleVersion!!
                material3Version = builder.material3Version
                material3AdaptiveVersion = builder.material3AdaptiveVersion
                navigationVersion = builder.navigationVersion
                navigation3Version = builder.navigation3Version
                navigationEventVersion = builder.navigationEventVersion
                savedStateVersion = builder.savedStateVersion
                windowVersion = builder.windowVersion
            } else if (useCache) {
                // Try to get versions from cache first, fallback to LIBRARY_BUNDLES
                val cache = ComposeVersionCache.getInstance()
                lifecycleVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.LIFECYCLE)
                    ?: libraryVersions.lifecycleVersion
                material3Version = cache.getLibraryVersion(builder.composeVersion, LibraryType.MATERIAL3)
                    ?: libraryVersions.material3Version
                material3AdaptiveVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.MATERIAL3_ADAPTIVE)
                    ?: libraryVersions.material3AdaptiveVersion
                navigationVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.NAVIGATION)
                    ?: libraryVersions.navigationVersion
                navigation3Version = cache.getLibraryVersion(builder.composeVersion, LibraryType.NAVIGATION3)
                    ?: libraryVersions.navigation3Version
                navigationEventVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.NAVIGATION_EVENT)
                    ?: libraryVersions.navigationEventVersion
                savedStateVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.SAVED_STATE)
                    ?: libraryVersions.savedStateVersion
                windowVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.WINDOW)
                    ?: libraryVersions.windowVersion
            } else {
                // Use only versions from LIBRARY_BUNDLES (for tests without Application context)
                lifecycleVersion = libraryVersions.lifecycleVersion
                material3Version = libraryVersions.material3Version
                material3AdaptiveVersion = libraryVersions.material3AdaptiveVersion
                navigationVersion = libraryVersions.navigationVersion
                navigation3Version = libraryVersions.navigation3Version
                navigationEventVersion = libraryVersions.navigationEventVersion
                savedStateVersion = libraryVersions.savedStateVersion
                windowVersion = libraryVersions.windowVersion
            }
            
            
            val config = io.github.heisiar.composewizard.generator.ProjectConfig(
                projectName = projectName,
                projectId = builder.projectId,
                composeVersion = builder.composeVersion,
                kotlinVersion = builder.kotlinVersion.takeIf { it.isNotEmpty() } ?: libraryVersions.kotlinVersion,
                targetDesktop = builder.targetDesktop,
                targetAndroid = builder.targetAndroid,
                targetIOS = builder.targetIOS,
                targetWeb = builder.targetWeb,
                includeTests = builder.includeTests,
                initGit = builder.initGit,
                enableDevVersions = builder.enableDevVersions,
                lifecycleVersion = lifecycleVersion,
                material3Version = material3Version,
                material3AdaptiveVersion = material3AdaptiveVersion,
                navigationVersion = navigationVersion,
                navigation3Version = navigation3Version,
                navigationEventVersion = navigationEventVersion,
                savedStateVersion = savedStateVersion,
                windowVersion = windowVersion,
                hotReloadVersion = builder.hotReloadVersion,
                includeMaterial3 = builder.includeMaterial3,
                includeMaterial3Adaptive = builder.includeMaterial3Adaptive,
                includeNavigation = builder.includeNavigation,
                includeNavigation3 = builder.includeNavigation3,
                includeNavigationEvent = builder.includeNavigationEvent,
                includeSavedState = builder.includeSavedState,
                includeWindow = builder.includeWindow,
                includeHotReload = builder.includeHotReload
            )
            
            val composer = io.github.heisiar.composewizard.composer.ProjectComposer(config)
            
            if (builder.targetAndroid) {
                composer.addModule(io.github.heisiar.composewizard.composer.modules.AndroidModule())
            }
            if (builder.targetIOS) {
                composer.addModule(io.github.heisiar.composewizard.composer.modules.iOSModule())
            }
            if (builder.targetDesktop) {
                composer.addModule(io.github.heisiar.composewizard.composer.modules.DesktopModule())
            }
            if (builder.targetWeb) {
                composer.addModule(io.github.heisiar.composewizard.composer.modules.WebModule())
            }
            
            if (builder.includeTests) {
                composer.addFeature(io.github.heisiar.composewizard.composer.features.TestsFeature())
            }
            if (builder.initGit) {
                composer.addFeature(io.github.heisiar.composewizard.composer.features.GitFeature())
            }
            if (builder.includeHotReload) {
                composer.addFeature(io.github.heisiar.composewizard.composer.features.HotReloadFeature())
            }
            
            composer.compose(projectPath)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Creates local.properties file for Android projects.
     * 
     * @param projectPath Path to the project directory
     */
    fun createLocalPropertiesFile(projectPath: String) {
        val localPropertiesContent = """## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.

sdk.dir=
"""
        File(projectPath, "local.properties").writeText(localPropertiesContent)
    }
    
    /**
     * Creates .gitignore file in the project directory.
     * 
     * @param projectPath Path to the project directory
     */
    private fun createGitignoreFile(projectPath: String) {
        try {
            val gitignoreContent = """
                *.iml
                .kotlin
                .gradle
                **/build/
                xcuserdata
                !src/**/build/
                local.properties
                .idea
                .DS_Store
                captures
                .externalNativeBuild
                .cxx
                *.xcodeproj/*
                !*.xcodeproj/project.pbxproj
                !*.xcodeproj/xcshareddata/
                !*.xcodeproj/project.xcworkspace/
                !*.xcworkspace/contents.xcworkspacedata
                **/xcshareddata/WorkspaceSettings.xcsettings
                node_modules/
            """.trimIndent() + "\n"
            
            val gitignoreFile = File(projectPath, ".gitignore")
            gitignoreFile.writeText(gitignoreContent)
        } catch (e: Exception) {
        }
    }
    
    /**
     * Initializes Git repository in the project directory.
     * 
     * Uses different strategies depending on available APIs:
     * - In IntelliJ IDEA: Uses GitRepositoryInitializer API (handled by caller)
     * - In Android Studio or fallback: Uses command-line git
     * 
     * @param projectPath Path to the project directory
     * @return true if git was initialized successfully, false otherwise
     */
    fun initializeGitRepository(projectPath: String): Boolean {
        return try {
            val projectDir = File(projectPath)
            
            // Step 1: Initialize Git repository
            val initProcess = Runtime.getRuntime().exec(
                arrayOf("git", "init"),
                null,
                projectDir
            )
            if (initProcess.waitFor() != 0) {
                return false
            }
            
            // Step 2: Add all files to staging
            // Files will be staged (green in IDE), user can commit when ready
            val addProcess = Runtime.getRuntime().exec(
                arrayOf("git", "add", "."),
                null,
                projectDir
            )
            addProcess.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}

