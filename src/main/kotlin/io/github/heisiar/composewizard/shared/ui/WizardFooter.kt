package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.textFieldStyle

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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = "v1.0.0",
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

