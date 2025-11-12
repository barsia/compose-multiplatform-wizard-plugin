package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.WizardStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.styling.ComboBoxColors
import org.jetbrains.jewel.ui.component.styling.ComboBoxStyle
import org.jetbrains.jewel.ui.component.styling.LocalDefaultComboBoxStyle
import org.jetbrains.jewel.ui.component.styling.LocalTextFieldStyle
import java.awt.Cursor

internal fun formatBrowseShortcut(): String {
    val isMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)
    return if (isMac) {
        "⇧ ↩"
    } else {
        "Shift+Enter"
    }
}

@Composable
fun textFieldStyleComboBox(): ComboBoxStyle {
    val defaultStyle = LocalDefaultComboBoxStyle.current
    val defaultColors = defaultStyle.colors
    val defaultMetrics = defaultStyle.metrics
    val defaultIcons = defaultStyle.icons
    
    val textFieldStyle = LocalTextFieldStyle.current
    val textFieldBackground = textFieldStyle.colors.background
    
    return ComboBoxStyle(
        colors = ComboBoxColors(
            background = textFieldBackground,
            nonEditableBackground = textFieldBackground,
            backgroundDisabled = defaultColors.backgroundDisabled,
            backgroundFocused = textFieldBackground,
            backgroundPressed = defaultColors.backgroundPressed,
            backgroundHovered = defaultColors.backgroundHovered,
            content = defaultColors.content,
            contentDisabled = defaultColors.contentDisabled,
            contentFocused = defaultColors.contentFocused,
            contentPressed = defaultColors.contentPressed,
            contentHovered = defaultColors.contentHovered,
            border = defaultColors.border,
            borderDisabled = defaultColors.borderDisabled,
            borderFocused = defaultColors.borderFocused,
            borderPressed = defaultColors.borderPressed,
            borderHovered = defaultColors.borderHovered,
        ),
        metrics = defaultMetrics,
        icons = defaultIcons,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectNameField(
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameError: String?,
    projectLocationWarning: String?,
    projectNameFocused: Boolean,
    projectNameInteractionSource: MutableInteractionSource,
    projectNameFocusRequester: FocusRequester,
    mainPanel: ComposePanel,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Project Name", style = JewelTheme.defaultTextStyle)
        }

        LaunchedEffect(projectNameState.text.toString()) {
            onNameChanged(projectNameState.text.toString())
        }

        Box {
            TextField(
                state = projectNameState,
                placeholder = {
                    Text(
                        "Project Name",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(projectNameFocusRequester),
                outline = Outline.of(
                    warning = projectLocationWarning != null && projectNameError == null,
                    error = projectNameError != null
                ),
                interactionSource = projectNameInteractionSource
            )

            if (projectNameError != null && projectNameFocused) {
                androidx.compose.runtime.key(projectNameError, projectNameFocused) {
                    ValidationPopupJB(
                        mainPanel = mainPanel,
                        message = projectNameError,
                        isWarning = false
                    )
                }
            }
            
            if (projectLocationWarning != null && projectNameFocused && projectNameError == null) {
                androidx.compose.runtime.key(projectLocationWarning, projectNameFocused) {
                    ValidationPopupJB(
                        mainPanel = mainPanel,
                        message = projectLocationWarning,
                        isWarning = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackageNameField(
    projectIdState: androidx.compose.foundation.text.input.TextFieldState,
    projectIdError: String?,
    projectIdFocused: Boolean,
    projectIdInteractionSource: MutableInteractionSource,
    projectIdFocusRequester: FocusRequester,
    onIdChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Package Name", style = JewelTheme.defaultTextStyle)
        }

        LaunchedEffect(projectIdState.text.toString()) {
            onIdChanged(projectIdState.text.toString())
        }

        Box {
            TextField(
                state = projectIdState,
                placeholder = { Text(WizardStrings.PACKAGE_NAME_LABEL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(projectIdFocusRequester),
                outline = if (projectIdError != null) Outline.Error else Outline.None,
                interactionSource = projectIdInteractionSource
            )

            if (projectIdError != null && projectIdFocused) {
                ValidationPopup(
                    message = projectIdError,
                    isWarning = false
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectLocationField(
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathError: String?,
    projectPathFocused: Boolean,
    projectPathInteractionSource: MutableInteractionSource,
    projectPathFocusRequester: FocusRequester,
    onPathChanged: (String) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(projectPathState.text.toString()) {
        onPathChanged(projectPathState.text.toString())
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.Enter &&
                        keyEvent.isShiftPressed
                    ) {
                        onBrowse()
                        true
                    } else {
                        false
                    }
                }
        ) {
            TextField(
                state = projectPathState,
                placeholder = { Text("Location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(projectPathFocusRequester),
                outline = if (projectPathError != null) Outline.Error else Outline.None,
                interactionSource = projectPathInteractionSource
            )

            if (projectPathError != null && projectPathFocused) {
                ValidationPopup(
                    message = projectPathError,
                    isWarning = false
                )
            }
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        
        Tooltip(
            tooltip = { Text("Browse... (${formatBrowseShortcut()})") }
        ) {
            Box(
                modifier = Modifier
                    .focusable()
                    .onKeyEvent { keyEvent: KeyEvent ->
                        when {
                            (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar) && 
                            keyEvent.type == KeyEventType.KeyDown -> {
                                onBrowse()
                                true
                            }
                            else -> false
                        }
                    }
                    .background(
                        color = if (isHovered) JewelTheme.globalColors.text.selected.copy(alpha = 0.08f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                    .clickable(
                        indication = null,
                        interactionSource = interactionSource
                    ) {
                        onBrowse()
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val folderIcon = if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
                    FolderOutlineIcon
                } else {
                    FolderIcon
                }
                
                Icon(
                    imageVector = folderIcon,
                    contentDescription = "Browse folder",
                    tint = JewelTheme.globalColors.text.normal.copy(
                        alpha = if (isHovered) 0.85f else 0.75f
                    ),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

