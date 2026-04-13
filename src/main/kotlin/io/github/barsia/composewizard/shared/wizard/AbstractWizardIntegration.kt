package io.github.barsia.composewizard.shared.wizard

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder

/**
 * Abstract wizard integration that defines the common workflow for project creation
 * across different IDEs (IntelliJ IDEA and Android Studio).
 * 
 * This pattern ensures consistent project creation flow while allowing IDE-specific
 * customization through abstract methods.
 * 
 * Workflow:
 * 1. Create project structure (common)
 * 2. Initialize Git if requested (IDE-specific with fallback)
 * 3. Perform IDE-specific setup (linking Gradle, SDK configuration, etc.)
 * 4. Open project (IDE-specific)
 * 5. Handle errors if any step fails
 */
abstract class AbstractWizardIntegration {
    
    /**
     * Main workflow for creating and opening a Compose Multiplatform project.
     * 
     * This method orchestrates the entire project creation process:
     * - Creates project structure from templates
     * - Initializes Git repository (if requested)
     * - Performs IDE-specific setup
     * - Opens the project in the IDE
     * 
     * @param projectPath Full path where the project should be created
     * @param projectName Name of the project
     * @param builder Module builder containing all project configuration
     * @return true if project was created and opened successfully, false otherwise
     */
    fun createAndOpenProject(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    ): Boolean {
        try {
            
            // Step 1: Create project structure (common logic)
            val success = ProjectCreator.createProjectStructure(
                projectPath = projectPath,
                projectName = projectName,
                builder = builder
            )
            
            if (!success) {
                handleCreationError(projectPath, projectName, "Failed to create project structure")
                return false
            }
            
            // Step 2: Initialize Git if requested (IDE-specific with fallback)
            if (builder.initGit) {
                initializeGit(projectPath)
            }
            
            // Step 3: IDE-specific setup (Gradle linking, SDK, etc.)
            performIdeSpecificSetup(projectPath, projectName, builder)
            
            // Step 4: Open project (IDE-specific)
            openProject(projectPath)
            
            return true
            
        } catch (e: Exception) {
            e.printStackTrace()
            handleCreationError(projectPath, projectName, e.message ?: "Unknown error")
            return false
        }
    }
    
    /**
     * Performs IDE-specific setup after project structure is created.
     * 
     * This is where each IDE can perform custom initialization:
     * - IntelliJ IDEA: Link Gradle project, setup project structure
     * - Android Studio: Configure Android SDK, sync Gradle
     * 
     * @param projectPath Full path to the project
     * @param projectName Name of the project
     * @param builder Module builder with project configuration
     */
    protected abstract fun performIdeSpecificSetup(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    )
    
    /**
     * Opens the created project in the IDE.
     * 
     * Implementation varies by IDE:
     * - IntelliJ IDEA: Project is already opened by the platform
     * - Android Studio: Use ProjectManager.loadAndOpenProject()
     * 
     * @param projectPath Full path to the project
     */
    protected abstract fun openProject(projectPath: String)
    
    /**
     * Initializes Git repository for the project.
     * 
     * Each IDE can use its preferred method:
     * - IntelliJ IDEA: GitRepositoryInitializer API (preferred)
     * - Android Studio: Command-line git init
     * - Fallback: ProjectCreator.initializeGitRepository()
     * 
     * @param projectPath Full path to the project
     */
    protected abstract fun initializeGit(projectPath: String)
    
    /**
     * Handles errors that occur during project creation.
     * 
     * Default implementation logs the error. Subclasses can override
     * to show user-facing error dialogs or notifications.
     * 
     * @param projectPath Full path where project creation was attempted
     * @param projectName Name of the project
     * @param errorMessage Description of what went wrong
     */
    protected open fun handleCreationError(
        projectPath: String,
        projectName: String,
        errorMessage: String
    ) {
    }
}

