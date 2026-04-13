package io.github.barsia.composewizard.shared.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.barsia.composewizard.shared.ComposeVersions
import io.github.barsia.composewizard.shared.services.ComposeVersionCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.lazy.rememberSelectableLazyListState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import java.awt.Cursor
import java.awt.Desktop
import java.net.URI

private const val ROTATION_DURATION_MS = 500

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposeVersionField(
    cache: ComposeVersionCache,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compose Version", style = JewelTheme.defaultTextStyle)

                // Cache the last known version for the link to avoid flickering
                // Reset cache when switching to Dev mode
                val lastKnownVersion = remember(enableDevVersions) { 
                    mutableStateOf(if (enableDevVersions) "" else selectedVersion) 
                }
                
                // Update cache when in Releases mode and version is available
                SideEffect {
                    if (!enableDevVersions && selectedVersion.isNotEmpty()) {
                        lastKnownVersion.value = selectedVersion
                    }
                }

                if (!enableDevVersions && lastKnownVersion.value.isNotEmpty()) {
                    val releaseNotesInteractionSource = remember { MutableInteractionSource() }
                    val isReleaseNotesFocused by releaseNotesInteractionSource.collectIsFocusedAsState()
                    val focusBorderColor = Color(0xFF3574F0)
                    
                    Tooltip(tooltip = { Text("Release Notes") }) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(
                                    width = if (isReleaseNotesFocused) 1.dp else 0.dp,
                                    color = if (isReleaseNotesFocused) focusBorderColor else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .pointerInput(lastKnownVersion.value) {
                                        detectTapGestures {
                                            try {
                                                if (Desktop.isDesktopSupported()) {
                                                    Desktop.getDesktop().browse(URI("https://github.com/JetBrains/compose-multiplatform/releases/tag/v${lastKnownVersion.value}"))
                                                }
                                            } catch (e: Exception) {
                                                // Ignore
                                            }
                                        }
                                    }
                                    .focusable(interactionSource = releaseNotesInteractionSource)
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    key = WizardIconKeys.ExternalLink,
                                    contentDescription = "Release Notes",
                                    modifier = Modifier.size(10.dp),
                                    tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
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
                        Text(if (enableDevVersions) "Dev Versions" else "Release Versions") 
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
        var toggleTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(enableDevVersions) {
            isLoading = true
            displayedVersion = ""
            onVersionSelected("")
            toggleTrigger++
            
            // Check if versions are already cached
            val cachedVersions = if (enableDevVersions) {
                cache.getDevVersions()
            } else {
                cache.getStableVersions()
            }
            
            // Only reload if cache is empty
            if (cachedVersions.isNullOrEmpty()) {
                if (enableDevVersions) {
                    cache.forceReloadDev()
                } else {
                    cache.forceReloadStable()
                }
            }
        }

        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger == 0) {
                return@LaunchedEffect
            }
            
            isLoading = true

            delay(200)
            
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
                        displayedVersion = firstVersion
                        onVersionSelected(firstVersion)
                        isLoading = false
                        break
                    }
                }

                delay(100)
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
                    displayedVersion = firstVersion
                    onVersionSelected(firstVersion)
                    isLoading = false
                    break
                }

                delay(100)
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
                        else -> versions ?: ComposeVersions.STABLE_VERSIONS_HARDCODED
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
                key(enableDevVersions) {
                    val comboBoxFocusRequester = remember { FocusRequester() }
                    val listState = rememberSelectableLazyListState(currentIndex)
                    var isPopupVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(currentIndex, items) {
                        if (currentIndex >= 0 && currentIndex < items.size) {
                            listState.selectedKeys = setOf(currentIndex)
                        }
                    }

                    ListComboBox(
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
                        onPopupVisibleChange = { visible -> isPopupVisible = visible },
                        enabled = !isLoading && availableVersions?.isNotEmpty() == true,
                        listState = listState,
                        itemKeys = { index, _ -> index },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(comboBoxFocusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && isPopupVisible) {
                                    val versions = availableVersions
                                    if (!versions.isNullOrEmpty()) {
                                        when (event.key) {
                                            Key.DirectionDown -> {
                                                val newIndex = (currentIndex + 1).coerceAtMost(versions.lastIndex)
                                                if (newIndex != currentIndex) {
                                                    listState.selectedKeys = setOf(newIndex)
                                                    displayedVersion = versions[newIndex]
                                                    onVersionSelected(versions[newIndex])
                                                }
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                val newIndex = (currentIndex - 1).coerceAtLeast(0)
                                                if (newIndex != currentIndex) {
                                                    listState.selectedKeys = setOf(newIndex)
                                                    displayedVersion = versions[newIndex]
                                                    onVersionSelected(versions[newIndex])
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                } else false
                            }
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
                val refreshFocusRequester = remember { FocusRequester() }
                val refreshInteractionSource = remember { MutableInteractionSource() }
                val isRefreshFocused by refreshInteractionSource.collectIsFocusedAsState()
                var isManualRefresh by remember { mutableStateOf(false) }
                var shouldRestoreFocus by remember { mutableStateOf(false) }
                val lastToggleTrigger = remember { mutableStateOf(0) }
                val lastIsLoading = remember { mutableStateOf<Boolean?>(null) }
                var isAnimating by remember { mutableStateOf(false) }
                val isLoadingState = remember { mutableStateOf(isLoading) }
                val coroutineScope = rememberCoroutineScope()
                
                // Update loading state on every recomposition
                SideEffect {
                    isLoadingState.value = isLoading
                }
                
                // Animation function that runs in rememberCoroutineScope (not cancelled on recomposition)
                val startAnimation = remember {
                    {
                        if (!isAnimating) {
                            coroutineScope.launch {
                                try {
                                    isAnimating = true
                                    val startTime = System.currentTimeMillis()
                                    var rotationCount = 0
                                    var hasMinimumRotation: Boolean

                                    do {
                                        rotationCount++
                                        rotation.animateTo(
                                            targetValue = rotation.value + 360f,
                                            animationSpec = tween(
                                                durationMillis = ROTATION_DURATION_MS,
                                                easing = LinearEasing
                                            )
                                        )
                                        val elapsed = System.currentTimeMillis() - startTime
                                        hasMinimumRotation = elapsed >= ROTATION_DURATION_MS
                                    } while (isLoadingState.value || !hasMinimumRotation)
                                } catch (e: CancellationException) {
                                    throw e
                                } finally {
                                    isAnimating = false
                                }
                            }
                        }
                    }
                }

                // Trigger animation on state changes
                LaunchedEffect(isLoading, toggleTrigger, isManualRefresh) {
                    val isFirstLoad = isLoading && lastIsLoading.value == null
                    val isLoadingRestarted = isLoading && lastIsLoading.value == false
                    val isToggleChanged = toggleTrigger > 0 && toggleTrigger != lastToggleTrigger.value
                    
                    if (isFirstLoad || isLoadingRestarted || isToggleChanged || isManualRefresh) {
                        if (isToggleChanged) lastToggleTrigger.value = toggleTrigger
                        if (isManualRefresh) isManualRefresh = false
                        startAnimation()
                    }
                    
                    lastIsLoading.value = isLoading
                }
                
                // Restore focus after loading completes
                LaunchedEffect(isLoading, shouldRestoreFocus) {
                    if (!isLoading && shouldRestoreFocus) {
                        delay(100)
                        refreshFocusRequester.requestFocus()
                        shouldRestoreFocus = false
                    }
                }
                
                val focusBorderColor = Color(0xFF3574F0)

               Tooltip(tooltip = { Text("Refresh Versions") }) {
                   Box(
                       modifier = Modifier
                           .size(20.dp)
                           .border(
                               width = if (isRefreshFocused) 1.dp else 0.dp,
                               color = if (isRefreshFocused) focusBorderColor else Color.Transparent,
                               shape = CircleShape
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
                                .focusRequester(refreshFocusRequester)
                                .onKeyEvent { keyEvent: KeyEvent ->
                                    if (!isLoading && 
                                        (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar) && 
                                        keyEvent.type == KeyEventType.KeyDown
                                    ) {
                                        isManualRefresh = true
                                        onRefreshVersions()
                                        refreshTrigger++
                                        shouldRestoreFocus = true
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
}

