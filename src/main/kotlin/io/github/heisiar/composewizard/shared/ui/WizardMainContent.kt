package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch
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
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Text
import java.awt.Cursor

private val SPACING_BETWEEN_SECTIONS = 16.dp
private val SPACING_BEFORE_LOCATION = 0.dp
private val LOCATION_SECTION_VERTICAL_OFFSET = 4.dp
private val TEXTFIELD_VERTICAL_OFFSET = 8.dp
private val TEXTFIELD_HEIGHT_REDUCTION = 16.dp

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
                        modifier = Modifier.weight(0.6f)
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
                        modifier = Modifier.weight(0.4f),
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

            Spacer(modifier = Modifier.height(SPACING_BETWEEN_SECTIONS))

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

            val shouldShowHotReload = remember(state.desktop, state.composeVersion) {
                state.desktop && isComposeVersionLessThan(state.composeVersion, "1.10.0-beta01")
            }
            
            val hotReloadVersion = remember(shouldShowHotReload) {
                if (shouldShowHotReload) {
                    io.github.heisiar.composewizard.shared.ComposeVersions.COMPOSE_HOT_RELOAD_VERSION
                } else {
                    ""
                }
            }
            
            val cache = io.github.heisiar.composewizard.shared.services.ComposeVersionCache.getInstance()
            var lifecycleVersion by remember { mutableStateOf("") }
            var isResolvingLifecycle by remember { mutableStateOf(false) }
            
            // Library versions state
            val libraryVersions = remember { mutableStateMapOf<io.github.heisiar.composewizard.shared.LibraryType, String>() }
            val libraryFromBundle = remember { mutableStateMapOf<io.github.heisiar.composewizard.shared.LibraryType, Boolean>() }
            
            // Load all library versions and subscribe to lifecycle updates
            LaunchedEffect(state.composeVersion) {
                if (state.composeVersion.isEmpty()) return@LaunchedEffect
                
                // Load library versions with polling
                println("DEBUG UI: Loading library versions for Compose ${state.composeVersion}")
                libraryVersions.clear()
                libraryFromBundle.clear()
                
                io.github.heisiar.composewizard.shared.LibraryType.values().forEach { type ->
                    launch {
                        var version = cache.getLibraryVersion(state.composeVersion, type)
                        
                        // Poll if version is still null (being resolved)
                        var attempts = 0
                        while (version == null && attempts < 50) {
                            kotlinx.coroutines.delay(100)
                            version = cache.getLibraryVersion(state.composeVersion, type)
                            attempts++
                        }
                        
                        if (version != null && version.isNotEmpty()) {
                            libraryVersions[type] = version
                            libraryFromBundle[type] = cache.isLibraryFromBundle(state.composeVersion, type)
                            println("DEBUG UI: Loaded ${type.displayName}: $version (fromBundle=${libraryFromBundle[type]})")
                        }
                    }
                }
                
                // Lifecycle version handling
                println("DEBUG UI: Subscribing to Lifecycle version updates for Compose ${state.composeVersion}")
                
                val cachedLifecycle = cache.getLifecycleVersion(state.composeVersion)
                println("DEBUG UI: cachedLifecycle = '$cachedLifecycle' (null=${cachedLifecycle == null}, empty=${cachedLifecycle?.isEmpty()})")
                
                if (cachedLifecycle != null && cachedLifecycle.isNotEmpty()) {
                    println("DEBUG UI: ✅ Initial Lifecycle value from cache: $cachedLifecycle")
                    lifecycleVersion = cachedLifecycle
                    isResolvingLifecycle = false
                } else {
                    println("DEBUG UI: ⏳ No cached Lifecycle, resolving...")
                    lifecycleVersion = ""
                    isResolvingLifecycle = true
                }
                
                cache.lifecycleVersionUpdates.collect { (version, lifecycle) ->
                    println("DEBUG UI: Flow event received: version=$version, lifecycle='$lifecycle', current=${state.composeVersion}")
                    if (version == state.composeVersion) {
                        println("DEBUG UI: ✅ Received Lifecycle update: '$lifecycle' for Compose $version")
                        lifecycleVersion = lifecycle
                        isResolvingLifecycle = false
                    } else {
                        println("DEBUG UI: ⚠️ Ignoring Lifecycle update for different version: $version != ${state.composeVersion}")
                    }
                }
            }
            
            LaunchedEffect(state.desktop, state.composeVersion, shouldShowHotReload) {
                println("DEBUG Hot Reload: desktop=${state.desktop}, version=${state.composeVersion}, shouldShow=$shouldShowHotReload, hotReloadVersion=$hotReloadVersion")
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
            val isLifecycleFallback = remember(state.composeVersion, lifecycleVersion) {
                if (lifecycleVersion.isNotEmpty()) {
                    cache.isLifecycleFallback(state.composeVersion)
                } else {
                    false
                }
            }
            
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
                },
                composeVersion = state.composeVersion,
                kotlinVersion = remember(state.composeVersion) {
                    io.github.heisiar.composewizard.shared.ComposeVersions.getLibraryBundle(state.composeVersion)?.kotlinVersion
                        ?: io.github.heisiar.composewizard.shared.ComposeVersions.DEFAULT_KOTLIN_VERSION
                },
                lifecycleVersion = lifecycleVersion,
                hotReloadVersion = hotReloadVersion,
                isResolvingLifecycle = isResolvingLifecycle,
                isFallback = isFallback,
                isLifecycleFallback = isLifecycleFallback,
                libraryVersions = libraryVersions,
                libraryFromBundle = libraryFromBundle
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

