package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeWizardStep
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent

/**
 * Action for creating Compose Multiplatform projects in Android Studio.
 * Uses the SAME beautiful Compose UI as in IntelliJ IDEA!
 */
class AndroidStudioComposeWizardAction : DumbAwareAction(
    "Compose Multiplatform Project",
    "Create a new Compose Multiplatform project with advanced Compose UI",
    null
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = PlatformDetector.isAndroidStudio
    }
    
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
        
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            event.project,
            "Creating Compose Multiplatform Project",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Creating project structure..."
                indicator.isIndeterminate = false
                    
                    indicator.fraction = 0.2
                    indicator.text = "Copying template files..."
                    
                // Use unified wizard integration
                val integration = ASWizardIntegration()
                val success = integration.createAndOpenProject(projectPath, projectName, builder)
                
                if (success) {
                                indicator.fraction = 1.0
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
    builder: ComposeMultiplatformModuleBuilder
) : DialogWrapper(null, true) {
    
    private val wizardStep = ComposeWizardStep(builder)
    
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
