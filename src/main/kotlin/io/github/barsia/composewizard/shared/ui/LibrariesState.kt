package io.github.barsia.composewizard.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.intellij.openapi.diagnostic.Logger
import io.github.barsia.composewizard.shared.LibraryType
import io.github.barsia.composewizard.shared.services.ComposeVersionCache
import io.github.barsia.composewizard.shared.services.NetworkConfig
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
    var isLoadingVersions by mutableStateOf(false)
    
    private val LOG = Logger.getInstance(LibrariesState::class.java)
    
    companion object {
        private const val MAX_CACHE_WAIT_ATTEMPTS = 16
        private const val CACHE_POLL_DELAY_MS = 500L
    }
    
    fun clearAllVersions() {
        LOG.info("[LibrariesState] ❌ clearAllVersions() called! Stack trace:", Exception("Stack trace"))
        libraryVersions.clear()
        isFromFallback.clear()
        versionForLibraries = ""
        hotReloadGithubVersion = null
        isLoadingVersions = false
    }
    
    suspend fun loadLibraryVersions(versionToLoad: String) {
        if (versionToLoad.isEmpty()) {
            LOG.info("[LibrariesState] loadLibraryVersions: versionToLoad is empty, skipping")
            return
        }
        
        LOG.info("[LibrariesState] ========== START loadLibraryVersions for version: $versionToLoad ==========")
        LOG.info("[LibrariesState] Current state: libraryVersions.size=${libraryVersions.size}, isLoadingVersions=$isLoadingVersions")
        
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
        
        LOG.info("[LibrariesState] Types to load: ${typesToLoad.joinToString()}")
        
        val tempVersions = mutableMapOf<LibraryType, String>()
        val tempFallback = mutableMapOf<LibraryType, Boolean>()
        val needsUpdate = mutableSetOf<LibraryType>()
        
        typesToLoad.forEach { type ->
            cache.clearNotFoundMarker(versionToLoad, type)
            
            val cachedVersion = cache.getRawCachedVersion(versionToLoad, type)
            LOG.info("[LibrariesState] Cache check for $type: cachedVersion=$cachedVersion")
            
            if (cachedVersion != null && cachedVersion != "NOT_FOUND") {
                tempVersions[type] = cachedVersion
                tempFallback[type] = cache.isLibraryFromBundle(versionToLoad, type)
                LOG.info("[LibrariesState] ✅ Using cached version for $type: $cachedVersion (fallback=${tempFallback[type]})")
                
                if (type == LibraryType.HOT_RELOAD && cachedVersion.isNotEmpty()) {
                    wizardState.hotReloadVersion = cachedVersion
                    hotReloadGithubVersion = cache.getHotReloadGithubVersion(versionToLoad)
                    wizardState.bundledHotReloadVersion = hotReloadGithubVersion
                }
            } else {
                needsUpdate.add(type)
                LOG.info("[LibrariesState] ⏳ Need to fetch $type (not in cache)")
            }
        }
        
        LOG.info("[LibrariesState] Cache summary: cached=${tempVersions.size}, needsUpdate=${needsUpdate.size}")
        
        if (needsUpdate.isEmpty()) {
            LOG.info("[LibrariesState] 🚀 ALL in cache! Updating immediately without loading state")
            libraryVersions.clear()
            libraryVersions.putAll(tempVersions)
            isFromFallback.clear()
            isFromFallback.putAll(tempFallback)
            LOG.info("[LibrariesState] ========== END (from cache) ==========")
            return
        }
        
        LOG.info("[LibrariesState] ⏳ Setting isLoadingVersions=true (need to fetch ${needsUpdate.size} types)")
        isLoadingVersions = true
        libraryVersions.clear()
        libraryVersions.putAll(tempVersions)
        isFromFallback.clear()
        isFromFallback.putAll(tempFallback)
        
        kotlinx.coroutines.coroutineScope {
            needsUpdate.forEach { type ->
                launch {
                    LOG.info("[LibrariesState] 📡 Fetching $type from network/cache...")
                    cache.getLibraryVersion(versionToLoad, type)
                    
                    kotlinx.coroutines.withTimeoutOrNull(NetworkConfig.TOTAL_RESOLVE_TIMEOUT_MS + 2000) {
                        var checkCount = 0
                        while (true) {
                            val rawCached = cache.getRawCachedVersion(versionToLoad, type)
                            if (rawCached != null) {
                                if (rawCached == "NOT_FOUND") {
                                    libraryVersions[type] = "N/A"
                                    isFromFallback[type] = false
                                    LOG.info("[LibrariesState] ❌ $type: NOT_FOUND")
                                } else {
                                    libraryVersions[type] = rawCached
                                    isFromFallback[type] = cache.isLibraryFromBundle(versionToLoad, type)
                                    LOG.info("[LibrariesState] ✅ $type fetched: $rawCached (checks=$checkCount)")
                                    
                                    if (type == LibraryType.HOT_RELOAD) {
                                        wizardState.hotReloadVersion = rawCached
                                        hotReloadGithubVersion = cache.getHotReloadGithubVersion(versionToLoad)
                                        wizardState.bundledHotReloadVersion = hotReloadGithubVersion
                                    }
                                }
                                break
                            }
                            checkCount++
                            delay(50)
                        }
                    } ?: run {
                        libraryVersions[type] = "N/A"
                        isFromFallback[type] = false
                        LOG.info("[LibrariesState] ⏱️ $type: TIMEOUT")
                    }
                }
            }
        }
        
        LOG.info("[LibrariesState] ✅ All fetches complete, setting isLoadingVersions=false")
        isLoadingVersions = false
        LOG.info("[LibrariesState] ========== END (after network fetch) ==========")
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
