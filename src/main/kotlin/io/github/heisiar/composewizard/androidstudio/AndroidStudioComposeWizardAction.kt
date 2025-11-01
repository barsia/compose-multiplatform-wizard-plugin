package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.heisiar.composewizard.idea.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.idea.ComposeMultiplatformWizardStep
import com.intellij.openapi.ui.DialogWrapper
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
            // Project will be created by the builder
            println("=== Creating Compose Multiplatform Project ===")
            println("Name: ${builder.projectName}")
            println("Package: ${builder.projectId}")
            println("Platforms: Android=${builder.targetAndroid}, iOS=${builder.targetIOS}, Desktop=${builder.targetDesktop}, Web=${builder.targetWeb}")
        }
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
        // Let DialogWrapper determine size automatically based on content (same as IDEA)
        return wizardStep.component
    }
    
    override fun doOKAction() {
        wizardStep.updateDataModel()
        super.doOKAction()
    }
}

