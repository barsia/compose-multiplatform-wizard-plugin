package io.github.heisiar.composewizard.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import java.awt.Cursor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposeVersionField(
    cache: io.github.heisiar.composewizard.shared.services.ComposeVersionCache,
    enableDevVersions: Boolean,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit,
    onRefreshVersions: () -> Unit,
    modifier: Modifier = Modifier,
    devCheckboxVisible: Boolean = false,
    onDevVersionsToggle: (Boolean) -> Unit = {}
) {
    var refreshTrigger by remember { mutableStateOf(0) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Compose Version", style = JewelTheme.defaultTextStyle)
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (devCheckboxVisible) {
                org.jetbrains.jewel.ui.component.Tooltip(
                    tooltip = { Text(if (enableDevVersions) "Dev versions" else "Stable versions") }
                ) {
                    CompactSwitch(
                        checked = enableDevVersions,
                        onCheckedChange = { onDevVersionsToggle(it) }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        val initialVersions = if (enableDevVersions) cache.getDevVersions() else cache.getStableVersions()
        val initialLoadingState = if (enableDevVersions) cache.isLoadingDevVersions() else cache.isLoadingStableVersions()

        var availableVersions by remember(enableDevVersions, refreshTrigger) { mutableStateOf(initialVersions) }
        var isLoading by remember(enableDevVersions) { mutableStateOf(initialVersions == null) }
        
        var displayedVersion by remember(enableDevVersions) { 
            val firstVersion = initialVersions?.firstOrNull() ?: ""
            mutableStateOf(firstVersion)
        }

        LaunchedEffect(enableDevVersions) {
            if (displayedVersion.isNotEmpty()) {
                onVersionSelected(displayedVersion)
            }
        }

        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger == 0) return@LaunchedEffect
            
            isLoading = true
            
            kotlinx.coroutines.delay(200)
            
            while (true) {
                val isCurrentlyLoading = if (enableDevVersions) {
                    cache.isLoadingDevVersions()
                } else {
                    cache.isLoadingStableVersions()
                }
                
                if (!isCurrentlyLoading) {
                    val newVersions = if (enableDevVersions) {
                        cache.getDevVersions()
                    } else {
                        cache.getStableVersions()
                    }
                    
                    if (newVersions != null) {
                        availableVersions = newVersions
                        val firstVersion = newVersions.firstOrNull() ?: ""
                        if (selectedVersion.isEmpty() || !newVersions.contains(selectedVersion)) {
                            displayedVersion = firstVersion
                            onVersionSelected(firstVersion)
                        }
                        isLoading = false
                        break
                    }
                }
                
                kotlinx.coroutines.delay(100)
            }
        }
        
        LaunchedEffect(enableDevVersions) {
            
            if (initialVersions != null && !initialLoadingState) {
                return@LaunchedEffect
            }
            
            while (true) {
                val isCurrentlyLoading = if (enableDevVersions) {
                    cache.isLoadingDevVersions()
                } else {
                    cache.isLoadingStableVersions()
                }
                
                val newVersions = if (enableDevVersions) {
                    cache.getDevVersions()
                } else {
                    cache.getStableVersions()
                }
                
                
                if (!isCurrentlyLoading && newVersions != null) {
                    availableVersions = newVersions
                    val firstVersion = newVersions.firstOrNull() ?: ""
                    if (selectedVersion.isEmpty() || !newVersions.contains(selectedVersion)) {
                        displayedVersion = firstVersion
                        onVersionSelected(firstVersion)
                    }
                    isLoading = false
                    break
                }
                
                kotlinx.coroutines.delay(100)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
            ) {
                val items = remember(enableDevVersions, availableVersions) {
                    if (isLoading && availableVersions == null) {
                        listOf("")
                    } else {
                        val versions = availableVersions ?: io.github.heisiar.composewizard.shared.ComposeVersions.STABLE_VERSIONS_HARDCODED
                        versions
                    }
                }
                
                val currentIndex = remember(displayedVersion, items, isLoading) {
                    val index = if (isLoading && availableVersions == null) 0 else items.indexOf(displayedVersion).takeIf { it >= 0 } ?: 0
                    index
                }
                
            Box(
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .weight(1f)
                        ) {
                            androidx.compose.runtime.key(enableDevVersions) {
                                org.jetbrains.jewel.ui.component.ListComboBox(
                                    items = items,
                                    selectedIndex = currentIndex,
                                    onSelectedItemChange = { index ->
                                        availableVersions?.let { versions ->
                                            if (versions.isNotEmpty() && index in versions.indices) {
                                                val newSelection = versions[index]
                                                displayedVersion = newSelection
                                                onVersionSelected(newSelection)
                                            }
                                        }
                                    },
                                    enabled = !isLoading && availableVersions != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                        maxPopupHeight = 280.dp,
                        style = textFieldStyleComboBox()
                    )
                }
            }

            Tooltip(
                tooltip = { Text("Refresh versions") },
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = androidx.compose.ui.Alignment.BottomCenter,
                    alignment = androidx.compose.ui.Alignment.BottomCenter,
                    offset = DpOffset(0.dp, 4.dp)
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    val rotation = remember { Animatable(0f) }

                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            while (isLoading) {
                                rotation.animateTo(
                                    targetValue = 360f,
                                    animationSpec = tween(
                                        durationMillis = 1000,
                                        easing = LinearEasing
                                    )
                                )
                                rotation.snapTo(0f)
                            }
                        }
                    }

                    Icon(
                        key = WizardIconKeys.RefreshVersions,
                        contentDescription = "Refresh versions",
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = rotation.value }
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                enabled = !isLoading
                            ) {
                                coroutineScope.launch {
                                    rotation.snapTo(0f)
                                    rotation.animateTo(
                                        targetValue = 360f,
                                        animationSpec = tween(
                                            durationMillis = 500,
                                            easing = LinearEasing
                                        )
                                    )
                                }
                                onRefreshVersions()
                                refreshTrigger++
                            },
                        tint = JewelTheme.globalColors.text.normal
                    )
                }
            }
        }
    }
}

