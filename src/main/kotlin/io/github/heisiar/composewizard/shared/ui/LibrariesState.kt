package io.github.heisiar.composewizard.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import io.github.heisiar.composewizard.shared.services.NetworkConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LibrariesState(
    private val cache: ComposeVersionCache,
    private val wizardState: WizardState
) {
    val libraryVersions = mutableStateMapOf<LibraryType, String>()
    val isFromFallback = mutableStateMapOf<LibraryType, Boolean>()
    var versionForLibraries by mutableStateOf("")
    var hotReloadGithubVersion by mutableStateOf<String?>(null)
    
    companion object {
        private const val MAX_CACHE_WAIT_ATTEMPTS = 16
        private const val CACHE_POLL_DELAY_MS = 500L
    }
    
    suspend fun loadLibraryVersions(versionToLoad: String) {
        if (versionToLoad.isEmpty()) return
        
        libraryVersions.clear()
        isFromFallback.clear()
        
        val numericVersionToLoad = versionToLoad.split("-").first().split("+").first()
        val shouldShowNavigationForVersion = isComposeVersionLessThan(versionToLoad, "1.10.0-alpha02")
        val shouldShowNavigation3AndNavigationEventForVersion = !isComposeVersionLessThan(versionToLoad, "1.10.0-alpha02") && versionToLoad.isNotEmpty()
        val shouldShowOptionalHotReloadForVersion = wizardState.desktop && isComposeVersionLessThan(versionToLoad, "1.10.0-beta01")
        val shouldShowBundledHotReloadForVersion = wizardState.desktop && !isComposeVersionLessThan(versionToLoad, "1.10.0-beta01") && versionToLoad.isNotEmpty()
        
        val typesToLoad = LibraryType.values().filter { 
            when (it) {
                LibraryType.HOT_RELOAD -> shouldShowOptionalHotReloadForVersion || shouldShowBundledHotReloadForVersion
                LibraryType.NAVIGATION -> shouldShowNavigationForVersion
                LibraryType.NAVIGATION3 -> shouldShowNavigation3AndNavigationEventForVersion
                LibraryType.NAVIGATION_EVENT -> shouldShowNavigation3AndNavigationEventForVersion
                else -> true
            }
        }
        
        val needsUpdate = mutableSetOf<LibraryType>()
        
        typesToLoad.forEach { type ->
            cache.clearNotFoundMarker(versionToLoad, type)
            
            val cachedVersion = cache.getRawCachedVersion(versionToLoad, type)
            if (cachedVersion != null && cachedVersion != "NOT_FOUND") {
                libraryVersions[type] = cachedVersion
                isFromFallback[type] = cache.isLibraryFromBundle(versionToLoad, type)
                
                if (type == LibraryType.HOT_RELOAD && cachedVersion.isNotEmpty()) {
                    wizardState.hotReloadVersion = cachedVersion
                    wizardState.includeHotReload = true
                    hotReloadGithubVersion = cache.getHotReloadGithubVersion(versionToLoad)
                }
            } else {
                needsUpdate.add(type)
            }
        }
        
        kotlinx.coroutines.coroutineScope {
            needsUpdate.forEach { type ->
                launch {
                    cache.getLibraryVersion(versionToLoad, type)
                    
                    kotlinx.coroutines.withTimeoutOrNull(NetworkConfig.TOTAL_RESOLVE_TIMEOUT_MS + 2000) {
                        while (true) {
                            val rawCached = cache.getRawCachedVersion(versionToLoad, type)
                            if (rawCached != null) {
                                if (rawCached == "NOT_FOUND") {
                                    libraryVersions[type] = "N/A"
                                    isFromFallback[type] = false
                                } else {
                                    libraryVersions[type] = rawCached
                                    isFromFallback[type] = cache.isLibraryFromBundle(versionToLoad, type)
                                    
                                    if (type == LibraryType.HOT_RELOAD) {
                                        wizardState.hotReloadVersion = rawCached
                                        wizardState.includeHotReload = true
                                        hotReloadGithubVersion = cache.getHotReloadGithubVersion(versionToLoad)
                                    }
                                }
                                break
                            }
                            delay(300)
                        }
                    } ?: run {
                        libraryVersions[type] = "N/A"
                        isFromFallback[type] = false
                    }
                }
            }
        }
    }
}

@Composable
fun rememberLibrariesState(
    cache: ComposeVersionCache,
    wizardState: WizardState
): LibrariesState {
    return remember(cache, wizardState) {
        LibrariesState(cache, wizardState)
    }
}
