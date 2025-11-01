package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import io.github.heisiar.composewizard.idea.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.idea.ComposeMultiplatformWizardStep
import io.github.heisiar.composewizard.shared.TemplateProcessor
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent

/**
 * Action for creating Compose Multiplatform projects in Android Studio.
 * Uses the SAME beautiful Compose UI as in IntelliJ IDEA!
 */
class AndroidStudioComposeWizardAction : AnAction(
    "Compose Multiplatform Project",
    "Create a new Compose Multiplatform project with advanced Compose UI",
    null
) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val builder = ComposeMultiplatformModuleBuilder()
        val dialog = ComposeMultiplatformDialog(builder)
        
        if (dialog.showAndGet()) {
            // Create project in background thread
            createProject(builder, e)
        }
    }
    
    private fun createProject(builder: ComposeMultiplatformModuleBuilder, event: AnActionEvent) {
        val projectName = builder.projectName
        val projectLocation = builder.contentEntryPath ?: return
        val projectPath = File(projectLocation).absolutePath
        
        println("=== Creating Compose Multiplatform Project ===")
        println("Name: $projectName")
        println("Location: $projectPath")
        println("Package: ${builder.projectId}")
        println("Platforms: Android=${builder.targetAndroid}, iOS=${builder.targetIOS}, Desktop=${builder.targetDesktop}, Web=${builder.targetWeb}")
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            event.project,
            "Creating Compose Multiplatform Project",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Creating project structure..."
                indicator.isIndeterminate = false
                
                try {
                    // Create TemplateProcessor with builder's settings
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
                    
                    indicator.fraction = 0.2
                    indicator.text = "Copying template files..."
                    
                    // Create project from template
                    processor.copyTemplateToProject(projectPath)
                    
                    indicator.fraction = 0.9
                    indicator.text = "Finalizing project..."
                    
                    // Open the project
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val projectManager = ProjectManager.getInstance()
                            val newProject = projectManager.loadAndOpenProject(projectPath)
                            
                            if (newProject != null) {
                                println("=== Project created successfully! ===")
                                indicator.fraction = 1.0
                            } else {
                                Messages.showErrorDialog(
                                    "Failed to open the created project",
                                    "Project Creation Error"
                                )
                            }
                        } catch (ex: Exception) {
                            Messages.showErrorDialog(
                                "Error opening project: ${ex.message}",
                                "Project Creation Error"
                            )
                            ex.printStackTrace()
                        }
                    }
                    
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            "Error creating project: ${ex.message}",
                            "Project Creation Error"
                        )
                    }
                    ex.printStackTrace()
                }
            }
        })
    }
}

/**
 * Dialog that uses the SAME Compose UI as IntelliJ IDEA.
 * Beautiful, consistent UI across both IDEs!
 */
private class ComposeMultiplatformDialog(
    private val builder: ComposeMultiplatformModuleBuilder
) : DialogWrapper(null, true) {
    
    private val wizardStep = ComposeMultiplatformWizardStep(builder)
    
    init {
        title = "New Compose Multiplatform Project"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        // Use the SAME advanced Compose UI from IDEA!
        return wizardStep.component.apply {
            // Standard wizard size for comfortable viewing
            preferredSize = Dimension(850, 650)
        }
    }
    
    override fun getPreferredSize(): Dimension {
        // Standard wizard dialog size
        return Dimension(850, 650)
    }
    
    override fun doOKAction() {
        wizardStep.updateDataModel()
        super.doOKAction()
    }
}

