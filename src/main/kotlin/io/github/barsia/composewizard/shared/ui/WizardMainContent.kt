package io.github.barsia.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.barsia.composewizard.shared.PlatformDetector
import io.github.barsia.composewizard.shared.analytics.AnalyticsLogger
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.painter.hints.HiDpi

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
    projectNameFocusRequester: androidx.compose.ui.focus.FocusRequester,
    projectPathFocusRequester: androidx.compose.ui.focus.FocusRequester,
    projectIdFocusRequester: androidx.compose.ui.focus.FocusRequester,
    mainPanel: ComposePanel,
    onBrowseFolder: () -> String?
) {
    val cache = io.github.barsia.composewizard.shared.services.ComposeVersionCache.getInstance()
    val librariesState = rememberLibrariesState(cache, state)
    
    val shouldShowOptionalHotReload = remember(state.desktop, state.composeVersion) {
        state.desktop && isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01")
    }
    
    val shouldShowNavigation = remember(state.composeVersion) {
        isComposeVersionLessThan(state.composeVersion, "1.10.0-alpha02")
    }
    
    val shouldShowNavigation3AndNavigationEvent = remember(state.composeVersion) {
        !isComposeVersionLessThan(state.composeVersion, "1.10.0-alpha02") && state.composeVersion.isNotEmpty()
    }
    
    val shouldShowBundledHotReload = remember(state.desktop, state.composeVersion) {
        state.desktop && !isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01") && state.composeVersion.isNotEmpty()
    }
    
    LaunchedEffect(shouldShowOptionalHotReload, shouldShowBundledHotReload) {
        if (shouldShowOptionalHotReload) {
            state.hotReloadVersion = io.github.barsia.composewizard.shared.ComposeVersions.COMPOSE_HOT_RELOAD_VERSION
        } else if (!shouldShowBundledHotReload) {
            state.hotReloadVersion = null
            // Don't reset includeHotReload here - let user control it via checkbox
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                    projectNameFocusRequester = projectNameFocusRequester,
                    projectPathFocusRequester = projectPathFocusRequester,
                    mainPanel = mainPanel,
                    onBrowseFolder = onBrowseFolder
                )
            } else {
                IntellijIdeaProjectFields(
                    state = state,
                    projectIdState = projectIdState,
                    projectIdFocused = projectIdFocused,
                    projectIdInteractionSource = projectIdInteractionSource,
                    projectIdFocusRequester = projectIdFocusRequester,
                    mainPanel = mainPanel
                )
            }

            PlatformsSection(
                desktop = state.desktop,
                android = state.android,
                ios = state.ios,
                web = state.web,
                onDesktopToggle = {
                    state.desktop = !state.desktop
                    AnalyticsLogger.logPlatformToggled("Desktop", state.desktop)
                },
                onAndroidToggle = {
                    state.android = !state.android
                    AnalyticsLogger.logPlatformToggled("Android", state.android)
                },
                onIosToggle = {
                    state.ios = !state.ios
                    AnalyticsLogger.logPlatformToggled("iOS", state.ios)
                },
                onWebToggle = {
                    state.web = !state.web
                    AnalyticsLogger.logPlatformToggled("Web", state.web)
                }
            )

            // Reduced spacing for IntelliJ IDEA to fit everything without scrolling
            Spacer(modifier = Modifier.height(
                if (PlatformDetector.isIntellijIdea) 2.dp
                else SPACING_BETWEEN_SECTIONS
            ))

            LibrariesSection(
                state = state,
                librariesState = librariesState,
                cache = cache,
                shouldShowOptionalHotReload = shouldShowOptionalHotReload,
                shouldShowBundledHotReload = shouldShowBundledHotReload,
                shouldShowNavigation = shouldShowNavigation,
                shouldShowNavigation3AndNavigationEvent = shouldShowNavigation3AndNavigationEvent
            )
            
            // Reduced spacing for IntelliJ IDEA (Git checkbox is at the top now)
            Spacer(modifier = Modifier.height(
                if (PlatformDetector.isIntellijIdea) 8.dp
                else SPACING_BETWEEN_SECTIONS * 1.5f
            ))

            OptionsSection(
                git = state.git,
                tests = state.tests,
                agentsMd = state.agentsMd,
                onGitToggle = {
                    state.git = !state.git
                    AnalyticsLogger.logGitToggled(state.git)
                },
                onTestsToggle = {
                    state.tests = !state.tests
                    AnalyticsLogger.logTestsToggled(state.tests)
                },
                onAgentsMdToggle = {
                    state.agentsMd = !state.agentsMd
                }
            )
            }

            WizardFooter(state = state)
        }
        
        // Watermark with Compose logo (color gradient from SVG)
        // Positioned so ~60% is visible, rest goes beyond screen edge
        Icon(
            key = WizardIconKeys.ComposeWatermark,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 0.dp, y = 135.dp)
                .size(250.dp)
                .alpha(0.075f),
            colorFilter = null,
            hint = HiDpi()
        )
    }
}
