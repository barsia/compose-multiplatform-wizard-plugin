package io.github.heisiar.composewizard.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LibrariesState(
    private val cache: ComposeVersionCache,
    private val wizardState: WizardState
) {
    val libraryVersions = mutableStateMapOf<LibraryType, String>()
    val isFromFallback = mutableStateMapOf<LibraryType, Boolean>()
    var versionForLibraries by mutableStateOf("")
    
    suspend fun loadLibraryVersions(versionToLoad: String) {
        if (versionToLoad.isEmpty()) return
        
        libraryVersions.clear()
        isFromFallback.clear()
        
        val numericVersionToLoad = versionToLoad.split("-").first().split("+").first()
        val shouldShowNavigationForVersion = isComposeVersionLessThan(numericVersionToLoad, "1.10.0")
        val shouldShowNavigation3AndNavigationEventForVersion = !isComposeVersionLessThan(numericVersionToLoad, "1.10.0") && versionToLoad.isNotEmpty()
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
        
        kotlinx.coroutines.coroutineScope {
            typesToLoad.forEach { type ->
                launch {
                    var version = cache.getLibraryVersion(versionToLoad, type)
                    
                    val maxAttempts = if (type == LibraryType.NAVIGATION) 150 else 50
                    var attempts = 0
                    while (version == null && attempts < maxAttempts) {
                        delay(100)
                        version = cache.getLibraryVersion(versionToLoad, type)
                        attempts++
                    }
                    
                    if (version != null) {
                        libraryVersions[type] = version
                        if (version.isNotEmpty()) {
                            isFromFallback[type] = cache.isLibraryFromBundle(versionToLoad, type)
                            
                            if (type == LibraryType.HOT_RELOAD) {
                                wizardState.hotReloadVersion = version
                                wizardState.includeHotReload = true
                            }
                        }
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
