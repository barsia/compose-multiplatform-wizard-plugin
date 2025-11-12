package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import org.jetbrains.jewel.foundation.theme.JewelTheme

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WizardMainContent(
    state: WizardState,
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectIdState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameFocused: Boolean,
    projectPathFocused: Boolean,
    projectIdFocused: Boolean,
    projectNameInteractionSource: MutableInteractionSource,
    projectPathInteractionSource: MutableInteractionSource,
    projectIdInteractionSource: MutableInteractionSource,
    mainPanel: ComposePanel,
    onBrowseFolder: () -> String?
) {
    val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
    val librariesState = rememberLibrariesState(cache, state)
    
    val shouldShowOptionalHotReload = remember(state.desktop, state.composeVersion) {
        state.desktop && isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01")
    }
    
    val shouldShowNavigation = remember(state.composeVersion) {
        val numericVersion = state.composeVersion.split("-").first().split("+").first()
        isComposeVersionLessThan(numericVersion, "1.10.0")
    }
    
    val shouldShowNavigation3AndNavigationEvent = remember(state.composeVersion) {
        val numericVersion = state.composeVersion.split("-").first().split("+").first()
        !isComposeVersionLessThan(numericVersion, "1.10.0") && state.composeVersion.isNotEmpty()
    }
    
    val shouldShowBundledHotReload = remember(state.desktop, state.composeVersion) {
        state.desktop && !isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01") && state.composeVersion.isNotEmpty()
    }
    
    LaunchedEffect(shouldShowOptionalHotReload, shouldShowBundledHotReload) {
        if (shouldShowOptionalHotReload) {
            state.hotReloadVersion = io.github.heisiar.composewizard.shared.ComposeVersions.COMPOSE_HOT_RELOAD_VERSION
        } else if (!shouldShowBundledHotReload) {
            state.hotReloadVersion = null
            state.includeHotReload = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(min = 220.dp)
                .verticalScroll(rememberScrollState())
                .padding(if (PlatformDetector.isAndroidStudio) 16.dp else 0.dp)
        ) {
            if (PlatformDetector.isAndroidStudio) {
                AndroidStudioProjectFields(
                    state = state,
                    projectNameState = projectNameState,
                    projectPathState = projectPathState,
                    projectNameFocused = projectNameFocused,
                    projectPathFocused = projectPathFocused,
                    projectNameInteractionSource = projectNameInteractionSource,
                    projectPathInteractionSource = projectPathInteractionSource,
                    mainPanel = mainPanel,
                    onBrowseFolder = onBrowseFolder
                )
            } else {
                IntellijIdeaProjectFields(
                    state = state,
                    projectIdState = projectIdState,
                    projectIdFocused = projectIdFocused,
                    projectIdInteractionSource = projectIdInteractionSource
                )
            }

            PlatformsSection(
                desktop = state.desktop,
                android = state.android,
                ios = state.ios,
                web = state.web,
                onDesktopToggle = {
                    state.desktop = !state.desktop
                    ComposeWizardUsageCollector.logPlatformToggled("Desktop", state.desktop)
                },
                onAndroidToggle = {
                    state.android = !state.android
                    ComposeWizardUsageCollector.logPlatformToggled("Android", state.android)
                },
                onIosToggle = {
                    state.ios = !state.ios
                    ComposeWizardUsageCollector.logPlatformToggled("iOS", state.ios)
                },
                onWebToggle = {
                    state.web = !state.web
                    ComposeWizardUsageCollector.logPlatformToggled("Web", state.web)
                }
            )

            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))

            LibrariesSection(
                state = state,
                librariesState = librariesState,
                cache = cache,
                shouldShowOptionalHotReload = shouldShowOptionalHotReload,
                shouldShowBundledHotReload = shouldShowBundledHotReload,
                shouldShowNavigation = shouldShowNavigation,
                shouldShowNavigation3AndNavigationEvent = shouldShowNavigation3AndNavigationEvent
            )
            
            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS * 1.5f))

            OptionsSection(
                git = state.git,
                tests = state.tests,
                onGitToggle = {
                    state.git = !state.git
                    ComposeWizardUsageCollector.logGitToggled(state.git)
                },
                onTestsToggle = {
                    state.tests = !state.tests
                    ComposeWizardUsageCollector.logTestsToggled(state.tests)
                }
            )
        }

        WizardFooter(state = state)
    }
}
