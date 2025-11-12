package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.LibraryType

class LibraryCacheManager(private val state: ComposeVersionCacheState) {
    
    companion object {
        private const val MAX_LIBRARY_CACHE_SIZE = 200
    }
    
    fun getVersionsMap(type: LibraryType): LinkedHashMap<String, String> {
        return when (type) {
            LibraryType.LIFECYCLE -> state.lifecycleVersions
            LibraryType.MATERIAL3 -> state.material3Versions
            LibraryType.MATERIAL3_ADAPTIVE -> state.material3AdaptiveVersions
            LibraryType.NAVIGATION -> state.navigationVersions
            LibraryType.NAVIGATION3 -> state.navigation3Versions
            LibraryType.NAVIGATION_EVENT -> state.navigationEventVersions
            LibraryType.SAVED_STATE -> state.savedStateVersions
            LibraryType.WINDOW -> state.windowVersions
            LibraryType.HOT_RELOAD -> state.hotReloadVersions
        }
    }
    
    fun getIsFromBundleMap(type: LibraryType): LinkedHashMap<String, Boolean> {
        return when (type) {
            LibraryType.LIFECYCLE -> state.lifecycleIsFromBundle
            LibraryType.MATERIAL3 -> state.material3IsFromBundle
            LibraryType.MATERIAL3_ADAPTIVE -> state.material3AdaptiveIsFromBundle
            LibraryType.NAVIGATION -> state.navigationIsFromBundle
            LibraryType.NAVIGATION3 -> state.navigation3IsFromBundle
            LibraryType.NAVIGATION_EVENT -> state.navigationEventIsFromBundle
            LibraryType.SAVED_STATE -> state.savedStateIsFromBundle
            LibraryType.WINDOW -> state.windowIsFromBundle
            LibraryType.HOT_RELOAD -> state.hotReloadIsFromBundle
        }
    }
    
    fun cacheLibraryVersion(composeVersion: String, type: LibraryType, version: String, fromBundle: Boolean) {
        val versionsMap = getVersionsMap(type)
        val isFromBundleMap = getIsFromBundleMap(type)
        
        synchronized(versionsMap) {
            versionsMap[composeVersion] = version
            isFromBundleMap[composeVersion] = fromBundle
            
            while (versionsMap.size > MAX_LIBRARY_CACHE_SIZE) {
                val oldestKey = versionsMap.keys.first()
                versionsMap.remove(oldestKey)
                isFromBundleMap.remove(oldestKey)
            }
        }
    }
    
    fun cacheLifecycleVersion(composeVersion: String, lifecycleVersion: String, fromBundle: Boolean = false) {
        synchronized(state.lifecycleVersions) {
            state.lifecycleVersions[composeVersion] = lifecycleVersion
            state.lifecycleIsFromBundle[composeVersion] = fromBundle
            
            while (state.lifecycleVersions.size > MAX_LIBRARY_CACHE_SIZE) {
                val oldestKey = state.lifecycleVersions.keys.first()
                state.lifecycleVersions.remove(oldestKey)
                state.lifecycleIsFromBundle.remove(oldestKey)
            }
        }
    }
    
    fun invalidateLibraryCache() {
        state.lifecycleVersions.clear()
        state.lifecycleIsFromBundle.clear()
        state.material3Versions.clear()
        state.material3IsFromBundle.clear()
        state.material3AdaptiveVersions.clear()
        state.material3AdaptiveIsFromBundle.clear()
        state.navigationVersions.clear()
        state.navigationIsFromBundle.clear()
        state.navigation3Versions.clear()
        state.navigation3IsFromBundle.clear()
        state.navigationEventVersions.clear()
        state.navigationEventIsFromBundle.clear()
        state.savedStateVersions.clear()
        state.savedStateIsFromBundle.clear()
        state.windowVersions.clear()
        state.windowIsFromBundle.clear()
        state.hotReloadVersions.clear()
        state.hotReloadIsFromBundle.clear()
    }
    
    fun getLibraryVersion(composeVersion: String, type: LibraryType): String? {
        val versionsMap = getVersionsMap(type)
        return versionsMap[composeVersion]
    }
    
    fun isLibraryFromBundle(composeVersion: String, type: LibraryType): Boolean {
        return getIsFromBundleMap(type)[composeVersion] == true
    }
    
    fun isLifecycleFallback(composeVersion: String): Boolean {
        return state.lifecycleIsFromBundle[composeVersion] == true
    }
}

