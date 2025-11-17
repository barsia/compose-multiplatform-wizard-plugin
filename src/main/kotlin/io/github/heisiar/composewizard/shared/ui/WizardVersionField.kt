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

private const val ROTATION_DURATION_MS = 500

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
        var isLoading by remember(enableDevVersions) { 
            println("🔵 [STATE] isLoading initialized to: true (enableDevVersions=$enableDevVersions)")
            mutableStateOf(true) 
        }
        var displayedVersion by remember(enableDevVersions) { mutableStateOf("") }
        var toggleTrigger by remember { 
            println("🔵 [STATE] toggleTrigger initialized to: 0")
            mutableStateOf(0) 
        }

        LaunchedEffect(enableDevVersions) {
            println("🔵 [STATE] LaunchedEffect(enableDevVersions) triggered: enableDevVersions=$enableDevVersions")
            onVersionSelected("")
            toggleTrigger++
            println("🔵 [STATE] toggleTrigger incremented to: $toggleTrigger")
            
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
            if (refreshTrigger == 0) {
                println("🔵 [STATE] LaunchedEffect(refreshTrigger) skipped: refreshTrigger=0")
                return@LaunchedEffect
            }
            
            println("🔵 [STATE] LaunchedEffect(refreshTrigger) setting isLoading = true")
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
                        println("🔵 [STATE] LaunchedEffect(refreshTrigger) setting isLoading = false (versions loaded)")
                        isLoading = false
                        break
                    }
                }
                
                kotlinx.coroutines.delay(100)
            }
        }
        
        LaunchedEffect(enableDevVersions, refreshTrigger) {
            println("🔵 [STATE] LaunchedEffect(enableDevVersions, refreshTrigger) started polling (enableDevVersions=$enableDevVersions, refreshTrigger=$refreshTrigger)")
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
                    println("🔵 [STATE] LaunchedEffect(enableDevVersions, refreshTrigger) setting isLoading = false (polling detected versions)")
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
                val rotation = remember { Animatable(0f) }
                val refreshInteractionSource = remember { MutableInteractionSource() }
                val isRefreshFocused by refreshInteractionSource.collectIsFocusedAsState()
                var isManualRefresh by remember { mutableStateOf(false) }
                val lastToggleTrigger = remember { mutableStateOf(0) }
                val lastIsLoading = remember { mutableStateOf<Boolean?>(null) }
                var isAnimating by remember { mutableStateOf(false) }
                val isLoadingState = remember { mutableStateOf(isLoading) }
                val coroutineScope = rememberCoroutineScope()
                
                // Update loading state on every recomposition
                androidx.compose.runtime.SideEffect {
                    isLoadingState.value = isLoading
                }
                
                // Animation function that runs in rememberCoroutineScope (not cancelled on recomposition)
                val startAnimation = remember {
                    {
                        if (!isAnimating) {
                            println("🔄 [ROTATION] Starting animation...")
                            coroutineScope.launch {
                                try {
                                    isAnimating = true
                                    val startTime = System.currentTimeMillis()
                                    var rotationCount = 0
                                    var hasMinimumRotation = false
                                    
                                    do {
                                        rotationCount++
                                        println("🔄 [ROTATION] Rotation #$rotationCount (isLoading=${isLoadingState.value}, elapsed=${System.currentTimeMillis() - startTime}ms)")
                                        rotation.animateTo(
                                            targetValue = rotation.value + 360f,
                                            animationSpec = tween(
                                                durationMillis = ROTATION_DURATION_MS,
                                                easing = LinearEasing
                                            )
                                        )
                                        val elapsed = System.currentTimeMillis() - startTime
                                        hasMinimumRotation = elapsed >= ROTATION_DURATION_MS
                                        println("🔄 [ROTATION] After rotation #$rotationCount: elapsed=${elapsed}ms, hasMin=$hasMinimumRotation, isLoading=${isLoadingState.value}")
                                    } while (isLoadingState.value || !hasMinimumRotation)
                                    
                                    println("🔄 [ROTATION] Animation completed: $rotationCount rotations")
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    println("🔄 [ROTATION] Animation cancelled: ${e.message}")
                                    throw e
                                } finally {
                                    isAnimating = false
                                }
                            }
                        } else {
                            println("🔄 [ROTATION] Animation skipped: already in progress")
                        }
                    }
                }

                // Trigger animation on state changes
                LaunchedEffect(isLoading, toggleTrigger, isManualRefresh) {
                    val isFirstLoad = isLoading && lastIsLoading.value == null
                    val isLoadingRestarted = isLoading && lastIsLoading.value == false
                    val isToggleChanged = toggleTrigger > 0 && toggleTrigger != lastToggleTrigger.value
                    
                    println("🔄 [ROTATION] Check: isFirstLoad=$isFirstLoad, isLoadingRestarted=$isLoadingRestarted, isToggleChanged=$isToggleChanged, isManualRefresh=$isManualRefresh")
                    
                    if (isFirstLoad || isLoadingRestarted || isToggleChanged || isManualRefresh) {
                        if (isToggleChanged) lastToggleTrigger.value = toggleTrigger
                        if (isManualRefresh) isManualRefresh = false
                        startAnimation()
                    }
                    
                    lastIsLoading.value = isLoading
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
                            .graphicsLayer { rotationZ = rotation.value % 360f }
                            .onKeyEvent { keyEvent: KeyEvent ->
                                if (!isLoading && 
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar) && 
                                    keyEvent.type == KeyEventType.KeyDown
                                ) {
                                    isManualRefresh = true
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
                                        isManualRefresh = true
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

