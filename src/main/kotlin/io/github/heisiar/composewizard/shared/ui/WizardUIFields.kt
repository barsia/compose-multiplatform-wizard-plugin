package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.WizardStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
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
    projectNameWarning: String?,
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
        Text(
            "Project Name", 
            style = JewelTheme.defaultTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LaunchedEffect(projectNameState.text.toString()) {
            onNameChanged(projectNameState.text.toString())
        }

        Box {
            val warningColor = androidx.compose.ui.graphics.Color(0xFFE6A23C)
            val hasWarning = projectNameWarning != null && projectNameError == null
            val hasError = projectNameError != null
            
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
                outline = when {
                    hasError -> Outline.Error
                    hasWarning -> Outline.Warning
                    else -> Outline.None
                },
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
            
            if (projectNameWarning != null && projectNameFocused) {
                androidx.compose.runtime.key(projectNameWarning, projectNameFocused) {
                    ValidationPopupJB(
                        mainPanel = mainPanel,
                        message = projectNameWarning,
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
    onVersionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Time for one full rotation (360 degrees) in milliseconds
    val ROTATION_DURATION_MS = 400
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Compose Version", 
            style = JewelTheme.defaultTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        var availableVersions by remember(enableDevVersions) { mutableStateOf<List<String>>(emptyList()) }
        var selectedVersion by remember(enableDevVersions) { mutableStateOf("") }
        var isLoading by remember(enableDevVersions) { mutableStateOf(false) }

        LaunchedEffect(selectedVersion) {
            if (selectedVersion.isNotEmpty()) {
                onVersionSelected(selectedVersion)
            }
        }

        LaunchedEffect(enableDevVersions) {
            val isToggling = selectedVersion.isNotEmpty() // Toggle if version already selected
            
            var loaderShownTime = 0L
            
            // Delay showing loader by 150ms (only show if loading takes longer)
            val loaderJob = launch {
                delay(150)
                loaderShownTime = System.currentTimeMillis()
                isLoading = true
            }
            
            // Wait for versions to load from Maven (or use cached)
            availableVersions = if (enableDevVersions) {
                cache.getDevVersionsSuspend(timeoutMs = 3000)
            } else {
                cache.getStableVersionsSuspend(timeoutMs = 3000)
            }
            if (availableVersions.isNotEmpty()) {
                selectedVersion = availableVersions.first()
            }
            
            loaderJob.cancel()
            
            if (isLoading) {
                // Loader is visible - ensure at least one full rotation from when loader appeared
                val loaderElapsed = System.currentTimeMillis() - loaderShownTime
                if (loaderElapsed < ROTATION_DURATION_MS) {
                    delay(ROTATION_DURATION_MS - loaderElapsed)
                }
            }
            
            // Stop loading AFTER ensuring minimum rotation time
            isLoading = false
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Note: Dropdown is deprecated, but ComboBox/ListComboBox API is not yet stable in current Jewel version
            @Suppress("DEPRECATION")
            Box(
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .weight(1f)
            ) {
                var isTextTruncated by remember { mutableStateOf(false) }
                
                Tooltip(
                    tooltip = { Text(selectedVersion) },
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset(0.dp, (-4).dp)
                    ),
                    enabled = isTextTruncated && !isLoading && selectedVersion.isNotEmpty()
                ) {
                    Dropdown(
                        menuContent = {
                            availableVersions.forEach { version ->
                                selectableItem(
                                    selected = version == selectedVersion,
                                    iconKey = null,
                                    keybinding = null,
                                    onClick = { selectedVersion = version },
                                    enabled = true
                                ) {
                                    Text(version)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && availableVersions.isNotEmpty()) {
                                    val currentIndex = availableVersions.indexOf(selectedVersion)
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            if (currentIndex > 0) {
                                                selectedVersion = availableVersions[currentIndex - 1]
                                            }
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            if (currentIndex < availableVersions.size - 1) {
                                                selectedVersion = availableVersions[currentIndex + 1]
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                    ) {
                        if (selectedVersion.isNotEmpty()) {
                            Text(
                                text = selectedVersion,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                onTextLayout = { layoutResult ->
                                    isTextTruncated = layoutResult.hasVisualOverflow
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
            ) {
                val coroutineScope = rememberCoroutineScope()
                val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
                val refreshInteractionSource = remember { MutableInteractionSource() }
                val isRefreshFocused by refreshInteractionSource.collectIsFocusedAsState()
                
                // Auto-rotate icon while loading versions - rotate exactly while isLoading is true
                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        rotation.snapTo(0f)
                        rotation.animateTo(
                            targetValue = 360f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(
                                    durationMillis = ROTATION_DURATION_MS,
                                    easing = androidx.compose.animation.core.LinearEasing
                                ),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                            )
                        )
                    } else {
                        rotation.stop()
                        rotation.snapTo(0f)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isRefreshFocused) {
                                JewelTheme.globalColors.outlines.focused.copy(alpha = 0.25f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        key = WizardIconKeys.RefreshVersions,
                        contentDescription = "Refresh versions",
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = rotation.value }
                            .clickable(
                                role = Role.Button,
                                indication = null,
                                interactionSource = refreshInteractionSource
                            ) {
                                if (!isLoading) {
                                    coroutineScope.launch {
                                        val loaderShownTime = System.currentTimeMillis()
                                        isLoading = true
                                        
                                        // Force reload versions from Maven
                                        if (enableDevVersions) {
                                            cache.forceReloadDev()
                                            availableVersions = cache.getDevVersionsSuspend(timeoutMs = 3000)
                                        } else {
                                            cache.forceReloadStable()
                                            availableVersions = cache.getStableVersionsSuspend(timeoutMs = 3000)
                                        }
                                        if (availableVersions.isNotEmpty()) {
                                            selectedVersion = availableVersions.first()
                                        }
                                        
                                        // Ensure loader is visible for at least one full rotation from when loader appeared
                                        val loaderElapsed = System.currentTimeMillis() - loaderShownTime
                                        if (loaderElapsed < ROTATION_DURATION_MS) {
                                            delay(ROTATION_DURATION_MS - loaderElapsed)
                                        }
                                        
                                        isLoading = false
                                    }
                                }
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
    projectLocationWarning: String?,
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
            if (projectLocationWarning != null && projectPathFocused) {
                ValidationPopup(
                    message = projectLocationWarning,
                    isWarning = true
                )
            }
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val isBrowseFocused by interactionSource.collectIsFocusedAsState()

        Box(
            modifier = Modifier
                .background(
                    color = when {
                        isBrowseFocused -> JewelTheme.globalColors.outlines.focused.copy(alpha = 0.25f)
                        isHovered -> JewelTheme.globalColors.text.selected.copy(alpha = 0.08f)
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                )
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                .clickable(
                    role = Role.Button,
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
                    alpha = if (isHovered || isBrowseFocused) 0.85f else 0.75f
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
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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
                    key = WizardIconKeys.iOS,
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            style = JewelTheme.defaultTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() }
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckboxOption(
            checked = git,
            onToggle = onGitToggle,
            label = "Initialize Git repository"
        )

        CheckboxOption(
            checked = tests,
            onToggle = onTestsToggle,
            label = "Add sample tests"
        )
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
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = label,
            style = JewelTheme.defaultTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() }
        )
    }
}

@Composable
fun ProjectPathHint(projectPath: String, projectName: String, modifier: Modifier = Modifier) {
    val displayPath = if (projectPath.isNotBlank() && projectName.isNotBlank()) {
        try {
            val expandedPath = WizardPathUtils.expandPath(projectPath)
            // projectPath is the base directory, projectName is added to form full path
            // This matches the logic in updateDataModel(): File(expandedPath, projectNameValue)
            val fullPath = java.io.File(expandedPath, projectName).absolutePath
            WizardPathUtils.collapsePath(fullPath)
        } catch (e: Exception) {
            "$projectPath/$projectName"
        }
    } else {
        ""
    }
    
    androidx.compose.foundation.text.selection.SelectionContainer(
        modifier = modifier
    ) {
        Text(
            text = "Project will be created at: $displayPath",
            style = JewelTheme.defaultTextStyle,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
        )
    }
}

