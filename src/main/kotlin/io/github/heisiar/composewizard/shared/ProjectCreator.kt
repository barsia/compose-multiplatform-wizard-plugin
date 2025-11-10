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
        return try {
            println("=== ProjectCreator: Creating project structure ===")
            println("Path: $projectPath")
            println("Name: $projectName")
            println("Package: ${builder.projectId}")
            println("Targets: Desktop=${builder.targetDesktop}, Android=${builder.targetAndroid}, iOS=${builder.targetIOS}, Web=${builder.targetWeb}")
            
            // Get library versions from ComposeVersionCache with fallback to LIBRARY_BUNDLES
            val cache = ComposeVersionCache.getInstance()
            val libraryVersions = ComposeVersions.getLibraryBundle(builder.composeVersion)
                ?: ComposeVersions.getLibraryBundle(ComposeVersions.DEFAULT_VERSION)!!
            
            // Try to get versions from cache first, fallback to LIBRARY_BUNDLES
            val lifecycleVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.LIFECYCLE)
                ?: libraryVersions.lifecycleVersion
            val material3Version = cache.getLibraryVersion(builder.composeVersion, LibraryType.MATERIAL3)
                ?: libraryVersions.material3Version
            val material3AdaptiveVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.MATERIAL3_ADAPTIVE)
                ?: libraryVersions.material3AdaptiveVersion
            val navigationVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.NAVIGATION)
                ?: libraryVersions.navigationVersion
            val navigation3Version = cache.getLibraryVersion(builder.composeVersion, LibraryType.NAVIGATION3)
                ?: libraryVersions.navigation3Version
            val navigationEventVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.NAVIGATION_EVENT)
                ?: libraryVersions.navigationEventVersion
            val savedStateVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.SAVED_STATE)
                ?: libraryVersions.savedStateVersion
            val windowVersion = cache.getLibraryVersion(builder.composeVersion, LibraryType.WINDOW)
                ?: libraryVersions.windowVersion
            
            println("=== ProjectCreator: Using library versions (Cache + LIBRARY_BUNDLES fallback) ===")
            println("Compose: ${builder.composeVersion}, Kotlin: ${libraryVersions.kotlinVersion}, Lifecycle: $lifecycleVersion")
            
            val processor = TemplateProcessor(
                projectName = projectName,
                projectId = builder.projectId,
                composeVersion = builder.composeVersion,
                kotlinVersion = libraryVersions.kotlinVersion,
                lifecycleVersion = lifecycleVersion,
                material3Version = material3Version,
                material3AdaptiveVersion = material3AdaptiveVersion,
                navigationVersion = navigationVersion,
                navigation3Version = navigation3Version,
                navigationEventVersion = navigationEventVersion,
                savedStateVersion = savedStateVersion,
                windowVersion = windowVersion,
                hotReloadVersion = builder.hotReloadVersion,
                includeTests = builder.includeTests,
                targetDesktop = builder.targetDesktop,
                targetAndroid = builder.targetAndroid,
                targetIOS = builder.targetIOS,
                targetWeb = builder.targetWeb,
                enableDevVersions = builder.enableDevVersions,
                includeMaterial3 = builder.includeMaterial3,
                includeMaterial3Adaptive = builder.includeMaterial3Adaptive,
                includeNavigation = builder.includeNavigation,
                includeNavigation3 = builder.includeNavigation3,
                includeNavigationEvent = builder.includeNavigationEvent,
                includeSavedState = builder.includeSavedState,
                includeWindow = builder.includeWindow,
                includeHotReload = builder.includeHotReload
            )
            
            processor.copyTemplateToProject(projectPath)
            
            // Create .gitignore file
            createGitignoreFile(projectPath)
            
            println("=== ProjectCreator: Project structure created successfully ===")
            true
        } catch (e: Exception) {
            println("ERROR in ProjectCreator: ${e.message}")
            e.printStackTrace()
            false
        }
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
            """.trimIndent()
            
            val gitignoreFile = File(projectPath, ".gitignore")
            gitignoreFile.writeText(gitignoreContent)
            println("Created .gitignore file")
        } catch (e: Exception) {
            println("WARNING: Could not create .gitignore: ${e.message}")
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
            println("=== ProjectCreator: Initializing Git repository ===")
            val projectDir = File(projectPath)
            
            val process = Runtime.getRuntime().exec(
                arrayOf("git", "init"),
                null,
                projectDir
            )
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                println("Git repository initialized successfully")
                true
            } else {
                println("WARNING: Git init exited with code $exitCode")
                false
            }
        } catch (e: Exception) {
            println("WARNING: Could not initialize git: ${e.message}")
            false
        }
    }
}

