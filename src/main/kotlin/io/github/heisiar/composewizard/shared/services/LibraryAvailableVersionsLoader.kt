package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LibraryAvailableVersionsLoader(
    private val state: ComposeVersionCacheState,
    private val scope: CoroutineScope,
    private val lifecycleVersionService: LifecycleVersionService,
    private val material3VersionService: Material3VersionService,
    private val material3AdaptiveVersionService: Material3AdaptiveVersionService,
    private val navigationVersionService: NavigationVersionService,
    private val navigation3VersionService: Navigation3VersionService,
    private val windowVersionService: WindowVersionService,
    private val savedStateVersionService: SavedStateVersionService,
    private val navigationEventVersionService: NavigationEventVersionService,
    private val hotReloadVersionService: HotReloadVersionService
) {
    
    private val logger = Logger.getInstance(LibraryAvailableVersionsLoader::class.java)
    
    @Volatile
    var isLoadingLifecycleAvailable = false
        private set
    
    @Volatile
    var isLoadingMaterial3Available = false
        private set
    
    @Volatile
    var isLoadingMaterial3AdaptiveAvailable = false
        private set
    
    @Volatile
    var isLoadingNavigationAvailable = false
        private set
    
    @Volatile
    var isLoadingNavigation3Available = false
        private set
    
    @Volatile
    var isLoadingWindowAvailable = false
        private set
    
    @Volatile
    var isLoadingSavedStateAvailable = false
        private set
    
    @Volatile
    var isLoadingNavigationEventAvailable = false
        private set
    
    @Volatile
    var isLoadingHotReloadAvailable = false
        private set
    
    fun loadLifecycleAvailableVersions() {
        synchronized(this) {
            if (isLoadingLifecycleAvailable) return
            isLoadingLifecycleAvailable = true
        }
        
        scope.launch {
            try {
                val versions = lifecycleVersionService.fetchLifecycleVersions()
                state.lifecycleAvailableVersions = versions
                state.lifecycleAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Lifecycle available versions: ${e.message}")
                state.lifecycleAvailableVersions = emptyList()
            } finally {
                isLoadingLifecycleAvailable = false
            }
        }
    }
    
    fun loadMaterial3AvailableVersions() {
        synchronized(this) {
            if (isLoadingMaterial3Available) return
            isLoadingMaterial3Available = true
        }
        
        scope.launch {
            try {
                val versions = material3VersionService.fetchMaterial3Versions()
                state.material3AvailableVersions = versions
                state.material3AvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Material3 available versions: ${e.message}")
                state.material3AvailableVersions = emptyList()
            } finally {
                isLoadingMaterial3Available = false
            }
        }
    }
    
    fun loadMaterial3AdaptiveAvailableVersions() {
        synchronized(this) {
            if (isLoadingMaterial3AdaptiveAvailable) return
            isLoadingMaterial3AdaptiveAvailable = true
        }
        
        scope.launch {
            try {
                val versions = material3AdaptiveVersionService.fetchMaterial3AdaptiveVersions()
                state.material3AdaptiveAvailableVersions = versions
                state.material3AdaptiveAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Material3 Adaptive available versions: ${e.message}")
                state.material3AdaptiveAvailableVersions = emptyList()
            } finally {
                isLoadingMaterial3AdaptiveAvailable = false
            }
        }
    }
    
    fun loadNavigationAvailableVersions() {
        synchronized(this) {
            if (isLoadingNavigationAvailable) return
            isLoadingNavigationAvailable = true
        }
        
        scope.launch {
            try {
                val versions = navigationVersionService.fetchNavigationVersions()
                state.navigationAvailableVersions = versions
                state.navigationAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Navigation available versions: ${e.message}")
                state.navigationAvailableVersions = emptyList()
            } finally {
                isLoadingNavigationAvailable = false
            }
        }
    }
    
    fun loadNavigation3AvailableVersions() {
        synchronized(this) {
            if (isLoadingNavigation3Available) return
            isLoadingNavigation3Available = true
        }
        
        scope.launch {
            try {
                val versions = navigation3VersionService.fetchNavigation3Versions()
                state.navigation3AvailableVersions = versions
                state.navigation3AvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Navigation3 available versions: ${e.message}")
                state.navigation3AvailableVersions = emptyList()
            } finally {
                isLoadingNavigation3Available = false
            }
        }
    }
    
    fun loadWindowAvailableVersions() {
        synchronized(this) {
            if (isLoadingWindowAvailable) return
            isLoadingWindowAvailable = true
        }
        
        scope.launch {
            try {
                val versions = windowVersionService.fetchWindowVersions()
                state.windowAvailableVersions = versions
                state.windowAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Window available versions: ${e.message}")
                state.windowAvailableVersions = emptyList()
            } finally {
                isLoadingWindowAvailable = false
            }
        }
    }
    
    fun loadSavedStateAvailableVersions() {
        synchronized(this) {
            if (isLoadingSavedStateAvailable) return
            isLoadingSavedStateAvailable = true
        }
        
        scope.launch {
            try {
                val versions = savedStateVersionService.fetchSavedStateVersions()
                state.savedStateAvailableVersions = versions
                state.savedStateAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load SavedState available versions: ${e.message}")
                state.savedStateAvailableVersions = emptyList()
            } finally {
                isLoadingSavedStateAvailable = false
            }
        }
    }
    
    fun loadNavigationEventAvailableVersions() {
        synchronized(this) {
            if (isLoadingNavigationEventAvailable) return
            isLoadingNavigationEventAvailable = true
        }
        
        scope.launch {
            try {
                val versions = navigationEventVersionService.fetchNavigationEventVersions()
                state.navigationEventAvailableVersions = versions
                state.navigationEventAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load NavigationEvent available versions: ${e.message}")
                state.navigationEventAvailableVersions = emptyList()
            } finally {
                isLoadingNavigationEventAvailable = false
            }
        }
    }
    
    fun loadHotReloadAvailableVersions() {
        synchronized(this) {
            if (isLoadingHotReloadAvailable) return
            isLoadingHotReloadAvailable = true
        }
        
        scope.launch {
            try {
                val versions = hotReloadVersionService.fetchHotReloadVersions()
                state.hotReloadAvailableVersions = versions
                state.hotReloadAvailableLastLoadTime = System.currentTimeMillis()
            } catch (e: Exception) {
                logger.warn("Failed to load Hot Reload available versions: ${e.message}")
                state.hotReloadAvailableVersions = emptyList()
            } finally {
                isLoadingHotReloadAvailable = false
            }
        }
    }
}
