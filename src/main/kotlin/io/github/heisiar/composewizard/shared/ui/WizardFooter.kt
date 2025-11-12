package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
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

@Composable
fun WizardFooter(state: WizardState) {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val isInternalMode = remember { com.intellij.openapi.application.ApplicationManager.getApplication().isInternal }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(horizontal = if (PlatformDetector.isAndroidStudio) 16.dp else 0.dp)
    ) {
        Text(
            text = "v1.0.0",
            style = JewelTheme.defaultTextStyle,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .let { modifier ->
                    if (!isInternalMode) {
                        modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime < 600) {
                                        clickCount++
                                        if (clickCount >= 3) {
                                            val wasVisible = state.devCheckboxVisible
                                            val newVisibility = !wasVisible
                                            if (newVisibility && !wasVisible) {
                                                ComposeWizardUsageCollector.logDevVersionsUnlocked()
                                            }
                                            state.devCheckboxVisible = newVisibility
                                            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().devCheckboxVisibleByUser = newVisibility
                                            if (!newVisibility) {
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
                    } else {
                        modifier
                    }
                }
        )

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
