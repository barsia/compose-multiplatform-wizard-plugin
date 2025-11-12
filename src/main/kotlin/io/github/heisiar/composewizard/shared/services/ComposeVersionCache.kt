package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil
import io.github.heisiar.composewizard.shared.LibraryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
@State(
    name = "ComposeVersionCache",
    storages = [Storage("composeVersionCache.xml")]
)
class ComposeVersionCache : Disposable, PersistentStateComponent<ComposeVersionCacheState> {
    
    private val logger = Logger.getInstance(ComposeVersionCache::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val versionService = ComposeVersionService()
    private val libraryVersionService = ComposeLibraryVersionService()
    private val lifecycleVersionService = LifecycleVersionService()
    private val material3VersionService = Material3VersionService()
    private val material3AdaptiveVersionService = Material3AdaptiveVersionService()
    private val navigationVersionService = NavigationVersionService()
    private val navigation3VersionService = Navigation3VersionService()
    private val windowVersionService = WindowVersionService()
    private val savedStateVersionService = SavedStateVersionService()
    private val navigationEventVersionService = NavigationEventVersionService()
    private val hotReloadVersionService = HotReloadVersionService()
    
    private var persistentState = ComposeVersionCacheState()
    
    private lateinit var coreCache: ComposeVersionCacheCore
    private lateinit var availableVersionsManager: LibraryAvailableVersionsManager
    private lateinit var cacheManager: LibraryCacheManager
    private lateinit var versionResolver: LibraryVersionResolver
    
    @Volatile
    private var initialized = false
    
    companion object {
        fun getInstance(): ComposeVersionCache {
            return ApplicationManager.getApplication().getService(ComposeVersionCache::class.java)
        }
    }
    
    init {
        initializeComponents()
    }
    
    private fun initializeComponents() {
        coreCache = ComposeVersionCacheCore(persistentState, scope, versionService)
        availableVersionsManager = LibraryAvailableVersionsManager(
            persistentState, scope, lifecycleVersionService, material3VersionService,
            material3AdaptiveVersionService, navigationVersionService, navigation3VersionService,
            windowVersionService, savedStateVersionService, navigationEventVersionService,
            hotReloadVersionService
        ) {
            coreCache.notifyCacheInvalidated()
        }
        cacheManager = LibraryCacheManager(persistentState)
        versionResolver = LibraryVersionResolver(scope, cacheManager, libraryVersionService)
    }
    
    override fun getState(): ComposeVersionCacheState {
        return persistentState
    }
    
    override fun loadState(state: ComposeVersionCacheState) {
        XmlSerializerUtil.copyBean(state, persistentState)
        initializeCache()
    }
    
    private fun initializeCache() {
        if (initialized) {
            return
        }
        
        initialized = true
        
        coreCache.initializeCache {
            availableVersionsManager.initializeAvailableVersions()
        }
    }
    
    fun getStableVersions(): List<String>? {
        if (!initialized) {
            initializeCache()
        }
        return coreCache.getStableVersions()
    }
    
    fun getDevVersions(): List<String>? {
        if (!initialized) {
            initializeCache()
        }
        return coreCache.getDevVersions()
    }
    
    suspend fun getStableVersionsSuspend(timeoutMs: Long = 3000): List<String> {
        return coreCache.getStableVersionsSuspend(timeoutMs)
    }
    
    fun getStableVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        return coreCache.getStableVersionsBlocking(timeoutMs)
    }
    
    suspend fun getDevVersionsSuspend(timeoutMs: Long = 3000): List<String> {
        return coreCache.getDevVersionsSuspend(timeoutMs)
    }
    
    fun getDevVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        return coreCache.getDevVersionsBlocking(timeoutMs)
    }
    
    fun isLoadingStableVersions(): Boolean {
        return coreCache.isLoadingStableVersions()
    }
    
    fun isLoadingDevVersions(): Boolean {
        return coreCache.isLoadingDevVersions()
    }
    
    fun isUsingFallbackVersions(): Boolean {
        return coreCache.isUsingFallbackVersions()
    }
    
    fun isUsingDevFallbackVersions(): Boolean {
        return coreCache.isUsingDevFallbackVersions()
    }
    
    fun getLifecycleAvailableVersions(): List<String> {
        return availableVersionsManager.getLifecycleAvailableVersions()
    }
    
    fun getMaterial3AvailableVersions(): List<String> {
        return availableVersionsManager.getMaterial3AvailableVersions()
    }
    
    fun getMaterial3AdaptiveAvailableVersions(): List<String> {
        return availableVersionsManager.getMaterial3AdaptiveAvailableVersions()
    }
    
    fun getNavigationAvailableVersions(): List<String> {
        return availableVersionsManager.getNavigationAvailableVersions()
    }
    
    fun getNavigation3AvailableVersions(): List<String> {
        return availableVersionsManager.getNavigation3AvailableVersions()
    }
    
    fun getWindowAvailableVersions(): List<String> {
        return availableVersionsManager.getWindowAvailableVersions()
    }
    
    fun getSavedStateAvailableVersions(): List<String> {
        return availableVersionsManager.getSavedStateAvailableVersions()
    }
    
    fun getNavigationEventAvailableVersions(): List<String> {
        return availableVersionsManager.getNavigationEventAvailableVersions()
    }
    
    fun getHotReloadAvailableVersions(): List<String> {
        return availableVersionsManager.getHotReloadAvailableVersions()
    }
    
    fun invalidateLifecycleAvailableCache() {
        availableVersionsManager.invalidateLifecycleAvailableCache()
    }
    
    fun invalidateMaterial3AvailableCache() {
        availableVersionsManager.invalidateMaterial3AvailableCache()
    }
    
    fun invalidateMaterial3AdaptiveAvailableCache() {
        availableVersionsManager.invalidateMaterial3AdaptiveAvailableCache()
    }
    
    fun invalidateNavigationAvailableCache() {
        availableVersionsManager.invalidateNavigationAvailableCache()
    }
    
    fun invalidateNavigation3AvailableCache() {
        availableVersionsManager.invalidateNavigation3AvailableCache()
    }
    
    fun invalidateWindowAvailableCache() {
        availableVersionsManager.invalidateWindowAvailableCache()
    }
    
    fun invalidateSavedStateAvailableCache() {
        availableVersionsManager.invalidateSavedStateAvailableCache()
    }
    
    fun invalidateNavigationEventAvailableCache() {
        availableVersionsManager.invalidateNavigationEventAvailableCache()
    }
    
    fun invalidateHotReloadAvailableCache() {
        availableVersionsManager.invalidateHotReloadAvailableCache()
    }
    
    fun invalidateStableCache() {
        coreCache.invalidateStableCache()
    }
    
    fun invalidateDevCache() {
        coreCache.invalidateDevCache()
    }
    
    fun forceReloadStable() {
        coreCache.forceReloadStable {
            invalidateLibraryCache()
        }
    }
    
    fun forceReloadDev() {
        coreCache.forceReloadDev {
            invalidateLibraryCache()
        }
    }
    
    private fun invalidateLibraryCache() {
        cacheManager.invalidateLibraryCache()
        logger.info("Library cache invalidated")
        
        scope.launch {
            coreCache.notifyCacheInvalidated()
        }
    }
    
    fun getLifecycleVersion(composeVersion: String): String? {
        if (!initialized) {
            initializeCache()
        }
        return versionResolver.getLifecycleVersion(composeVersion)
    }
    
    fun isResolvingLifecycle(composeVersion: String): Boolean {
        return versionResolver.isResolvingLibrary(composeVersion, LibraryType.LIFECYCLE)
    }
    
    fun isLifecycleFallback(composeVersion: String): Boolean {
        return cacheManager.isLifecycleFallback(composeVersion)
    }
    
    fun getLibraryVersion(composeVersion: String, type: LibraryType): String? {
        if (!initialized) {
            initializeCache()
        }
        return versionResolver.getLibraryVersion(composeVersion, type)
    }
    
    fun isResolvingLibrary(composeVersion: String, type: LibraryType): Boolean {
        return versionResolver.isResolvingLibrary(composeVersion, type)
    }
    
    fun isLibraryFromBundle(composeVersion: String, type: LibraryType): Boolean {
        return cacheManager.isLibraryFromBundle(composeVersion, type)
    }
    
    fun getHotReloadGithubVersion(composeVersion: String): String? {
        return cacheManager.getHotReloadGithubVersion(composeVersion)
    }
    
    val lifecycleVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.lifecycleVersionUpdates
    
    val material3VersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.material3VersionUpdates
    
    val material3AdaptiveVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.material3AdaptiveVersionUpdates
    
    val navigationVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.navigationVersionUpdates
    
    val navigation3VersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.navigation3VersionUpdates
    
    val windowVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.windowVersionUpdates
    
    val savedStateVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.savedStateVersionUpdates
    
    val navigationEventVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.navigationEventVersionUpdates
    
    val hotReloadVersionUpdates: SharedFlow<Pair<String, String>>
        get() = versionResolver.hotReloadVersionUpdates
    
    val cacheInvalidated: SharedFlow<Unit>
        get() = coreCache.cacheInvalidated
    
    override fun dispose() {
        logger.info("Disposing ComposeVersionCache, cancelling all background tasks")
        scope.cancel()
    }
}
