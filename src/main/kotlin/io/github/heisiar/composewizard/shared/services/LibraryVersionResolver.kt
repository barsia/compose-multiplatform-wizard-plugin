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
        val cached = cacheManager.getLibraryVersion(composeVersion, LibraryType.LIFECYCLE)
        
        if (cached != null) {
            return cached
        }
        
        if (!isResolvingLifecycle(composeVersion)) {
            resolveLifecycleVersionInBackground(composeVersion)
        }
        
        return null
    }
    
    fun isResolvingLifecycle(composeVersion: String): Boolean {
        return synchronized(lifecycleResolvingVersions) {
            lifecycleResolvingVersions.contains(composeVersion)
        }
    }
    
    fun isResolvingLibrary(composeVersion: String, type: LibraryType): Boolean {
        val key = "$composeVersion:${type.name}"
        return synchronized(lifecycleResolvingVersions) {
            lifecycleResolvingVersions.contains(key)
        }
    }
    
    private fun resolveLifecycleVersionInBackground(composeVersion: String) {
        val versionsMap = cacheManager.getVersionsMap(LibraryType.LIFECYCLE)
        if (versionsMap.containsKey(composeVersion)) {
            return
        }
        
        synchronized(lifecycleResolvingVersions) {
            if (lifecycleResolvingVersions.contains(composeVersion)) {
                return
            }
            lifecycleResolvingVersions.add(composeVersion)
        }
        
        scope.launch {
            try {
                val baseVersion = composeVersion.substringBefore("+dev")
                
                val result = libraryVersionService.fetchLifecycleFromWebUIWithStatus(composeVersion)
                
                if (result.lifecycle != null) {
                    cacheManager.cacheLifecycleVersion(composeVersion, result.lifecycle)
                    _lifecycleVersionUpdates.emit(composeVersion to result.lifecycle)
                    return@launch
                }
                
                if (result.pageExists) {
                    if (baseVersion != composeVersion) {
                        val baseLifecycle = libraryVersionService.fetchLifecycleFromWebUI(baseVersion)
                        if (baseLifecycle != null) {
                            cacheManager.cacheLifecycleVersion(composeVersion, baseLifecycle)
                            _lifecycleVersionUpdates.emit(composeVersion to baseLifecycle)
                            return@launch
                        }
                        
                        val baseBundle = ComposeVersions.getLibraryBundle(baseVersion)
                        if (baseBundle?.lifecycleVersion != null) {
                            cacheManager.cacheLifecycleVersion(composeVersion, baseBundle.lifecycleVersion, fromBundle = true)
                            _lifecycleVersionUpdates.emit(composeVersion to baseBundle.lifecycleVersion)
                            return@launch
                        }
                    }
                } else {
                    cacheManager.cacheLifecycleVersion(composeVersion, "")
                    _lifecycleVersionUpdates.emit(composeVersion to "")
                    return@launch
                }
                
                val fallbackVersions = libraryVersionService.generateFallbackVersions(baseVersion)
                var isRateLimited = false
                
                for (fallbackVersion in fallbackVersions) {
                    val bundle = ComposeVersions.getLibraryBundle(fallbackVersion)
                    if (bundle?.lifecycleVersion != null) {
                        cacheManager.cacheLifecycleVersion(composeVersion, bundle.lifecycleVersion, fromBundle = true)
                        _lifecycleVersionUpdates.emit(composeVersion to bundle.lifecycleVersion)
                        return@launch
                    }
                    
                    if (!isRateLimited) {
                        delay(150)
                        val fallbackResult = libraryVersionService.fetchLifecycleFromWebUIWithStatus(fallbackVersion)
                        
                        if (fallbackResult.isRateLimited) {
                            isRateLimited = true
                        } else if (fallbackResult.lifecycle != null) {
                            cacheManager.cacheLifecycleVersion(composeVersion, fallbackResult.lifecycle)
                            _lifecycleVersionUpdates.emit(composeVersion to fallbackResult.lifecycle)
                            return@launch
                        }
                    }
                }
                
                cacheManager.cacheLifecycleVersion(composeVersion, "")
                _lifecycleVersionUpdates.emit(composeVersion to "")
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve Lifecycle version for Compose $composeVersion: ${e.message}")
                cacheManager.cacheLifecycleVersion(composeVersion, "")
                _lifecycleVersionUpdates.emit(composeVersion to "")
            } finally {
                synchronized(lifecycleResolvingVersions) {
                    lifecycleResolvingVersions.remove(composeVersion)
                }
            }
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
                        cacheManager.cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                        synchronized(lifecycleResolvingVersions) {
                            lifecycleResolvingVersions.remove(key)
                        }
                        return@launch
                    }
                }
                
                if (type == LibraryType.HOT_RELOAD) {
                    val hotReloadVersion = libraryVersionService.fetchHotReloadVersion(composeVersion)
                    if (hotReloadVersion != null) {
                        cacheManager.cacheLibraryVersion(composeVersion, type, hotReloadVersion, fromBundle = true)
                        return@launch
                    } else {
                        cacheManager.cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                        return@launch
                    }
                }
                
                val result = libraryVersionService.fetchLibraryVersionsFromWebUI(composeVersion)
                val version = result.versions[type]
                
                if (version != null) {
                    cacheManager.cacheLibraryVersion(composeVersion, type, version, fromBundle = false)
                    return@launch
                }
                
                if (result.pageExists) {
                    val bundle = ComposeVersions.getLibraryBundle(composeVersion)
                    val bundleVersion = bundle?.getVersion(type)
                    if (bundleVersion != null) {
                        cacheManager.cacheLibraryVersion(composeVersion, type, bundleVersion, fromBundle = true)
                        return@launch
                    }
                    
                    val baseVersion = composeVersion.substringBefore("+dev")
                    if (baseVersion != composeVersion) {
                        val baseBundle = ComposeVersions.getLibraryBundle(baseVersion)
                        val baseVersionLib = baseBundle?.getVersion(type)
                        if (baseVersionLib != null) {
                            cacheManager.cacheLibraryVersion(composeVersion, type, baseVersionLib, fromBundle = true)
                            return@launch
                        }
                    }
                } else {
                    cacheManager.cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                    return@launch
                }
                
                val fallbackVersions = libraryVersionService.generateFallbackVersions(composeVersion)
                var isRateLimited = false
                
                for (fallbackVersion in fallbackVersions) {
                    val fallbackBundle = ComposeVersions.getLibraryBundle(fallbackVersion)
                    val fallbackVersionLib = fallbackBundle?.getVersion(type)
                    if (fallbackVersionLib != null) {
                        cacheManager.cacheLibraryVersion(composeVersion, type, fallbackVersionLib, fromBundle = true)
                        return@launch
                    }
                    
                    if (!isRateLimited) {
                        delay(150)
                        val fallbackResult = libraryVersionService.fetchLibraryVersionsFromWebUI(fallbackVersion)
                        
                        if (fallbackResult.isRateLimited) {
                            isRateLimited = true
                        } else {
                            val fallbackLibVersion = fallbackResult.versions[type]
                            if (fallbackLibVersion != null) {
                                cacheManager.cacheLibraryVersion(composeVersion, type, fallbackLibVersion, fromBundle = true)
                                return@launch
                            }
                        }
                    }
                }
                
                cacheManager.cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve ${type.displayName} version for Compose $composeVersion: ${e.message}")
                cacheManager.cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
            } finally {
                synchronized(lifecycleResolvingVersions) {
                    lifecycleResolvingVersions.remove(key)
                }
            }
        }
    }
}

