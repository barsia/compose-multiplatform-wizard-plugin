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
import io.github.heisiar.composewizard.shared.LibraryType
import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ComposeVersionCacheState(
    var stableVersions: List<String> = emptyList(),
    var stableLastLoadTime: Long = 0L,
    var devVersions: List<String> = emptyList(),
    var devLastLoadTime: Long = 0L,
    
    // Library versions cache (LinkedHashMap preserves insertion order for FIFO cleanup)
    var lifecycleVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var lifecycleIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var material3Versions: LinkedHashMap<String, String> = linkedMapOf(),
    var material3IsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var material3AdaptiveVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var material3AdaptiveIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var navigationVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var navigationIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var navigation3Versions: LinkedHashMap<String, String> = linkedMapOf(),
    var navigation3IsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var navigationEventVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var navigationEventIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var savedStateVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var savedStateIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var windowVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var windowIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf(),
    
    var hotReloadVersions: LinkedHashMap<String, String> = linkedMapOf(),
    var hotReloadIsFromBundle: LinkedHashMap<String, Boolean> = linkedMapOf()
)

/**
 * Application-level service that caches Compose Multiplatform versions with TTL.
 * 
 * ## Version Sources
 * 
 * **Stable versions** are combined from:
 * 1. Hardcoded versions from LIBRARY_BUNDLES (instant availability, no internet)
 * 2. New versions from Maven Central (filtered: only versions newer than first in LIBRARY_BUNDLES)
 * 
 * **Dev versions** are fetched from JetBrains Space Maven (no filtering).
 * 
 * Versions are loaded in background on IDE startup and cached with a Time To Live (TTL).
 * Cache is automatically refreshed when TTL expires (24 hours).
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
 * cache.forceReloadStable()
 * 
 * // Invalidate cache (refresh on next access)
 * cache.invalidateStableCache()
 * ```
 * 
 * ## TTL Configuration
 * 
 * Current TTL: 24 hours (CACHE_TTL_MS)
 * - Compose stable versions: released every 2-3 weeks
 * - 24 hours = 1 check per day = optimal balance
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
    
    // Emit event when library cache is invalidated (for UI to reload libraries)
    private val _cacheInvalidated = MutableSharedFlow<Unit>(replay = 0)
    val cacheInvalidated = _cacheInvalidated.asSharedFlow()
    
    companion object {
        // Max number of library versions to cache per type (FIFO cleanup when exceeded)
        private const val MAX_LIBRARY_CACHE_SIZE = 200
        
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
     * Returns combined list of:
     * - Hardcoded versions from LIBRARY_BUNDLES
     * - New versions from Maven (filtered: only newer than first in LIBRARY_BUNDLES)
     * 
     * Logic:
     * 1. If cache exists and not expired → return cached (sorted by semver)
     * 2. If no cache or expired → trigger background fetch, return null while loading
     * 3. If fetch fails → fallback to hardcoded versions only
     * 
     * Returns null if loading not yet completed.
     * Returns cached versions (or hardcoded fallback) if loading completed.
     * Automatically refreshes cache in background if TTL expired (24 hours).
     */
    fun getStableVersions(): List<String>? {
        if (!initialized) {
            println("DEBUG ComposeVersionCache.getStableVersions(): Not initialized yet, initializing now")
            initializeCache()
        }
        
        // Check if cache is valid and not expired
        if (persistentState.stableVersions.isNotEmpty() && !isStableCacheExpired()) {
            // Return cached versions as-is (already sorted when saved)
            println("DEBUG ComposeVersionCache.getStableVersions(): returning cached ${persistentState.stableVersions.take(3)}")
            return persistentState.stableVersions
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
        
        // Loading completed → return result (no fallback for dev versions)
        val versions = persistentState.devVersions
        println("DEBUG ComposeVersionCache.getDevVersions(): loading completed, returning ${versions.take(3)}")
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
            return@withContext if (persistentState.stableVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else persistentState.stableVersions
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
        
        if (persistentState.stableVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else persistentState.stableVersions
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
            return@withContext if (persistentState.devVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else persistentState.devVersions
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
        
        if (persistentState.devVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else persistentState.devVersions
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
    
    fun isUsingDevFallbackVersions(): Boolean {
        // If never loaded from Maven (initial state with empty cache)
        if (persistentState.devLastLoadTime == 0L) {
            println("DEBUG isUsingDevFallbackVersions: devLastLoadTime=0, returning true (never loaded)")
            return true
        }
        
        // If cache is empty after failed load
        if (persistentState.devVersions.isEmpty()) {
            println("DEBUG isUsingDevFallbackVersions: devVersions is empty, returning true")
            return true
        }
        
        // Dev versions are never hardcoded, so if we have any versions loaded, it's not fallback
        println("DEBUG isUsingDevFallbackVersions: devLastLoadTime=${persistentState.devLastLoadTime}, devVersions.size=${persistentState.devVersions.size}, returning false")
        return false
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
        invalidateLibraryCache()
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
        invalidateLibraryCache()
        loadDevVersionsInBackground()
    }
    
    /**
     * Invalidate library cache for all Compose versions.
     * This forces re-fetching of all library versions from GitHub on next access.
     * Called when user clicks Refresh button.
     */
    private fun invalidateLibraryCache() {
        println("DEBUG ComposeVersionCache: Invalidating all library caches")
        persistentState.lifecycleVersions.clear()
        persistentState.lifecycleIsFromBundle.clear()
        persistentState.material3Versions.clear()
        persistentState.material3IsFromBundle.clear()
        persistentState.material3AdaptiveVersions.clear()
        persistentState.material3AdaptiveIsFromBundle.clear()
        persistentState.navigationVersions.clear()
        persistentState.navigationIsFromBundle.clear()
        persistentState.navigation3Versions.clear()
        persistentState.navigation3IsFromBundle.clear()
        persistentState.navigationEventVersions.clear()
        persistentState.navigationEventIsFromBundle.clear()
        persistentState.savedStateVersions.clear()
        persistentState.savedStateIsFromBundle.clear()
        persistentState.windowVersions.clear()
        persistentState.windowIsFromBundle.clear()
        persistentState.hotReloadVersions.clear()
        persistentState.hotReloadIsFromBundle.clear()
        logger.info("Library cache invalidated")
        
        // Notify UI to reload libraries
        scope.launch {
            _cacheInvalidated.emit(Unit)
        }
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
                
                // Fetch filtered versions from Maven (only newer than first in LIBRARY_BUNDLES)
                val versionsFromMaven = versionService.fetchAvailableVersions(includeDevVersions = false)
                
                if (!isActive) {
                    logger.info("Coroutine cancelled after loading stable versions")
                    return@launch
                }
                
                // Combine hardcoded versions from LIBRARY_BUNDLES with new versions from Maven
                val hardcodedVersions = ComposeVersions.LIBRARY_BUNDLES.keys.toList()
                val allVersions = (hardcodedVersions + versionsFromMaven).distinct()
                
                // Sort by semantic version (descending - newest first)
                val sortedVersions = allVersions.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
                
                // Limit to MAX_CACHED_VERSIONS
                val finalVersions = sortedVersions.take(MAX_CACHED_VERSIONS)
                
                println("DEBUG ComposeVersionCache: Loaded ${versionsFromMaven.size} new versions from Maven")
                println("DEBUG ComposeVersionCache: Combined with ${hardcodedVersions.size} hardcoded versions")
                println("DEBUG ComposeVersionCache: Final stable versions (${finalVersions.size}): ${finalVersions.take(5)}")
                
                persistentState.stableVersions = finalVersions
                persistentState.stableLastLoadTime = System.currentTimeMillis()
                
                logger.info("Cached ${finalVersions.size} stable Compose versions (${hardcodedVersions.size} hardcoded + ${versionsFromMaven.size} from Maven)")
            } catch (e: CancellationException) {
                logger.info("Stable version loading cancelled due to plugin unload")
                throw e
            } catch (e: Exception) {
                println("DEBUG ComposeVersionCache: FAILED to load stable versions: ${e.message}, using hardcoded fallback")
                logger.warn("Failed to load stable Compose versions, using hardcoded fallback: ${e.message}")
                if (persistentState.stableVersions.isEmpty()) {
                    persistentState.stableVersions = ComposeVersions.STABLE_VERSIONS_HARDCODED
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
                logger.warn("Failed to load dev Compose versions: ${e.message}")
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
        
        // If cached (including empty string = "not found"), return it
        if (cached != null) {
            println("DEBUG getLifecycleVersion: Returning cached value: '$cached'")
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
     * Get versions map for given library type.
     */
    private fun getVersionsMap(type: LibraryType): LinkedHashMap<String, String> {
        return when (type) {
            LibraryType.LIFECYCLE -> persistentState.lifecycleVersions
            LibraryType.MATERIAL3 -> persistentState.material3Versions
            LibraryType.MATERIAL3_ADAPTIVE -> persistentState.material3AdaptiveVersions
            LibraryType.NAVIGATION -> persistentState.navigationVersions
            LibraryType.NAVIGATION3 -> persistentState.navigation3Versions
            LibraryType.NAVIGATION_EVENT -> persistentState.navigationEventVersions
            LibraryType.SAVED_STATE -> persistentState.savedStateVersions
            LibraryType.WINDOW -> persistentState.windowVersions
            LibraryType.HOT_RELOAD -> persistentState.hotReloadVersions
        }
    }
    
    /**
     * Get isFromBundle map for given library type.
     */
    private fun getIsFromBundleMap(type: LibraryType): LinkedHashMap<String, Boolean> {
        return when (type) {
            LibraryType.LIFECYCLE -> persistentState.lifecycleIsFromBundle
            LibraryType.MATERIAL3 -> persistentState.material3IsFromBundle
            LibraryType.MATERIAL3_ADAPTIVE -> persistentState.material3AdaptiveIsFromBundle
            LibraryType.NAVIGATION -> persistentState.navigationIsFromBundle
            LibraryType.NAVIGATION3 -> persistentState.navigation3IsFromBundle
            LibraryType.NAVIGATION_EVENT -> persistentState.navigationEventIsFromBundle
            LibraryType.SAVED_STATE -> persistentState.savedStateIsFromBundle
            LibraryType.WINDOW -> persistentState.windowIsFromBundle
            LibraryType.HOT_RELOAD -> persistentState.hotReloadIsFromBundle
        }
    }
    
    /**
     * Get library version for given Compose version and library type.
     * Returns cached value if available, triggers background resolution if not.
     * Returns null while resolving (to show loading indicator in UI).
     * Empty string in cache means "not found" - treated as null.
     */
    fun getLibraryVersion(composeVersion: String, type: LibraryType): String? {
        if (!initialized) {
            initializeCache()
        }
        
        val versionsMap = getVersionsMap(type)
        val cached = versionsMap[composeVersion]
        
        // If cached (including empty string = "not found"), return it
        if (cached != null) {
            val fromBundle = isLibraryFromBundle(composeVersion, type)
            println("DEBUG: Cache HIT for ${type.displayName} / $composeVersion: '$cached' (fromBundle=$fromBundle)")
            return cached
        }
        
        // Trigger background resolution if not already resolving
        if (!isResolvingLibrary(composeVersion, type)) {
            println("DEBUG: Cache MISS for ${type.displayName} / $composeVersion, triggering resolution")
            resolveLibraryVersionInBackground(composeVersion, type)
        }
        
        // Return null while resolving (UI should show loading indicator)
        return null
    }
    
    /**
     * Check if library version is currently being resolved.
     */
    fun isResolvingLibrary(composeVersion: String, type: LibraryType): Boolean {
        val key = "$composeVersion:${type.name}"
        return synchronized(lifecycleResolvingVersions) {
            lifecycleResolvingVersions.contains(key)
        }
    }
    
    /**
     * Check if library version is from fallback (bundle).
     * Returns true only if version was loaded from hardcoded bundle, not from GitHub.
     */
    fun isLibraryFromBundle(composeVersion: String, type: LibraryType): Boolean {
        return getIsFromBundleMap(type)[composeVersion] == true
    }
    
    /**
     * Add lifecycle version to cache with FIFO cleanup.
     * If cache exceeds MAX_LIFECYCLE_CACHE_SIZE, removes oldest entries.
     * @param fromBundle true if version is from hardcoded bundle (fallback), false if from GitHub
     * 
     * NOTE: Even when fromBundle=true, the version was originally fetched from GitHub
     * and hardcoded in LIBRARY_BUNDLES to avoid unnecessary requests to GitHub.
     * "fromBundle" only means it's used as a fallback when the version is not published
     * in the current tag page.
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
        // Check cache first - if already cached (including empty string), skip resolution
        if (persistentState.lifecycleVersions.containsKey(composeVersion)) {
            return
        }
        
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
                
                val baseVersion = composeVersion.substringBefore("+dev")
                
                // Step 1: Direct GitHub Web UI request for full version
                val result = libraryVersionService.fetchLifecycleFromWebUIWithStatus(composeVersion)
                
                if (result.lifecycle != null) {
                    println("DEBUG: ✅ Found lifecycle in GitHub for $composeVersion: ${result.lifecycle}")
                    cacheLifecycleVersion(composeVersion, result.lifecycle)
                    _lifecycleVersionUpdates.emit(composeVersion to result.lifecycle)
                    return@launch
                }
                
                // Step 1.5: If tag page exists but library not found - try fallback
                if (result.pageExists) {
                    println("DEBUG: ⚠️ Tag page exists for $composeVersion but lifecycle not published")
                    
                    // For +dev versions: try base version as fallback
                    if (baseVersion != composeVersion) {
                        println("DEBUG: Dev version detected, trying base version fallback: $baseVersion")
                        
                        // Try GitHub for base version
                        val baseLifecycle = libraryVersionService.fetchLifecycleFromWebUI(baseVersion)
                        if (baseLifecycle != null) {
                            println("DEBUG: ✅ Found base in GitHub: $baseVersion → $baseLifecycle")
                            cacheLifecycleVersion(composeVersion, baseLifecycle)
                            _lifecycleVersionUpdates.emit(composeVersion to baseLifecycle)
                            return@launch
                        }
                        
                        // Try Bundle for base version
                        val baseBundle = ComposeVersions.getLibraryBundle(baseVersion)
                        if (baseBundle?.lifecycleVersion != null) {
                            println("DEBUG: 📦 Found base in Bundle: $baseVersion → ${baseBundle.lifecycleVersion}")
                            cacheLifecycleVersion(composeVersion, baseBundle.lifecycleVersion, fromBundle = true)
                            _lifecycleVersionUpdates.emit(composeVersion to baseBundle.lifecycleVersion)
                            return@launch
                        }
                    }
                } else {
                    // Tag page doesn't exist - skip this version entirely
                    println("DEBUG: ❌ Tag page does not exist for $composeVersion, caching empty")
                    cacheLifecycleVersion(composeVersion, "")
                    _lifecycleVersionUpdates.emit(composeVersion to "")
                    return@launch
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
                
                // Step 3: Nothing found → cache empty string
                println("DEBUG: ⚠️ Lifecycle not found, caching empty string")
                cacheLifecycleVersion(composeVersion, "")
                _lifecycleVersionUpdates.emit(composeVersion to "")
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve Lifecycle version for Compose $composeVersion: ${e.message}")
                cacheLifecycleVersion(composeVersion, "")
                _lifecycleVersionUpdates.emit(composeVersion to "")
            } finally {
                synchronized(lifecycleResolvingVersions) {
                    lifecycleResolvingVersions.remove(composeVersion)
                }
            }
        }
    }
    
    /**
     * Universal library version resolution in background for any LibraryType.
     */
    private fun resolveLibraryVersionInBackground(composeVersion: String, type: LibraryType) {
        val key = "$composeVersion:${type.name}"
        
        // Check cache first - if already cached (including empty string), skip resolution
        val versionsMap = getVersionsMap(type)
        if (versionsMap.containsKey(composeVersion)) {
            return
        }
        
        synchronized(lifecycleResolvingVersions) {
            if (lifecycleResolvingVersions.contains(key)) {
                println("DEBUG: Already resolving ${type.displayName} for Compose $composeVersion")
                return
            }
            lifecycleResolvingVersions.add(key)
        }
        
        scope.launch {
            try {
                println("DEBUG: Resolving ${type.displayName} version for Compose $composeVersion in background")
                
                // Special handling for NAVIGATION: deprecated for Compose >= 1.10.0 (replaced by NAVIGATION3)
                if (type == LibraryType.NAVIGATION) {
                    val baseVersion = composeVersion.split("+").first().split("-").first()
                    val parts = baseVersion.split(".")
                    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    
                    if (major > 1 || (major == 1 && minor >= 10)) {
                        println("DEBUG: ⚠️ Navigation2 is deprecated for Compose >= 1.10.0 (use Navigation3 instead), caching empty for $composeVersion")
                        cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                        synchronized(lifecycleResolvingVersions) {
                            lifecycleResolvingVersions.remove(key)
                        }
                        return@launch
                    }
                }
                
                // Special handling for HOT_RELOAD: fetch from libs.versions.toml
                if (type == LibraryType.HOT_RELOAD) {
                    val hotReloadVersion = libraryVersionService.fetchHotReloadVersion(composeVersion)
                    if (hotReloadVersion != null) {
                        println("DEBUG: ✅ Found hot reload version from libs.versions.toml: $hotReloadVersion")
                        cacheLibraryVersion(composeVersion, type, hotReloadVersion, fromBundle = true)
                        return@launch
                    } else {
                        println("DEBUG: ⚠️ Hot reload version not found in libs.versions.toml, caching empty")
                        cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                        return@launch
                    }
                }
                
                // Step 1: Fetch all library versions from GitHub
                val result = libraryVersionService.fetchLibraryVersionsFromWebUI(composeVersion)
                val version = result.versions[type]
                
                if (version != null) {
                    println("DEBUG: ✅ Found ${type.displayName} in GitHub for $composeVersion: $version")
                    cacheLibraryVersion(composeVersion, type, version, fromBundle = false)
                    return@launch
                }
                
                // Step 1.5: If tag page exists but library not found - try fallback
                if (result.pageExists) {
                    println("DEBUG: ⚠️ Tag page exists for $composeVersion but ${type.displayName} not published")
                    
                    // Try Bundle for requested version first
                    val bundle = ComposeVersions.getLibraryBundle(composeVersion)
                    val bundleVersion = bundle?.getVersion(type)
                    if (bundleVersion != null) {
                        println("DEBUG: 📦 Found ${type.displayName} in Bundle for $composeVersion: $bundleVersion")
                        cacheLibraryVersion(composeVersion, type, bundleVersion, fromBundle = true)
                        return@launch
                    }
                    
                    // For +dev versions: try base version as fallback
                    val baseVersion = composeVersion.substringBefore("+dev")
                    if (baseVersion != composeVersion) {
                        println("DEBUG: Dev version detected, trying base version fallback: $baseVersion")
                        
                        // Try base version bundle
                        val baseBundle = ComposeVersions.getLibraryBundle(baseVersion)
                        val baseVersionLib = baseBundle?.getVersion(type)
                        if (baseVersionLib != null) {
                            println("DEBUG: 📦 Found ${type.displayName} in Bundle for base version $baseVersion: $baseVersionLib")
                            cacheLibraryVersion(composeVersion, type, baseVersionLib, fromBundle = true)
                            return@launch
                        }
                    }
                } else {
                    // Tag page doesn't exist - skip this version entirely
                    println("DEBUG: ❌ Tag page does not exist for $composeVersion, caching empty for ${type.displayName}")
                    cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                    return@launch
                }
                
                // Step 4: Try fallback versions (Bundle → GitHub for each)
                val fallbackVersions = libraryVersionService.generateFallbackVersions(composeVersion)
                var isRateLimited = false
                
                for ((index, fallbackVersion) in fallbackVersions.withIndex()) {
                    println("DEBUG: Fallback [$index/${fallbackVersions.size}] for ${type.displayName}: $fallbackVersion")
                    
                    // 4.1: Check Bundle first (fast, no network)
                    val fallbackBundle = ComposeVersions.getLibraryBundle(fallbackVersion)
                    val fallbackVersionLib = fallbackBundle?.getVersion(type)
                    if (fallbackVersionLib != null) {
                        println("DEBUG: 📦 Found ${type.displayName} in Bundle for fallback version $fallbackVersion: $fallbackVersionLib")
                        cacheLibraryVersion(composeVersion, type, fallbackVersionLib, fromBundle = true)
                        return@launch
                    }
                    
                    // 4.2: Bundle not found → GitHub (if not rate limited)
                    if (!isRateLimited) {
                        delay(150) // Small delay between requests
                        val fallbackResult = libraryVersionService.fetchLibraryVersionsFromWebUI(fallbackVersion)
                        
                        if (fallbackResult.isRateLimited) {
                            isRateLimited = true
                            println("DEBUG: ⚠️ Rate limited, switching to Bundle-only mode for ${type.displayName}")
                        } else {
                            val fallbackLibVersion = fallbackResult.versions[type]
                            if (fallbackLibVersion != null) {
                                println("DEBUG: ✅ Found ${type.displayName} in GitHub for fallback $fallbackVersion: $fallbackLibVersion")
                                cacheLibraryVersion(composeVersion, type, fallbackLibVersion, fromBundle = true)
                                return@launch
                            }
                        }
                    }
                }
                
                // Step 5: Not found - cache empty string to avoid repeated lookups
                println("DEBUG: ⚠️ ${type.displayName} not found for $composeVersion, caching empty string")
                cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
                
            } catch (e: Exception) {
                logger.warn("Failed to resolve ${type.displayName} version for Compose $composeVersion: ${e.message}")
                cacheLibraryVersion(composeVersion, type, "", fromBundle = false)
            } finally {
                synchronized(lifecycleResolvingVersions) {
                    lifecycleResolvingVersions.remove(key)
                }
            }
        }
    }
    
    /**
     * Universal library version caching with FIFO cleanup.
     * 
     * NOTE: Even when fromBundle=true, the version was originally fetched from GitHub
     * and hardcoded in LIBRARY_BUNDLES to avoid unnecessary requests to GitHub.
     * "fromBundle" only means it's used as a fallback when the version is not published
     * in the current tag page.
     */
    private fun cacheLibraryVersion(composeVersion: String, type: LibraryType, version: String, fromBundle: Boolean) {
        val versionsMap = getVersionsMap(type)
        val isFromBundleMap = getIsFromBundleMap(type)
        
        synchronized(versionsMap) {
            versionsMap[composeVersion] = version
            isFromBundleMap[composeVersion] = fromBundle
            
            // FIFO cleanup: remove oldest entries if exceeds limit
            while (versionsMap.size > MAX_LIFECYCLE_CACHE_SIZE) {
                val oldestKey = versionsMap.keys.first()
                versionsMap.remove(oldestKey)
                isFromBundleMap.remove(oldestKey)
                println("DEBUG: Removed oldest ${type.displayName} cache entry: $oldestKey (FIFO cleanup)")
            }
            
            val source = if (fromBundle) "Bundle 📦" else "GitHub"
            println("DEBUG: Cached ${type.displayName}: $composeVersion → $version from $source (cache size: ${versionsMap.size}/$MAX_LIFECYCLE_CACHE_SIZE)")
        }
    }
    
    /**
     * Compare two Compose versions.
     * Returns true if version < threshold.
     * 
     * Examples:
     * - isComposeVersionLessThan("1.9.3", "1.10.0") → true
     * - isComposeVersionLessThan("1.10.0-beta01", "1.10.0") → true (beta < stable)
     * - isComposeVersionLessThan("1.10.0", "1.10.0") → false
     * - isComposeVersionLessThan("1.10.0-beta02+dev3234", "1.10.0") → true
     */
    private fun isComposeVersionLessThan(version: String, threshold: String): Boolean {
        val versionBase = version.split("+").first()
        val thresholdBase = threshold.split("+").first()
        
        val versionNumeric = versionBase.split("-").first()
        val versionQualifier = versionBase.substringAfter("-", "")
        
        val thresholdNumeric = thresholdBase.split("-").first()
        val thresholdQualifier = thresholdBase.substringAfter("-", "")
        
        val versionParts = versionNumeric.split(".").map { it.toIntOrNull() ?: 0 }
        val thresholdParts = thresholdNumeric.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(versionParts.size, thresholdParts.size)) {
            val v = versionParts.getOrNull(i) ?: 0
            val t = thresholdParts.getOrNull(i) ?: 0
            if (v < t) return true
            if (v > t) return false
        }
        
        if (thresholdQualifier.isNotEmpty() && versionQualifier.isEmpty()) {
            return false
        }
        
        if (versionQualifier.isNotEmpty() && thresholdQualifier.isEmpty()) {
            return true
        }
        
        return versionQualifier < thresholdQualifier
    }
    
    override fun dispose() {
        logger.info("Disposing ComposeVersionCache, cancelling all background tasks")
        scope.cancel()
    }
}

