package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.LibraryType

class LibraryCacheManager(private val state: ComposeVersionCacheState) {
    
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
            versionsMap[composeVersion] = version
            isFromBundleMap[composeVersion] = isFromFallback
            
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
        for (type in LibraryRegistry.getAllTypes()) {
            getVersionsMap(type).clear()
            getIsFromBundleMap(type).clear()
        }
    }
    
    fun getLibraryVersion(composeVersion: String, type: LibraryType): String? {
        val versionsMap = getVersionsMap(type)
        return versionsMap[composeVersion]
    }
    

    fun getRawCachedVersion(composeVersion: String, type: LibraryType): String? {
        return getVersionsMap(type)[composeVersion]
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

