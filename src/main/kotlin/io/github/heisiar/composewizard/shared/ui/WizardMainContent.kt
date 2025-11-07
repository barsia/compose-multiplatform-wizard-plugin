package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Text
import java.awt.Cursor

private val SPACING_BETWEEN_SECTIONS = 16.dp
private val LOCATION_SECTION_VERTICAL_OFFSET = 4.dp
private val TEXTFIELD_VERTICAL_OFFSET = 8.dp
private val TEXTFIELD_HEIGHT_REDUCTION = 16.dp

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
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            ProjectInfoSection(
                state = state,
                projectNameState = projectNameState,
                projectNameFocused = projectNameFocused,
                projectNameInteractionSource = projectNameInteractionSource,
                mainPanel = mainPanel
            )

            Spacer(
                modifier = Modifier.height(
                    if (state.devCheckboxVisible) 0.dp else 24.dp
                )
            )

            Box(modifier = Modifier.offset(y = (-LOCATION_SECTION_VERTICAL_OFFSET))) {
                ProjectLocationSection(
                    state = state,
                    projectPathState = projectPathState,
                    projectPathFocused = projectPathFocused,
                    projectPathInteractionSource = projectPathInteractionSource,
                    onBrowseFolder = onBrowseFolder
                )
            }

            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))

            PackageNameField(
                projectIdState = projectIdState,
                projectIdError = state.projectIdError,
                projectIdFocused = projectIdFocused,
                projectIdInteractionSource = projectIdInteractionSource,
                onIdChanged = { }
            )

            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))

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

        FooterSection(state = state)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProjectInfoSection(
    state: WizardState,
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameFocused: Boolean,
    projectNameInteractionSource: MutableInteractionSource,
    mainPanel: ComposePanel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProjectNameField(
            projectNameState = projectNameState,
            projectNameError = state.projectNameError,
            projectNameWarning = state.projectNameWarning,
            projectNameFocused = projectNameFocused,
            projectNameInteractionSource = projectNameInteractionSource,
            mainPanel = mainPanel,
            onNameChanged = { },
            modifier = Modifier.weight(2f)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComposeVersionField(
                cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance(),
                enableDevVersions = state.enableDevVersions,
                onVersionSelected = { state.composeVersion = it }
            )

            if (state.devCheckboxVisible) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
            ) {
                Checkbox(
                    checked = state.enableDevVersions,
                    onCheckedChange = {
                        state.enableDevVersions = it
                            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = it
                        ComposeWizardUsageCollector.logDevVersionsToggled(it)
                    }
                )
                    Text(
                        text = "Dev maven",
                        style = JewelTheme.defaultTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                state.enableDevVersions = !state.enableDevVersions
                                io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = state.enableDevVersions
                                ComposeWizardUsageCollector.logDevVersionsToggled(state.enableDevVersions)
                            }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProjectLocationSection(
    state: WizardState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathFocused: Boolean,
    projectPathInteractionSource: MutableInteractionSource,
    onBrowseFolder: () -> String?
) {
    Column {
        Text("Location", style = JewelTheme.defaultTextStyle)
        
        Box(modifier = Modifier.compactVerticalSpacing()) {
            ProjectLocationField(
                projectPathState = projectPathState,
                projectPathError = state.projectPathError,
                projectLocationWarning = state.projectLocationWarning,
                projectPathFocused = projectPathFocused,
                projectPathInteractionSource = projectPathInteractionSource,
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

private fun Modifier.compactVerticalSpacing(): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val verticalOffset = TEXTFIELD_VERTICAL_OFFSET.roundToPx()
    val heightReduction = TEXTFIELD_HEIGHT_REDUCTION.roundToPx()
    
    layout(placeable.width, placeable.height - heightReduction) {
        placeable.place(0, -verticalOffset)
    }
}

@Composable
private fun FooterSection(state: WizardState) {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "v1.0.0",
            style = JewelTheme.defaultTextStyle,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 500) {
                        clickCount++
                        if (clickCount == 2) { // third click (0, 1, 2)
                            val isInternalMode = com.intellij.openapi.application.ApplicationManager
                                .getApplication().isInternal
                            
                            // Easter egg only works in Regular Mode (not in Internal Mode)
                            if (!isInternalMode) {
                                val settings = io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance()
                                if (state.devCheckboxVisible) {
                                    // Hide (but keep current enabled/disabled state)
                                    state.devCheckboxVisible = false
                                    settings.devCheckboxVisible = false
                                } else {
                                    // Show (with current enabled/disabled state)
                                    state.devCheckboxVisible = true
                                    settings.devCheckboxVisible = true
                                }
                            }
                            clickCount = 0
                        }
                    } else {
                        clickCount = 0
                    }
                    lastClickTime = currentTime
                }
        )

        if (state.hasNoTargets) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = "At least one platform must be selected",
                        color = JewelTheme.globalColors.text.error,
                        style = JewelTheme.defaultTextStyle
                    )
                }
                Text(
                    text = "⚡",
                    fontSize = 14.sp,
                    color = JewelTheme.globalColors.text.error
                )
            }
        }
    }
}

