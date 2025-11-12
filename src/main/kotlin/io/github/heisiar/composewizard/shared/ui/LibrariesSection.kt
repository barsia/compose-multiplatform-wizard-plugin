package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import io.github.heisiar.composewizard.shared.services.LifecycleVersionService
import io.github.heisiar.composewizard.shared.services.Material3AdaptiveVersionService
import io.github.heisiar.composewizard.shared.services.Material3VersionService
import io.github.heisiar.composewizard.shared.services.Navigation3VersionService
import io.github.heisiar.composewizard.shared.services.NavigationEventVersionService
import io.github.heisiar.composewizard.shared.services.NavigationVersionService
import io.github.heisiar.composewizard.shared.services.SavedStateVersionService
import io.github.heisiar.composewizard.shared.services.WindowVersionService
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

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
    val lifecycleVersionService = remember { LifecycleVersionService() }
    val material3VersionService = remember { Material3VersionService() }
    val material3AdaptiveVersionService = remember { Material3AdaptiveVersionService() }
    val navigationVersionService = remember { NavigationVersionService() }
    val navigation3VersionService = remember { Navigation3VersionService() }
    val windowVersionService = remember { WindowVersionService() }
    val savedStateVersionService = remember { SavedStateVersionService() }
    val navigationEventVersionService = remember { NavigationEventVersionService() }
    
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
    
    val currentBundle = ComposeVersions.getLibraryBundle(state.composeVersion)
    val isPinnedMap = remember(state.composeVersion) {
        LibraryType.entries.associateWith { type ->
            val versionInCurrentBundle = currentBundle?.getVersion(type)
            versionInCurrentBundle.isNullOrEmpty()
        }
    }
    
    Column(modifier = modifier) {
        Text("Libraries", style = JewelTheme.defaultTextStyle)
        
        Spacer(modifier = Modifier.height(LIBRARIES_SECTION_SPACING))
        
        if (state.composeVersion.isEmpty()) {
            LibrariesLoadingPlaceholder()
        } else {
            LibrariesContent(
                state = state,
                librariesState = librariesState,
                cache = cache,
                isPinnedMap = isPinnedMap,
                lifecycleVersionService = lifecycleVersionService,
                material3VersionService = material3VersionService,
                material3AdaptiveVersionService = material3AdaptiveVersionService,
                navigationVersionService = navigationVersionService,
                navigation3VersionService = navigation3VersionService,
                windowVersionService = windowVersionService,
                savedStateVersionService = savedStateVersionService,
                navigationEventVersionService = navigationEventVersionService,
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
            modifier = Modifier.weight(1f).widthIn(min = 380.dp),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            repeat(LEFT_COLUMN_LIBRARIES_COUNT) {
                SkeletonText(width = 180.dp)
            }
        }
        Column(
            modifier = Modifier.weight(1f).widthIn(min = 380.dp),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            repeat(RIGHT_COLUMN_BASE_LIBRARIES_COUNT) {
                SkeletonText(width = 180.dp)
            }
        }
    }
}

@Composable
private fun LibrariesContent(
    state: WizardState,
    librariesState: LibrariesState,
    cache: ComposeVersionCache,
    isPinnedMap: Map<LibraryType, Boolean>,
    lifecycleVersionService: LifecycleVersionService,
    material3VersionService: Material3VersionService,
    material3AdaptiveVersionService: Material3AdaptiveVersionService,
    navigationVersionService: NavigationVersionService,
    navigation3VersionService: Navigation3VersionService,
    windowVersionService: WindowVersionService,
    savedStateVersionService: SavedStateVersionService,
    navigationEventVersionService: NavigationEventVersionService,
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
            modifier = Modifier.weight(1f).widthIn(min = 120.dp),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            LibraryVersionDropdown(
                libraryType = LibraryType.LIFECYCLE,
                label = "Lifecycle",
                currentVersion = librariesState.libraryVersions[LibraryType.LIFECYCLE] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                isPinnedMap = isPinnedMap,
                versionService = lifecycleVersionService,
                enabled = false,
                onVersionChange = {}
            )
            
            LibraryVersionDropdown(
                libraryType = LibraryType.MATERIAL3_ADAPTIVE,
                label = "Material3 Adaptive",
                currentVersion = librariesState.libraryVersions[LibraryType.MATERIAL3_ADAPTIVE] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                isPinnedMap = isPinnedMap,
                versionService = material3AdaptiveVersionService,
                checked = state.includeMaterial3Adaptive,
                onVersionChange = { state.includeMaterial3Adaptive = !state.includeMaterial3Adaptive }
            )
            
            if (shouldShowNavigation3AndNavigationEvent) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.NAVIGATION_EVENT,
                    label = "NavigationEvent",
                    currentVersion = librariesState.libraryVersions[LibraryType.NAVIGATION_EVENT] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    isPinnedMap = isPinnedMap,
                    versionService = navigationEventVersionService,
                    checked = state.includeNavigationEvent,
                    onVersionChange = { state.includeNavigationEvent = !state.includeNavigationEvent }
                )
            }
            
            LibraryVersionDropdown(
                libraryType = LibraryType.WINDOW,
                label = "Window",
                currentVersion = librariesState.libraryVersions[LibraryType.WINDOW] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                isPinnedMap = isPinnedMap,
                versionService = windowVersionService,
                checked = state.includeWindow,
                onVersionChange = { state.includeWindow = !state.includeWindow }
            )
        }
        
        Column(
            modifier = Modifier.weight(1f).widthIn(min = 120.dp),
            verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
        ) {
            LibraryVersionDropdown(
                libraryType = LibraryType.MATERIAL3,
                label = "Material3",
                currentVersion = librariesState.libraryVersions[LibraryType.MATERIAL3] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                isPinnedMap = isPinnedMap,
                versionService = material3VersionService,
                checked = state.includeMaterial3,
                onVersionChange = { state.includeMaterial3 = !state.includeMaterial3 }
            )
            
            if (shouldShowNavigation) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.NAVIGATION,
                    label = "Navigation",
                    currentVersion = librariesState.libraryVersions[LibraryType.NAVIGATION] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    isPinnedMap = isPinnedMap,
                    versionService = navigationVersionService,
                    checked = state.includeNavigation,
                    onVersionChange = { state.includeNavigation = !state.includeNavigation }
                )
            }
            
            if (shouldShowNavigation3AndNavigationEvent) {
                LibraryVersionDropdown(
                    libraryType = LibraryType.NAVIGATION3,
                    label = "Navigation3",
                    currentVersion = librariesState.libraryVersions[LibraryType.NAVIGATION3] ?: "",
                    cache = cache,
                    state = state,
                    librariesState = librariesState,
                    isPinnedMap = isPinnedMap,
                    versionService = navigation3VersionService,
                    checked = state.includeNavigation3,
                    onVersionChange = { state.includeNavigation3 = !state.includeNavigation3 }
                )
            }
            
            LibraryVersionDropdown(
                libraryType = LibraryType.SAVED_STATE,
                label = "SavedState",
                currentVersion = librariesState.libraryVersions[LibraryType.SAVED_STATE] ?: "",
                cache = cache,
                state = state,
                librariesState = librariesState,
                isPinnedMap = isPinnedMap,
                versionService = savedStateVersionService,
                checked = state.includeSavedState,
                onVersionChange = { state.includeSavedState = !state.includeSavedState }
            )
            
            if (shouldShowOptionalHotReload) {
                val hotReloadVersion = state.hotReloadVersion ?: ""
                CheckboxOption(
                    checked = state.includeHotReload,
                    onToggle = { state.includeHotReload = !state.includeHotReload },
                    label = "Hot Reload${if (hotReloadVersion.isNotEmpty()) " $hotReloadVersion" else ""}"
                )
            }
            
            if (shouldShowBundledHotReload) {
                BundledHotReloadItem(
                    version = librariesState.libraryVersions[LibraryType.HOT_RELOAD] ?: ""
                )
            }
        }
    }
}
