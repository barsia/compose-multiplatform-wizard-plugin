package io.github.heisiar.composewizard.android.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import io.github.heisiar.composewizard.shared.TemplateProcessor
import io.github.heisiar.composewizard.shared.ValidationUtils
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
            val settings = dialog.getSettings()
            createProject(settings)
        }
    }
    
    private fun createProject(settings: ProjectSettings) {
        println("=== Creating Compose Multiplatform Project ===")
        println("Name: ${settings.projectName}")
        println("Location: ${settings.projectLocation}")
        println("Package: ${settings.packageName}")
        println("Platforms: iOS=${settings.includeIos}, Desktop=${settings.includeDesktop}, Web=${settings.includeWeb}")
        
        // Validation
        val validation = ValidationUtils.validateProjectId(settings.packageName)
        if (!validation.isValid) {
            println("ERROR: ${validation.errors.first()}")
            return
        }
        
        // Create project directory
        val projectDir = File(settings.projectLocation, settings.projectName)
        if (!projectDir.mkdirs()) {
            println("ERROR: Cannot create project directory")
            return
        }
        
        // Use TemplateProcessor to generate project
        val processor = TemplateProcessor(
            projectName = settings.projectName,
            projectId = settings.packageName,
            composeVersion = "1.10.0-alpha03",
            includeTests = false,
            targetDesktop = settings.includeDesktop,
            targetAndroid = true, // Always true in AS
            targetIOS = settings.includeIos,
            targetWeb = settings.includeWeb,
            enableDevVersions = false
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

data class ProjectSettings(
    val projectName: String,
    val projectLocation: String,
    val packageName: String,
    val includeIos: Boolean,
    val includeDesktop: Boolean,
    val includeWeb: Boolean
)

private class ComposeMultiplatformDialog : DialogWrapper(null, true) {
    
    private val composeUI = ComposeMultiplatformDialogUI()
    
    init {
        title = "New Compose Multiplatform Project"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return composeUI.getComponent()
    }
    
    fun getSettings(): ProjectSettings {
        return composeUI.getSettings()
    }
    
    override fun doValidate(): com.intellij.openapi.ui.ValidationInfo? {
        val settings = composeUI.getSettings()
        
        // Validate project name
        if (settings.projectName.isEmpty()) {
            return com.intellij.openapi.ui.ValidationInfo("Project name cannot be empty")
        }
        
        // Validate package
        val validation = ValidationUtils.validateProjectId(settings.packageName)
        if (!validation.isValid) {
            return com.intellij.openapi.ui.ValidationInfo(validation.errors.first())
        }
        
        // Validate at least one platform
        if (!settings.includeIos && !settings.includeDesktop && !settings.includeWeb) {
            return com.intellij.openapi.ui.ValidationInfo("At least one platform must be selected")
        }
        
        return null
    }
}

// Simple Swing version for now - will add Compose UI later
private class ComposeMultiplatformDialogUI {
    
    private val panel = javax.swing.JPanel(java.awt.BorderLayout())
    
    private val projectNameField = javax.swing.JTextField("MyComposeApp", 30)
    private val projectLocationField = javax.swing.JTextField(System.getProperty("user.home") + "/AndroidStudioProjects", 30)
    private val packageNameField = javax.swing.JTextField("com.example.myapp", 30)
    
    private val iosCheckbox = javax.swing.JCheckBox("Include iOS", true)
    private val desktopCheckbox = javax.swing.JCheckBox("Include Desktop", true)
    private val webCheckbox = javax.swing.JCheckBox("Include Web", false)
    
    init {
        val formPanel = javax.swing.JPanel()
        formPanel.layout = javax.swing.BoxLayout(formPanel, javax.swing.BoxLayout.Y_AXIS)
        
        formPanel.add(createRow("Project name:", projectNameField))
        formPanel.add(javax.swing.Box.createVerticalStrut(10))
        formPanel.add(createRow("Location:", projectLocationField))
        formPanel.add(javax.swing.Box.createVerticalStrut(10))
        formPanel.add(createRow("Package name:", packageNameField))
        formPanel.add(javax.swing.Box.createVerticalStrut(20))
        
        formPanel.add(javax.swing.JLabel("Target platforms:"))
        formPanel.add(javax.swing.Box.createVerticalStrut(5))
        formPanel.add(iosCheckbox)
        formPanel.add(desktopCheckbox)
        formPanel.add(webCheckbox)
        
        panel.add(formPanel, java.awt.BorderLayout.CENTER)
    }
    
    private fun createRow(label: String, field: javax.swing.JTextField): javax.swing.JPanel {
        val row = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        row.add(javax.swing.JLabel(label))
        row.add(field)
        return row
    }
    
    fun getComponent(): JComponent = panel
    
    fun getSettings(): ProjectSettings {
        return ProjectSettings(
            projectName = projectNameField.text,
            projectLocation = projectLocationField.text,
            packageName = packageNameField.text,
            includeIos = iosCheckbox.isSelected,
            includeDesktop = desktopCheckbox.isSelected,
            includeWeb = webCheckbox.isSelected
        )
    }
}

