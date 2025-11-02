package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import io.github.heisiar.composewizard.shared.ProjectCreator
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.wizard.AbstractWizardIntegration

/**
 * Android Studio specific wizard integration.
 * 
 * Handles AS-specific project setup:
 * - Opens project using ProjectManager.loadAndOpenProject()
 * - Uses command-line git init for Git initialization
 * - No additional setup needed before opening (AS handles Gradle sync automatically)
 */
class ASWizardIntegration : AbstractWizardIntegration() {
    
    override fun performIdeSpecificSetup(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    ) {
        println("ASWizardIntegration: No additional setup needed, AS will auto-sync Gradle")
    }
    
    override fun openProject(projectPath: String) {
        println("ASWizardIntegration: Opening project via ProjectManager")
        
        ApplicationManager.getApplication().invokeLater {
            try {
                val projectManager = ProjectManager.getInstance()
                val newProject = projectManager.loadAndOpenProject(projectPath)
                
                if (newProject != null) {
                    println("ASWizardIntegration: Project opened successfully")
                } else {
                    println("ERROR: ProjectManager.loadAndOpenProject returned null")
                    Messages.showErrorDialog(
                        "Failed to open the created project",
                        "Project Creation Error"
                    )
                }
            } catch (e: Exception) {
                println("ERROR: Exception opening project: ${e.message}")
                e.printStackTrace()
                Messages.showErrorDialog(
                    "Error opening project: ${e.message}",
                    "Project Creation Error"
                )
            }
        }
    }
    
    override fun initializeGit(projectPath: String) {
        println("ASWizardIntegration: Initializing Git via command line")
        ProjectCreator.initializeGitRepository(projectPath)
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

