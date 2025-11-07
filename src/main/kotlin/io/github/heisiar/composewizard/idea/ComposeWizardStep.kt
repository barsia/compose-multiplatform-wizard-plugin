package io.github.heisiar.composewizard.idea

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposePanel
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import io.github.heisiar.composewizard.shared.ui.SetupAnalytics
import io.github.heisiar.composewizard.shared.ui.SetupPathSynchronization
import io.github.heisiar.composewizard.shared.ui.SetupValidation
import io.github.heisiar.composewizard.shared.ui.WizardMainContent
import io.github.heisiar.composewizard.shared.ui.WizardPathUtils
import io.github.heisiar.composewizard.shared.ui.rememberWizardState
import javax.swing.JComponent

/**
 * Fully Jewel-based Compose Wizard for IntelliJ IDEA
 */
class ComposeWizardStep(parent: NewProjectWizardStep) 
    : AbstractNewProjectWizardStep(parent), NewProjectWizardBaseData {

    val builder = ComposeMultiplatformModuleBuilder()
    
    override val nameProperty: GraphProperty<String> = propertyGraph.property(
        WizardPathUtils.suggestUniqueName(WizardDefaults.PROJECT_NAME, WizardDefaults.getDefaultProjectPath())
    )
    override val pathProperty: GraphProperty<String> = propertyGraph.property(
        if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            WizardDefaults.findUniqueProjectLocation(WizardDefaults.PROJECT_NAME_DISPLAY, WizardDefaults.getDefaultProjectPath())
        } else {
            WizardDefaults.getDefaultProjectPath()
        }
    )
    
    override var name: String by nameProperty
    override var path: String by pathProperty
    
    private val validationProperty = propertyGraph.property(true)
    
    private val wizardStartTime = System.currentTimeMillis()
    
    private var projectIdValue = WizardDefaults.PACKAGE_NAME
    private var composeVersionValue = ""
    private var targetDesktop = WizardDefaults.TARGET_DESKTOP
    private var targetAndroid = WizardDefaults.TARGET_ANDROID
    private var targetIOS = WizardDefaults.TARGET_IOS
    private var targetWeb = WizardDefaults.TARGET_WEB
    private var initGit = WizardDefaults.INIT_GIT
    private var includeTests = WizardDefaults.INCLUDE_TESTS
    private var enableDevVersions = false
    
    private var mainPanel: ComposePanel? = null
    
    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    private fun CreateComposeUI() {
        val projectNameState = rememberTextFieldState(nameProperty.get())
        val projectPathState = rememberTextFieldState(pathProperty.get())
        val projectIdState = rememberTextFieldState(WizardDefaults.PACKAGE_NAME)
        
        val cachedVersions = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance().getStableVersions()
        val initialComposeVersion = if (cachedVersions.isNotEmpty()) cachedVersions.first() else ""
        
        val state = rememberWizardState().apply {
            projectName = nameProperty.get()
            projectPath = pathProperty.get()
            projectId = WizardDefaults.PACKAGE_NAME
            composeVersion = initialComposeVersion
            desktop = WizardDefaults.TARGET_DESKTOP
            android = WizardDefaults.TARGET_ANDROID
            ios = WizardDefaults.TARGET_IOS
            web = WizardDefaults.TARGET_WEB
            git = WizardDefaults.INIT_GIT
            tests = WizardDefaults.INCLUDE_TESTS
            
            val settings = io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance()
            enableDevVersions = settings.enableDevVersions
            devCheckboxVisible = settings.devCheckboxActivatedByUser
        }
        
        val projectNameInteractionSource = remember { MutableInteractionSource() }
        val projectPathInteractionSource = remember { MutableInteractionSource() }
        val projectIdInteractionSource = remember { MutableInteractionSource() }
        
        val projectNameFocused by projectNameInteractionSource.collectIsFocusedAsState()
        val projectPathFocused by projectPathInteractionSource.collectIsFocusedAsState()
        val projectIdFocused by projectIdInteractionSource.collectIsFocusedAsState()
        
        LaunchedEffect(state.projectName) {
            nameProperty.set(state.projectName)
        }
        
        LaunchedEffect(state.projectPath) {
            pathProperty.set(state.projectPath)
        }
        
        SetupValidation(
            state = state,
            projectNameState = projectNameState,
            projectPathState = projectPathState,
            projectIdState = projectIdState,
            builder = builder,
            onValidationChanged = { isValid ->
                projectIdValue = state.projectId
                composeVersionValue = state.composeVersion
                this@ComposeWizardStep.enableDevVersions = state.enableDevVersions
                targetDesktop = state.desktop
                targetAndroid = state.android
                targetIOS = state.ios
                targetWeb = state.web
                initGit = state.git
                includeTests = state.tests
                
                // Update validation property to trigger dialog validation
                validationProperty.set(isValid)
            }
        )
        
        SetupPathSynchronization(
            state = state,
            projectNameState = projectNameState,
            projectPathState = projectPathState,
            projectNameValue = nameProperty.get()
        )
        
        SetupAnalytics(
            state = state,
            projectIdValue = WizardDefaults.PACKAGE_NAME
        )
        
        WizardMainContent(
            state = state,
            projectNameState = projectNameState,
            projectPathState = projectPathState,
            projectIdState = projectIdState,
            projectNameFocused = projectNameFocused,
            projectPathFocused = projectPathFocused,
            projectIdFocused = projectIdFocused,
            projectNameInteractionSource = projectNameInteractionSource,
            projectPathInteractionSource = projectPathInteractionSource,
            projectIdInteractionSource = projectIdInteractionSource,
            onBrowseFolder = { browseForFolder() }
        )
    }
    
    init {
        data.putUserData(NewProjectWizardBaseData.KEY, this)
    }
    
    override fun setupUI(builder: com.intellij.ui.dsl.builder.Panel) {
        try {
            // Use BorderLayoutPanel like Jewel does - it handles sizes better
            val wrapperPanel = com.intellij.util.ui.components.BorderLayoutPanel()
            
            mainPanel = ComposePanel().apply {
                // Delay setContent until component is added and has valid size
                javax.swing.SwingUtilities.invokeLater {
                    setContent {
                        org.jetbrains.jewel.bridge.theme.SwingBridgeTheme {
                            CreateComposeUI()
                        }
                    }
                }
            }
            
            // Add to center layout
            wrapperPanel.add(mainPanel, java.awt.BorderLayout.CENTER)
            
            builder.row {
                cell(wrapperPanel)
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
                    .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                    .validationRequestor(com.intellij.openapi.ui.validation.WHEN_PROPERTY_CHANGED(validationProperty))
                    .validation(com.intellij.openapi.ui.validation.DialogValidation {
                        if (!validationProperty.get()) {
                            com.intellij.openapi.ui.ValidationInfo("Please fix validation errors")
                        } else {
                            null
                        }
                    })
            }.resizableRow()
        } catch (e: Exception) {
            com.intellij.openapi.diagnostic.Logger.getInstance(ComposeWizardStep::class.java)
                .error("Failed to initialize Compose UI", e)
                throw e
        }
    }
    
    private fun browseForFolder(): String? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select Project Location"
        }
        val chosen = FileChooser.chooseFile(descriptor, null, null)
        return chosen?.path?.let { WizardPathUtils.collapsePath(it) }
    }
    
    override fun setupProject(project: Project) {
        // Используем данные из GraphProperty
        val projectName = name
        val projectLocation = path
        
        builder.projectName = projectName
        builder.projectId = projectIdValue
        builder.composeVersion = composeVersionValue
        builder.targetDesktop = targetDesktop
        builder.targetAndroid = targetAndroid
        builder.targetIOS = targetIOS
        builder.targetWeb = targetWeb
        builder.initGit = initGit
        builder.includeTests = includeTests
        builder.enableDevVersions = enableDevVersions
        
        // Используем basePath проекта
        val projectPath = project.basePath ?: return
        
        val integration = IdeaWizardIntegration(project)
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        val timeSpentMs = System.currentTimeMillis() - wizardStartTime
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = listOf(
                builder.targetDesktop,
                builder.targetAndroid,
                builder.targetIOS,
                builder.targetWeb
            ).count { it },
            includeTests = builder.includeTests,
            includeGit = builder.initGit,
            usedDevVersions = builder.enableDevVersions,
            timeSpentMs = timeSpentMs
        )
    }
}
