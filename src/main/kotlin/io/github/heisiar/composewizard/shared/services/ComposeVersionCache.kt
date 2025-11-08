package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil
import io.github.heisiar.composewizard.shared.ComposeVersions
import kotlinx.coroutines.*

data class ComposeVersionCacheState(
    var stableVersions: List<String> = emptyList(),
    var stableLastLoadTime: Long = 0L,
    var devVersions: List<String> = emptyList(),
    var devLastLoadTime: Long = 0L
)

/**
 * Application-level service that caches Compose Multiplatform versions with TTL.
 * 
 * Versions are loaded in background on IDE startup and cached with a Time To Live (TTL).
 * Cache is automatically refreshed when TTL expires (12 hours by default).
 * Cache is persisted to disk between IDE restarts.
 * This allows Template API wizard (which is synchronous) to access pre-loaded versions.
 * 
 * ## Manual Cache Control
 * 
 * ```kotlin
 * val cache = ComposeVersionCache.getInstance()
 * 
 * // Get versions (auto-refreshes if expired)
 * val versions = cache.getStableVersions()
 * 
 * // Force immediate refresh (non-blocking)
 * cache.forceReload()
 * 
 * // Invalidate cache (refresh on next access)
 * cache.invalidateCache()
 * ```
 * 
 * ## TTL Configuration
 * 
 * Current TTL: 12 hours (CACHE_TTL_MS)
 * - Compose stable versions: released every 2-3 weeks
 * - 12 hours = 2 checks per day = optimal balance
 * - Can be adjusted based on release frequency
 */
@Service(Service.Level.APP)
@State(
    name = "ComposeVersionCache",
    storages = [Storage("composeVersionCache.xml")]
)
class ComposeVersionCache : Disposable, PersistentStateComponent<ComposeVersionCacheState> {
    
    private val logger = Logger.getInstance(ComposeVersionCache::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val versionService = ComposeVersionService()
    
    private var persistentState = ComposeVersionCacheState()
    
    @Volatile
    private var isLoadingStable = false
    
    @Volatile
    private var isLoadingDev = false
    
    @Volatile
    private var initialized = false
    
    companion object {
        // Cache TTL: 12 hours (in milliseconds)
        // Compose stable versions are released every 2-3 weeks
        // 12 hours is optimal balance: fresh twice a day, minimal Maven load
        // Users can manually refresh via UI button anytime
        private const val CACHE_TTL_MS = 12 * 60 * 60 * 1000L
        
        fun getInstance(): ComposeVersionCache {
            return ApplicationManager.getApplication().getService(ComposeVersionCache::class.java)
        }
    }
    
    init {
        println("DEBUG ComposeVersionCache: Constructor called")
    }
    
    override fun getState(): ComposeVersionCacheState {
        return persistentState
    }
    
    override fun loadState(state: ComposeVersionCacheState) {
        XmlSerializerUtil.copyBean(state, persistentState)
        println("DEBUG ComposeVersionCache: Loaded state from disk - stable: ${persistentState.stableVersions.take(3)}, lastLoad: ${persistentState.stableLastLoadTime}")
        initializeCache()
    }
    
    private fun initializeCache() {
        if (initialized) {
            println("DEBUG ComposeVersionCache: Already initialized, skipping")
            return
        }
        
        initialized = true
        println("DEBUG ComposeVersionCache: Initializing after state load, cached stable: ${persistentState.stableVersions.take(3)}")
        
        if (isStableCacheExpired()) {
            println("DEBUG ComposeVersionCache: Cache expired or empty, starting background loading...")
            loadStableVersionsInBackground()
            loadDevVersionsInBackground()
        } else {
            println("DEBUG ComposeVersionCache: Using cached versions, no need to reload")
        }
    }
    
    /**
     * Get cached stable versions (non-blocking).
     * Returns null if loading not yet completed.
     * Returns cached versions (or fallback) if loading completed.
     * Automatically refreshes cache in background if TTL expired.
     */
    fun getStableVersions(): List<String>? {
        if (!initialized) {
            println("DEBUG ComposeVersionCache.getStableVersions(): Not initialized yet, initializing now")
            initializeCache()
        }
        
        val versions = if (persistentState.stableVersions.isEmpty()) null else persistentState.stableVersions
        println("DEBUG ComposeVersionCache.getStableVersions(): returning ${versions?.take(3)}, isLoading=$isLoadingStable")
        
        if (!isLoadingStable && isStableCacheExpired()) {
            logger.info("Stable cache expired (TTL: ${CACHE_TTL_MS}ms), refreshing in background")
            println("DEBUG ComposeVersionCache: Starting stable version load because cache expired")
            loadStableVersionsInBackground()
        }
        return versions
    }
    
    /**
     * Get cached dev versions (non-blocking).
     * Returns null if loading not yet completed.
     * Returns cached versions (or fallback) if loading completed.
     * Automatically refreshes cache in background if TTL expired.
     */
    fun getDevVersions(): List<String>? {
        if (!initialized) {
            println("DEBUG ComposeVersionCache.getDevVersions(): Not initialized yet, initializing now")
            initializeCache()
        }
        
        val versions = if (persistentState.devVersions.isEmpty()) null else persistentState.devVersions
        println("DEBUG ComposeVersionCache.getDevVersions(): returning ${versions?.take(3)}, isLoading=$isLoadingDev")
        
        if (!isLoadingDev && isDevCacheExpired()) {
            logger.info("Dev cache expired (TTL: ${CACHE_TTL_MS}ms), refreshing in background")
            loadDevVersionsInBackground()
        }
        return versions
    }
    
    /**
     * Check if stable cache has expired based on TTL.
     */
    private fun isStableCacheExpired(): Boolean {
        if (persistentState.stableLastLoadTime == 0L || persistentState.stableVersions.isEmpty()) {
            return true
        }
        val age = System.currentTimeMillis() - persistentState.stableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    /**
     * Check if dev cache has expired based on TTL.
     */
    private fun isDevCacheExpired(): Boolean {
        if (persistentState.devLastLoadTime == 0L || persistentState.devVersions.isEmpty()) {
            return true
        }
        val age = System.currentTimeMillis() - persistentState.devLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    /**
     * Get stable versions, waiting for background loading to complete if necessary (suspend version).
     * 
     * This method suspends until versions are loaded or timeout occurs.
     * Prefer this over blocking version when in coroutine context.
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 3000ms)
     * @return List of stable versions (from Maven or fallback)
     */
    suspend fun getStableVersionsSuspend(timeoutMs: Long = 3000): List<String> = withContext(Dispatchers.IO) {
        if (!isLoadingStable) {
            return@withContext if (persistentState.stableVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS else persistentState.stableVersions
        }
        
        logger.info("Waiting for stable Compose versions to load (timeout: ${timeoutMs}ms)...")
        
        withTimeoutOrNull(timeoutMs) {
            while (isLoadingStable) {
                delay(100)
            }
        }
        
        if (isLoadingStable) {
            logger.warn("Timeout waiting for stable Compose versions, using fallback")
        } else {
            logger.info("Stable Compose versions loaded successfully")
        }
        
        if (persistentState.stableVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS else persistentState.stableVersions
    }
    
    /**
     * Get stable versions, waiting for background loading to complete if necessary (blocking version).
     * 
     * This method blocks until versions are loaded or timeout occurs.
     * Use this only for Template API wizard or other synchronous contexts where coroutines cannot be used.
     * Prefer getStableVersionsSuspend() when in coroutine context.
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 3000ms)
     * @return List of stable versions (from Maven or fallback)
     */
    fun getStableVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        return runBlocking {
            getStableVersionsSuspend(timeoutMs)
        }
    }
    
    /**
     * Get dev versions, waiting for background loading to complete if necessary (suspend version).
     * 
     * This method suspends until versions are loaded or timeout occurs.
     * Prefer this over blocking version when in coroutine context.
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 3000ms)
     * @return List of dev versions (from Maven or fallback)
     */
    suspend fun getDevVersionsSuspend(timeoutMs: Long = 3000): List<String> = withContext(Dispatchers.IO) {
        if (!isLoadingDev) {
            return@withContext if (persistentState.devVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS else persistentState.devVersions
        }
        
        logger.info("Waiting for dev Compose versions to load (timeout: ${timeoutMs}ms)...")
        
        withTimeoutOrNull(timeoutMs) {
            while (isLoadingDev) {
                delay(100)
            }
        }
        
        if (isLoadingDev) {
            logger.warn("Timeout waiting for dev Compose versions, using fallback")
        } else {
            logger.info("Dev Compose versions loaded successfully")
        }
        
        if (persistentState.devVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS else persistentState.devVersions
    }
    
    /**
     * Get dev versions, waiting for background loading to complete if necessary (blocking version).
     * 
     * This method blocks until versions are loaded or timeout occurs.
     * Use this only for Template API wizard or other synchronous contexts where coroutines cannot be used.
     * Prefer getDevVersionsSuspend() when in coroutine context.
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 3000ms)
     * @return List of dev versions (from Maven or fallback)
     */
    fun getDevVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        return runBlocking {
            getDevVersionsSuspend(timeoutMs)
        }
    }
    
    /**
     * Check if stable versions are currently being loaded.
     */
    fun isLoadingStableVersions(): Boolean {
        return isLoadingStable
    }
    
    /**
     * Check if dev versions are currently being loaded.
     */
    fun isLoadingDevVersions(): Boolean {
        return isLoadingDev
    }
    
    /**
     * Manually invalidate stable cache (reset TTL).
     * Next call to getStableVersions() will trigger background refresh.
     * Non-blocking - doesn't reload immediately.
     */
    fun invalidateStableCache() {
        logger.info("Stable cache manually invalidated, will refresh on next access")
        persistentState.stableLastLoadTime = 0L
    }
    
    /**
     * Manually invalidate dev cache (reset TTL).
     * Next call to getDevVersions() will trigger background refresh.
     * Non-blocking - doesn't reload immediately.
     */
    fun invalidateDevCache() {
        logger.info("Dev cache manually invalidated, will refresh on next access")
        persistentState.devLastLoadTime = 0L
    }
    
    /**
     * Force immediate reload of stable versions from Maven.
     * Invalidates cache and starts loading in background.
     * Non-blocking - returns immediately.
     * 
     * Use this when user explicitly requests fresh stable versions.
     */
    fun forceReloadStable() {
        logger.info("Force reload stable requested, invalidating cache and reloading")
        persistentState.stableLastLoadTime = 0L
        loadStableVersionsInBackground()
    }
    
    /**
     * Force immediate reload of dev versions from Maven.
     * Invalidates cache and starts loading in background.
     * Non-blocking - returns immediately.
     * 
     * Use this when user explicitly requests fresh dev versions.
     */
    fun forceReloadDev() {
        logger.info("Force reload dev requested, invalidating cache and reloading")
        persistentState.devLastLoadTime = 0L
        loadDevVersionsInBackground()
    }
    
    private fun loadStableVersionsInBackground() {
        if (isLoadingStable) {
            logger.info("Stable version loading already in progress, skipping")
            return
        }
        
        isLoadingStable = true
        logger.info("Starting background loading of stable Compose versions from Maven")
        
        scope.launch {
            try {
                if (!isActive) {
                    logger.info("Coroutine cancelled before loading stable versions")
                    return@launch
                }
                
                val versions = versionService.fetchAvailableVersions(includeDevVersions = false)
                
                if (!isActive) {
                    logger.info("Coroutine cancelled after loading stable versions")
                    return@launch
                }
                
                persistentState.stableVersions = versions
                persistentState.stableLastLoadTime = System.currentTimeMillis()
                println("DEBUG ComposeVersionCache: Successfully loaded ${versions.size} stable versions from Maven: ${versions.take(5).joinToString(", ")}")
                logger.info("Successfully loaded ${versions.size} stable Compose versions: ${versions.take(5).joinToString(", ")}... (TTL: ${CACHE_TTL_MS / 1000 / 60} minutes)")
            } catch (e: CancellationException) {
                logger.info("Stable version loading cancelled due to plugin unload")
                throw e
            } catch (e: Exception) {
                println("DEBUG ComposeVersionCache: FAILED to load stable versions: ${e.message}, using fallback")
                logger.warn("Failed to load stable Compose versions, using fallback: ${e.message}")
                if (persistentState.stableVersions.isEmpty()) {
                    persistentState.stableVersions = ComposeVersions.STABLE_VERSIONS
                }
                persistentState.stableLastLoadTime = System.currentTimeMillis()
            } finally {
                isLoadingStable = false
            }
        }
    }
    
    private fun loadDevVersionsInBackground() {
        if (isLoadingDev) {
            logger.info("Dev version loading already in progress, skipping")
            return
        }
        
        isLoadingDev = true
        logger.info("Starting background loading of dev Compose versions from Maven")
        
        scope.launch {
            try {
                if (!isActive) {
                    logger.info("Coroutine cancelled before loading dev versions")
                    return@launch
                }
                
                val versions = versionService.fetchAvailableVersions(includeDevVersions = true)
                
                if (!isActive) {
                    logger.info("Coroutine cancelled after loading dev versions")
                    return@launch
                }
                
                persistentState.devVersions = versions
                persistentState.devLastLoadTime = System.currentTimeMillis()
                logger.info("Successfully loaded ${versions.size} dev Compose versions: ${versions.take(5).joinToString(", ")}... (TTL: ${CACHE_TTL_MS / 1000 / 60} minutes)")
            } catch (e: CancellationException) {
                logger.info("Dev version loading cancelled due to plugin unload")
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to load dev Compose versions, using fallback: ${e.message}")
                if (persistentState.devVersions.isEmpty()) {
                    persistentState.devVersions = ComposeVersions.STABLE_VERSIONS
                }
                persistentState.devLastLoadTime = System.currentTimeMillis()
            } finally {
                isLoadingDev = false
            }
        }
    }
    
    override fun dispose() {
        logger.info("Disposing ComposeVersionCache, cancelling all background tasks")
        scope.cancel()
    }
}

