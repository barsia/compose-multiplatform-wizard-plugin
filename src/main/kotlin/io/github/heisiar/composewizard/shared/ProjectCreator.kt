package io.github.heisiar.composewizard.shared

import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
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
            
            val processor = TemplateProcessor(
                projectName = projectName,
                projectId = builder.projectId,
                composeVersion = builder.composeVersion,
                includeTests = builder.includeTests,
                targetDesktop = builder.targetDesktop,
                targetAndroid = builder.targetAndroid,
                targetIOS = builder.targetIOS,
                targetWeb = builder.targetWeb,
                enableDevVersions = builder.enableDevVersions
            )
            
            processor.copyTemplateToProject(projectPath)
            
            println("=== ProjectCreator: Project structure created successfully ===")
            true
        } catch (e: Exception) {
            println("ERROR in ProjectCreator: ${e.message}")
            e.printStackTrace()
            false
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

