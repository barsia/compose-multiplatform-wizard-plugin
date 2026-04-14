package io.github.barsia.composewizard.shared.services

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.xmlb.XmlSerializerUtil
import io.github.barsia.composewizard.shared.LibraryType
import io.github.barsia.composewizard.shared.settings.WizardSettings
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
    private val unifiedLibraryVersionService = LibraryVersionService()
    
    private var persistentState = ComposeVersionCacheState()
    
    private lateinit var coreCache: ComposeVersionCacheCore
    private lateinit var availableVersionsLoader: LibraryAvailableVersionsLoader
    private lateinit var availableVersionsManager: LibraryAvailableVersionsManager
    private lateinit var cacheManager: LibraryCacheManager
    private lateinit var versionResolver: LibraryVersionResolver
    
    @Volatile
    private var initialized = false
    
    companion object {
        private const val PLUGIN_ID = "io.github.barsia.compose-multiplatform-wizard"
        
        fun getInstance(): ComposeVersionCache {
            return ApplicationManager.getApplication().getService(ComposeVersionCache::class.java)
        }
        
        private fun getCurrentPluginVersion(): String? {
            return PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version
        }
    }
    
    init {
        initializeComponents()
        checkPluginUpdate()
    }
    
    private fun initializeComponents() {
        coreCache = ComposeVersionCacheCore(persistentState, scope, versionService)
        
        availableVersionsLoader = LibraryAvailableVersionsLoader(
            persistentState,
            scope,
            unifiedLibraryVersionService
        ) {
            coreCache.notifyCacheInvalidated()
        }
        
        availableVersionsManager = LibraryAvailableVersionsManager(
            persistentState,
            availableVersionsLoader
        )
        
        cacheManager = LibraryCacheManager(persistentState)
        versionResolver = LibraryVersionResolver(
            scope, 
            cacheManager, 
            libraryVersionService,
            getAvailableVersions = { 
                // Return all available versions (stable + dev) for fallback logic
                (getStableVersions() ?: emptyList()) + (getDevVersions() ?: emptyList())
            },
            getLibraryAvailableVersions = { type ->
                availableVersionsManager.getLibraryVersions(type)
            }
        )
    }
    
    private fun checkPluginUpdate() {
        val currentVersion = getCurrentPluginVersion()
        logger.info("[ComposeVersionCache] checkPluginUpdate: currentVersion=$currentVersion")
        if (currentVersion == null) {
            logger.warn("Unable to determine current plugin version")
            return
        }
        
        val settings = WizardSettings.getInstance()
        val lastVersion = settings.lastPluginVersion
        logger.info("[ComposeVersionCache] checkPluginUpdate: lastVersion=$lastVersion")
        
        if (lastVersion.isNotEmpty() && lastVersion != currentVersion) {
            logger.info("[ComposeVersionCache] ❌ Plugin updated from $lastVersion to $currentVersion, CLEARING ALL CACHE")
            clearAllCache()
        } else {
            logger.info("[ComposeVersionCache] ✅ Plugin version unchanged, keeping cache")
        }
        
        settings.lastPluginVersion = currentVersion
    }
    
    private fun clearAllCache() {
        persistentState.stableVersions = emptyList()
        persistentState.devVersions = emptyList()
        persistentState.stableLastLoadTime = 0L
        persistentState.devLastLoadTime = 0L
        
        persistentState.libraryVersions?.clear()
        persistentState.libraryIsFromBundle?.clear()
        persistentState.libraryAvailableVersions?.clear()
        persistentState.libraryAvailableLastLoadTime?.clear()
        persistentState.hotReloadGithubVersions = linkedMapOf()
        
        logger.info("All cache cleared after plugin update")
    }
    
    override fun getState(): ComposeVersionCacheState {
        logger.info("[ComposeVersionCache] getState() called")
        return persistentState
    }
    
    override fun loadState(state: ComposeVersionCacheState) {
        logger.info("[ComposeVersionCache] loadState() called")
        
        val libraryVersions = state.libraryVersions
        if (libraryVersions != null) {
            logger.info("[ComposeVersionCache] Incoming state has ${libraryVersions.size} library types cached")
            libraryVersions.forEach { (type, versions) ->
                if (versions != null) {
                    logger.info("[ComposeVersionCache]   - $type: ${versions.size} versions cached, keys=${versions.keys.take(3)}")
                } else {
                    logger.info("[ComposeVersionCache]   - $type: versions is null")
                }
            }
        } else {
            logger.info("[ComposeVersionCache] Incoming state has null libraryVersions")
        }
        
        XmlSerializerUtil.copyBean(state, persistentState)
        
        val persistentLibraryVersions = persistentState.libraryVersions
        if (persistentLibraryVersions != null) {
            logger.info("[ComposeVersionCache] After copyBean, persistentState has ${persistentLibraryVersions.size} library types")
        } else {
            logger.warn("[ComposeVersionCache] After copyBean, persistentState.libraryVersions is null, initializing with empty map")
            persistentState.libraryVersions = mutableMapOf()
        }
        
        if (persistentState.libraryIsFromBundle == null) {
            logger.warn("[ComposeVersionCache] persistentState.libraryIsFromBundle is null, initializing with empty map")
            persistentState.libraryIsFromBundle = mutableMapOf()
        }
        
        if (persistentState.libraryAvailableVersions == null) {
            logger.warn("[ComposeVersionCache] persistentState.libraryAvailableVersions is null, initializing with empty map")
            persistentState.libraryAvailableVersions = mutableMapOf()
        }
        
        if (persistentState.libraryAvailableLastLoadTime == null) {
            logger.warn("[ComposeVersionCache] persistentState.libraryAvailableLastLoadTime is null, initializing with empty map")
            persistentState.libraryAvailableLastLoadTime = mutableMapOf()
        }
        
        if (persistentState.hotReloadGithubVersions == null) {
            logger.warn("[ComposeVersionCache] persistentState.hotReloadGithubVersions is null, initializing with empty map")
            persistentState.hotReloadGithubVersions = linkedMapOf()
        }
        
        if (persistentState.stableVersions == null) {
            logger.warn("[ComposeVersionCache] persistentState.stableVersions is null, initializing with empty list")
            persistentState.stableVersions = emptyList()
        }
        
        if (persistentState.devVersions == null) {
            logger.warn("[ComposeVersionCache] persistentState.devVersions is null, initializing with empty list")
            persistentState.devVersions = emptyList()
        }
        
        checkPluginUpdate()
        initializeCache()
    }
    
    private fun initializeCache() {
        if (initialized) {
            return
        }
        
        initialized = true
        
        coreCache.initializeCache {
            // Lazy loading: dropdown lists will be loaded on first access
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
        return availableVersionsManager.getLibraryVersions(LibraryType.LIFECYCLE)
    }
    
    fun getMaterial3AvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.MATERIAL3)
    }
    
    fun getMaterial3AdaptiveAvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.MATERIAL3_ADAPTIVE)
    }
    
    fun getNavigationAvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.NAVIGATION)
    }
    
    fun getNavigation3AvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.NAVIGATION3)
    }
    
    fun getWindowAvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.WINDOW)
    }
    
    fun getSavedStateAvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.SAVED_STATE)
    }
    
    fun getNavigationEventAvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.NAVIGATION_EVENT)
    }
    
    fun getHotReloadAvailableVersions(): List<String> {
        return availableVersionsManager.getLibraryVersions(LibraryType.HOT_RELOAD)
    }
    
    fun invalidateLifecycleAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.LIFECYCLE)
    }
    
    fun invalidateMaterial3AvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.MATERIAL3)
    }
    
    fun invalidateMaterial3AdaptiveAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.MATERIAL3_ADAPTIVE)
    }
    
    fun invalidateNavigationAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.NAVIGATION)
    }
    
    fun invalidateNavigation3AvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.NAVIGATION3)
    }
    
    fun invalidateWindowAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.WINDOW)
    }
    
    fun invalidateSavedStateAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.SAVED_STATE)
    }
    
    fun invalidateNavigationEventAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.NAVIGATION_EVENT)
    }
    
    fun invalidateHotReloadAvailableCache() {
        availableVersionsManager.invalidateCache(LibraryType.HOT_RELOAD)
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
    
    fun clearNotFoundMarker(composeVersion: String, type: LibraryType) {
        versionResolver.clearNotFoundMarker(composeVersion, type)
    }
    
    fun getRawCachedVersion(composeVersion: String, type: LibraryType): String? {
        return cacheManager.getRawCachedVersion(composeVersion, type)
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
