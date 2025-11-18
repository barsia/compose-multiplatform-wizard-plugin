package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.GitRepositoryInitializer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.wizard.AbstractWizardIntegration
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File

/**
 * Android Studio specific wizard integration.
 * 
 * Handles AS-specific project setup:
 * - Opens project using ProjectManager.loadAndOpenProject()
 * - Uses GitRepositoryInitializer API for proper Git initialization (respects .gitignore)
 * - No additional setup needed before opening (AS handles Gradle sync automatically)
 */
class ASWizardIntegration : AbstractWizardIntegration() {
    
    private var openedProject: com.intellij.openapi.project.Project? = null
    private var shouldInitGit = false
    
    override fun performIdeSpecificSetup(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    ) {
        shouldInitGit = builder.initGit
    }
    
    override fun openProject(projectPath: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val projectManager = com.intellij.openapi.project.ex.ProjectManagerEx.getInstanceEx()
                val openTask = com.intellij.ide.impl.OpenProjectTask {
                    forceOpenInNewFrame = true
                    isNewProject = false
                    useDefaultProjectAsTemplate = false
                    beforeOpen = { project ->
                        // Configure project BEFORE opening (like Android Studio does)
                        val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectPath))
                        if (root != null) {
                            // Synchronous VFS refresh
                            root.refresh(false, true)
                            
                            // Configure Gradle and Git on EDT
                            configureProjectBeforeOpen(project, projectPath, root)
                        }
                        true
                    }
                }
                
                val newProject = projectManager.openProject(File(projectPath).toPath(), openTask)
                
                if (newProject != null) {
                    openedProject = newProject
                } else {
                    Messages.showErrorDialog(
                        "Failed to open the created project",
                        "Project Creation Error"
                    )
                }
            } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                Messages.showErrorDialog(
                    "Error opening project: ${e.message}",
                    "Project Creation Error"
                )
            }
        }
    }
    
    /**
     * Configure project in beforeOpen callback (like Android Studio does).
     * This runs on EDT before project is fully opened.
     */
    private fun configureProjectBeforeOpen(
        project: com.intellij.openapi.project.Project,
        projectPath: String,
        root: com.intellij.openapi.vfs.VirtualFile
    ) {
        val logFile = File(projectPath, "wizard-debug.log")
        logFile.writeText("CONFIG: Starting configuration\n")
        
        try {
            // Configure Gradle (like AS does in GradleProjectImporter.configureNewProject)
            val gradleSettings = GradleSettings.getInstance(project)
            val projectSettings = GradleProjectSettings()
            projectSettings.externalProjectPath = projectPath
            projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
            projectSettings.isResolveModulePerSourceSet = false
            
            logFile.appendText("CONFIG: Linking Gradle project\n")
            gradleSettings.linkProject(projectSettings)
            
            // Initialize Git
            if (shouldInitGit) {
                logFile.appendText("CONFIG: Initializing Git\n")
                GitRepositoryInitializer.getInstance()?.initRepository(project, root, true)
                logFile.appendText("CONFIG: Git initialized\n")
            }
            
            logFile.appendText("CONFIG: Configuration complete\n")
        } catch (e: Exception) {
            logFile.appendText("CONFIG ERROR: ${e.message}\n${e.stackTraceToString()}\n")
            e.printStackTrace()
        }
    }
    
    override fun initializeGit(projectPath: String) {
        // Git initialization is handled in openProject() for AS
        // This is necessary because GitRepositoryInitializer requires an open project
    }
    
    override fun handleCreationError(
        projectPath: String,
        projectName: String,
        errorMessage: String
    ) {
        super.handleCreationError(projectPath, projectName, errorMessage)
        
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                "Failed to create project '$projectName'\n\nReason: $errorMessage",
                "Project Creation Error"
            )
        }
    }
}

