package io.github.barsia.composewizard.shared.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.intellij.openapi.application.ApplicationManager
import io.github.barsia.composewizard.shared.LibraryType
import io.github.barsia.composewizard.shared.services.*
import io.github.barsia.composewizard.shared.settings.WizardSettings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun LibrariesSection(
    state: WizardState,
    librariesState: LibrariesState,
    cache: ComposeVersionCache,
    shouldShowOptionalHotReload: Boolean,
    shouldShowBundledHotReload: Boolean,
    shouldShowNavigation: Boolean,
    shouldShowNavigation3AndNavigationEvent: Boolean,
    modifier: Modifier = Modifier
) {
    val settings = remember { WizardSettings.getInstance() }
    val isInternalMode = remember { ApplicationManager.getApplication().isInternal }
    
    val defaultExpanded = settings.isLibrariesExpanded ?: isInternalMode
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    
    LaunchedEffect(isExpanded) {
        if (settings.isLibrariesExpanded != isExpanded) {
            settings.isLibrariesExpanded = isExpanded
        }
    }
    
    Column(modifier = modifier) {
        CollapsibleHeader(
            text = "Libraries",
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded }
        )
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            LibrariesSectionContent(
                state = state,
                librariesState = librariesState,
                cache = cache,
                shouldShowOptionalHotReload = shouldShowOptionalHotReload,
                shouldShowBundledHotReload = shouldShowBundledHotReload,
                shouldShowNavigation = shouldShowNavigation,
                shouldShowNavigation3AndNavigationEvent = shouldShowNavigation3AndNavigationEvent
            )
        }
    }
}

@Composable
private fun CollapsibleHeader(
    text: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusBorderColor = Color(0xFF3574F0)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = if (isFocused) 1.dp else 0.dp,
                    color = if (isFocused) focusBorderColor else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 12.dp)
                .semantics {
                    role = Role.Button
                }
                .onKeyEvent { keyEvent ->
                    when {
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar) && 
                        keyEvent.type == KeyEventType.KeyDown -> {
                            onToggle()
                            true
                        }
                        else -> false
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onToggle()
                    }
                }
                .focusable(interactionSource = interactionSource)
                .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                key = if (isExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp),
                tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
            )
            
            Text(
                text = text,
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.8f)
            )
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                            JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                            JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                            JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

@Composable
private fun LibrariesSectionContent(
    state: WizardState,
    librariesState: LibrariesState,
    cache: ComposeVersionCache,
    shouldShowOptionalHotReload: Boolean,
    shouldShowBundledHotReload: Boolean,
    shouldShowNavigation: Boolean,
    shouldShowNavigation3AndNavigationEvent: Boolean
) {
    val lifecycleVersionService = remember { LifecycleVersionService() }
    val material3VersionService = remember { Material3VersionService() }
    val material3AdaptiveVersionService = remember { Material3AdaptiveVersionService() }
    val navigationVersionService = remember { NavigationVersionService() }
    val navigation3VersionService = remember { Navigation3VersionService() }
    val windowVersionService = remember { WindowVersionService() }
    val savedStateVersionService = remember { SavedStateVersionService() }
    val navigationEventVersionService = remember { NavigationEventVersionService() }
    val hotReloadVersionService = remember { HotReloadVersionService() }
    
    var devVersions by remember { mutableStateOf<List<String>?>(null) }
    
    LaunchedEffect(state.enableDevVersions, state.composeVersion) {
        if (state.enableDevVersions) {
            devVersions = null
            while (true) {
                devVersions = cache.getDevVersions()
                if (devVersions != null) break
                kotlinx.coroutines.delay(100)
            }
        } else {
            devVersions = null
        }
    }
    
    LaunchedEffect(state.composeVersion, state.enableDevVersions, shouldShowBundledHotReload, shouldShowNavigation, shouldShowNavigation3AndNavigationEvent) {
        if (state.composeVersion.isEmpty()) {
            librariesState.clearAllVersions()
            return@LaunchedEffect
        }
        
        val isDevLoading = state.enableDevVersions && devVersions == null
        
        if (!isDevLoading) {
            librariesState.versionForLibraries = state.composeVersion
            librariesState.loadLibraryVersions(state.composeVersion)
        }
    }
    
    LaunchedEffect(Unit) {
        cache.cacheInvalidated.collect {
            if (librariesState.versionForLibraries.isNotEmpty()) {
                librariesState.loadLibraryVersions(librariesState.versionForLibraries)
            }
        }
    }
    
    androidx.compose.runtime.SideEffect {
        if (state.composeVersion.isEmpty() && librariesState.libraryVersions.isNotEmpty()) {
            librariesState.clearAllVersions()
        }
    }
    
    val librariesLoaded = librariesState.libraryVersions.isNotEmpty() && !librariesState.isLoadingVersions
    val showError = state.composeVersion.isEmpty() && state.enableDevVersions && devVersions?.isEmpty() == true
    val showSkeletons = (state.composeVersion.isEmpty() && !showError) || (!state.composeVersion.isEmpty() && !librariesLoaded)
    
    when {
        showError -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Compose version is not available",
                    style = JewelTheme.defaultTextStyle,
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                )
            }
        }
        showSkeletons -> {
            LibrariesLoadingPlaceholder()
        }
        else -> {
            LibrariesContent(
            state = state,
            librariesState = librariesState,
            cache = cache,
            lifecycleVersionService = lifecycleVersionService,
            material3VersionService = material3VersionService,
            material3AdaptiveVersionService = material3AdaptiveVersionService,
            navigationVersionService = navigationVersionService,
            navigation3VersionService = navigation3VersionService,
            windowVersionService = windowVersionService,
            savedStateVersionService = savedStateVersionService,
            navigationEventVersionService = navigationEventVersionService,
            hotReloadVersionService = hotReloadVersionService,
            shouldShowOptionalHotReload = shouldShowOptionalHotReload,
            shouldShowBundledHotReload = shouldShowBundledHotReload,
            shouldShowNavigation = shouldShowNavigation,
            shouldShowNavigation3AndNavigationEvent = shouldShowNavigation3AndNavigationEvent
        )
        }
    }
}

@Composable
private fun LibrariesLoadingPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            repeat(LEFT_COLUMN_LIBRARIES_COUNT) {
                LibraryVersionDropdownSkeleton()
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            repeat(RIGHT_COLUMN_BASE_LIBRARIES_COUNT) {
                LibraryVersionDropdownSkeleton()
            }
        }
    }
}

@Composable
private fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    
    val skeletonColor = if (isDarkTheme()) {
        Color.White.copy(alpha = alpha)
    } else {
        Color.Gray.copy(alpha = alpha)
    }
    
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(skeletonColor)
    )
}

@Composable
private fun LibrariesContent(
    state: WizardState,
    librariesState: LibrariesState,
    cache: ComposeVersionCache,
    lifecycleVersionService: LifecycleVersionService,
    material3VersionService: Material3VersionService,
    material3AdaptiveVersionService: Material3AdaptiveVersionService,
    navigationVersionService: NavigationVersionService,
    navigation3VersionService: Navigation3VersionService,
    windowVersionService: WindowVersionService,
    savedStateVersionService: SavedStateVersionService,
    navigationEventVersionService: NavigationEventVersionService,
    hotReloadVersionService: HotReloadVersionService,
    shouldShowOptionalHotReload: Boolean,
    shouldShowBundledHotReload: Boolean,
    shouldShowNavigation: Boolean,
    shouldShowNavigation3AndNavigationEvent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            LibraryVersionDropdown(
                libraryType = LibraryType.LIFECYCLE,
                label = "Lifecycle",
                currentVersion = librariesState.libraryVersions[LibraryType.LIFECYCLE] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                versionService = lifecycleVersionService,
                enabled = false
            )
            
            LibraryVersionDropdown(
                libraryType = LibraryType.MATERIAL3,
                label = "Material3",
                currentVersion = librariesState.libraryVersions[LibraryType.MATERIAL3] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                versionService = material3VersionService,
                checked = state.includeMaterial3,
                onCheckedChange = { state.includeMaterial3 = !state.includeMaterial3 }
            )
            
            LibraryVersionDropdown(
                libraryType = LibraryType.MATERIAL3_ADAPTIVE,
                label = "Material3 Adaptive",
                currentVersion = librariesState.libraryVersions[LibraryType.MATERIAL3_ADAPTIVE] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                versionService = material3AdaptiveVersionService,
                checked = state.includeMaterial3Adaptive,
                onCheckedChange = { state.includeMaterial3Adaptive = !state.includeMaterial3Adaptive }
            )
            
            if (shouldShowNavigation) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.NAVIGATION,
                    label = "Navigation",
                    currentVersion = librariesState.libraryVersions[LibraryType.NAVIGATION] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    versionService = navigationVersionService,
                    checked = state.includeNavigation,
                    onCheckedChange = { state.includeNavigation = !state.includeNavigation }
                )
            }
            
            if (shouldShowNavigation3AndNavigationEvent) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.NAVIGATION_EVENT,
                    label = "NavigationEvent",
                    currentVersion = librariesState.libraryVersions[LibraryType.NAVIGATION_EVENT] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    versionService = navigationEventVersionService,
                    checked = state.includeNavigationEvent,
                    onCheckedChange = { state.includeNavigationEvent = !state.includeNavigationEvent }
                )
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            if (shouldShowNavigation3AndNavigationEvent) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.NAVIGATION3,
                    label = "Navigation3",
                    currentVersion = librariesState.libraryVersions[LibraryType.NAVIGATION3] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    versionService = navigation3VersionService,
                    checked = state.includeNavigation3,
                    onCheckedChange = { state.includeNavigation3 = !state.includeNavigation3 }
                )
            }
            
            LibraryVersionDropdown(
                libraryType = LibraryType.WINDOW,
                label = "Window",
                currentVersion = librariesState.libraryVersions[LibraryType.WINDOW] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                versionService = windowVersionService,
                checked = state.includeWindow,
                onCheckedChange = { state.includeWindow = !state.includeWindow }
            )
            
            LibraryVersionDropdown(
                libraryType = LibraryType.SAVED_STATE,
                label = "SavedState",
                currentVersion = librariesState.libraryVersions[LibraryType.SAVED_STATE] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                versionService = savedStateVersionService,
                checked = state.includeSavedState,
                onCheckedChange = { state.includeSavedState = !state.includeSavedState }
            )
            
            if (shouldShowOptionalHotReload || shouldShowBundledHotReload) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.HOT_RELOAD,
                    label = "Hot Reload",
                    currentVersion = librariesState.libraryVersions[LibraryType.HOT_RELOAD] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    versionService = hotReloadVersionService,
                    checked = state.includeHotReload,
                    enabled = shouldShowOptionalHotReload,
                    onCheckedChange = { state.includeHotReload = !state.includeHotReload },
                    onVersionChange = { state.hotReloadVersion = it }
                )
            }
        }
    }
}
