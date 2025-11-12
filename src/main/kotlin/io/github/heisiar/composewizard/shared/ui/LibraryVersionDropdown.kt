package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryVersionDropdown(
    libraryType: LibraryType,
    label: String,
    currentVersion: String,
    cache: ComposeVersionCache,
    state: WizardState,
    librariesState: LibrariesState,
    versionService: Any?,
    checked: Boolean = true,
    enabled: Boolean = true,
    onVersionChange: (String) -> Unit
) {
    if (currentVersion.isEmpty()) {
        SkeletonText()
        return
    }
    
    val originalVersion = remember(state.composeVersion, currentVersion) {
        if (currentVersion.isNotEmpty()) currentVersion else ""
    }
    
    val originalIsFromFallback = remember(state.composeVersion, currentVersion) {
        librariesState.isFromFallback[libraryType] ?: false
    }
    
    var cacheVersion by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        cache.cacheInvalidated.collect {
            cacheVersion++
        }
    }
    
    val selectedVersion = when (libraryType) {
        LibraryType.LIFECYCLE -> state.lifecycleVersion?.takeIf { it.isNotEmpty() }
        LibraryType.MATERIAL3 -> state.material3Version?.takeIf { it.isNotEmpty() }
        LibraryType.MATERIAL3_ADAPTIVE -> state.material3AdaptiveVersion?.takeIf { it.isNotEmpty() }
        LibraryType.NAVIGATION -> state.navigationVersion?.takeIf { it.isNotEmpty() }
        LibraryType.NAVIGATION3 -> state.navigation3Version?.takeIf { it.isNotEmpty() }
        LibraryType.WINDOW -> state.windowVersion?.takeIf { it.isNotEmpty() }
        LibraryType.SAVED_STATE -> state.savedStateVersion?.takeIf { it.isNotEmpty() }
        LibraryType.NAVIGATION_EVENT -> state.navigationEventVersion?.takeIf { it.isNotEmpty() }
        LibraryType.HOT_RELOAD -> state.hotReloadVersion?.takeIf { it.isNotEmpty() }
    } ?: currentVersion
    
    val allAvailableVersions = remember(cacheVersion, libraryType) {
        when (libraryType) {
            LibraryType.LIFECYCLE -> cache.getLifecycleAvailableVersions()
            LibraryType.MATERIAL3 -> cache.getMaterial3AvailableVersions()
            LibraryType.MATERIAL3_ADAPTIVE -> cache.getMaterial3AdaptiveAvailableVersions()
            LibraryType.NAVIGATION -> cache.getNavigationAvailableVersions()
            LibraryType.NAVIGATION3 -> cache.getNavigation3AvailableVersions()
            LibraryType.WINDOW -> cache.getWindowAvailableVersions()
            LibraryType.SAVED_STATE -> cache.getSavedStateAvailableVersions()
            LibraryType.NAVIGATION_EVENT -> cache.getNavigationEventAvailableVersions()
            LibraryType.HOT_RELOAD -> cache.getHotReloadAvailableVersions()
        }
    }
    
    val bundledVersion = remember(state.composeVersion, libraryType) {
        io.github.heisiar.composewizard.shared.ComposeVersions.getLibraryBundle(state.composeVersion)?.getVersion(libraryType)
    }
    
    val filteredVersions = remember(allAvailableVersions, originalVersion, bundledVersion, versionService) {
        if (allAvailableVersions.isNotEmpty() && originalVersion.isNotEmpty() && versionService != null) {
            when (versionService) {
                is io.github.heisiar.composewizard.shared.services.LifecycleVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.Material3VersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.Material3AdaptiveVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.NavigationVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.Navigation3VersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.WindowVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.SavedStateVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.NavigationEventVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                is io.github.heisiar.composewizard.shared.services.HotReloadVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, bundledVersion, 5)
                else -> listOf(originalVersion)
            }
        } else {
            listOf(originalVersion)
        }
    }
    
    val selectedIndex = remember(selectedVersion, filteredVersions) {
        filteredVersions.indexOf(selectedVersion).coerceAtLeast(0)
    }
    
    val isFromFallback = librariesState.isFromFallback[libraryType] ?: false
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        if (enabled) {
            org.jetbrains.jewel.ui.component.Checkbox(
                checked = checked,
                onCheckedChange = { onVersionChange(selectedVersion) },
                modifier = Modifier.pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
            )
        } else {
            Tooltip(tooltip = { Text("Included in the base template and cannot be disabled") }) {
                org.jetbrains.jewel.ui.component.Checkbox(
                    checked = true,
                    onCheckedChange = { },
                    enabled = false
                )
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(16.dp)
            ) {
                Text(
                    text = label,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = JewelTheme.defaultTextStyle.fontSize * 0.85),
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (isFromFallback) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier.size(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (libraryType == LibraryType.HOT_RELOAD) {
                            BundledLibraryIndicator()
                        } else {
                            PinnedVersionIndicator(isPinned = true)
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp)
            ) {
                val comboBox = @Composable {
                        org.jetbrains.jewel.ui.component.ListComboBox(
                            items = filteredVersions,
                            selectedIndex = selectedIndex,
                            onSelectedItemChange = { index ->
                                if (index in filteredVersions.indices) {
                                    val newVersion = filteredVersions[index]
                                    
                                    when (libraryType) {
                                        LibraryType.LIFECYCLE -> state.lifecycleVersion = newVersion
                                        LibraryType.MATERIAL3 -> state.material3Version = newVersion
                                        LibraryType.MATERIAL3_ADAPTIVE -> state.material3AdaptiveVersion = newVersion
                                        LibraryType.NAVIGATION -> state.navigationVersion = newVersion
                                        LibraryType.NAVIGATION3 -> state.navigation3Version = newVersion
                                        LibraryType.WINDOW -> state.windowVersion = newVersion
                                        LibraryType.SAVED_STATE -> state.savedStateVersion = newVersion
                                        LibraryType.NAVIGATION_EVENT -> state.navigationEventVersion = newVersion
                                        LibraryType.HOT_RELOAD -> state.hotReloadVersion = newVersion
                                    }
                                    
                                    if (newVersion == originalVersion) {
                                        librariesState.isFromFallback[libraryType] = originalIsFromFallback
                                    } else {
                                        librariesState.isFromFallback[libraryType] = false
                                    }
                                    
                                    onVersionChange(newVersion)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
                            maxPopupHeight = 280.dp,
                            style = textFieldStyleComboBox()
                        )
                    }
                    
                    val isLongVersion = selectedVersion.length > 21
                    if (isLongVersion) {
                        Tooltip(
                            tooltip = { Text(selectedVersion) },
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomCenter,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset(0.dp, 4.dp)
                            )
                        ) {
                            comboBox()
                        }
                    } else {
                        comboBox()
                    }
                }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Box(
            modifier = Modifier.width(16.dp).height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LibraryVersionCopyIcon(version = filteredVersions.getOrNull(selectedIndex) ?: currentVersion)
        }
    }
}
