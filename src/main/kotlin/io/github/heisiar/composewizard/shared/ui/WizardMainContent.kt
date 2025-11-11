package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

private val SPACING_BETWEEN_SECTIONS = 8.dp
private val SPACING_BEFORE_LOCATION = 16.dp
private val LOCATION_SECTION_VERTICAL_OFFSET = 4.dp
private val TEXTFIELD_VERTICAL_OFFSET = 8.dp
private val TEXTFIELD_HEIGHT_REDUCTION = 16.dp
private val LIBRARIES_SECTION_SPACING = 4.dp
private val LIBRARY_ITEM_SPACING = 2.dp

private const val LEFT_COLUMN_LIBRARIES_COUNT = 4
private const val RIGHT_COLUMN_BASE_LIBRARIES_COUNT = 4

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WizardMainContent(
    state: WizardState,
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectIdState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameFocused: Boolean,
    projectPathFocused: Boolean,
    projectIdFocused: Boolean,
    projectNameInteractionSource: MutableInteractionSource,
    projectPathInteractionSource: MutableInteractionSource,
    projectIdInteractionSource: MutableInteractionSource,
    mainPanel: ComposePanel,
    onBrowseFolder: () -> String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(min = 220.dp)
                .verticalScroll(rememberScrollState())
                .padding(if (PlatformDetector.isAndroidStudio) 16.dp else 0.dp)
        ) {
            // In IntelliJ IDEA: Name and Location are provided by platform fields (with validation)
            // Only show Name and Location in Android Studio
            if (PlatformDetector.isAndroidStudio) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ProjectNameField(
                        projectNameState = projectNameState,
                        projectNameError = state.projectNameError,
                        projectLocationWarning = state.projectLocationWarning,
                        projectNameFocused = projectNameFocused,
                        projectNameInteractionSource = projectNameInteractionSource,
                        mainPanel = mainPanel,
                        onNameChanged = { },
                        modifier = Modifier.weight(0.6f)
                    )
                    
                    // Compose Version section on the right
                    ComposeVersionField(
                        cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance(),
                        enableDevVersions = state.enableDevVersions,
                        selectedVersion = state.composeVersion,
                        onVersionSelected = { 
                            println("DEBUG WizardMainContent [Desktop]: onVersionSelected called with '$it', old value='${state.composeVersion}'")
                            state.composeVersion = it 
                            println("DEBUG WizardMainContent [Desktop]: state.composeVersion updated to '${state.composeVersion}'")
                        },
                        onRefreshVersions = {
                            val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
                            if (state.enableDevVersions) {
                                cache.forceReloadDev()
                            } else {
                                cache.forceReloadStable()
                            }
                        },
                        modifier = Modifier.weight(0.4f),
                        devCheckboxVisible = state.devCheckboxVisible,
                        onDevVersionsToggle = { 
                            state.enableDevVersions = it
                            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersionsSetByUser = true
                            ComposeWizardUsageCollector.logDevVersionsToggled(it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(SPACING_BEFORE_LOCATION))

                Box(modifier = Modifier.offset(y = (-LOCATION_SECTION_VERTICAL_OFFSET))) {
                    ProjectLocationSection(
                        state = state,
                        projectPathState = projectPathState,
                        projectPathFocused = projectPathFocused,
                        projectPathInteractionSource = projectPathInteractionSource,
                        onBrowseFolder = onBrowseFolder
                    )
                }
                
                Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))
            } else {
                // In IntelliJ IDEA: Package name left, Compose Version right
                // Name and Location are shown by platform fields above
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    PackageNameField(
                        projectIdState = projectIdState,
                        projectIdError = state.projectIdError,
                        projectIdFocused = projectIdFocused,
                        projectIdInteractionSource = projectIdInteractionSource,
                        onIdChanged = { },
                        modifier = Modifier.weight(0.5f)
                    )
                    
                    ComposeVersionField(
                        cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance(),
                        enableDevVersions = state.enableDevVersions,
                        selectedVersion = state.composeVersion,
                        onVersionSelected = { state.composeVersion = it },
                        onRefreshVersions = {
                            val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
                            if (state.enableDevVersions) {
                                cache.forceReloadDev()
                            } else {
                                cache.forceReloadStable()
                            }
                        },
                        modifier = Modifier.weight(0.5f),
                        devCheckboxVisible = state.devCheckboxVisible,
                        onDevVersionsToggle = { 
                            state.enableDevVersions = it
                            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersionsSetByUser = true
                            ComposeWizardUsageCollector.logDevVersionsToggled(it)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))
            }

            PlatformsSection(
                desktop = state.desktop,
                android = state.android,
                ios = state.ios,
                web = state.web,
                onDesktopToggle = {
                    state.desktop = !state.desktop
                    ComposeWizardUsageCollector.logPlatformToggled("Desktop", state.desktop)
                },
                onAndroidToggle = {
                    state.android = !state.android
                    ComposeWizardUsageCollector.logPlatformToggled("Android", state.android)
                },
                onIosToggle = {
                    state.ios = !state.ios
                    ComposeWizardUsageCollector.logPlatformToggled("iOS", state.ios)
                },
                onWebToggle = {
                    state.web = !state.web
                    ComposeWizardUsageCollector.logPlatformToggled("Web", state.web)
                }
            )

            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))

            // Hot reload: < 10.0.0-beta01 - optional, >= 10.0.0-beta01 - bundled
            val shouldShowOptionalHotReload = remember(state.desktop, state.composeVersion) {
                state.desktop && isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01")
            }
            
            val shouldShowNavigation = remember(state.composeVersion) {
                isComposeVersionLessThan(state.composeVersion, "1.10.0")
            }
            
            val shouldShowBundledHotReload = remember(state.desktop, state.composeVersion) {
                state.desktop && !isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01") && state.composeVersion.isNotEmpty()
            }
            
            LaunchedEffect(shouldShowOptionalHotReload, shouldShowBundledHotReload) {
                if (shouldShowOptionalHotReload) {
                    state.hotReloadVersion = io.github.heisiar.composewizard.shared.ComposeVersions.COMPOSE_HOT_RELOAD_VERSION
                } else if (!shouldShowBundledHotReload) {
                    state.hotReloadVersion = null
                    state.includeHotReload = false
                }
            }
            
            val hotReloadVersion = state.hotReloadVersion ?: ""
            
            val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
            val lifecycleVersionService = remember { io.github.heisiar.composewizard.shared.services.LifecycleVersionService() }
            var lifecycleVersion by remember { mutableStateOf("") }
            var isResolvingLifecycle by remember { mutableStateOf(false) }
            
            // Library versions state
            val libraryVersions = remember { mutableStateMapOf<io.github.heisiar.composewizard.shared.LibraryType, String>() }
            val libraryFromBundle = remember { mutableStateMapOf<io.github.heisiar.composewizard.shared.LibraryType, Boolean>() }
            
            // Remember version for library loading (snapshot before Refresh)
            var versionForLibraries by remember { mutableStateOf("") }
            
            // Function to load library versions
            suspend fun loadLibraryVersions(versionToLoad: String) {
                if (versionToLoad.isEmpty()) return
                
                println("DEBUG UI: Loading library versions for Compose $versionToLoad (dev=${state.enableDevVersions})")
                libraryVersions.clear()
                libraryFromBundle.clear()
                
                // Calculate shouldShowNavigation based on versionToLoad (not cached state.composeVersion)
                val shouldShowNavigationForVersion = isComposeVersionLessThan(versionToLoad, "1.10.0")
                val shouldShowBundledHotReloadForVersion = state.desktop && !isComposeVersionLessThan(versionToLoad, "1.10.0-beta01") && versionToLoad.isNotEmpty()
                
                val typesToLoad = io.github.heisiar.composewizard.shared.LibraryType.values()
                    .filter { 
                        when (it) {
                            io.github.heisiar.composewizard.shared.LibraryType.HOT_RELOAD -> shouldShowBundledHotReloadForVersion
                            io.github.heisiar.composewizard.shared.LibraryType.NAVIGATION -> shouldShowNavigationForVersion
                            else -> true
                        }
                    }
                
                kotlinx.coroutines.coroutineScope {
                    typesToLoad.forEach { type ->
                        launch {
                            var version = cache.getLibraryVersion(versionToLoad, type)
                            
                            // Poll if version is still null (being resolved)
                            // Navigation may need more time due to fallback chain (up to 30 versions)
                            val maxAttempts = if (type == io.github.heisiar.composewizard.shared.LibraryType.NAVIGATION) 150 else 50
                            var attempts = 0
                            while (version == null && attempts < maxAttempts) {
                                kotlinx.coroutines.delay(100)
                                version = cache.getLibraryVersion(versionToLoad, type)
                                attempts++
                            }
                            
                            if (attempts > 0) {
                                println("DEBUG UI: ${type.displayName} loaded after $attempts attempts (${attempts * 100}ms)")
                            }
                            
                            // Handle result
                            if (version != null) {
                                libraryVersions[type] = version
                                if (version.isNotEmpty()) {
                                    libraryFromBundle[type] = cache.isLibraryFromBundle(versionToLoad, type)
                                    println("DEBUG UI: Loaded ${type.displayName}: $version (fromBundle=${libraryFromBundle[type]})")
                                    
                                    // Update state for hot reload
                                    if (type == io.github.heisiar.composewizard.shared.LibraryType.HOT_RELOAD) {
                                        state.hotReloadVersion = version
                                        state.includeHotReload = true
                                    }
                                } else {
                                    println("DEBUG UI: ${type.displayName} not found for $versionToLoad")
                                }
                            } else {
                                println("DEBUG UI: ⚠️ Timeout waiting for ${type.displayName} after $maxAttempts attempts (${maxAttempts * 100}ms)")
                            }
                        }
                    }
                }
            }
            
            // Load libraries when version changes (user selection)
            // But ignore changes when dev versions are loading (Refresh in progress)
            // Include shouldShowNavigation in dependencies to reload when Navigation visibility changes
            LaunchedEffect(state.composeVersion, state.enableDevVersions, shouldShowBundledHotReload, shouldShowNavigation) {
                if (state.composeVersion.isEmpty()) return@LaunchedEffect
                
                val devVersions = if (state.enableDevVersions) cache.getDevVersions() else null
                val isDevLoading = state.enableDevVersions && devVersions == null
                
                if (isDevLoading) {
                    println("DEBUG UI: Ignoring version change to ${state.composeVersion} (dev versions loading, Refresh in progress)")
                } else {
                    println("DEBUG UI: Version changed to ${state.composeVersion}, saving and loading libraries")
                    versionForLibraries = state.composeVersion
                    loadLibraryVersions(state.composeVersion)
                }
            }
            
            // Reload libraries when cache is invalidated (Refresh button)
            LaunchedEffect(Unit) {
                cache.cacheInvalidated.collect {
                    println("DEBUG UI: Cache invalidation event received, reloading libraries for saved version: $versionForLibraries")
                    if (versionForLibraries.isNotEmpty()) {
                        loadLibraryVersions(versionForLibraries)
                    }
                }
            }
            
            LaunchedEffect(state.desktop, state.composeVersion, shouldShowOptionalHotReload, shouldShowBundledHotReload) {
                println("DEBUG Hot Reload: desktop=${state.desktop}, version=${state.composeVersion}, shouldShowOptional=$shouldShowOptionalHotReload, shouldShowBundled=$shouldShowBundledHotReload, hotReloadVersion=$hotReloadVersion")
                println("DEBUG Lifecycle: compose=${state.composeVersion}, lifecycle=$lifecycleVersion")
            }
            
            val isFallback = remember(state.enableDevVersions) { 
                val result = if (state.enableDevVersions) {
                    cache.isUsingDevFallbackVersions()
                } else {
                    cache.isUsingFallbackVersions()
                }
                println("DEBUG WizardMainContent: isFallback=$result, enableDevVersions=${state.enableDevVersions}")
                result
            }
            val isLifecycleFallback = remember(state.composeVersion, libraryVersions[io.github.heisiar.composewizard.shared.LibraryType.LIFECYCLE]) {
                val lifecycle = libraryVersions[io.github.heisiar.composewizard.shared.LibraryType.LIFECYCLE]
                if (lifecycle != null && lifecycle.isNotEmpty()) {
                    cache.isLifecycleFallback(state.composeVersion)
                } else {
                    false
                }
            }
                        
            Text("Libraries", style = JewelTheme.defaultTextStyle)
            
            Spacer(modifier = Modifier.height(LIBRARIES_SECTION_SPACING))
            
            // Show loading state if compose version not selected yet
            if (state.composeVersion.isEmpty()) {
                // Always show max possible count (with Hot Reload) since we don't know the version yet
                val rightColumnMaxCount = RIGHT_COLUMN_BASE_LIBRARIES_COUNT + 1
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
                    ) {
                        repeat(LEFT_COLUMN_LIBRARIES_COUNT) {
                            SkeletonText(width = 180.dp)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
                    ) {
                        repeat(rightColumnMaxCount) {
                            SkeletonText(width = 180.dp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                val currentBundle = io.github.heisiar.composewizard.shared.ComposeVersions.getLibraryBundle(state.composeVersion)
                
                // Pre-compute isPinned for all library types
                val isPinnedMap = io.github.heisiar.composewizard.shared.LibraryType.entries.associateWith { type ->
                    val versionInCurrentBundle = currentBundle?.getVersion(type)
                    versionInCurrentBundle.isNullOrEmpty()
                }
                
                // Helper to render library option with skeleton support
                @Composable
                fun LibraryOption(
                    type: io.github.heisiar.composewizard.shared.LibraryType,
                    checked: Boolean,
                    onToggle: () -> Unit,
                    label: String,
                    enabled: Boolean = true,
                    disabledTooltip: String? = null
                ) {
                    val version = libraryVersions[type]
                    
                    // Show skeleton while this specific library is loading (version not yet in map)
                    if (version == null) {
                        SkeletonText(width = 180.dp)
                    } else if (version.isNotEmpty()) {
                        // Show option only if version is found
                        val isFromBundle = libraryFromBundle[type] == true
                        val isBundledAndDisabled = !enabled && isFromBundle
                        
                        val trailingContent: (@Composable () -> Unit) = {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = version,
                                    style = org.jetbrains.jewel.foundation.theme.JewelTheme.defaultTextStyle,
                                    color = if (enabled) org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.normal
                                            else org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.normal.copy(alpha = 0.5f)
                                )
                                
                                // Show bundled indicator for disabled bundled libraries (like hot reload >= 1.10.0)
                                if (isBundledAndDisabled) {
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(3.dp))
                                    BundledLibraryIndicator()
                                }
                                // Show pinned indicator for enabled libraries from bundle fallback
                                else if (isFromBundle) {
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(3.dp))
                                    val isPinned = isPinnedMap[type] ?: false
                                    PinnedVersionIndicator(isPinned = isPinned)
                                }
                                
                                LibraryVersionCopyIcon(version = version)
                            }
                        }
                        CheckboxOption(
                            checked = checked,
                            onToggle = onToggle,
                            label = label,
                            enabled = enabled,
                            trailingContent = trailingContent,
                            disabledTooltip = disabledTooltip ?: "Included in the base template and cannot be disabled"
                        )
                    }
                    // If version not found (empty string) and not loading - don't show anything
                }
                
                // Lifecycle version dropdown with configurable versions
                @Composable
                fun LifecycleVersionDropdown() {
                    val currentVersion = libraryVersions[io.github.heisiar.composewizard.shared.LibraryType.LIFECYCLE] ?: ""
                    
                    if (currentVersion.isEmpty()) {
                        // Show skeleton while loading
                        SkeletonText(width = 180.dp)
                        return
                    }
                    
                    // Get available versions from Maven and filter
                    val allAvailableVersions = cache.getLifecycleAvailableVersions()
                    val filteredVersions = remember(allAvailableVersions, currentVersion) {
                        if (allAvailableVersions.isNotEmpty() && currentVersion.isNotEmpty()) {
                            lifecycleVersionService.filterVersionsForDropdown(
                                allVersions = allAvailableVersions,
                                currentVersion = currentVersion,
                                maxCount = 5
                            )
                        } else {
                            listOf(currentVersion)
                        }
                    }
                    
                    // Find index of current version (should be 0, first in list)
                    val selectedIndex = remember(currentVersion, filteredVersions) {
                        filteredVersions.indexOf(currentVersion).coerceAtLeast(0)
                    }
                    
                    Row(
                        modifier = Modifier.height(28.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        org.jetbrains.jewel.ui.component.Tooltip(
                            tooltip = { Text("Included in the base template and cannot be disabled") }
                        ) {
                            org.jetbrains.jewel.ui.component.CheckboxRow(
                                checked = true,
                                onCheckedChange = { },
                                enabled = false
                            ) {
                                Text(
                                    text = "Lifecycle",
                                    style = org.jetbrains.jewel.foundation.theme.JewelTheme.defaultTextStyle
                                )
                            }
                        }
                        
                        // Version dropdown using Jewel ListComboBox
                        org.jetbrains.jewel.ui.component.ListComboBox(
                            items = filteredVersions,
                            selectedIndex = selectedIndex,
                            onSelectedItemChange = { index ->
                                if (index in filteredVersions.indices) {
                                    val newVersion = filteredVersions[index]
                                    state.lifecycleVersion = newVersion
                                    println("DEBUG: Lifecycle version changed to $newVersion")
                                }
                            },
                            modifier = Modifier
                                .width(200.dp)
                                .pointerHoverIcon(
                                    PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))
                                ),
                            maxPopupHeight = 280.dp
                        )
                        
                        LibraryVersionCopyIcon(version = filteredVersions.getOrNull(selectedIndex) ?: currentVersion)
                    }
                }
                
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
                ) {
                    // Lifecycle - always included with version dropdown
                    LifecycleVersionDropdown()
                    
                    LibraryOption(
                        type = io.github.heisiar.composewizard.shared.LibraryType.MATERIAL3_ADAPTIVE,
                        checked = state.includeMaterial3Adaptive,
                        onToggle = { state.includeMaterial3Adaptive = !state.includeMaterial3Adaptive },
                        label = "Material3 Adaptive"
                    )
                    
                    LibraryOption(
                        type = io.github.heisiar.composewizard.shared.LibraryType.NAVIGATION_EVENT,
                        checked = state.includeNavigationEvent,
                        onToggle = { state.includeNavigationEvent = !state.includeNavigationEvent },
                        label = "NavigationEvent"
                    )
                    
                    LibraryOption(
                        type = io.github.heisiar.composewizard.shared.LibraryType.WINDOW,
                        checked = state.includeWindow,
                        onToggle = { state.includeWindow = !state.includeWindow },
                        label = "Window"
                    )
                }
                
                // Right column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_SPACING)
                ) {
                    LibraryOption(
                        type = io.github.heisiar.composewizard.shared.LibraryType.MATERIAL3,
                        checked = state.includeMaterial3,
                        onToggle = { state.includeMaterial3 = !state.includeMaterial3 },
                        label = "Material3"
                    )
                    
                    // Navigation - only for versions < 1.10.0
                    if (shouldShowNavigation) {
                        LibraryOption(
                            type = io.github.heisiar.composewizard.shared.LibraryType.NAVIGATION,
                            checked = state.includeNavigation,
                            onToggle = { state.includeNavigation = !state.includeNavigation },
                            label = "Navigation"
                        )
                    }
                    
                    LibraryOption(
                        type = io.github.heisiar.composewizard.shared.LibraryType.NAVIGATION3,
                        checked = state.includeNavigation3,
                        onToggle = { state.includeNavigation3 = !state.includeNavigation3 },
                        label = "Navigation3"
                    )
                    
                    LibraryOption(
                        type = io.github.heisiar.composewizard.shared.LibraryType.SAVED_STATE,
                        checked = state.includeSavedState,
                        onToggle = { state.includeSavedState = !state.includeSavedState },
                        label = "SavedState"
                    )
                    
                    // Optional hot reload for versions < 10.0.0-beta01
                    if (shouldShowOptionalHotReload) {
                        CheckboxOption(
                            checked = state.includeHotReload,
                            onToggle = { 
                                state.includeHotReload = !state.includeHotReload
                            },
                            label = "Compose Hot Reload${if (hotReloadVersion.isNotEmpty()) " $hotReloadVersion" else ""}"
                        )
                    }
                    
                    // Bundled hot reload for versions >= 10.0.0-beta01
                    if (shouldShowBundledHotReload) {
                        LibraryOption(
                            type = io.github.heisiar.composewizard.shared.LibraryType.HOT_RELOAD,
                            checked = true,
                            onToggle = { },
                            label = "Compose Hot Reload",
                            enabled = false,
                            disabledTooltip = "Compose Hot Reload is bundled and cannot be disabled"
                        )
                    }
                }
                }
            }
            
            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                                JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                                JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                                JewelTheme.globalColors.text.normal.copy(alpha = 0.05f),
                                androidx.compose.ui.graphics.Color.Transparent
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))
            
            // Show OptionsSection (it will update reactively when state.composeVersion is set)
            OptionsSection(
                git = state.git,
                tests = state.tests,
                onGitToggle = {
                    state.git = !state.git
                    ComposeWizardUsageCollector.logGitToggled(state.git)
                },
                onTestsToggle = {
                    state.tests = !state.tests
                    ComposeWizardUsageCollector.logTestsToggled(state.tests)
                }
            )
        }

        FooterSection(state = state)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProjectLocationSection(
    state: WizardState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathFocused: Boolean,
    projectPathInteractionSource: MutableInteractionSource,
    onBrowseFolder: () -> String?
) {
    Column {
        Text("Location", style = JewelTheme.defaultTextStyle)
        
        Box(modifier = Modifier.compactVerticalSpacing()) {
            ProjectLocationField(
                projectPathState = projectPathState,
                projectPathError = state.projectPathError,
                projectPathFocused = projectPathFocused,
                projectPathInteractionSource = projectPathInteractionSource,
                onPathChanged = { },
                onBrowse = {
                    onBrowseFolder()?.let { state.projectPath = it }
                }
            )
        }

        ProjectPathHint(
            projectPath = state.projectPath,
            projectName = state.projectName
        )
    }
}

private fun Modifier.compactVerticalSpacing(): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val verticalOffset = TEXTFIELD_VERTICAL_OFFSET.roundToPx()
    val heightReduction = TEXTFIELD_HEIGHT_REDUCTION.roundToPx()
    
    layout(placeable.width, placeable.height - heightReduction) {
        placeable.place(0, -verticalOffset)
    }
}

@Composable
private fun FooterSection(state: WizardState) {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    val isInternalMode = remember { com.intellij.openapi.application.ApplicationManager.getApplication().isInternal }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = if (PlatformDetector.isAndroidStudio) 16.dp else 0.dp)
    ) {
        Text(
            text = "v1.0.0",
            style = JewelTheme.defaultTextStyle,
            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .let { modifier ->
                    if (!isInternalMode) {
                        modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime < 600) {
                                        clickCount++
                                        if (clickCount >= 3) {
                                            val wasVisible = state.devCheckboxVisible
                                            val newVisibility = !wasVisible
                                            if (newVisibility && !wasVisible) {
                                                ComposeWizardUsageCollector.logDevVersionsUnlocked()
                                            }
                                            state.devCheckboxVisible = newVisibility
                                            io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().devCheckboxVisibleByUser = newVisibility
                                            if (!newVisibility) {
                                                state.enableDevVersions = false
                                                io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = false
                                            }
                                            clickCount = 0
                                        }
                                    } else {
                                        clickCount = 1
                                    }
                                    lastClickTime = currentTime
                                }
                            )
                        }
                    } else {
                        modifier
                    }
                }
        )

        if (state.hasNoTargets) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = "At least one platform must be selected",
                        color = JewelTheme.globalColors.text.error,
                        style = JewelTheme.defaultTextStyle
                    )
                }
                Text(
                    text = "⚡",
                    fontSize = 14.sp,
                    color = JewelTheme.globalColors.text.error
                )
            }
        }
    }
}

private fun isComposeVersionLessThan(version: String, threshold: String): Boolean {
    // Parse version: "1.9.2" or "1.10.0-beta01" or "1.10.0-beta01+dev3194"
    val versionBase = version.split("+").first() // Remove dev suffix
    val thresholdBase = threshold.split("+").first()
    
    // Split into numeric and qualifier parts
    val versionNumeric = versionBase.split("-").first()
    val versionQualifier = versionBase.substringAfter("-", "")
    
    val thresholdNumeric = thresholdBase.split("-").first()
    val thresholdQualifier = thresholdBase.substringAfter("-", "")
    
    // Compare numeric parts (1.9.2 vs 1.10.0)
    val versionParts = versionNumeric.split(".").map { it.toIntOrNull() ?: 0 }
    val thresholdParts = thresholdNumeric.split(".").map { it.toIntOrNull() ?: 0 }
    
    for (i in 0 until maxOf(versionParts.size, thresholdParts.size)) {
        val v = versionParts.getOrNull(i) ?: 0
        val t = thresholdParts.getOrNull(i) ?: 0
        if (v < t) return true
        if (v > t) return false
    }
    
    // Numeric parts are equal, compare qualifiers
    // If threshold has qualifier but version doesn't, version is greater (stable > beta)
    if (thresholdQualifier.isNotEmpty() && versionQualifier.isEmpty()) {
        return false
    }
    
    // If version has qualifier but threshold doesn't, version is less (beta < stable)
    if (versionQualifier.isNotEmpty() && thresholdQualifier.isEmpty()) {
        return true
    }
    
    // Both have qualifiers or both don't - compare lexicographically
    return versionQualifier < thresholdQualifier
}

