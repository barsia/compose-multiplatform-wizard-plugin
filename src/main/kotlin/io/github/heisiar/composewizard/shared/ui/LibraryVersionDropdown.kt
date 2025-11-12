package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
    isPinnedMap: Map<LibraryType, Boolean>,
    versionService: Any?,
    checked: Boolean = true,
    enabled: Boolean = true,
    onVersionChange: (String) -> Unit
) {
    if (currentVersion.isEmpty()) {
        SkeletonText(width = 180.dp)
        return
    }
    
    val originalVersion = remember(state.composeVersion, currentVersion) {
        if (currentVersion.isNotEmpty()) currentVersion else ""
    }
    
    val originalIsFromBundle = remember(state.composeVersion, currentVersion) {
        librariesState.libraryFromBundle[libraryType] ?: false
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
    
    val allAvailableVersions = when (libraryType) {
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
    
    val filteredVersions = remember(allAvailableVersions, originalVersion, versionService) {
        if (allAvailableVersions.isNotEmpty() && originalVersion.isNotEmpty() && versionService != null) {
            when (versionService) {
                is io.github.heisiar.composewizard.shared.services.LifecycleVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.Material3VersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.Material3AdaptiveVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.NavigationVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.Navigation3VersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.WindowVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.SavedStateVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.NavigationEventVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                is io.github.heisiar.composewizard.shared.services.HotReloadVersionService ->
                    versionService.filterVersionsForDropdown(allAvailableVersions, originalVersion, 5)
                else -> listOf(originalVersion)
            }
        } else {
            listOf(originalVersion)
        }
    }
    
    val selectedIndex = remember(selectedVersion, filteredVersions) {
        filteredVersions.indexOf(selectedVersion).coerceAtLeast(0)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (enabled) {
            org.jetbrains.jewel.ui.component.CheckboxRow(
                checked = checked,
                onCheckedChange = { onVersionChange(selectedVersion) }
            ) {
                LibraryLabelWithIcon(label, libraryType, librariesState, isPinnedMap)
            }
        } else {
            Tooltip(tooltip = { Text("Included in the base template and cannot be disabled") }) {
                org.jetbrains.jewel.ui.component.CheckboxRow(
                    checked = true,
                    onCheckedChange = { },
                    enabled = false
                ) {
                    LibraryLabelWithIcon(label, libraryType, librariesState, isPinnedMap)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f)
        ) {
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
                            librariesState.libraryFromBundle[libraryType] = originalIsFromBundle
                        } else {
                            librariesState.libraryFromBundle[libraryType] = false
                        }
                        
                        onVersionChange(newVersion)
                    }
                },
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 200.dp)
                    .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
                maxPopupHeight = 280.dp,
                style = textFieldStyleComboBox()
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            LibraryVersionCopyIcon(version = filteredVersions.getOrNull(selectedIndex) ?: currentVersion)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryLabelWithIcon(
    label: String,
    libraryType: LibraryType,
    librariesState: LibrariesState,
    isPinnedMap: Map<LibraryType, Boolean>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = JewelTheme.defaultTextStyle,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isFromBundle = librariesState.libraryFromBundle[libraryType] ?: false
            if (isFromBundle) {
                val isPinned = isPinnedMap[libraryType] ?: false
                PinnedVersionIndicator(isPinned = isPinned)
            }
        }
    }
}

