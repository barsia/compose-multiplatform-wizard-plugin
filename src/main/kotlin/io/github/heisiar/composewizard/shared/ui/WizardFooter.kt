package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.application.ApplicationInfo
import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.theme.textFieldStyle
import java.awt.Cursor
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WizardFooter(state: WizardState) {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val isInternalMode = remember { com.intellij.openapi.application.ApplicationManager.getApplication().isInternal }
    
    LaunchedEffect(state.showDevSwitcherTooltip, state.showInternalModeTooltip) {
        if (state.showDevSwitcherTooltip || state.showInternalModeTooltip) {
            kotlinx.coroutines.delay(3000)
            state.showDevSwitcherTooltip = false
            state.showInternalModeTooltip = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = if (PlatformDetector.isAndroidStudio) 16.dp else 0.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val bugIconInteractionSource = remember { MutableInteractionSource() }
            val isBugIconHovered by bugIconInteractionSource.collectIsHoveredAsState()
            
            Tooltip(tooltip = { Text("Report a bug or feature request") }) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                try {
                                    if (Desktop.isDesktopSupported()) {
                                        val appInfo = ApplicationInfo.getInstance()
                                        val ideName = if (PlatformDetector.isAndroidStudio) "Android Studio" else "IntelliJ IDEA"
                                        val ideVersion = appInfo.fullVersion
                                        val issueTitle = URLEncoder.encode("[BUG / FEATURE] ", "UTF-8")
                                        val issueBody = URLEncoder.encode(
                                            "**Plugin:** Compose Multiplatform Wizard 0.1.0\n" +
                                            "**IDE:** $ideName $ideVersion\n\n" +
                                            "**Description:**\n" +
                                            "<!-- Please describe the issue you encountered -->\n\n" +
                                            "**Steps to Reproduce:**\n" +
                                            "1. \n" +
                                            "2. \n" +
                                            "3. \n\n" +
                                            "**Expected Result:**\n" +
                                            "<!-- What did you expect to happen? -->\n\n" +
                                            "**Actual Result:**\n" +
                                            "<!-- What actually happened? -->\n\n" +
                                            "**Screenshots / Screencast:**\n" +
                                            "<!-- If applicable, drag and drop screenshots or screencast here -->\n\n" +
                                            "**Logs:**\n" +
                                            "<!-- Attach relevant logs: Open Help → Show Log in Finder / Explorer → Drag and drop the log file or paste error messages here -->",
                                            "UTF-8"
                                        )
                                        Desktop.getDesktop().browse(
                                            URI("https://github.com/heisiar/compose-multiplatform-wizard-project/issues/new?title=$issueTitle&body=$issueBody")
                                        )
                                    }
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        }
                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        .hoverable(interactionSource = bugIconInteractionSource),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        BugIcon,
                        contentDescription = "Report a bug",
                        modifier = Modifier.size(18.dp),
                        tint = JewelTheme.globalColors.text.normal.copy(
                            alpha = if (isBugIconHovered) 0.8f else 0.4f
                        )
                    )
                }
            }
            
            Text(
                text = "v0.1.0",
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.4f),
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime < 600) {
                                clickCount++
                                if (clickCount >= 3) {
                                    if (isInternalMode) {
                                        state.triggerDevSwitcherShake++
                                        state.showInternalModeTooltip = true
                                        clickCount = 0
                                        return@detectTapGestures
                                    }
                                    
                                    val wasSwitcherVisible = state.devCheckboxVisible
                                    val newSwitcherVisibility = !wasSwitcherVisible
                                    
                                    if (!newSwitcherVisibility && state.enableDevVersions) {
                                        state.triggerDevSwitcherShake++
                                        state.showDevSwitcherTooltip = true
                                        clickCount = 0
                                        return@detectTapGestures
                                    }
                                    
                                    if (newSwitcherVisibility && !wasSwitcherVisible) {
                                        ComposeWizardUsageCollector.logDevVersionsUnlocked()
                                    }
                                    state.devCheckboxVisible = newSwitcherVisibility
                                    io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().devCheckboxVisibleByUser = newSwitcherVisibility
                                    if (!newSwitcherVisibility) {
                                        state.enableDevVersions = false
                                        io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = false
                                    }
                                    clickCount = 0
                                }
                            } else {
                                clickCount = 1
                            }
                            lastClickTime = currentTime
                        }
                    )
                }
            )
            
            if (state.showInternalModeTooltip || state.showDevSwitcherTooltip) {
                val tooltipBackground = JewelTheme.textFieldStyle.colors.background
                val tooltipBorder = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f)
                
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .background(
                            color = tooltipBackground,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = tooltipBorder,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (state.showInternalModeTooltip) {
                            "Toggle is always visible in internal mode"
                        } else {
                            "Switch to Release to hide the toggle"
                        },
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
                        color = JewelTheme.globalColors.text.normal
                    )
                }
            }
        }

        if (state.hasNoTargets) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                SelectionContainer {
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

