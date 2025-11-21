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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import io.github.heisiar.composewizard.shared.services.LibraryVersionService
import org.jetbrains.jewel.foundation.lazy.rememberSelectableLazyListState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.SimpleListItem
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
    
    var initialVersionRef by remember(state.composeVersion, libraryType) { mutableStateOf<String?>(null) }
    if (initialVersionRef == null && currentVersion.isNotEmpty()) {
        initialVersionRef = currentVersion
    }
    
    val originalVersion = remember(state.composeVersion, libraryType, initialVersionRef) {
        val bundle = ComposeVersions.getLibraryBundle(state.composeVersion)
        val bundleVersion = bundle?.getVersion(libraryType)
        bundleVersion ?: (initialVersionRef ?: currentVersion)
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
    
    val filteredVersions = remember(versionsToFilter, originalVersion, bundledVersion, versionService, selectedVersion) {
        val baseList = if (versionsToFilter.isNotEmpty() && originalVersion.isNotEmpty() && versionService != null && originalVersion != "N/A") {
            versionService.filterVersionsForDropdown(versionsToFilter, originalVersion, bundledVersion, 5)
        } else if (originalVersion != "N/A") {
            listOf(originalVersion)
        } else {
            emptyList()
        }
        
        if (selectedVersion.isNotEmpty() && selectedVersion !in baseList) {
            (listOf(selectedVersion) + baseList).distinct()
        } else {
            baseList
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
        val checkboxModifier = if (effectiveEnabled) {
            Modifier.pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)))
        } else {
            Modifier.focusProperties { canFocus = false }
        }
        
        val checkboxContent = @Composable {
            org.jetbrains.jewel.ui.component.Checkbox(
                checked = if (effectiveEnabled) checked else true,
                onCheckedChange = if (effectiveEnabled) { { onCheckedChange() } } else { { } },
                enabled = effectiveEnabled,
                modifier = checkboxModifier
            )
        }
        
        if (effectiveEnabled) {
            checkboxContent()
        } else {
            val tooltipText = if (isNewComposeForHotReload) {
                "This version is bundled with the Compose Multiplatform release"
            } else {
                "Included in the base template and cannot be disabled"
            }
            Tooltip(tooltip = { Text(tooltipText) }) {
                checkboxContent()
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
                                  libraryType != LibraryType.LIFECYCLE &&
                                  selectedVersion != bundledVersion
                
                val showRequiredLibraryIcon = !effectiveEnabled && 
                                              libraryType == LibraryType.LIFECYCLE
                
                if (showHotReloadLock || showPinIcon || showRequiredLibraryIcon) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .focusProperties { canFocus = false },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            showHotReloadLock -> BundledLibraryIndicator()
                            showRequiredLibraryIcon -> RequiredLibraryIndicator()
                            else -> PinnedVersionIndicator(isPinned = true)
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp)
            ) {
                key(state.composeVersion) {
                    val dropdownFocusRequester = remember { FocusRequester() }
                    var isPopupVisible by remember { mutableStateOf(false) }
                    var hoveredIndex by remember { mutableIntStateOf(-1) }
                    var shouldRestoreFocus by remember { mutableStateOf(false) }
                    var wasPopupVisible by remember { mutableStateOf(false) }
                    val listState = rememberSelectableLazyListState()
                    
                    LaunchedEffect(selectedIndex, filteredVersions) {
                        if (selectedIndex in filteredVersions.indices) {
                            listState.selectedKeys = setOf(selectedIndex)
                        } else {
                            listState.selectedKeys = emptySet()
                        }
                    }
                    
                    LaunchedEffect(isPopupVisible) {
                        if (isPopupVisible && !wasPopupVisible) {
                            hoveredIndex = -1
                        } else if (!isPopupVisible && wasPopupVisible) {
                            if (hoveredIndex >= 0 && hoveredIndex != selectedIndex && hoveredIndex in filteredVersions.indices) {
                                val newVersion = filteredVersions[hoveredIndex]
                                librariesState.libraryVersions[libraryType] = newVersion
                                if (newVersion == originalVersion) {
                                    librariesState.isFromFallback[libraryType] = originalIsFromFallback
                                } else {
                                    librariesState.isFromFallback[libraryType] = false
                                }
                                onVersionChange(newVersion)
                            }
                            hoveredIndex = -1
                            shouldRestoreFocus = true
                        }
                        wasPopupVisible = isPopupVisible
                    }
                    
                    LaunchedEffect(shouldRestoreFocus) {
                        if (shouldRestoreFocus) {
                            try {
                                dropdownFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            shouldRestoreFocus = false
                        }
                    }
                    
                    org.jetbrains.jewel.ui.component.ListComboBox(
                        items = filteredVersions,
                        selectedIndex = selectedIndex,
                        listState = listState,
                        itemKeys = { index, _ -> index },
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
                    onPopupVisibleChange = { visible -> 
                        isPopupVisible = visible 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(dropdownFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && isPopupVisible) {
                                if (filteredVersions.isNotEmpty()) {
                                    val currentIndex = if (hoveredIndex >= 0) hoveredIndex else selectedIndex
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            val newIndex = (currentIndex + 1).coerceAtMost(filteredVersions.lastIndex)
                                            if (newIndex != currentIndex) {
                                                hoveredIndex = newIndex
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            val newIndex = (currentIndex - 1).coerceAtLeast(0)
                                            if (newIndex != currentIndex) {
                                                hoveredIndex = newIndex
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                        .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
                    maxPopupHeight = 280.dp,
                    style = textFieldStyleComboBox(),
                    itemContent = { item, isSelected, isActive ->
                        if (!isActive) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        } else {
                            val itemIndex = filteredVersions.indexOf(item)
                            val isHighlighted = hoveredIndex >= 0 && itemIndex == hoveredIndex
                            val showAsSelected = if (hoveredIndex >= 0) isHighlighted else isSelected
                            SimpleListItem(
                                text = item,
                                selected = showAsSelected,
                                active = true
                            )
                        }
                    }
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
internal fun LibraryVersionDropdownSkeleton() {
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
