package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.WizardStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import javax.swing.DefaultComboBoxModel
import java.awt.Dimension as AwtDimension
import java.awt.Cursor
import java.io.File
import java.io.InputStream

private object ResourceLoader {
    fun loadResource(path: String): InputStream? {
        return Thread.currentThread().contextClassLoader?.getResourceAsStream(path)
            ?: javaClass.classLoader?.getResourceAsStream(path)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProjectNameField(
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameError: String?,
    projectLocationWarning: String?,
    projectNameFocused: Boolean,
    projectNameInteractionSource: MutableInteractionSource,
    mainPanel: androidx.compose.ui.awt.ComposePanel,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Project Name", style = JewelTheme.defaultTextStyle)

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
                modifier = Modifier.fillMaxWidth(),
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


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
            modifier = Modifier.fillMaxWidth(),
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
        var isFirstRender by remember(enableDevVersions) { mutableStateOf(true) }
        var currentSelectedVersion by remember(enableDevVersions) { 
            // Always select first version when switching Dev/Stable
            val initialSelection = initialVersions?.firstOrNull() ?: ""
            println("DEBUG ComposeVersionField: Initializing with enableDev=$enableDevVersions, always selecting first version: '$initialSelection'")
            mutableStateOf(initialSelection)
        }
        // Show loading ONLY if no cached versions (background refresh shouldn't block UI)
        var isLoading by remember(enableDevVersions) { mutableStateOf(initialVersions == null) }
        var minLoadingTimeElapsed by remember(enableDevVersions) { mutableStateOf(initialVersions != null) }

        // Sync currentSelectedVersion with external selectedVersion
        LaunchedEffect(selectedVersion) {
            if (selectedVersion.isNotEmpty() && selectedVersion != currentSelectedVersion) {
                println("DEBUG ComposeVersionField: Syncing selectedVersion: '$currentSelectedVersion' -> '$selectedVersion'")
                currentSelectedVersion = selectedVersion
            }
        }

        LaunchedEffect(currentSelectedVersion) {
            println("DEBUG ComposeVersionField: LaunchedEffect triggered - isFirstRender=$isFirstRender, currentSelectedVersion='$currentSelectedVersion', selectedVersion='$selectedVersion'")
            
            // On first render: notify parent if we auto-selected from dropdown (different from selectedVersion)
            if (isFirstRender) {
                isFirstRender = false
                if (currentSelectedVersion.isNotEmpty() && currentSelectedVersion != selectedVersion) {
                    println("DEBUG ComposeVersionField: ✅ First render with auto-selection from dropdown: '$currentSelectedVersion' (was: '$selectedVersion')")
                    onVersionSelected(currentSelectedVersion)
                } else {
                    println("DEBUG ComposeVersionField: First render with matching selectedVersion='$selectedVersion', skipping notification")
                }
                return@LaunchedEffect
            }
            
            if (currentSelectedVersion.isNotEmpty() && currentSelectedVersion != selectedVersion) {
                println("DEBUG ComposeVersionField: ✅ Notifying parent about selection change: '$selectedVersion' -> '$currentSelectedVersion'")
                onVersionSelected(currentSelectedVersion)
            } else {
                println("DEBUG ComposeVersionField: ❌ NOT notifying parent - condition not met (empty=${currentSelectedVersion.isEmpty()}, equal=${currentSelectedVersion == selectedVersion})")
            }
        }

        LaunchedEffect(enableDevVersions) {
            if (initialVersions == null) {
                minLoadingTimeElapsed = false
                println("DEBUG: No cached versions, starting min loading timer (1000ms)")
                kotlinx.coroutines.delay(1000)
                minLoadingTimeElapsed = true
                println("DEBUG: Minimum loading time (1000ms) elapsed")
            }
        }

        // Poll for updated versions after manual refresh
        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger == 0) return@LaunchedEffect // Skip initial render
            
            println("DEBUG: Refresh triggered, waiting for new versions...")
            isLoading = true
            
            kotlinx.coroutines.delay(200) // Small delay to let cache start loading
            
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
                        println("DEBUG: Refresh complete, got ${newVersions.size} versions: ${newVersions.take(3)}")
                        availableVersions = newVersions
                        if (currentSelectedVersion.isEmpty() || !newVersions.contains(currentSelectedVersion)) {
                            currentSelectedVersion = newVersions.firstOrNull() ?: ""
                        }
                        isLoading = false
                        break
                    }
                }
                
                kotlinx.coroutines.delay(100)
            }
        }
        
        LaunchedEffect(enableDevVersions) {
            println("DEBUG: Starting version loading check, enableDev=$enableDevVersions, initialVersions=${initialVersions?.take(3)}")
            
            if (initialVersions != null && !initialLoadingState) {
                println("DEBUG: Versions already cached and not loading, no need to wait")
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
                
                println("DEBUG: Polling - isLoading=$isCurrentlyLoading, versions: ${newVersions?.take(3)}, minTimeElapsed=$minLoadingTimeElapsed")
                
                if (!isCurrentlyLoading && newVersions != null) {
                    if (minLoadingTimeElapsed) {
                        println("DEBUG: Loading complete and min time elapsed, updating UI with ${newVersions.take(3)}")
                        availableVersions = newVersions
                        // Reset selection if current version not in new list (e.g., loaded from empty cache with DEFAULT_VERSION)
                        if (currentSelectedVersion.isNotEmpty() && !newVersions.contains(currentSelectedVersion)) {
                            println("DEBUG: Current selection '$currentSelectedVersion' not in new list, resetting to first: ${newVersions.firstOrNull()}")
                            currentSelectedVersion = newVersions.firstOrNull() ?: ""
                        } else if (currentSelectedVersion.isEmpty() && selectedVersion.isEmpty()) {
                            // Only set selection if both are empty (initial state)
                            currentSelectedVersion = newVersions.firstOrNull() ?: ""
                        }
                        isLoading = false
                        break
                    }
                }
                
                kotlinx.coroutines.delay(100)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .weight(1f)
            ) {
                val items = if (isLoading && availableVersions == null) {
                    listOf("")
                } else {
                    println("DEBUG WizardUIFields: availableVersions (enableDev=$enableDevVersions, null=${availableVersions == null}): ${availableVersions?.take(10)}")
                    val versions = availableVersions ?: io.github.heisiar.composewizard.shared.ComposeVersions.STABLE_VERSIONS
                    println("DEBUG WizardUIFields: Final dropdown items (enableDev=$enableDevVersions): ${versions.take(10)}")
                    versions
                }
                val currentIndex = if (isLoading && availableVersions == null) 0 else items.indexOf(currentSelectedVersion).takeIf { it >= 0 } ?: 0
                
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val textMeasurer = rememberTextMeasurer()
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    
                    val textWidth = if (currentSelectedVersion.isNotEmpty()) {
                        with(density) {
                            textMeasurer.measure(
                                text = currentSelectedVersion,
                                style = JewelTheme.defaultTextStyle
                            ).size.width.toDp()
                        }
                    } else {
                        0.dp
                    }
                    
                    // Show tooltip only if text is truncated (with some margin for dropdown button)
                    val showTooltip = textWidth > (maxWidth - 40.dp) && currentSelectedVersion.isNotEmpty()
                    
                    if (showTooltip) {
                        Tooltip(
                            tooltip = { Text(currentSelectedVersion) },
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = androidx.compose.ui.Alignment.BottomCenter,
                                alignment = androidx.compose.ui.Alignment.BottomCenter,
                                offset = DpOffset(0.dp, 4.dp)
                            )
                        ) {
                            org.jetbrains.jewel.ui.component.ListComboBox(
                                items = items,
                                selectedIndex = currentIndex,
                                onSelectedItemChange = { index ->
                                    availableVersions?.let { versions ->
                                        if (versions.isNotEmpty() && index in versions.indices) {
                                            val newSelection = versions[index]
                                            println("DEBUG ComposeVersionField: Dropdown selection changed: index=$index, newSelection='$newSelection', old='$currentSelectedVersion'")
                                            currentSelectedVersion = newSelection
                                        }
                                    }
                                },
                                enabled = !isLoading && availableVersions != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                                maxPopupHeight = 280.dp
                            )
                        }
                    } else {
                        org.jetbrains.jewel.ui.component.ListComboBox(
                            items = items,
                            selectedIndex = currentIndex,
                            onSelectedItemChange = { index ->
                                availableVersions?.let { versions ->
                                    if (versions.isNotEmpty() && index in versions.indices) {
                                        val newSelection = versions[index]
                                        println("DEBUG ComposeVersionField: Dropdown selection changed: index=$index, newSelection='$newSelection', old='$currentSelectedVersion'")
                                        currentSelectedVersion = newSelection
                                    }
                                }
                            },
                            enabled = !isLoading && availableVersions != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                            maxPopupHeight = 280.dp
                        )
                    }
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
                    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }

                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            while (isLoading) {
                                rotation.animateTo(
                                    targetValue = 360f,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 1000,
                                        easing = androidx.compose.animation.core.LinearEasing
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
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 500,
                                            easing = androidx.compose.animation.core.LinearEasing
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProjectLocationField(
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathError: String?,
    projectPathFocused: Boolean,
    projectPathInteractionSource: MutableInteractionSource,
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
        Box(modifier = Modifier.weight(1f)) {
            TextField(
                state = projectPathState,
                placeholder = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
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

        Box(
            modifier = Modifier
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
            Icon(
                imageVector = FolderIcon,
                contentDescription = "Browse folder",
                tint = JewelTheme.globalColors.text.normal.copy(
                    alpha = if (isHovered) 0.85f else 0.75f
                ),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PackageNameField(
    projectIdState: androidx.compose.foundation.text.input.TextFieldState,
    projectIdError: String?,
    projectIdFocused: Boolean,
    projectIdInteractionSource: MutableInteractionSource,
    onIdChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Package Name", style = JewelTheme.defaultTextStyle)

        LaunchedEffect(projectIdState.text.toString()) {
            onIdChanged(projectIdState.text.toString())
        }

        Box {
            TextField(
                state = projectIdState,
                placeholder = { Text(WizardStrings.PACKAGE_NAME_LABEL) },
                modifier = Modifier.fillMaxWidth(),
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle() }
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(label, style = JewelTheme.defaultTextStyle)
    }
}

@Composable
fun OptionsSection(
    git: Boolean,
    tests: Boolean,
    onGitToggle: () -> Unit,
    onTestsToggle: () -> Unit,
    modifier: Modifier = Modifier,
    composeVersion: String = "",
    kotlinVersion: String = "",
    lifecycleVersion: String = "",
    hotReloadVersion: String = "",
    isResolvingLifecycle: Boolean = false,
    isFallback: Boolean = false,
    isLifecycleFallback: Boolean = false,
    libraryVersions: Map<io.github.heisiar.composewizard.shared.LibraryType, String> = emptyMap(),
    libraryFromBundle: Map<io.github.heisiar.composewizard.shared.LibraryType, Boolean> = emptyMap()
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckboxOption(
            checked = git,
            onToggle = onGitToggle,
            label = "Create Git repository"
        )

        CheckboxOption(
            checked = tests,
            onToggle = onTestsToggle,
            label = "Add sample tests"
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show versions section (with reduced opacity while loading)
        val versionsReady = composeVersion.isNotEmpty() && kotlinVersion.isNotEmpty()
        androidx.compose.foundation.text.selection.SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (versionsReady) 1f else 0.3f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Layout: 2-column table with versions (including title and Kotlin as first row)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // First row: Title and Kotlin version
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left: Title with fallback indicator
                        val titleText = if (isFallback) {
                            "Versions will be used: 📦 (offline fallback)"
                        } else {
                            "Versions will be used:"
                        }
                        Text(
                            text = titleText,
                            style = JewelTheme.defaultTextStyle,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Right: Kotlin version
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Kotlin:",
                                style = JewelTheme.defaultTextStyle,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = kotlinVersion,
                                style = JewelTheme.defaultTextStyle,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // Collect other versions for 2-column layout
                    val allVersionItems = buildList {
                        add("Compose" to composeVersion)
                        io.github.heisiar.composewizard.shared.LibraryType.values().forEach { type ->
                            val version = libraryVersions[type]
                            if (!version.isNullOrEmpty()) {
                                val displayText = if (libraryFromBundle[type] == true) {
                                    "$version 📦"
                                } else {
                                    version
                                }
                                add(type.displayName to displayText)
                            }
                        }
                        if (hotReloadVersion.isNotEmpty()) {
                            add("Compose Hot Reload" to hotReloadVersion)
                        }
                    }
                    
                    // Split into two columns
                    val midPoint = (allVersionItems.size + 1) / 2
                    val leftColumn = allVersionItems.take(midPoint)
                    val rightColumn = allVersionItems.drop(midPoint)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            leftColumn.forEach { (label, value) ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$label:",
                                        style = JewelTheme.defaultTextStyle,
                                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = value,
                                        style = JewelTheme.defaultTextStyle,
                                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        // Right column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            rightColumn.forEach { (label, value) ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$label:",
                                        style = JewelTheme.defaultTextStyle,
                                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = value,
                                        style = JewelTheme.defaultTextStyle,
                                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckboxOption(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle() }
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Text(label, style = JewelTheme.defaultTextStyle)
    }
}

@Composable
private fun CompactSwitch(
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
    
    val trackColor = if (checked) {
        JewelTheme.globalColors.text.info.copy(alpha = 0.3f)
    } else {
        JewelTheme.globalColors.text.normal.copy(alpha = 0.2f)
    }
    
    val thumbColor = if (checked) {
        Color(0xFFFFC107).copy(alpha = 0.5f)  // Yellow for Dev
    } else {
        Color(0xFF4CAF50).copy(alpha = 0.5f)  // Green for Stable
    }
    
    val textColor = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
    
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .background(
                color = trackColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(trackHeight / 2)
            )
            .border(
                width = 0.5.dp,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(trackHeight / 2)
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
                // Dev: text centered in the area 0-(trackWidth-thumbSize)
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
                // Stable: text centered in the area thumbSize-trackWidth
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


