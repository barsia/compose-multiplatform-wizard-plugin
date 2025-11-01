package io.github.heisiar.composewizard.android.actions

import androidx.compose.runtime.mutableStateOf
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import io.github.heisiar.composewizard.shared.TemplateProcessor
import io.github.heisiar.composewizard.shared.ValidationUtils
import io.github.heisiar.composewizard.shared.ui.WizardData
import io.github.heisiar.composewizard.shared.ui.createComposeWizardPanel
import java.io.File
import javax.swing.JComponent

class NewComposeMultiplatformProjectAction : AnAction(
    "Compose Multiplatform Project",
    "Create a new Compose Multiplatform project with Compose UI wizard",
    null
) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = ComposeMultiplatformDialog()
        if (dialog.showAndGet()) {
            val data = dialog.getData()
            createProject(data)
        }
    }
    
    private fun createProject(data: WizardData) {
        println("=== Creating Compose Multiplatform Project ===")
        println("Name: ${data.projectName}")
        println("Location: ${data.projectLocation}")
        println("Package: ${data.packageName}")
        println("Platforms: Android=${data.includeAndroid}, iOS=${data.includeIos}, Desktop=${data.includeDesktop}, Web=${data.includeWeb}")
        
        // Validation
        val validation = ValidationUtils.validateProjectId(data.packageName)
        if (!validation.isValid) {
            println("ERROR: ${validation.errors.first()}")
            return
        }
        
        // Create project directory
        val projectDir = File(data.projectLocation, data.projectName)
        if (!projectDir.mkdirs()) {
            println("ERROR: Cannot create project directory")
            return
        }
        
        // Use TemplateProcessor to generate project
        val processor = TemplateProcessor(
            projectName = data.projectName,
            projectId = data.packageName,
            composeVersion = data.composeVersion,
            includeTests = data.includeTests,
            targetDesktop = data.includeDesktop,
            targetAndroid = data.includeAndroid,
            targetIOS = data.includeIos,
            targetWeb = data.includeWeb,
            enableDevVersions = data.enableDevVersions
        )
        
        try {
            processor.copyTemplateToProject(projectDir.absolutePath)
            println("=== Project created successfully at ${projectDir.absolutePath} ===")
            
            // TODO: Open project in AS
            // ProjectUtil.openOrImport(projectDir.toPath(), null, true)
        } catch (ex: Exception) {
            println("ERROR: ${ex.message}")
            ex.printStackTrace()
        }
    }
}

private class ComposeMultiplatformDialog : DialogWrapper(null, true) {
    
    private val wizardDataState = mutableStateOf(
        WizardData(
            projectLocation = System.getProperty("user.home") + "/AndroidStudioProjects",
            includeAndroid = true // Always true in AS
        )
    )
    
    init {
        title = "New Compose Multiplatform Project"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        // Use shared Compose UI!
        return createComposeWizardPanel(
            data = wizardDataState,
            showAndroidOption = false // In AS, Android is always included
        )
    }
    
    fun getData(): WizardData {
        return wizardDataState.value
    }
    
    override fun doValidate(): ValidationInfo? {
        val data = wizardDataState.value
        
        // Validate project name
        if (data.projectName.isBlank()) {
            return ValidationInfo("Project name cannot be empty")
        }
        
        // Validate package
        val validation = ValidationUtils.validateProjectId(data.packageName)
        if (!validation.isValid) {
            return ValidationInfo(validation.errors.first())
        }
        
        // Validate at least one platform
        if (!data.isValid()) {
            return ValidationInfo("At least one platform must be selected")
        }
        
        return null
    }
}

