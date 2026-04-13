package io.github.barsia.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.focus.FocusRequester
import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.barsia.composewizard.shared.services.ComposeVersionCache

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WizardUIRoot(
    projectNameValue: String,
    projectPathValue: String,
    projectIdValue: String,
    composeVersionValue: String,
    targetDesktop: Boolean,
    targetAndroid: Boolean,
    targetIOS: Boolean,
    targetWeb: Boolean,
    initGit: Boolean,
    includeTests: Boolean,
    enableDevVersionsValue: Boolean,
    builder: ComposeMultiplatformModuleBuilder,
    revalidationTrigger: Int,
    mainPanel: ComposePanel,
    onBrowseFolder: () -> String?,
    onStateUpdate: (state: WizardState, isValid: Boolean) -> Unit
) {
    val projectNameState = androidx.compose.foundation.text.input.rememberTextFieldState(projectNameValue)
    val projectPathState = androidx.compose.foundation.text.input.rememberTextFieldState(projectPathValue)
    val projectIdState = androidx.compose.foundation.text.input.rememberTextFieldState(projectIdValue)
    
    val settings = io.github.barsia.composewizard.shared.settings.WizardSettings.getInstance()
    val isInternalMode = com.intellij.openapi.application.ApplicationManager.getApplication().isInternal
    
    val devCheckboxVisible = isInternalMode || settings.devCheckboxVisibleByUser
    val enableDevVersions = if (devCheckboxVisible && !settings.enableDevVersionsSetByUser) {
        false
    } else {
        settings.enableDevVersions
    }
    
    val cache = ComposeVersionCache.getInstance()
    val cachedVersions = if (enableDevVersions) cache.getDevVersions() else cache.getStableVersions()
    
    val initialComposeVersion = if (composeVersionValue.isNotEmpty() && cachedVersions?.contains(composeVersionValue) == true) {
        composeVersionValue
    } else if (cachedVersions != null) {
        cachedVersions.firstOrNull() ?: ""
    } else {
        ""
    }
    
    
    val state = rememberWizardState()
    
    LaunchedEffect(Unit) {
        state.projectName = projectNameValue
        state.projectPath = projectPathValue
        state.projectId = projectIdValue
        state.composeVersion = initialComposeVersion
        state.desktop = targetDesktop
        state.android = targetAndroid
        state.ios = targetIOS
        state.web = targetWeb
        state.git = initGit
        state.tests = includeTests
        state.devCheckboxVisible = devCheckboxVisible
        state.enableDevVersions = enableDevVersions
    }
    
    val projectNameInteractionSource = remember { MutableInteractionSource() }
    val projectPathInteractionSource = remember { MutableInteractionSource() }
    val projectIdInteractionSource = remember { MutableInteractionSource() }
    
    val projectNameFocused by projectNameInteractionSource.collectIsFocusedAsState()
    val projectPathFocused by projectPathInteractionSource.collectIsFocusedAsState()
    val projectIdFocused by projectIdInteractionSource.collectIsFocusedAsState()
    
    val projectNameFocusRequester = remember { FocusRequester() }
    val projectPathFocusRequester = remember { FocusRequester() }
    val projectIdFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        if (io.github.barsia.composewizard.shared.PlatformDetector.isAndroidStudio) {
            kotlinx.coroutines.delay(100)
            projectNameFocusRequester.requestFocus()
        }
    }
    
    SetupValidation(
        state = state, 
        projectNameState = projectNameState, 
        projectPathState = projectPathState, 
        projectIdState = projectIdState, 
        builder = builder,
        revalidationTrigger = revalidationTrigger
    ) { isValid ->
        onStateUpdate(state, isValid)
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
        projectNameFocusRequester = projectNameFocusRequester,
        projectPathFocusRequester = projectPathFocusRequester,
        projectIdFocusRequester = projectIdFocusRequester,
        mainPanel = mainPanel,
        onBrowseFolder = onBrowseFolder
    )
}

