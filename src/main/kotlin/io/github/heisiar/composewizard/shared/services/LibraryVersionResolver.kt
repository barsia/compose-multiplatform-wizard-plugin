package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class LibraryVersionResolver(
    private val scope: CoroutineScope,
    private val cacheManager: LibraryCacheManager,
    private val libraryVersionService: ComposeLibraryVersionService,
    private val getAvailableVersions: () -> List<String>,
    private val getLibraryAvailableVersions: (LibraryType) -> List<String>
) {
    
    private val logger = Logger.getInstance(LibraryVersionResolver::class.java)
    
    private val lifecycleResolvingVersions = mutableSetOf<String>()
    private val webUIResultsCache = mutableMapOf<String, ComposeLibraryVersionService.LibraryVersionsResult?>()
    private val webUIFetchMutexes = mutableMapOf<String, Mutex>()
    
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
        
        if (cached != null && cached != "NOT_FOUND") {
            return cached
        }
        
        if (!isResolvingLibrary(composeVersion, type)) {
            resolveLibraryVersionInBackground(composeVersion, type)
        }
        
        return null
    }
    
    fun clearNotFoundMarker(composeVersion: String, type: LibraryType) {
        val versionsMap = cacheManager.getVersionsMap(type)
        if (versionsMap[composeVersion] == "NOT_FOUND") {
            versionsMap.remove(composeVersion)
        }
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
                val result = withTimeoutOrNull(NetworkConfig.TOTAL_RESOLVE_TIMEOUT_MS) {
                    resolveLibraryVersionInternal(composeVersion, type)
                }
                
                if (result == null) {
                    val bundle = ComposeVersions.getLibraryBundle(composeVersion)
                    val bundleVersion = bundle?.getVersion(type)
                    if (bundleVersion != null) {
                        cacheLibrary(composeVersion, type, bundleVersion, isFromFallback = false)
                    } else {
                        cacheLibrary(composeVersion, type, "NOT_FOUND", isFromFallback = false)
                    }
                }
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve ${type.displayName} version for Compose $composeVersion: ${e.message}")
                cacheLibrary(composeVersion, type, "NOT_FOUND", isFromFallback = false)
            }
            
            synchronized(lifecycleResolvingVersions) {
                lifecycleResolvingVersions.remove(key)
            }
        }
    }
    
    private suspend fun resolveLibraryVersionInternal(composeVersion: String, type: LibraryType) {
        if (type == LibraryType.NAVIGATION) {
            if (!VersionComparison.isComposeVersionLessThan(composeVersion, "1.10.0-alpha02")) {
                cacheLibrary(composeVersion, type, "", isFromFallback = false)
                return
            }
        }
                
        if (type == LibraryType.HOT_RELOAD) {
            if (VersionComparison.isComposeVersionLessThan(composeVersion, "1.10.0-beta01")) {
                cacheLibrary(composeVersion, type, ComposeVersions.COMPOSE_HOT_RELOAD_VERSION, isFromFallback = false)
                return
            } else {
                val githubVersion = libraryVersionService.fetchHotReloadVersion(composeVersion)
                
                if (githubVersion != null) {
                    cacheManager.cacheHotReloadGithubVersion(composeVersion, githubVersion)
                    cacheLibrary(composeVersion, type, githubVersion, isFromFallback = false)
                    return
                }
                
                val mavenVersions = try {
                    val hotReloadService = HotReloadVersionService()
                    val metadata = LibraryRegistry.getMetadata(LibraryType.HOT_RELOAD)
                    hotReloadService.fetchVersions(metadata.mavenMetadataUrl)
                } catch (e: Exception) {
                    emptyList()
                }
                
                val firstMavenVersion = mavenVersions.firstOrNull()
                if (firstMavenVersion != null) {
                    cacheLibrary(composeVersion, type, firstMavenVersion, isFromFallback = false)
                    return
                }
                
                cacheLibrary(composeVersion, type, ComposeVersions.COMPOSE_HOT_RELOAD_VERSION, isFromFallback = false)
                return
            }
        }
        
        val mutex = synchronized(webUIFetchMutexes) {
            webUIFetchMutexes.getOrPut(composeVersion) { Mutex() }
        }
        
        val result = mutex.withLock {
            val cached = synchronized(webUIResultsCache) {
                webUIResultsCache[composeVersion]
            }
            
            if (cached != null) {
                logger.info("Using cached web UI result for $composeVersion")
                if (cached.pageExists) cached else null
            } else {
                logger.info("Fetching web UI for $composeVersion")
                var isNetworkError = false
                val fetchedResult = try {
                    libraryVersionService.fetchLibraryVersionsFromWebUI(composeVersion)
                } catch (e: Exception) {
                    logger.warn("Failed to fetch web UI for $composeVersion: ${e.message}")
                    isNetworkError = true
                    null
                }
                
                synchronized(webUIResultsCache) {
                    webUIResultsCache[composeVersion] = fetchedResult
                }
                
                if (isNetworkError || fetchedResult == null || !fetchedResult.pageExists) {
                    logger.info("Web UI result for $composeVersion: pageExists=${fetchedResult?.pageExists}, error=$isNetworkError")
                    null
                } else {
                    logger.info("Successfully fetched web UI for $composeVersion with ${fetchedResult.versions.size} libraries")
                    fetchedResult
                }
            }
        }
        
        if (result == null) {
            val bundle = ComposeVersions.getLibraryBundle(composeVersion)
            val bundleVersion = bundle?.getVersion(type)
            if (bundleVersion != null) {
                cacheLibrary(composeVersion, type, bundleVersion, isFromFallback = false)
                return
            }
            cacheLibrary(composeVersion, type, "NOT_FOUND", isFromFallback = false)
            return
        }
        
        val version = result.versions[type]
        if (version != null) {
            cacheLibrary(composeVersion, type, version, isFromFallback = false)
            return
        }
        
        logger.info("Library ${type.displayName} not found for $composeVersion, checking fallback versions")
        
        val libraryAvailableVersions = getLibraryAvailableVersions(type)
        if (libraryAvailableVersions.isNotEmpty()) {
            val stableLibraryVersions = libraryAvailableVersions
                .filter { !it.contains("+dev", ignoreCase = true) }
                .filter { !it.contains("-alpha") && !it.contains("-beta") && !it.contains("-rc") && !it.contains("-dev") }
                .sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
            
            val latestStableVersion = stableLibraryVersions.firstOrNull()
            if (latestStableVersion != null) {
                logger.info("Found latest stable ${type.displayName} version in Maven: $latestStableVersion")
                cacheLibrary(composeVersion, type, latestStableVersion, isFromFallback = true)
                return
            }
            
            val latestVersion = libraryAvailableVersions
                .filter { !it.contains("+dev", ignoreCase = true) }
                .sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
                .firstOrNull()
            
            if (latestVersion != null) {
                logger.info("Found latest ${type.displayName} version in Maven: $latestVersion")
                cacheLibrary(composeVersion, type, latestVersion, isFromFallback = true)
                return
            }
        }
        
        val availableVersions = getAvailableVersions()
        logger.info("Available Compose versions from Maven: ${availableVersions.take(10).joinToString()}")
        
        // For NavigationEvent and Navigation3, which appeared later, try newer versions first
        val shouldTryNewerVersions = (type == LibraryType.NAVIGATION_EVENT || type == LibraryType.NAVIGATION3) &&
                                      !VersionComparison.isComposeVersionLessThan(composeVersion, "1.10.0-alpha02")
        
        val fallbackVersions = if (shouldTryNewerVersions) {
            availableVersions
                .filter { version ->
                    !version.contains("+dev") && 
                    !VersionComparison.isComposeVersionLessThan(version, composeVersion) &&
                    version != composeVersion
                }
                .take(ComposeFallbackVersionGenerator.MAX_FALLBACK_VERSIONS)
        } else {
            ComposeFallbackVersionGenerator.generateFallbackVersions(composeVersion, availableVersions)
        }
        
        logger.info("Trying ${fallbackVersions.size} fallback versions for ${type.displayName}: ${fallbackVersions.joinToString()}")
        
        for (fallbackVersion in fallbackVersions) {
            logger.info("Checking fallback version $fallbackVersion for ${type.displayName}")
            
            val fallbackBundle = ComposeVersions.getLibraryBundle(fallbackVersion)
            val fallbackBundleVersion = fallbackBundle?.getVersion(type)
            if (fallbackBundleVersion != null) {
                logger.info("Found ${type.displayName} in bundle for fallback version $fallbackVersion: $fallbackBundleVersion")
                cacheLibrary(composeVersion, type, fallbackBundleVersion, isFromFallback = true)
                return
            }
            logger.info("Not found in bundle for $fallbackVersion, checking GitHub...")
            
            delay(100)
            
            val fallbackMutex = synchronized(webUIFetchMutexes) {
                webUIFetchMutexes.getOrPut(fallbackVersion) { Mutex() }
            }
            
            val fallbackResult = fallbackMutex.withLock {
                val cached = synchronized(webUIResultsCache) {
                    webUIResultsCache[fallbackVersion]
                }
                
                if (cached != null) {
                    logger.info("Using cached GitHub result for $fallbackVersion")
                    if (cached.pageExists) cached else null
                } else {
                    logger.info("Fetching GitHub for $fallbackVersion...")
                    try {
                        val fetched = libraryVersionService.fetchLibraryVersionsFromWebUI(fallbackVersion)
                        logger.info("GitHub fetch result for $fallbackVersion: pageExists=${fetched.pageExists}, libraries=${fetched.versions.size}")
                        synchronized(webUIResultsCache) {
                            webUIResultsCache[fallbackVersion] = fetched
                        }
                        if (fetched.pageExists) fetched else null
                    } catch (e: Exception) {
                        logger.warn("GitHub fetch failed for $fallbackVersion: ${e.message}")
                        null
                    }
                }
            }
            
            val fallbackLibVersion = fallbackResult?.versions?.get(type)
            if (fallbackLibVersion != null) {
                logger.info("Found ${type.displayName} in web UI for fallback version $fallbackVersion: $fallbackLibVersion")
                cacheLibrary(composeVersion, type, fallbackLibVersion, isFromFallback = true)
                return
            } else {
                logger.info("${type.displayName} not found in GitHub for $fallbackVersion")
            }
        }
        
        logger.warn("No version found for ${type.displayName} after checking all fallback versions and GitHub, trying bundle as last resort")
        
        val bundle = ComposeVersions.getLibraryBundle(composeVersion)
        val bundleVersion = bundle?.getVersion(type)
        if (bundleVersion != null) {
            logger.info("Using bundle version as last resort: $bundleVersion")
            cacheLibrary(composeVersion, type, bundleVersion, isFromFallback = true)
            return
        }
        
        cacheLibrary(composeVersion, type, "NOT_FOUND", isFromFallback = false)
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

