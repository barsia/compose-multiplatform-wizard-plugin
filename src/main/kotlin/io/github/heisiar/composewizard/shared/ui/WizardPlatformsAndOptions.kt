package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import java.awt.Cursor

@Composable
fun PlatformsSection(
    desktop: Boolean,
    android: Boolean,
    ios: Boolean,
    web: Boolean,
    onDesktopToggle: () -> Unit,
    onAndroidToggle: () -> Unit,
    onIosToggle: () -> Unit,
    onWebToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PlatformItem(
            checked = android,
            onToggle = onAndroidToggle,
            icon = {
                Icon(
                    key = WizardIconKeys.Android,
                    contentDescription = "Android",
                    modifier = Modifier.size(46.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            },
            label = "Android"
        )

        PlatformItem(
            checked = ios,
            onToggle = onIosToggle,
            icon = {
                Icon(
                    key = WizardIconKeys.Apple,
                    contentDescription = "iOS",
                    modifier = Modifier.size(46.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            },
            label = "iOS"
        )

        PlatformItem(
            checked = desktop,
            onToggle = onDesktopToggle,
            icon = {
                Icon(
                    key = WizardIconKeys.Desktop,
                    contentDescription = "Desktop",
                    modifier = Modifier.size(46.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            },
            label = "Desktop"
        )

        PlatformItem(
            checked = web,
            onToggle = onWebToggle,
            icon = {
                Icon(
                    key = WizardIconKeys.Web,
                    contentDescription = "Web",
                    modifier = Modifier.size(46.dp),
                    tint = JewelTheme.globalColors.text.normal
                )
            },
            label = "Web"
        )
    }
}

@Composable
private fun PlatformItem(
    checked: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    // Reduced spacing for IntelliJ IDEA to fit everything without scrolling
    val spacing = if (io.github.heisiar.composewizard.shared.PlatformDetector.isIntellijIdea) 2.dp else 4.dp
    val padding = if (io.github.heisiar.composewizard.shared.PlatformDetector.isIntellijIdea) 2.dp else 4.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.padding(padding)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 48.dp)
                .focusProperties { canFocus = false }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() }
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                .graphicsLayer {
                    alpha = if (checked) 1f else 0.3f
                },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            style = JewelTheme.defaultTextStyle,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() }
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
        )
    }
}

@Composable
fun OptionsSection(
    git: Boolean,
    tests: Boolean,
    onGitToggle: () -> Unit,
    onTestsToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            // In Android Studio, show both Git and Tests checkboxes side by side
            CheckboxOption(
                checked = git,
                onToggle = onGitToggle,
                label = "Create Git repository",
                modifier = Modifier.weight(1f)
            )
            
            CheckboxOption(
                checked = tests,
                onToggle = onTestsToggle,
                label = "Add sample tests",
                modifier = Modifier.weight(1f)
            )
        } else {
            // In IntelliJ IDEA, Git checkbox is provided by GitNewProjectWizardStep (above)
            // Show only Tests checkbox, taking full width
            CheckboxOption(
                checked = tests,
                onToggle = onTestsToggle,
                label = "Add sample tests",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckboxOption(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconKey: org.jetbrains.jewel.ui.icon.IconKey? = null,
    useColoredIcon: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    disabledTooltip: String = "Included in the base template and cannot be disabled"
) {
    val content = @Composable {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            org.jetbrains.jewel.ui.component.CheckboxRow(
                checked = checked,
                onCheckedChange = { onToggle() },
                enabled = enabled,
                modifier = Modifier
                    .pointerHoverIcon(
                        if (enabled) PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                        else PointerIcon(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconKey != null) {
                        Icon(
                            key = iconKey,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp),
                            tint = if (useColoredIcon) {
                                Color.Unspecified
                            } else if (enabled) {
                                JewelTheme.globalColors.text.normal
                            } else {
                                JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = label,
                        style = JewelTheme.defaultTextStyle,
                        color = if (enabled) JewelTheme.globalColors.text.normal 
                                else JewelTheme.globalColors.text.normal.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(6.dp))
            trailingContent()
        }
    }
    }
    
    if (!enabled) {
        Tooltip(
            tooltip = { Text(disabledTooltip) },
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.TopCenter,
                alignment = Alignment.TopCenter
            )
        ) {
            content()
        }
    } else {
        content()
    }
}

