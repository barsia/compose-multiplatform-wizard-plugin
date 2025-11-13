package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AndroidStudioProjectFields(
    state: WizardState,
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameFocused: Boolean,
    projectPathFocused: Boolean,
    projectNameInteractionSource: MutableInteractionSource,
    projectPathInteractionSource: MutableInteractionSource,
    projectNameFocusRequester: androidx.compose.ui.focus.FocusRequester,
    projectPathFocusRequester: androidx.compose.ui.focus.FocusRequester,
    mainPanel: ComposePanel,
    onBrowseFolder: () -> String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProjectNameField(
            projectNameState = projectNameState,
            projectNameError = state.projectNameError,
            projectLocationWarning = state.projectLocationWarning,
            projectNameFocused = projectNameFocused,
            projectNameInteractionSource = projectNameInteractionSource,
            projectNameFocusRequester = projectNameFocusRequester,
            mainPanel = mainPanel,
            onNameChanged = { },
            modifier = Modifier.weight(0.6f)
        )
        
        ComposeVersionFieldWrapper(state, modifier = Modifier.weight(0.4f))
    }

    Spacer(modifier = Modifier.height(SPACING_BEFORE_LOCATION))

    Box(modifier = Modifier.offset(y = (-LOCATION_SECTION_VERTICAL_OFFSET))) {
        ProjectLocationSection(
            state = state,
            projectPathState = projectPathState,
            projectPathFocused = projectPathFocused,
            projectPathInteractionSource = projectPathInteractionSource,
            projectPathFocusRequester = projectPathFocusRequester,
            onBrowseFolder = onBrowseFolder
        )
    }
    
    Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntellijIdeaProjectFields(
    state: WizardState,
    projectIdState: androidx.compose.foundation.text.input.TextFieldState,
    projectIdFocused: Boolean,
    projectIdInteractionSource: MutableInteractionSource,
    projectIdFocusRequester: androidx.compose.ui.focus.FocusRequester
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        PackageNameField(
            projectIdState = projectIdState,
            projectIdError = state.projectIdError,
            projectIdFocused = projectIdFocused,
            projectIdInteractionSource = projectIdInteractionSource,
            projectIdFocusRequester = projectIdFocusRequester,
            onIdChanged = { },
            modifier = Modifier.weight(0.5f)
        )
        
        ComposeVersionFieldWrapper(state, modifier = Modifier.weight(0.5f))
    }
    
    Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))
}

@Composable
fun ComposeVersionFieldWrapper(state: WizardState, modifier: Modifier = Modifier) {
    ComposeVersionField(
        cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance(),
        enableDevVersions = state.enableDevVersions,
        selectedVersion = state.composeVersion,
        onVersionSelected = { state.composeVersion = it },
        onRefreshVersions = {
            val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
            if (state.enableDevVersions) {
                cache.forceReloadDev()
            } else {
                cache.forceReloadStable()
            }
        },
        modifier = modifier,
        devCheckboxVisible = state.devCheckboxVisible,
        onDevVersionsToggle = { 
            state.enableDevVersions = it
            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersionsSetByUser = true
            ComposeWizardUsageCollector.logDevVersionsToggled(it)
        },
        state = state
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectLocationSection(
    state: WizardState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathFocused: Boolean,
    projectPathInteractionSource: MutableInteractionSource,
    projectPathFocusRequester: androidx.compose.ui.focus.FocusRequester,
    onBrowseFolder: () -> String?
) {
    Column {
        Text("Location", style = JewelTheme.defaultTextStyle)
        
        Box(modifier = Modifier.compactVerticalSpacing()) {
            ProjectLocationField(
                projectPathState = projectPathState,
                projectPathError = state.projectPathError,
                projectPathFocused = projectPathFocused,
                projectPathInteractionSource = projectPathInteractionSource,
                projectPathFocusRequester = projectPathFocusRequester,
                onPathChanged = { },
                onBrowse = {
                    onBrowseFolder()?.let { state.projectPath = it }
                }
            )
        }

        ProjectPathHint(
            projectPath = state.projectPath,
            projectName = state.projectName
        )
    }
}

