package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope

class LibraryAvailableVersionsManager(
    private val state: ComposeVersionCacheState,
    scope: CoroutineScope,
    lifecycleVersionService: LifecycleVersionService,
    material3VersionService: Material3VersionService,
    material3AdaptiveVersionService: Material3AdaptiveVersionService,
    navigationVersionService: NavigationVersionService,
    navigation3VersionService: Navigation3VersionService,
    windowVersionService: WindowVersionService,
    savedStateVersionService: SavedStateVersionService,
    navigationEventVersionService: NavigationEventVersionService,
    hotReloadVersionService: HotReloadVersionService,
    onCacheInvalidated: suspend () -> Unit
) {
    
    private val logger = Logger.getInstance(LibraryAvailableVersionsManager::class.java)
    
    private val loader = LibraryAvailableVersionsLoader(
        state, scope, lifecycleVersionService, material3VersionService,
        material3AdaptiveVersionService, navigationVersionService, navigation3VersionService,
        windowVersionService, savedStateVersionService, navigationEventVersionService,
        hotReloadVersionService, onCacheInvalidated
    )
    
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }
    
    fun initializeAvailableVersions() {
        if (isLifecycleAvailableCacheExpired()) {
            loader.loadLifecycleAvailableVersions()
        }
        
        if (isMaterial3AvailableCacheExpired()) {
            loader.loadMaterial3AvailableVersions()
        }
        
        if (isMaterial3AdaptiveAvailableCacheExpired()) {
            loader.loadMaterial3AdaptiveAvailableVersions()
        }
        
        if (isNavigationAvailableCacheExpired()) {
            loader.loadNavigationAvailableVersions()
        }
        
        if (isNavigation3AvailableExpired()) {
            loader.loadNavigation3AvailableVersions()
        }
        
        if (isWindowAvailableExpired()) {
            loader.loadWindowAvailableVersions()
        }
        
        if (isSavedStateAvailableExpired()) {
            loader.loadSavedStateAvailableVersions()
        }
        
        if (isNavigationEventAvailableExpired()) {
            loader.loadNavigationEventAvailableVersions()
        }
        
        if (isHotReloadAvailableExpired()) {
            loader.loadHotReloadAvailableVersions()
        }
    }
    
    fun getLifecycleAvailableVersions(): List<String> {
        if (isLifecycleAvailableCacheExpired() && !loader.isLoadingLifecycleAvailable) {
            loader.loadLifecycleAvailableVersions()
        }
        return state.lifecycleAvailableVersions
    }
    
    fun getMaterial3AvailableVersions(): List<String> {
        if (isMaterial3AvailableCacheExpired() && !loader.isLoadingMaterial3Available) {
            loader.loadMaterial3AvailableVersions()
        }
        return state.material3AvailableVersions
    }
    
    fun getMaterial3AdaptiveAvailableVersions(): List<String> {
        if (isMaterial3AdaptiveAvailableCacheExpired() && !loader.isLoadingMaterial3AdaptiveAvailable) {
            loader.loadMaterial3AdaptiveAvailableVersions()
        }
        return state.material3AdaptiveAvailableVersions
    }
    
    fun getNavigationAvailableVersions(): List<String> {
        if (isNavigationAvailableCacheExpired() && !loader.isLoadingNavigationAvailable) {
            loader.loadNavigationAvailableVersions()
        }
        return state.navigationAvailableVersions
    }
    
    fun getNavigation3AvailableVersions(): List<String> {
        return state.navigation3AvailableVersions
    }
    
    fun getWindowAvailableVersions(): List<String> {
        return state.windowAvailableVersions
    }
    
    fun getSavedStateAvailableVersions(): List<String> {
        return state.savedStateAvailableVersions
    }
    
    fun getNavigationEventAvailableVersions(): List<String> {
        return state.navigationEventAvailableVersions
    }
    
    fun getHotReloadAvailableVersions(): List<String> {
        return state.hotReloadAvailableVersions
    }
    
    private fun isLifecycleAvailableCacheExpired(): Boolean {
        if (state.lifecycleAvailableLastLoadTime == 0L) return true
        if (state.lifecycleAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.lifecycleAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    private fun isMaterial3AvailableCacheExpired(): Boolean {
        if (state.material3AvailableLastLoadTime == 0L) return true
        if (state.material3AvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.material3AvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    private fun isMaterial3AdaptiveAvailableCacheExpired(): Boolean {
        if (state.material3AdaptiveAvailableLastLoadTime == 0L) return true
        if (state.material3AdaptiveAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.material3AdaptiveAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    private fun isNavigationAvailableCacheExpired(): Boolean {
        if (state.navigationAvailableLastLoadTime == 0L) return true
        if (state.navigationAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.navigationAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    fun isNavigation3AvailableExpired(): Boolean {
        if (state.navigation3AvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.navigation3AvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    fun isWindowAvailableExpired(): Boolean {
        if (state.windowAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.windowAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    fun isSavedStateAvailableExpired(): Boolean {
        if (state.savedStateAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.savedStateAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    fun isNavigationEventAvailableExpired(): Boolean {
        if (state.navigationEventAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.navigationEventAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    fun isHotReloadAvailableExpired(): Boolean {
        if (state.hotReloadAvailableVersions.isEmpty()) return true
        val age = System.currentTimeMillis() - state.hotReloadAvailableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    fun invalidateLifecycleAvailableCache() {
        logger.info("Lifecycle available cache manually invalidated")
        state.lifecycleAvailableLastLoadTime = 0L
    }
    
    fun invalidateMaterial3AvailableCache() {
        logger.info("Material3 available cache manually invalidated")
        state.material3AvailableLastLoadTime = 0L
    }
    
    fun invalidateMaterial3AdaptiveAvailableCache() {
        logger.info("Material3 Adaptive available cache manually invalidated")
        state.material3AdaptiveAvailableLastLoadTime = 0L
    }
    
    fun invalidateNavigationAvailableCache() {
        logger.info("Navigation available cache manually invalidated")
        state.navigationAvailableLastLoadTime = 0L
    }
    
    fun invalidateNavigation3AvailableCache() {
        logger.info("Navigation3 available cache manually invalidated")
        state.navigation3AvailableLastLoadTime = 0L
    }
    
    fun invalidateWindowAvailableCache() {
        logger.info("Window available cache manually invalidated")
        state.windowAvailableLastLoadTime = 0L
    }
    
    fun invalidateSavedStateAvailableCache() {
        logger.info("SavedState available cache manually invalidated")
        state.savedStateAvailableLastLoadTime = 0L
    }
    
    fun invalidateNavigationEventAvailableCache() {
        logger.info("NavigationEvent available cache manually invalidated")
        state.navigationEventAvailableLastLoadTime = 0L
    }
    
    fun invalidateHotReloadAvailableCache() {
        logger.info("Hot Reload available cache manually invalidated")
        state.hotReloadAvailableLastLoadTime = 0L
    }
}
