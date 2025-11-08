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
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.styling.ComboBoxStyle
import org.jetbrains.jewel.ui.theme.comboBoxStyle
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
    onVersionSelected: (String) -> Unit,
    onRefreshVersions: () -> Unit,
    modifier: Modifier = Modifier,
    devCheckboxVisible: Boolean = false,
    onDevVersionsToggle: (Boolean) -> Unit = {}
) {
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
        val initialVersion = initialVersions?.firstOrNull() ?: ""
        val initialLoadingState = if (enableDevVersions) cache.isLoadingDevVersions() else cache.isLoadingStableVersions()

        var availableVersions by remember(enableDevVersions) { mutableStateOf(initialVersions) }
        var selectedVersion by remember(enableDevVersions) { mutableStateOf(initialVersion) }
        var isLoading by remember(enableDevVersions) { mutableStateOf(initialVersions == null || initialLoadingState) }
        var minLoadingTimeElapsed by remember(enableDevVersions) { mutableStateOf(initialVersions != null) }

        LaunchedEffect(selectedVersion) {
            if (selectedVersion.isNotEmpty()) {
                onVersionSelected(selectedVersion)
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
                        if (selectedVersion.isEmpty()) {
                            selectedVersion = newVersions.firstOrNull() ?: ""
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
                    availableVersions ?: io.github.heisiar.composewizard.shared.ComposeVersions.STABLE_VERSIONS
                }
                val currentIndex = if (isLoading && availableVersions == null) 0 else items.indexOf(selectedVersion).takeIf { it >= 0 } ?: 0
                
                val defaultStyle = JewelTheme.comboBoxStyle
                val transparentStyle = remember(defaultStyle) {
                    val colors = org.jetbrains.jewel.ui.component.styling.ComboBoxColors(
                        background = defaultStyle.colors.background,
                        nonEditableBackground = Color.Transparent,
                        backgroundDisabled = defaultStyle.colors.backgroundDisabled,
                        backgroundFocused = defaultStyle.colors.backgroundFocused,
                        backgroundPressed = defaultStyle.colors.backgroundPressed,
                        backgroundHovered = defaultStyle.colors.backgroundHovered,
                        content = defaultStyle.colors.content,
                        contentDisabled = defaultStyle.colors.contentDisabled,
                        contentFocused = defaultStyle.colors.contentFocused,
                        contentPressed = defaultStyle.colors.contentPressed,
                        contentHovered = defaultStyle.colors.contentHovered,
                        border = defaultStyle.colors.border,
                        borderDisabled = defaultStyle.colors.borderDisabled,
                        borderFocused = defaultStyle.colors.borderFocused,
                        borderPressed = defaultStyle.colors.borderPressed,
                        borderHovered = defaultStyle.colors.borderHovered
                    )
                    ComboBoxStyle(colors, defaultStyle.metrics, defaultStyle.icons)
                }
                
                val textMeasurer = rememberTextMeasurer()
                val textStyle = JewelTheme.defaultTextStyle
                val textWidth = remember(selectedVersion, textStyle) {
                    textMeasurer.measure(selectedVersion, textStyle).size.width
                }
                val availableWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 
                    maxWidth.toPx() - 40.dp.toPx()
                }
                val isTextTruncated = textWidth > availableWidth
                
                if (isTextTruncated) {
                    org.jetbrains.jewel.ui.component.Tooltip(
                        tooltip = { Text(selectedVersion) },
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.BottomCenter,
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset(0.dp, 4.dp)
                        )
                    ) {
                        ListComboBox(
                            items = items,
                            selectedIndex = currentIndex,
                            onSelectedItemChange = { index ->
                                availableVersions?.let { versions ->
                                    if (versions.isNotEmpty() && index in versions.indices) {
                                        selectedVersion = versions[index]
                                    }
                                }
                            },
                            enabled = !isLoading && availableVersions != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(
                                    if (isLoading || availableVersions == null) {
                                        PointerIcon(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
                                    } else {
                                        PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                                    }
                                ),
                            style = transparentStyle
                        )
                    }
                } else {
                    ListComboBox(
                        items = items,
                        selectedIndex = currentIndex,
                        onSelectedItemChange = { index ->
                            availableVersions?.let { versions ->
                                if (versions.isNotEmpty() && index in versions.indices) {
                                    selectedVersion = versions[index]
                                }
                            }
                        },
                        enabled = !isLoading && availableVersions != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(
                                if (isLoading || availableVersions == null) {
                                    PointerIcon(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
                                } else {
                                    PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                                }
                            ),
                        style = transparentStyle
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
    hotReloadVersion: String = ""
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
        
        if (composeVersion.isNotEmpty() && kotlinVersion.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Versions will be used:",
                    style = JewelTheme.defaultTextStyle,
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• Kotlin:",
                        style = JewelTheme.defaultTextStyle,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = kotlinVersion,
                        style = JewelTheme.defaultTextStyle,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• Compose Multiplatform:",
                        style = JewelTheme.defaultTextStyle,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = composeVersion,
                        style = JewelTheme.defaultTextStyle,
                        color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                }
                if (lifecycleVersion.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• AndroidX Lifecycle:",
                            style = JewelTheme.defaultTextStyle,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = lifecycleVersion,
                            style = JewelTheme.defaultTextStyle,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
                    }
                }
                if (hotReloadVersion.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• Compose Hot Reload:",
                            style = JewelTheme.defaultTextStyle,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = hotReloadVersion,
                            style = JewelTheme.defaultTextStyle,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.3f),
                            fontSize = 13.sp
                        )
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


