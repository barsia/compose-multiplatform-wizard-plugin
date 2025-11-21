package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.LibraryType

class LibraryCacheManager(private val state: ComposeVersionCacheState) {
    
    private val LOG = Logger.getInstance(LibraryCacheManager::class.java)
    
    companion object {
        private const val MAX_LIBRARY_CACHE_SIZE = 200
    }
    
    fun getVersionsMap(type: LibraryType): LinkedHashMap<String, String> {
        return state.getLibraryVersions(type)
    }
    
    fun getIsFromBundleMap(type: LibraryType): LinkedHashMap<String, Boolean> {
        return state.getLibraryIsFromBundle(type)
    }
    
    fun cacheLibraryVersion(composeVersion: String, type: LibraryType, version: String, isFromFallback: Boolean) {
        val versionsMap = getVersionsMap(type)
        val isFromBundleMap = getIsFromBundleMap(type)
        
        synchronized(versionsMap) {
            LOG.info("[CacheManager] 💾 Caching $type for $composeVersion: version=$version, isFromFallback=$isFromFallback, mapInstance=${System.identityHashCode(versionsMap)}")
            versionsMap[composeVersion] = version
            isFromBundleMap[composeVersion] = isFromFallback
            LOG.info("[CacheManager] 💾 After caching: cacheSize=${versionsMap.size}, keys=${versionsMap.keys.toList()}")
            
            while (versionsMap.size > MAX_LIBRARY_CACHE_SIZE) {
                val oldestKey = versionsMap.keys.first()
                versionsMap.remove(oldestKey)
                isFromBundleMap.remove(oldestKey)
            }
        }
    }
    
    fun cacheLifecycleVersion(composeVersion: String, lifecycleVersion: String, isFromFallback: Boolean = false) {
        cacheLibraryVersion(composeVersion, LibraryType.LIFECYCLE, lifecycleVersion, isFromFallback)
    }
    
    fun invalidateLibraryCache() {
        LOG.warn("[CacheManager] ❌❌❌ invalidateLibraryCache() called! Clearing ALL library caches!", Exception("Stack trace"))
        for (type in LibraryRegistry.getAllTypes()) {
            val map = getVersionsMap(type)
            LOG.warn("[CacheManager] ❌ Clearing $type cache (had ${map.size} versions, keys=${map.keys.take(3)})")
            map.clear()
            getIsFromBundleMap(type).clear()
        }
    }
    
    fun getLibraryVersion(composeVersion: String, type: LibraryType): String? {
        val versionsMap = getVersionsMap(type)
        return versionsMap[composeVersion]
    }
    

    fun getRawCachedVersion(composeVersion: String, type: LibraryType): String? {
        val versionsMap = getVersionsMap(type)
        val result = versionsMap[composeVersion]
        LOG.info("[CacheManager] getRawCachedVersion($composeVersion, $type): result=$result, cacheSize=${versionsMap.size}, cacheKeys=${versionsMap.keys.take(5)}, mapInstance=${System.identityHashCode(versionsMap)}")
        return result
    }

    fun isLibraryFromBundle(composeVersion: String, type: LibraryType): Boolean {
        return getIsFromBundleMap(type)[composeVersion] == true
    }
    
    fun isLifecycleFallback(composeVersion: String): Boolean {
        return isLibraryFromBundle(composeVersion, LibraryType.LIFECYCLE)
    }
    
    fun cacheHotReloadGithubVersion(composeVersion: String, githubVersion: String) {
        synchronized(state.hotReloadGithubVersions) {
            state.hotReloadGithubVersions[composeVersion] = githubVersion
            
            while (state.hotReloadGithubVersions.size > MAX_LIBRARY_CACHE_SIZE) {
                val oldestKey = state.hotReloadGithubVersions.keys.first()
                state.hotReloadGithubVersions.remove(oldestKey)
            }
        }
    }
    
    fun getHotReloadGithubVersion(composeVersion: String): String? {
        return state.hotReloadGithubVersions[composeVersion]
    }
}

