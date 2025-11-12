package io.github.heisiar.composewizard.shared.ui

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.intellij.openapi.application.ApplicationManager
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import io.github.heisiar.composewizard.shared.services.HotReloadVersionService
import io.github.heisiar.composewizard.shared.services.LifecycleVersionService
import io.github.heisiar.composewizard.shared.services.Material3AdaptiveVersionService
import io.github.heisiar.composewizard.shared.services.Material3VersionService
import io.github.heisiar.composewizard.shared.services.Navigation3VersionService
import io.github.heisiar.composewizard.shared.services.NavigationEventVersionService
import io.github.heisiar.composewizard.shared.services.NavigationVersionService
import io.github.heisiar.composewizard.shared.services.SavedStateVersionService
import io.github.heisiar.composewizard.shared.services.WindowVersionService
import io.github.heisiar.composewizard.shared.settings.WizardSettings
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
    
    LaunchedEffect(state.composeVersion, state.enableDevVersions, shouldShowBundledHotReload, shouldShowNavigation, shouldShowNavigation3AndNavigationEvent) {
        if (state.composeVersion.isEmpty()) return@LaunchedEffect
        
        val devVersions = if (state.enableDevVersions) cache.getDevVersions() else null
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
    
    if (state.composeVersion.isEmpty()) {
        LibrariesLoadingPlaceholder()
    } else {
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
                LibrarySkeletonItem()
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            repeat(RIGHT_COLUMN_BASE_LIBRARIES_COUNT) {
                LibrarySkeletonItem()
            }
        }
    }
}

@Composable
private fun LibrarySkeletonItem() {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth()
    ) {
        org.jetbrains.jewel.ui.component.Checkbox(
            checked = false,
            onCheckedChange = { },
            enabled = false
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(16.dp)
            ) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 13.dp)
            }
            
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp)
            ) {
                org.jetbrains.jewel.ui.component.ListComboBox(
                    items = listOf("..."),
                    selectedIndex = 0,
                    onSelectedItemChange = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    style = textFieldStyleComboBox()
                )
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Box(
            modifier = Modifier.width(16.dp).height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeletonBox(width = 14.dp, height = 14.dp)
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
                    currentVersion = librariesState.libraryVersions[LibraryType.HOT_RELOAD] ?: state.hotReloadVersion ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    versionService = hotReloadVersionService,
                    checked = state.includeHotReload,
                    enabled = shouldShowOptionalHotReload,
                    onCheckedChange = { state.includeHotReload = !state.includeHotReload }
                )
            }
        }
    }
}
