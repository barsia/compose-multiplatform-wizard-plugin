package io.github.heisiar.composewizard.shared.ui

import androidx.compose.ui.awt.ComposePanel
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ComposeWizardStep(
    private val builder: ComposeMultiplatformModuleBuilder
) : ModuleWizardStep() {

    init {
    }

    private var projectNameValue = WizardPathUtils.suggestUniqueName(WizardDefaults.PROJECT_NAME, WizardDefaults.getDefaultProjectPath())

    private var projectPathValue = if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
        WizardDefaults.findUniqueProjectLocation(WizardDefaults.PROJECT_NAME_DISPLAY, WizardDefaults.getDefaultProjectPath())
    } else {
        WizardDefaults.getDefaultProjectPath()
    }
    
    private var projectIdValue = WizardDefaults.PACKAGE_NAME
    private var composeVersionValue = WizardDefaults.COMPOSE_VERSION
    private var initGit = WizardDefaults.INIT_GIT
    private var includeTests = WizardDefaults.INCLUDE_TESTS
    private var targetDesktop = WizardDefaults.TARGET_DESKTOP
    private var targetAndroid = WizardDefaults.TARGET_ANDROID
    private var targetIOS = WizardDefaults.TARGET_IOS
    private var targetWeb = WizardDefaults.TARGET_WEB
    private var enableDevVersions = WizardDefaults.ENABLE_DEV_VERSIONS
    
    private var includeMaterial3 = false
    private var includeMaterial3Adaptive = false
    private var includeNavigation = false
    private var includeNavigation3 = false
    private var includeNavigationEvent = false
    private var includeSavedState = false
    private var includeWindow = false
    private var includeHotReload = false
    
    private val wizardStartTime = System.currentTimeMillis()
    private var revalidationTrigger = androidx.compose.runtime.mutableIntStateOf(0)
    
    private val buttonManager by lazy { WizardButtonManager(mainPanel) }

    private val mainPanel: ComposePanel by lazy {
        ComposePanel().apply {
            preferredSize = Dimension(500, 450)
            minimumSize = Dimension(500, 450)
            maximumSize = Dimension(800, 600)
            setContent {
                org.jetbrains.jewel.bridge.theme.SwingBridgeTheme {
                    WizardUIRoot(
                        projectNameValue = projectNameValue,
                        projectPathValue = projectPathValue,
                        projectIdValue = projectIdValue,
                        composeVersionValue = composeVersionValue,
                        targetDesktop = targetDesktop,
                        targetAndroid = targetAndroid,
                        targetIOS = targetIOS,
                        targetWeb = targetWeb,
                        initGit = initGit,
                        includeTests = includeTests,
                        enableDevVersionsValue = enableDevVersions,
                        builder = builder,
                        revalidationTrigger = revalidationTrigger.intValue,
                        mainPanel = mainPanel,
                        onBrowseFolder = ::browseForFolder,
                        onStateUpdate = { state, isValid ->
                            syncStateFromUI(state)
                            buttonManager.updateButtonState(isValid)
                        }
                    )
                }
            }
        }
    }
    
    private fun syncStateFromUI(state: WizardState) {
        projectNameValue = state.projectName
        projectPathValue = state.projectPath
        projectIdValue = state.projectId
        composeVersionValue = state.composeVersion
        enableDevVersions = state.enableDevVersions
        io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = state.enableDevVersions
        targetDesktop = state.desktop
        targetAndroid = state.android
        targetIOS = state.ios
        targetWeb = state.web
        initGit = state.git
        includeTests = state.tests
        includeMaterial3 = state.includeMaterial3
        includeMaterial3Adaptive = state.includeMaterial3Adaptive
        includeNavigation = state.includeNavigation
        includeNavigation3 = state.includeNavigation3
        includeNavigationEvent = state.includeNavigationEvent
        includeSavedState = state.includeSavedState
        includeWindow = state.includeWindow
        includeHotReload = state.includeHotReload
    }
    
    override fun getComponent(): JComponent = mainPanel
    
    fun triggerRevalidation() {
        revalidationTrigger.intValue++
    }

    private fun browseForFolder(): String? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select Project Location"
        }

        val chosen = FileChooser.chooseFile(descriptor, null, null)
        return chosen?.path?.let { WizardPathUtils.collapsePath(it) }
    }

    override fun _init() {
        super._init()
        SwingUtilities.invokeLater {
            buttonManager.updateButtonText()
            val isValid = validate()
            buttonManager.updateButtonState(isValid)
        }
    }
    
    override fun updateStep() {
        super.updateStep()
        SwingUtilities.invokeLater {
            val isValid = validate()
            buttonManager.updateButtonState(isValid)
        }
    }

    override fun updateDataModel() {
        val expandedPath = WizardPathUtils.expandPath(projectPathValue)
        
        // In Android Studio, projectPath already includes project name
        // In IntelliJ IDEA, projectPath is just parent directory
        val fullPath = if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            expandedPath
        } else {
            File(expandedPath, projectNameValue).absolutePath
        }
        
        builder.contentEntryPath = fullPath
        builder.name = projectNameValue
        builder.moduleFilePath = "$fullPath/$projectNameValue.iml"
        builder.projectName = projectNameValue
        builder.projectId = projectIdValue
        builder.composeVersion = composeVersionValue
        builder.targetDesktop = targetDesktop
        builder.targetAndroid = targetAndroid
        builder.targetIOS = targetIOS
        builder.targetWeb = targetWeb
        builder.initGit = initGit
        builder.includeTests = includeTests
        builder.enableDevVersions = io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions
        builder.includeMaterial3 = includeMaterial3
        builder.includeMaterial3Adaptive = includeMaterial3Adaptive
        builder.includeNavigation = includeNavigation
        builder.includeNavigation3 = includeNavigation3
        builder.includeNavigationEvent = includeNavigationEvent
        builder.includeSavedState = includeSavedState
        builder.includeWindow = includeWindow
        builder.includeHotReload = includeHotReload

        val timeSpent = System.currentTimeMillis() - wizardStartTime
        val platformsCount = listOf(targetDesktop, targetAndroid, targetIOS, targetWeb).count { it }
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = platformsCount,
            includeTests = includeTests,
            includeGit = initGit,
            usedDevVersions = enableDevVersions,
            timeSpentMs = timeSpent
        )
    }

    override fun validate(): Boolean {
        val nameError = WizardValidation.validateProjectName(projectNameValue)
        if (nameError != null) {
            return false
        }
        
        val pathError = WizardValidation.validateProjectPath(projectPathValue, WizardPathUtils::expandPath)
        if (pathError != null) {
            return false
        }
        
        val locationError = WizardValidation.validateProjectLocationBlocking(projectNameValue, projectPathValue, WizardPathUtils::expandPath)
        if (locationError != null) {
            return false
        }
        
        val projectIdValidation = builder.validateProjectId(projectIdValue)
        if (!projectIdValidation.isValid) {
            return false
        }
        
        val hasTargets = targetDesktop || targetAndroid || targetIOS || targetWeb
        if (!hasTargets) {
            return false
        }
        
        return true
    }

    fun getBuilder(): ComposeMultiplatformModuleBuilder = builder
    fun getProjectName(): String = projectNameValue
    fun getProjectPath(): String = projectPathValue
    fun getProjectId(): String = projectIdValue
    fun getComposeVersion(): String = composeVersionValue
    
    // Setter for syncing compose version from platform fields (IntelliJ IDEA only)
    fun setComposeVersion(version: String) {
        composeVersionValue = version
    }
}
