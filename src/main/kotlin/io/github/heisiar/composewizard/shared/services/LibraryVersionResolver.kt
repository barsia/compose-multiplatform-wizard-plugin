package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.LibraryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LibraryVersionResolver(
    private val scope: CoroutineScope,
    private val cacheManager: LibraryCacheManager,
    private val libraryVersionService: ComposeLibraryVersionService
) {
    
    private val logger = Logger.getInstance(LibraryVersionResolver::class.java)
    
    private val lifecycleResolvingVersions = mutableSetOf<String>()
    
    private val _lifecycleVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val lifecycleVersionUpdates = _lifecycleVersionUpdates.asSharedFlow()
    
    private val _material3VersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val material3VersionUpdates = _material3VersionUpdates.asSharedFlow()
    
    private val _material3AdaptiveVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val material3AdaptiveVersionUpdates = _material3AdaptiveVersionUpdates.asSharedFlow()
    
    private val _navigationVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val navigationVersionUpdates = _navigationVersionUpdates.asSharedFlow()
    
    private val _navigation3VersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val navigation3VersionUpdates = _navigation3VersionUpdates.asSharedFlow()
    
    private val _windowVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val windowVersionUpdates = _windowVersionUpdates.asSharedFlow()
    
    private val _savedStateVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val savedStateVersionUpdates = _savedStateVersionUpdates.asSharedFlow()
    
    private val _navigationEventVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val navigationEventVersionUpdates = _navigationEventVersionUpdates.asSharedFlow()
    
    private val _hotReloadVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val hotReloadVersionUpdates = _hotReloadVersionUpdates.asSharedFlow()
    
    fun getLibraryVersion(composeVersion: String, type: LibraryType): String? {
        val cached = cacheManager.getLibraryVersion(composeVersion, type)
        
        if (cached != null) {
            return cached
        }
        
        if (!isResolvingLibrary(composeVersion, type)) {
            resolveLibraryVersionInBackground(composeVersion, type)
        }
        
        return null
    }
    
    fun getLifecycleVersion(composeVersion: String): String? {
        return getLibraryVersion(composeVersion, LibraryType.LIFECYCLE)
    }
    
    fun isResolvingLibrary(composeVersion: String, type: LibraryType): Boolean {
        val key = "$composeVersion:${type.name}"
        return synchronized(lifecycleResolvingVersions) {
            lifecycleResolvingVersions.contains(key)
        }
    }
    
    private fun resolveLibraryVersionInBackground(composeVersion: String, type: LibraryType) {
        val key = "$composeVersion:${type.name}"
        
        val versionsMap = cacheManager.getVersionsMap(type)
        if (versionsMap.containsKey(composeVersion)) {
            return
        }
        
        synchronized(lifecycleResolvingVersions) {
            if (lifecycleResolvingVersions.contains(key)) {
                return
            }
            lifecycleResolvingVersions.add(key)
        }
        
        scope.launch {
            try {
                if (type == LibraryType.NAVIGATION) {
                    val baseVersion = composeVersion.split("+").first().split("-").first()
                    val parts = baseVersion.split(".")
                    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    
                    if (major > 1 || (major == 1 && minor >= 10)) {
                        cacheLibrary(composeVersion, type, "", isFromFallback = false)
                        return@launch
                    }
                }
                
                if (type == LibraryType.HOT_RELOAD) {
                    if (!VersionComparison.isComposeVersionLessThan(composeVersion, "1.10.0-beta01")) {
                        val hotReloadVersion = libraryVersionService.fetchHotReloadVersion(composeVersion)
                        if (hotReloadVersion != null) {
                            cacheLibrary(composeVersion, type, hotReloadVersion, isFromFallback = true)
                            return@launch
                        } else {
                            cacheLibrary(composeVersion, type, "", isFromFallback = false)
                            return@launch
                        }
                    } else {
                        cacheLibrary(composeVersion, type, ComposeVersions.COMPOSE_HOT_RELOAD_VERSION, isFromFallback = false)
                        return@launch
                    }
                }
                
                var isNetworkError = false
                val result = try {
                    libraryVersionService.fetchLibraryVersionsFromWebUI(composeVersion)
                } catch (e: Exception) {
                    isNetworkError = true
                    null
                }
                
                if (isNetworkError || result == null) {
                    val bundle = ComposeVersions.getLibraryBundle(composeVersion)
                    val bundleVersion = bundle?.getVersion(type)
                    if (bundleVersion != null) {
                        cacheLibrary(composeVersion, type, bundleVersion, isFromFallback = false)
                        return@launch
                    }
                    cacheLibrary(composeVersion, type, "", isFromFallback = false)
                    return@launch
                }
                
                val version = result.versions[type]
                if (version != null) {
                    cacheLibrary(composeVersion, type, version, isFromFallback = false)
                    return@launch
                }
                
                if (result.pageExists) {
                    val bundle = ComposeVersions.getLibraryBundle(composeVersion)
                    val bundleVersion = bundle?.getVersion(type)
                    if (bundleVersion != null) {
                        cacheLibrary(composeVersion, type, bundleVersion, isFromFallback = true)
                        return@launch
                    }
                }
                
                val fallbackVersions = libraryVersionService.generateFallbackVersions(composeVersion)
                var isRateLimited = false
                
                for (fallbackVersion in fallbackVersions) {
                    val fallbackBundle = ComposeVersions.getLibraryBundle(fallbackVersion)
                    val fallbackVersionLib = fallbackBundle?.getVersion(type)
                    if (fallbackVersionLib != null) {
                        cacheLibrary(composeVersion, type, fallbackVersionLib, isFromFallback = true)
                        return@launch
                    }
                    
                    if (!isRateLimited) {
                        delay(150)
                        val fallbackResult = try {
                            libraryVersionService.fetchLibraryVersionsFromWebUI(fallbackVersion)
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (fallbackResult == null) {
                            continue
                        }
                        
                        if (fallbackResult.isRateLimited) {
                            isRateLimited = true
                        } else {
                            val fallbackLibVersion = fallbackResult.versions[type]
                            if (fallbackLibVersion != null) {
                                cacheLibrary(composeVersion, type, fallbackLibVersion, isFromFallback = true)
                                return@launch
                            }
                        }
                    }
                }
                
                cacheLibrary(composeVersion, type, "", isFromFallback = false)
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve ${type.displayName} version for Compose $composeVersion: ${e.message}")
                cacheLibrary(composeVersion, type, "", isFromFallback = false)
            } finally {
                synchronized(lifecycleResolvingVersions) {
                    lifecycleResolvingVersions.remove(key)
                }
            }
        }
    }
    
    private suspend fun cacheLibrary(composeVersion: String, type: LibraryType, version: String, isFromFallback: Boolean) {
        cacheManager.cacheLibraryVersion(composeVersion, type, version, isFromFallback)
        
        val update = composeVersion to version
        when (type) {
            LibraryType.LIFECYCLE -> _lifecycleVersionUpdates.emit(update)
            LibraryType.MATERIAL3 -> _material3VersionUpdates.emit(update)
            LibraryType.MATERIAL3_ADAPTIVE -> _material3AdaptiveVersionUpdates.emit(update)
            LibraryType.NAVIGATION -> _navigationVersionUpdates.emit(update)
            LibraryType.NAVIGATION3 -> _navigation3VersionUpdates.emit(update)
            LibraryType.WINDOW -> _windowVersionUpdates.emit(update)
            LibraryType.SAVED_STATE -> _savedStateVersionUpdates.emit(update)
            LibraryType.NAVIGATION_EVENT -> _navigationEventVersionUpdates.emit(update)
            LibraryType.HOT_RELOAD -> _hotReloadVersionUpdates.emit(update)
        }
    }
}

