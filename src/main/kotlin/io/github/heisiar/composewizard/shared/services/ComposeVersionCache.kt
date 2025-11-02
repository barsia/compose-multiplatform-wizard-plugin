package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import kotlinx.coroutines.*

/**
 * Application-level service that caches Compose Multiplatform versions with TTL.
 * 
 * Versions are loaded in background on IDE startup and cached with a Time To Live (TTL).
 * Cache is automatically refreshed when TTL expires (12 hours by default).
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
class ComposeVersionCache {
    
    private val logger = Logger.getInstance(ComposeVersionCache::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val versionService = ComposeVersionService()
    
    // Stable versions cache
    @Volatile
    private var cachedStableVersions: List<String> = ComposeVersions.STABLE_VERSIONS
    
    @Volatile
    private var stableLastLoadTime: Long = 0L
    
    @Volatile
    private var isLoadingStable = false
    
    // Dev versions cache
    @Volatile
    private var cachedDevVersions: List<String> = ComposeVersions.STABLE_VERSIONS // Same fallback
    
    @Volatile
    private var devLastLoadTime: Long = 0L
    
    @Volatile
    private var isLoadingDev = false
    
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
        // Start loading both stable and dev versions in background on IDE startup
        loadStableVersionsInBackground()
        loadDevVersionsInBackground()
    }
    
    /**
     * Get cached stable versions (non-blocking).
     * Automatically refreshes cache in background if TTL expired.
     * Returns cached versions immediately (never blocks).
     */
    fun getStableVersions(): List<String> {
        // Check if cache expired and refresh in background if needed
        if (isStableCacheExpired() && !isLoadingStable) {
            logger.info("Stable cache expired (TTL: ${CACHE_TTL_MS}ms), refreshing in background")
            loadStableVersionsInBackground()
        }
        return cachedStableVersions
    }
    
    /**
     * Get cached dev versions (non-blocking).
     * Automatically refreshes cache in background if TTL expired.
     * Returns cached versions immediately (never blocks).
     */
    fun getDevVersions(): List<String> {
        // Check if cache expired and refresh in background if needed
        if (isDevCacheExpired() && !isLoadingDev) {
            logger.info("Dev cache expired (TTL: ${CACHE_TTL_MS}ms), refreshing in background")
            loadDevVersionsInBackground()
        }
        return cachedDevVersions
    }
    
    /**
     * Check if stable cache has expired based on TTL.
     */
    private fun isStableCacheExpired(): Boolean {
        if (stableLastLoadTime == 0L) {
            return true // Never loaded
        }
        val age = System.currentTimeMillis() - stableLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    /**
     * Check if dev cache has expired based on TTL.
     */
    private fun isDevCacheExpired(): Boolean {
        if (devLastLoadTime == 0L) {
            return true // Never loaded
        }
        val age = System.currentTimeMillis() - devLastLoadTime
        return age > CACHE_TTL_MS
    }
    
    /**
     * Get stable versions, waiting for background loading to complete if necessary.
     * 
     * This method blocks until versions are loaded or timeout occurs.
     * Use this for Template API wizard where we need versions synchronously.
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 3000ms)
     * @return List of stable versions (from Maven or fallback)
     */
    fun getStableVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        if (!isLoadingStable) {
            return cachedStableVersions
        }
        
        logger.info("Waiting for stable Compose versions to load (timeout: ${timeoutMs}ms)...")
        val startTime = System.currentTimeMillis()
        
        while (isLoadingStable && (System.currentTimeMillis() - startTime) < timeoutMs) {
            Thread.sleep(100)
        }
        
        if (isLoadingStable) {
            logger.warn("Timeout waiting for stable Compose versions, using fallback")
        } else {
            logger.info("Stable Compose versions loaded successfully")
        }
        
        return cachedStableVersions
    }
    
    /**
     * Get dev versions, waiting for background loading to complete if necessary.
     * 
     * This method blocks until versions are loaded or timeout occurs.
     * Use this when you need dev versions synchronously.
     * 
     * @param timeoutMs Maximum time to wait in milliseconds (default: 3000ms)
     * @return List of dev versions (from Maven or fallback)
     */
    fun getDevVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        if (!isLoadingDev) {
            return cachedDevVersions
        }
        
        logger.info("Waiting for dev Compose versions to load (timeout: ${timeoutMs}ms)...")
        val startTime = System.currentTimeMillis()
        
        while (isLoadingDev && (System.currentTimeMillis() - startTime) < timeoutMs) {
            Thread.sleep(100)
        }
        
        if (isLoadingDev) {
            logger.warn("Timeout waiting for dev Compose versions, using fallback")
        } else {
            logger.info("Dev Compose versions loaded successfully")
        }
        
        return cachedDevVersions
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
        stableLastLoadTime = 0L
    }
    
    /**
     * Manually invalidate dev cache (reset TTL).
     * Next call to getDevVersions() will trigger background refresh.
     * Non-blocking - doesn't reload immediately.
     */
    fun invalidateDevCache() {
        logger.info("Dev cache manually invalidated, will refresh on next access")
        devLastLoadTime = 0L
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
        stableLastLoadTime = 0L
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
        devLastLoadTime = 0L
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
                val versions = versionService.fetchAvailableVersions(includeDevVersions = false)
                cachedStableVersions = versions
                stableLastLoadTime = System.currentTimeMillis()
                logger.info("Successfully loaded ${versions.size} stable Compose versions: ${versions.take(5).joinToString(", ")}... (TTL: ${CACHE_TTL_MS / 1000 / 60} minutes)")
            } catch (e: Exception) {
                logger.warn("Failed to load stable Compose versions, using fallback: ${e.message}")
                cachedStableVersions = ComposeVersions.STABLE_VERSIONS
                stableLastLoadTime = System.currentTimeMillis() // Set time even on failure to avoid constant retries
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
                val versions = versionService.fetchAvailableVersions(includeDevVersions = true)
                cachedDevVersions = versions
                devLastLoadTime = System.currentTimeMillis()
                logger.info("Successfully loaded ${versions.size} dev Compose versions: ${versions.take(5).joinToString(", ")}... (TTL: ${CACHE_TTL_MS / 1000 / 60} minutes)")
            } catch (e: Exception) {
                logger.warn("Failed to load dev Compose versions, using fallback: ${e.message}")
                cachedDevVersions = ComposeVersions.STABLE_VERSIONS
                devLastLoadTime = System.currentTimeMillis() // Set time even on failure to avoid constant retries
            } finally {
                isLoadingDev = false
            }
        }
    }
}

