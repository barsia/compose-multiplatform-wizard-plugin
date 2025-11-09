package io.github.heisiar.composewizard.shared.ui

/**
 * Main wizard step implementation for Compose Multiplatform project creation.
 * 
 * This class serves as the bridge between IntelliJ Platform's wizard system and the Compose UI:
 * - Extends ModuleWizardStep to integrate with IDEA/AS wizard framework
 * - Uses ComposePanel to embed Jetpack Compose UI in Swing
 * - Manages wizard state and validation
 * - Coordinates with ComposeMultiplatformModuleBuilder for project creation
 * 
 * UI Architecture:
 * - ComposeWizardStep (this file) - Wizard step integration and state management
 * - WizardMainContent - Main UI layout composition
 * - WizardUIFields - Reusable UI components (fields, checkboxes, platform selectors)
 * - WizardStateManager - State management and validation logic
 */

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposePanel
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.util.ui.UIUtil
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ComposeWizardStep(
    private val builder: ComposeMultiplatformModuleBuilder
) : ModuleWizardStep() {

    init {
        println("===== ComposeWizardStep: init() called =====")
        println("===== Using Android Studio Module Wizard (ModuleWizardStep) =====")
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
    private var includeNavigationEvent = false
    private var includeSavedState = false
    private var includeWindow = false
    private var includeHotReload = false
    
    private val wizardStartTime = System.currentTimeMillis()
    
    // Trigger for re-validation when component becomes visible
    private var revalidationTrigger = androidx.compose.runtime.mutableIntStateOf(0)

    private val mainPanel: ComposePanel by lazy {
        ComposePanel().apply {
            preferredSize = Dimension(500, 450)
            minimumSize = Dimension(500, 450)
            maximumSize = Dimension(800, 600)
            setContent {
                org.jetbrains.jewel.bridge.theme.SwingBridgeTheme {
                    CreateComposeUI()
                }
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    private fun CreateComposeUI() {
        val projectNameState = androidx.compose.foundation.text.input.rememberTextFieldState(projectNameValue)
        val projectPathState = androidx.compose.foundation.text.input.rememberTextFieldState(projectPathValue)
        val projectIdState = androidx.compose.foundation.text.input.rememberTextFieldState(projectIdValue)
        
        val settings = io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance()
        val isInternalMode = com.intellij.openapi.application.ApplicationManager.getApplication().isInternal
        
        // Determine dev checkbox visibility and enableDevVersions FIRST
        val devCheckboxVisible = isInternalMode || settings.devCheckboxVisibleByUser
        val enableDevVersions = if (devCheckboxVisible && !settings.enableDevVersionsSetByUser) {
            true
        } else {
            settings.enableDevVersions
        }
        
        // Now get versions from the correct list based on enableDevVersions
        val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
        val cachedVersions = if (enableDevVersions) cache.getDevVersions() else cache.getStableVersions()
        
        // Use saved value if it exists in current list, otherwise use first from list OR empty if cache is empty (will load from GitHub)
        val initialComposeVersion = if (composeVersionValue.isNotEmpty() && cachedVersions?.contains(composeVersionValue) == true) {
            // Saved value exists in current list - use it
            composeVersionValue
        } else if (cachedVersions != null) {
            // Cache has versions - use first from list (synced with dropdown)
            cachedVersions.firstOrNull() ?: ""
        } else {
            // Cache is empty - wait for GitHub loading (don't use DEFAULT_VERSION yet)
            ""
        }
        
        println("DEBUG ComposeWizardStep: devCheckboxVisible=$devCheckboxVisible, enableDevVersions=$enableDevVersions, composeVersionValue='$composeVersionValue', cachedVersions=${cachedVersions?.take(3)}, initialComposeVersion='$initialComposeVersion'")
        
        val state = rememberWizardState().apply {
            projectName = projectNameValue
            projectPath = projectPathValue
            projectId = projectIdValue
            composeVersion = initialComposeVersion
            desktop = targetDesktop
            android = targetAndroid
            ios = targetIOS
            web = targetWeb
            git = initGit
            tests = includeTests
            this.devCheckboxVisible = devCheckboxVisible
            this.enableDevVersions = enableDevVersions
        }
        
        val projectNameInteractionSource = remember { MutableInteractionSource() }
        val projectPathInteractionSource = remember { MutableInteractionSource() }
        val projectIdInteractionSource = remember { MutableInteractionSource() }
        
        val projectNameFocused by projectNameInteractionSource.collectIsFocusedAsState()
        val projectPathFocused by projectPathInteractionSource.collectIsFocusedAsState()
        val projectIdFocused by projectIdInteractionSource.collectIsFocusedAsState()
        
        SetupValidation(
            state = state, 
            projectNameState = projectNameState, 
            projectPathState = projectPathState, 
            projectIdState = projectIdState, 
            builder = builder,
            revalidationTrigger = revalidationTrigger.intValue
        ) { isValid ->
            projectNameValue = state.projectName
            projectPathValue = state.projectPath
            projectIdValue = state.projectId
            composeVersionValue = state.composeVersion
            this@ComposeWizardStep.enableDevVersions = state.enableDevVersions
            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = state.enableDevVersions
            targetDesktop = state.desktop
            targetAndroid = state.android
            targetIOS = state.ios
            targetWeb = state.web
            initGit = state.git
            includeTests = state.tests
            this@ComposeWizardStep.includeMaterial3 = state.includeMaterial3
            this@ComposeWizardStep.includeMaterial3Adaptive = state.includeMaterial3Adaptive
            this@ComposeWizardStep.includeNavigation = state.includeNavigation
            this@ComposeWizardStep.includeNavigationEvent = state.includeNavigationEvent
            this@ComposeWizardStep.includeSavedState = state.includeSavedState
            this@ComposeWizardStep.includeWindow = state.includeWindow
            this@ComposeWizardStep.includeHotReload = state.includeHotReload
            updateButtonState(isValid)
        }
        
        SetupPathSynchronization(state, projectNameState, projectPathState, projectNameValue)
        SetupAnalytics(state, projectIdValue)

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
            mainPanel = mainPanel,
            onBrowseFolder = ::browseForFolder
        )
    }
    
    override fun getComponent(): JComponent = mainPanel
    
    fun triggerRevalidation() {
        println("ComposeWizardStep: triggerRevalidation() called, incrementing trigger")
        revalidationTrigger.intValue++
    }

    private fun browseForFolder(): String? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select Project Location"
        }

        val chosen = FileChooser.chooseFile(descriptor, null, null)
        return chosen?.path?.let { WizardPathUtils.collapsePath(it) }
    }

    private var createButton: JButton? = null
    private var lastButtonState: Boolean? = null

    override fun _init() {
        super._init()
        println("ComposeWizardStep: _init() called")
        SwingUtilities.invokeLater {
            updateButtonText()
            val isValid = validate()
            println("ComposeWizardStep: _init() - validate returned $isValid")
            updateButtonState(isValid)
        }
    }
    
    override fun updateStep() {
        super.updateStep()
        println("ComposeWizardStep: updateStep() called - re-validating")
        SwingUtilities.invokeLater {
            val isValid = validate()
            println("ComposeWizardStep: updateStep() - validate returned $isValid")
            updateButtonState(isValid)
        }
    }

    private fun updateButtonText() {
        val comp = component
        var parent = comp.parent
        while (parent != null) {
            val buttons = UIUtil.findComponentsOfType(parent as? JComponent ?: return, JButton::class.java)
            for (button in buttons) {
                if (button.text?.contains("Next") == true || button.text?.contains("OK") == true || button.text?.contains("Create") == true) {
                    button.text = com.intellij.openapi.util.text.StringUtil.replace(
                        IdeBundle.message("button.create"), "&", ""
                    )
                    button.mnemonic = KeyEvent.VK_C
                    createButton = button
                    return
                }
            }
            parent = parent.parent
        }
    }

    private fun updateButtonState(enabled: Boolean) {
        if (lastButtonState == false && enabled == true) {
            println("===== WARNING: Button transitioning from DISABLED to ENABLED =====")
            println("ComposeWizardStep: This might be the problem - button should stay disabled!")
            Thread.dumpStack()
        }
        println("ComposeWizardStep: updateButtonState called with enabled=$enabled (lastState=$lastButtonState)")
        lastButtonState = enabled
        SwingUtilities.invokeLater {
            if (createButton == null) {
                updateButtonText()
            }
            println("ComposeWizardStep: Setting createButton.isEnabled=$enabled, button=${createButton?.text}")
            createButton?.isEnabled = enabled
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
        println("ComposeWizardStep: validate() called")
        val nameError = WizardValidation.validateProjectName(projectNameValue)
        println("ComposeWizardStep: nameError=$nameError")
        if (nameError != null) {
            return false
        }
        
        val pathError = WizardValidation.validateProjectPath(projectPathValue, WizardPathUtils::expandPath)
        println("ComposeWizardStep: pathError=$pathError")
        if (pathError != null) {
            return false
        }
        
        val locationError = WizardValidation.validateProjectLocationBlocking(projectNameValue, projectPathValue, WizardPathUtils::expandPath)
        println("ComposeWizardStep: locationError=$locationError")
        if (locationError != null) {
            return false
        }
        
        val projectIdValidation = builder.validateProjectId(projectIdValue)
        println("ComposeWizardStep: projectIdValidation.isValid=${projectIdValidation.isValid}")
        if (!projectIdValidation.isValid) {
            return false
        }
        
        val hasTargets = targetDesktop || targetAndroid || targetIOS || targetWeb
        println("ComposeWizardStep: hasTargets=$hasTargets")
        if (!hasTargets) {
            return false
        }
        
        println("ComposeWizardStep: validate() returning true")
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
