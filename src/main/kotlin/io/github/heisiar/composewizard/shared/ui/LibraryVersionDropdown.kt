package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import io.github.heisiar.composewizard.shared.services.LibraryVersionService
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
    versionService: LibraryVersionService?,
    checked: Boolean = true,
    enabled: Boolean = true,
    onCheckedChange: () -> Unit = {},
    onVersionChange: (String) -> Unit = {}
) {
    if (currentVersion.isEmpty()) {
        LibraryVersionDropdownSkeleton()
        return
    }
    
    if (currentVersion == "N/A") {
        LibraryVersionDropdownError(label = label)
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
    
    val selectedVersion = librariesState.libraryVersions[libraryType] ?: currentVersion
    
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
        ComposeVersions.getLibraryBundle(state.composeVersion)?.getVersion(libraryType)
    }
    
    val versionsToFilter = allAvailableVersions
    
    val filteredVersions = remember(versionsToFilter, originalVersion, bundledVersion, versionService) {
        if (versionsToFilter.isNotEmpty() && originalVersion.isNotEmpty() && versionService != null && originalVersion != "N/A") {
            versionService.filterVersionsForDropdown(versionsToFilter, originalVersion, bundledVersion, 5)
        } else if (originalVersion != "N/A") {
            listOf(originalVersion)
        } else {
            emptyList()
        }
    }
    
    val selectedIndex = remember(selectedVersion, filteredVersions) {
        filteredVersions.indexOf(selectedVersion).coerceAtLeast(0)
    }
    
    val isFromFallback = librariesState.isFromFallback[libraryType] ?: false
    
    val isNewComposeForHotReload = remember(state.composeVersion, libraryType) {
        libraryType == LibraryType.HOT_RELOAD && 
        !isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01")
    }
    
    val effectiveEnabled = if (isNewComposeForHotReload) false else enabled
    
    val showHotReloadLock = remember(libraryType, selectedVersion, librariesState.hotReloadGithubVersion) {
        libraryType == LibraryType.HOT_RELOAD && 
        librariesState.hotReloadGithubVersion != null &&
        selectedVersion == librariesState.hotReloadGithubVersion
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        if (effectiveEnabled) {
            org.jetbrains.jewel.ui.component.Checkbox(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
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
                
                val showPinIcon = isFromFallback && 
                                  libraryType != LibraryType.HOT_RELOAD && 
                                  selectedVersion != bundledVersion
                
                if (showHotReloadLock || showPinIcon) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier.size(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showHotReloadLock) {
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
                key(state.composeVersion, selectedVersion) {
                    org.jetbrains.jewel.ui.component.ListComboBox(
                        items = filteredVersions,
                        selectedIndex = selectedIndex,
                        onSelectedItemChange = { index ->
                        if (index in filteredVersions.indices) {
                            val newVersion = filteredVersions[index]
                            
                            librariesState.libraryVersions[libraryType] = newVersion
                            
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

@Composable
private fun LibraryVersionDropdownSkeleton() {
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
            SkeletonText(width = 100.dp, height = 16.dp)
            SkeletonText(height = 24.dp)
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Box(
            modifier = Modifier.width(16.dp).height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeletonText(width = 14.dp, height = 18.dp)
        }
    }
}

@Composable
private fun LibraryVersionDropdownError(label: String) {
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
                modifier = Modifier.height(16.dp)
            ) {
                Text(
                    text = label,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = JewelTheme.defaultTextStyle.fontSize * 0.85),
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Failed to load version",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = JewelTheme.defaultTextStyle.fontSize * 0.9),
                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Box(
            modifier = Modifier.width(16.dp).height(24.dp)
        )
    }
}
