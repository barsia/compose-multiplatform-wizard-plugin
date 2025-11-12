package io.github.heisiar.composewizard.shared.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.styling.LocalTextFieldStyle
import java.io.File
import java.io.InputStream

@Composable
fun PlatformCheckbox(
    checked: Boolean,
    label: String,
    onCheckedChange: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {},
            modifier = Modifier.size(20.dp),
            enabled = false
        )
        Text(label)
    }
}

internal object ResourceLoader {
    fun loadResource(path: String): InputStream? {
        return Thread.currentThread().contextClassLoader?.getResourceAsStream(path)
            ?: javaClass.classLoader?.getResourceAsStream(path)
    }
}

@Composable
internal fun CompactSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 46.dp
    val trackHeight = 16.dp
    val thumbSize = 12.dp
    val thumbPadding = 2.dp
    
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "thumbOffset"
    )
    
    val textFieldBackground = LocalTextFieldStyle.current.colors.background
    val trackColor = textFieldBackground
    
    val thumbColor = if (checked) {
        Color(0xFFF44336).copy(alpha = 0.75f)
    } else {
        Color(0xFF4CAF50).copy(alpha = 0.75f)
    }
    
    val textColor = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
    
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .background(
                color = trackColor,
                shape = RoundedCornerShape(trackHeight / 2)
            )
            .border(
                width = 0.5.dp,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                shape = RoundedCornerShape(trackHeight / 2)
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            }
            .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.Center)
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .width(trackWidth - thumbSize)
                        .height(trackHeight)
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dev",
                        fontSize = 8.sp,
                        color = textColor,
                        style = JewelTheme.defaultTextStyle
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(trackWidth - thumbSize)
                        .height(trackHeight)
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Stable",
                        fontSize = 8.sp,
                        color = textColor,
                        style = JewelTheme.defaultTextStyle
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(
                    x = thumbPadding + (trackWidth - thumbSize - thumbPadding * 2) * thumbOffset
                )
                .size(thumbSize)
                .background(
                    color = thumbColor,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun ProjectPathHint(projectPath: String, projectName: String, modifier: Modifier = Modifier) {
    val finalPath = if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
        WizardPathUtils.expandPath(projectPath)
    } else {
        File(WizardPathUtils.expandPath(projectPath), projectName).absolutePath
    }
    
    val displayPath = WizardPathUtils.collapsePath(finalPath)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "Project will be created at: $displayPath",
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
            )
        }
        
        if (!io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            Icon(
                key = org.jetbrains.jewel.ui.icons.AllIconsKeys.Actions.Copy,
                contentDescription = "Copy path",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                        val stringSelection = java.awt.datatransfer.StringSelection(finalPath)
                        clipboard.setContents(stringSelection, null)
                    }
                    .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedVersionIndicator(
    isPinned: Boolean = false
) {
    val tooltipText = if (isPinned) {
        "This version was not published with the selected Compose release"
    } else {
        "Could not retrieve version from GitHub"
    }
    
    Tooltip(
        tooltip = { Text(tooltipText) },
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter
        )
    ) {
        Icon(
            key = WizardIconKeys.Pin,
            contentDescription = "Pinned version",
            modifier = Modifier
                .size(12.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
                .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR))),
            tint = JewelTheme.globalColors.text.normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BundledLibraryIndicator() {
    Tooltip(
        tooltip = { Text("This library is bundled with the Compose release") },
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter
        )
    ) {
        Icon(
            key = WizardIconKeys.Lock,
            contentDescription = "Bundled library",
            modifier = Modifier
                .size(12.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
                .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR))),
            tint = JewelTheme.globalColors.text.normal
        )
    }
}

