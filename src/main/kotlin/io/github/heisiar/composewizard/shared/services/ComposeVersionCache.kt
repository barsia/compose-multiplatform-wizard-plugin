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
import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ComposeVersionCacheState(
    var cacheVersion: Int = 0, // Cache version for invalidation
    var stableVersions: List<String> = emptyList(),
    var stableLastLoadTime: Long = 0L,
    var devVersions: List<String> = emptyList(),
    var devLastLoadTime: Long = 0L,
    // LinkedHashMap preserves insertion order for FIFO cleanup
    var lifecycleVersions: LinkedHashMap<String, String> = linkedMapOf(),
    // Track if lifecycle version is from hardcoded bundle (true) or GitHub (false)
    var lifecycleIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf()
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
    private val libraryVersionService = ComposeLibraryVersionService()
    
    private var persistentState = ComposeVersionCacheState()
    
    @Volatile
    private var isLoadingStable = false
    
    @Volatile
    private var isLoadingDev = false
    
    @Volatile
    private var initialized = false
    
    private val lifecycleResolvingVersions = mutableSetOf<String>()
    
    private val _lifecycleVersionUpdates = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val lifecycleVersionUpdates = _lifecycleVersionUpdates.asSharedFlow()
    
    companion object {
        // Cache version - increment ONLY when cache DATA STRUCTURE changes (e.g., new fields in ComposeVersionCacheState)
        // DO NOT increment for version updates - TTL and Refresh button handle that!
        // Current version 2: LinkedHashMap for lifecycle versions (FIFO cleanup)
        private const val CURRENT_CACHE_VERSION = 2
        
        // Cache TTL: 24 hours (in milliseconds)
        // Compose stable versions are released every 2-3 weeks
        // Dev versions may be released multiple times per day
        // Check for new versions once per day to minimize Maven load
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        
        // Maximum number of versions to keep in cache (hardcoded + dynamic new versions)
        private const val MAX_CACHED_VERSIONS = 20
        
        // Maximum number of lifecycle version entries (FIFO cleanup when exceeded)
        private const val MAX_LIFECYCLE_CACHE_SIZE = 200
        
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
        
        // Check cache version and invalidate if outdated
        if (persistentState.cacheVersion != CURRENT_CACHE_VERSION) {
            println("DEBUG ComposeVersionCache: Cache version mismatch (${persistentState.cacheVersion} != $CURRENT_CACHE_VERSION), invalidating old cache")
            persistentState.cacheVersion = CURRENT_CACHE_VERSION
            persistentState.lifecycleVersions.clear()
            persistentState.stableVersions = emptyList()
            persistentState.devVersions = emptyList()
            persistentState.stableLastLoadTime = 0L
            persistentState.devLastLoadTime = 0L
        }
        
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
     * 
     * Logic:
     * 1. If cache exists → return cache
     * 2. If no cache → return null (loading), trigger fetch
     * 3. If fetch fails → fallback to hardcoded (handled in loadStableVersionsInBackground)
     * 
     * Returns null if loading not yet completed.
     * Returns cached versions (or hardcoded fallback) if loading completed.
     * Automatically refreshes cache in background if TTL expired.
     */
    fun getStableVersions(): List<String>? {
        if (!initialized) {
            println("DEBUG ComposeVersionCache.getStableVersions(): Not initialized yet, initializing now")
            initializeCache()
        }
        
        // Check if cache is valid and not expired
        if (persistentState.stableVersions.isNotEmpty() && !isStableCacheExpired()) {
            // Always sort cached versions to fix old incorrectly sorted caches
            val sorted = persistentState.stableVersions
                .sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
            println("DEBUG ComposeVersionCache.getStableVersions(): returning cached (sorted) ${sorted.take(3)}")
            return sorted
        }
        
        // No cache or expired → trigger loading (if not already loading)
        if (!isLoadingStable) {
            logger.info("Stable cache missing or expired, triggering background load")
            println("DEBUG ComposeVersionCache: Starting stable version load (cache missing or expired)")
            loadStableVersionsInBackground()
        }
        
        // While loading → return null (UI will show loading indicator)
        if (isLoadingStable) {
            println("DEBUG ComposeVersionCache.getStableVersions(): loading in progress, returning null")
            return null
        }
        
        // Loading completed → return result (either from Maven or hardcoded fallback), sorted
        val versions = persistentState.stableVersions.ifEmpty { ComposeVersions.STABLE_VERSIONS_HARDCODED }
        val sorted = versions.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        println("DEBUG ComposeVersionCache.getStableVersions(): loading completed, returning (sorted) ${sorted.take(3)}")
        return sorted
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
        
        // Check if cache is valid and not expired
        if (persistentState.devVersions.isNotEmpty() && !isDevCacheExpired()) {
            // Return dev versions as-is (no sorting for dev)
            println("DEBUG ComposeVersionCache.getDevVersions(): returning cached (unsorted) ${persistentState.devVersions.take(3)}")
            return persistentState.devVersions
        }
        
        // No cache or expired → trigger loading (if not already loading)
        if (!isLoadingDev) {
            logger.info("Dev cache missing or expired, triggering background load")
            println("DEBUG ComposeVersionCache: Starting dev version load (cache missing or expired)")
            loadDevVersionsInBackground()
        }
        
        // While loading → return null (UI will show loading indicator)
        if (isLoadingDev) {
            println("DEBUG ComposeVersionCache.getDevVersions(): loading in progress, returning null")
            return null
        }
        
        // Loading completed → return result (either from Maven or hardcoded fallback), unsorted
        val versions = persistentState.devVersions.ifEmpty { ComposeVersions.STABLE_VERSIONS }
        println("DEBUG ComposeVersionCache.getDevVersions(): loading completed, returning (unsorted) ${versions.take(3)}")
        return versions
    }
    
    /**
     * Check if stable cache has expired based on TTL.
     */
    private fun isStableCacheExpired(): Boolean {
        if (persistentState.stableLastLoadTime == 0L || persistentState.stableVersions.isEmpty()) {
            println("DEBUG isStableCacheExpired: Cache empty or never loaded, expired=true")
            return true
        }
        val currentTime = System.currentTimeMillis()
        val age = currentTime - persistentState.stableLastLoadTime
        val expired = age > CACHE_TTL_MS
        println("DEBUG isStableCacheExpired: currentTime=$currentTime, lastLoad=${persistentState.stableLastLoadTime}, age=$age ms (${age / 1000 / 60 / 60} hours), TTL=${CACHE_TTL_MS} ms (24 hours), expired=$expired")
        return expired
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
     * Check if using hardcoded fallback versions (no successful fetch from Maven yet).
     * Returns true if stable versions are empty or haven't been loaded from Maven.
     */
    fun isUsingFallbackVersions(): Boolean {
        // If never loaded from Maven (initial state with empty cache)
        if (persistentState.stableLastLoadTime == 0L) {
            return true
        }
        
        // If cache is empty after failed load
        if (persistentState.stableVersions.isEmpty()) {
            return true
        }
        
        // If all cached versions are from hardcoded list (no new versions from Maven)
        val hardcoded = ComposeVersions.STABLE_VERSIONS_HARDCODED.toSet()
        val cached = persistentState.stableVersions.toSet()
        return cached.all { it in hardcoded }
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
                
                val versionsFromMaven = versionService.fetchAvailableVersions(includeDevVersions = false)
                
                if (!isActive) {
                    logger.info("Coroutine cancelled after loading stable versions")
                    return@launch
                }
                
                // Use versions from Maven as-is (already sorted and limited to 20)
                println("DEBUG ComposeVersionCache: Loaded ${versionsFromMaven.size} stable versions from Maven: ${versionsFromMaven.take(10)}")
                
                persistentState.stableVersions = versionsFromMaven
                persistentState.stableLastLoadTime = System.currentTimeMillis()
                
                println("DEBUG ComposeVersionCache: Cached ${versionsFromMaven.size} stable versions (TTL: 24h)")
                logger.info("Cached ${versionsFromMaven.size} stable Compose versions from Maven")
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
                
                val versionsFromMaven = versionService.fetchAvailableVersions(includeDevVersions = true)
                
                if (!isActive) {
                    logger.info("Coroutine cancelled after loading dev versions")
                    return@launch
                }
                
                // Use versions from Maven as-is (already sorted and limited to 20)
                println("DEBUG ComposeVersionCache: Loaded ${versionsFromMaven.size} dev versions from Maven: ${versionsFromMaven.take(10)}")
                
                persistentState.devVersions = versionsFromMaven
                persistentState.devLastLoadTime = System.currentTimeMillis()
                
                println("DEBUG ComposeVersionCache: Cached ${versionsFromMaven.size} dev versions (TTL: 24h)")
                logger.info("Cached ${versionsFromMaven.size} dev Compose versions from Maven")
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
    
    /**
     * Get Lifecycle version for given Compose version.
     * Returns cached value if available, triggers background resolution if not.
     * Returns null while resolving (to show loading indicator in UI).
     * Empty string in cache means "not found" - treated as null.
     */
    fun getLifecycleVersion(composeVersion: String): String? {
        if (!initialized) {
            initializeCache()
        }
        
        val cached = persistentState.lifecycleVersions[composeVersion]
        println("DEBUG getLifecycleVersion: composeVersion=$composeVersion, cached='$cached' (null=${cached == null}, empty=${cached?.isEmpty()})")
        
        // Empty string in cache means "not found" - don't return it, treat as null
        if (cached != null && cached.isNotEmpty()) {
            println("DEBUG getLifecycleVersion: Returning cached value: $cached")
            return cached
        }
        
        // Trigger background resolution if not already resolving
        if (!isResolvingLifecycle(composeVersion)) {
            println("DEBUG getLifecycleVersion: Starting background resolution for $composeVersion")
            resolveLifecycleVersionInBackground(composeVersion)
        } else {
            println("DEBUG getLifecycleVersion: Already resolving $composeVersion")
        }
        
        // Return null while resolving (UI should show loading indicator)
        return null
    }
    
    /**
     * Check if Lifecycle version is currently being resolved for given Compose version.
     */
    fun isResolvingLifecycle(composeVersion: String): Boolean {
        return synchronized(lifecycleResolvingVersions) {
            lifecycleResolvingVersions.contains(composeVersion)
        }
    }
    
    /**
     * Check if Lifecycle version for given Compose version is from fallback (bundle).
     * Returns true only if version was loaded from hardcoded bundle, not from GitHub.
     */
    fun isLifecycleFallback(composeVersion: String): Boolean {
        return persistentState.lifecycleIsFromBundle[composeVersion] == true
    }
    
    /**
     * Add lifecycle version to cache with FIFO cleanup.
     * If cache exceeds MAX_LIFECYCLE_CACHE_SIZE, removes oldest entries.
     * @param fromBundle true if version is from hardcoded bundle (fallback), false if from GitHub
     */
    private fun cacheLifecycleVersion(composeVersion: String, lifecycleVersion: String, fromBundle: Boolean = false) {
        synchronized(persistentState.lifecycleVersions) {
            // Add new entry
            persistentState.lifecycleVersions[composeVersion] = lifecycleVersion
            persistentState.lifecycleIsFromBundle[composeVersion] = fromBundle
            
            // FIFO cleanup: remove oldest entries if exceeds limit
            while (persistentState.lifecycleVersions.size > MAX_LIFECYCLE_CACHE_SIZE) {
                val oldestKey = persistentState.lifecycleVersions.keys.first()
                persistentState.lifecycleVersions.remove(oldestKey)
                persistentState.lifecycleIsFromBundle.remove(oldestKey)
                println("DEBUG: Removed oldest lifecycle cache entry: $oldestKey (FIFO cleanup)")
            }
            
            val source = if (fromBundle) "Bundle 📦" else "GitHub"
            println("DEBUG: Cached lifecycle: $composeVersion → $lifecycleVersion from $source (cache size: ${persistentState.lifecycleVersions.size}/$MAX_LIFECYCLE_CACHE_SIZE)")
        }
    }
    
    private fun resolveLifecycleVersionInBackground(composeVersion: String) {
        synchronized(lifecycleResolvingVersions) {
            if (lifecycleResolvingVersions.contains(composeVersion)) {
                println("DEBUG: Already resolving Lifecycle for Compose $composeVersion")
                return
            }
            lifecycleResolvingVersions.add(composeVersion)
        }
        
        scope.launch {
            try {
                println("DEBUG: Resolving Lifecycle version for Compose $composeVersion in background")
                
                // Step 1: Direct GitHub Web UI request for full version
                var lifecycleVersion = libraryVersionService.fetchLifecycleFromWebUI(composeVersion)
                
                if (lifecycleVersion != null) {
                    println("DEBUG: ✅ Found lifecycle in GitHub for $composeVersion: $lifecycleVersion")
                    cacheLifecycleVersion(composeVersion, lifecycleVersion)
                    _lifecycleVersionUpdates.emit(composeVersion to lifecycleVersion)
                    return@launch
                }
                
                // Step 1.5: Fast-path for +dev versions
                val baseVersion = composeVersion.substringBefore("+dev")
                if (baseVersion != composeVersion) {
                    println("DEBUG: Dev version detected, trying base version: $baseVersion")
                    
                    // Try GitHub for base version first (priority for freshness!)
                    val baseLifecycle = libraryVersionService.fetchLifecycleFromWebUI(baseVersion)
                    if (baseLifecycle != null) {
                        println("DEBUG: ✅ Found base in GitHub: $baseVersion → $baseLifecycle")
                        cacheLifecycleVersion(composeVersion, baseLifecycle)
                        _lifecycleVersionUpdates.emit(composeVersion to baseLifecycle)
                        return@launch
                    }
                    
                    // If GitHub didn't respond - Bundle for base version (quick fallback)
                    val baseBundle = ComposeVersions.getLibraryBundle(baseVersion)
                    if (baseBundle?.lifecycleVersion != null) {
                        println("DEBUG: 📦 Found base in Bundle (network fallback): $baseVersion → ${baseBundle.lifecycleVersion}")
                        cacheLifecycleVersion(composeVersion, baseBundle.lifecycleVersion, fromBundle = true)
                        _lifecycleVersionUpdates.emit(composeVersion to baseBundle.lifecycleVersion)
                        return@launch
                    }
                }
                
                // Step 2: Fallback chain (Bundle → GitHub for each fallback version)
                println("DEBUG: Lifecycle not found for $composeVersion, trying fallback chain")
                val fallbackVersions = libraryVersionService.generateFallbackVersions(baseVersion)
                var isRateLimited = false
                
                for ((index, fallbackVersion) in fallbackVersions.withIndex()) {
                    println("DEBUG: Fallback [$index/${fallbackVersions.size}]: $fallbackVersion")
                    
                    // 2.1: Check Bundle first (fast, no network)
                    val bundle = ComposeVersions.getLibraryBundle(fallbackVersion)
                    if (bundle?.lifecycleVersion != null) {
                        println("DEBUG: 📦 Found in Bundle: $fallbackVersion → ${bundle.lifecycleVersion}")
                        cacheLifecycleVersion(composeVersion, bundle.lifecycleVersion, fromBundle = true)  // Cache for REQUESTED version!
                        _lifecycleVersionUpdates.emit(composeVersion to bundle.lifecycleVersion)
                        return@launch
                    }
                    
                    // 2.2: Bundle not found → GitHub Web UI (if not rate limited)
                    if (!isRateLimited) {
                        delay(150) // Small delay between requests
                        val result = libraryVersionService.fetchLifecycleFromWebUIWithStatus(fallbackVersion)
                        
                        if (result.isRateLimited) {
                            isRateLimited = true
                            println("DEBUG: ⚠️ Rate limited, switching to Bundle-only mode")
                        } else if (result.lifecycle != null) {
                            println("DEBUG: ✅ Found in GitHub: $fallbackVersion → ${result.lifecycle}")
                            cacheLifecycleVersion(composeVersion, result.lifecycle)  // Cache for REQUESTED version!
                            _lifecycleVersionUpdates.emit(composeVersion to result.lifecycle)
                            return@launch
                        }
                    }
                }
                
                // Step 3: Nothing found → use default fallback
                println("DEBUG: ⚠️ Lifecycle not found, using default fallback: ${ComposeVersions.DEFAULT_ANDROIDX_LIFECYCLE_VERSION}")
                cacheLifecycleVersion(composeVersion, ComposeVersions.DEFAULT_ANDROIDX_LIFECYCLE_VERSION)
                _lifecycleVersionUpdates.emit(composeVersion to ComposeVersions.DEFAULT_ANDROIDX_LIFECYCLE_VERSION)
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve Lifecycle version for Compose $composeVersion: ${e.message}")
                cacheLifecycleVersion(composeVersion, ComposeVersions.DEFAULT_ANDROIDX_LIFECYCLE_VERSION)
                _lifecycleVersionUpdates.emit(composeVersion to ComposeVersions.DEFAULT_ANDROIDX_LIFECYCLE_VERSION)
            } finally {
                synchronized(lifecycleResolvingVersions) {
                    lifecycleResolvingVersions.remove(composeVersion)
                }
            }
        }
    }
    
    override fun dispose() {
        logger.info("Disposing ComposeVersionCache, cancelling all background tasks")
        scope.cancel()
    }
}

