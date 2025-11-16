package io.github.heisiar.composewizard.shared.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
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
    onDevVersionsToggle: (Boolean) -> Unit = {},
    state: WizardState
) {
    var refreshTrigger by remember { mutableStateOf(0) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Compose Version", style = JewelTheme.defaultTextStyle)
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (devCheckboxVisible) {
                val shakeOffset = remember { Animatable(0f) }
                
                LaunchedEffect(state.triggerDevSwitcherShake) {
                    if (state.triggerDevSwitcherShake > 0) {
                        repeat(4) {
                            shakeOffset.animateTo(8f, animationSpec = tween(50, easing = LinearEasing))
                            shakeOffset.animateTo(-8f, animationSpec = tween(50, easing = LinearEasing))
                        }
                        shakeOffset.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
                    }
                }
                
                Box(modifier = Modifier.offset(x = shakeOffset.value.dp)) {
                    Tooltip(tooltip = { 
                        Text(if (enableDevVersions) "Dev versions" else "Release versions") 
                    }) {
                        CompactSwitch(
                            checked = enableDevVersions,
                            onCheckedChange = { onDevVersionsToggle(it) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        var availableVersions by remember(enableDevVersions, refreshTrigger) { mutableStateOf<List<String>?>(null) }
        var isLoading by remember(enableDevVersions) { mutableStateOf(true) }
        var displayedVersion by remember(enableDevVersions) { mutableStateOf("") }

        LaunchedEffect(enableDevVersions) {
            onVersionSelected("")
            
            if (enableDevVersions) {
                cache.forceReloadDev()
            } else {
                cache.forceReloadStable()
            }
            
            kotlinx.coroutines.delay(50)
            
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
        
        LaunchedEffect(enableDevVersions, refreshTrigger) {
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
                val items = remember(enableDevVersions, availableVersions, isLoading) {
                    val versions = availableVersions
                    when {
                        isLoading && versions == null -> listOf("")
                        versions != null && versions.isEmpty() && !isLoading -> listOf("Dev versions unavailable")
                        else -> versions ?: io.github.heisiar.composewizard.shared.ComposeVersions.STABLE_VERSIONS_HARDCODED
                    }
                }
                
                val currentIndex = remember(displayedVersion, items, isLoading, availableVersions) {
                    val versions = availableVersions
                    when {
                        isLoading && versions == null -> 0
                        versions != null && versions.isEmpty() && !isLoading -> 0
                        else -> items.indexOf(displayedVersion).takeIf { it >= 0 } ?: 0
                    }
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
                                    enabled = !isLoading && availableVersions?.isNotEmpty() == true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                        maxPopupHeight = 280.dp,
                        style = textFieldStyleComboBox()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                contentAlignment = Alignment.Center
            ) {
                val coroutineScope = rememberCoroutineScope()
                val rotation = remember { Animatable(0f) }
                val refreshInteractionSource = remember { MutableInteractionSource() }
                val isRefreshFocused by refreshInteractionSource.collectIsFocusedAsState()

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
                
                val focusBorderColor = Color(0xFF3574F0)

               Box(
                   modifier = Modifier
                       .size(20.dp)
                       .border(
                           width = if (isRefreshFocused) 1.dp else 0.dp,
                           color = if (isRefreshFocused) focusBorderColor else Color.Transparent,
                           shape = androidx.compose.foundation.shape.CircleShape
                       )
                       .padding(2.dp),
                   contentAlignment = Alignment.Center
               ) {
                    Icon(
                        key = WizardIconKeys.RefreshVersions,
                        contentDescription = "Refresh versions",
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = rotation.value }
                            .onKeyEvent { keyEvent: KeyEvent ->
                                if (!isLoading && 
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar) && 
                                    keyEvent.type == KeyEventType.KeyDown
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
                                    true
                                } else {
                                    false
                                }
                            }
                            .pointerInput(isLoading) {
                                if (!isLoading) {
                                    detectTapGestures {
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
                                    }
                                }
                            }
                            .focusable(interactionSource = refreshInteractionSource, enabled = !isLoading),
                        tint = JewelTheme.globalColors.text.normal
                    )
                }
            }
        }
    }
}

