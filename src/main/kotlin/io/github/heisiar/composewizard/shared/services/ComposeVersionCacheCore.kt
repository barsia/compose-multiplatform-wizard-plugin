package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ComposeVersionCacheCore(
    private val state: ComposeVersionCacheState,
    private val scope: CoroutineScope,
    private val versionService: ComposeVersionService
) {
    
    private val logger = Logger.getInstance(ComposeVersionCacheCore::class.java)
    
    @Volatile
    private var isLoadingStable = false
    
    @Volatile
    private var isLoadingDev = false
    
    @Volatile
    private var initialized = false
    
    private val _cacheInvalidated = MutableSharedFlow<Unit>(replay = 0)
    val cacheInvalidated = _cacheInvalidated.asSharedFlow()
    
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val MAX_CACHED_VERSIONS = 10
    }
    
    fun initializeCache(onInitComplete: () -> Unit) {
        if (initialized) {
            return
        }
        
        initialized = true
        
        if (isStableCacheExpired()) {
            loadStableVersionsInBackground()
            loadDevVersionsInBackground()
        }
        
        onInitComplete()
    }
    
    fun getStableVersions(): List<String>? {
        if (!initialized) {
            return null
        }
        
        if (state.stableVersions.isNotEmpty() && !isStableCacheExpired()) {
            return state.stableVersions
        }
        
        if (!isLoadingStable) {
            logger.info("Stable cache missing or expired, triggering background load")
            loadStableVersionsInBackground()
        }
        
        if (isLoadingStable) {
            return null
        }
        
        val versions = state.stableVersions.ifEmpty { ComposeVersions.STABLE_VERSIONS_HARDCODED }
        val sorted = versions.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        return sorted
    }
    
    fun getDevVersions(): List<String>? {
        if (!initialized) {
            return null
        }
        
        if (isLoadingDev) {
            return null
        }
        
        if (!isDevCacheExpired()) {
            return state.devVersions
        }
        
        logger.info("Dev cache missing or expired, triggering background load")
        loadDevVersionsInBackground()
        return null
    }
    
    private fun isStableCacheExpired(): Boolean {
        if (state.stableLastLoadTime == 0L || state.stableVersions.isEmpty()) return true
        return (System.currentTimeMillis() - state.stableLastLoadTime) > CACHE_TTL_MS
    }
    
    private fun isDevCacheExpired(): Boolean {
        if (state.devLastLoadTime == 0L) return true
        return (System.currentTimeMillis() - state.devLastLoadTime) > CACHE_TTL_MS
    }
    
    suspend fun getStableVersionsSuspend(timeoutMs: Long = 3000): List<String> = withContext(Dispatchers.IO) {
        if (!isLoadingStable) {
            return@withContext if (state.stableVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else state.stableVersions
        }
        
        withTimeoutOrNull(timeoutMs) {
            while (isLoadingStable) {
                delay(100)
            }
        }
        
        if (state.stableVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else state.stableVersions
    }
    
    fun getStableVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        return runBlocking {
            getStableVersionsSuspend(timeoutMs)
        }
    }
    
    suspend fun getDevVersionsSuspend(timeoutMs: Long = 3000): List<String> = withContext(Dispatchers.IO) {
        if (!isLoadingDev) {
            return@withContext if (state.devVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else state.devVersions
        }
        
        withTimeoutOrNull(timeoutMs) {
            while (isLoadingDev) {
                delay(100)
            }
        }
        
        if (state.devVersions.isEmpty()) ComposeVersions.STABLE_VERSIONS_HARDCODED else state.devVersions
    }
    
    fun getDevVersionsBlocking(timeoutMs: Long = 3000): List<String> {
        return runBlocking {
            getDevVersionsSuspend(timeoutMs)
        }
    }
    
    fun isLoadingStableVersions(): Boolean {
        return isLoadingStable
    }
    
    fun isLoadingDevVersions(): Boolean {
        return isLoadingDev
    }
    
    fun isUsingFallbackVersions(): Boolean {
        if (state.stableLastLoadTime == 0L) {
            return true
        }
        
        if (state.stableVersions.isEmpty()) {
            return true
        }
        
        val hardcoded = ComposeVersions.STABLE_VERSIONS_HARDCODED.toSet()
        val cached = state.stableVersions.toSet()
        return cached.all { it in hardcoded }
    }
    
    fun isUsingDevFallbackVersions(): Boolean {
        if (state.devLastLoadTime == 0L) {
            return true
        }
        
        if (state.devVersions.isEmpty()) {
            return true
        }
        
        return false
    }
    
    fun invalidateStableCache() {
        logger.info("Stable cache manually invalidated, will refresh on next access")
        state.stableLastLoadTime = 0L
    }
    
    fun invalidateDevCache() {
        logger.info("Dev cache manually invalidated, will refresh on next access")
        state.devLastLoadTime = 0L
    }
    
    fun forceReloadStable(onInvalidate: () -> Unit) {
        logger.info("Force reload stable requested, invalidating cache and reloading")
        state.stableLastLoadTime = 0L
        onInvalidate()
        loadStableVersionsInBackground()
    }
    
    fun forceReloadDev(onInvalidate: () -> Unit) {
        logger.info("Force reload dev requested, invalidating cache and reloading")
        state.devLastLoadTime = 0L
        onInvalidate()
        loadDevVersionsInBackground()
    }
    
    suspend fun notifyCacheInvalidated() {
        _cacheInvalidated.emit(Unit)
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
                
                state.stableVersions = versionsFromMaven
                state.stableLastLoadTime = System.currentTimeMillis()
                
                logger.info("Cached ${versionsFromMaven.size} stable Compose versions from Maven")
            } catch (e: CancellationException) {
                logger.info("Stable version loading cancelled due to plugin unload")
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to load stable Compose versions from Maven, using hardcoded fallback: ${e.message}")
                if (state.stableVersions.isEmpty()) {
                    // Fallback: use LIBRARY_BUNDLES as the only source when Maven is unreachable
                    state.stableVersions = ComposeVersions.STABLE_VERSIONS_HARDCODED
                    state.stableLastLoadTime = System.currentTimeMillis()
                }
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
                
                state.devVersions = versionsFromMaven
                state.devLastLoadTime = System.currentTimeMillis()
                
                logger.info("Cached ${versionsFromMaven.size} dev Compose versions from Maven")
            } catch (e: CancellationException) {
                logger.info("Dev version loading cancelled due to plugin unload")
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to load dev Compose versions: ${e.message}")
                state.devVersions = emptyList()
                // Dev режим: без fallback, UI покажет "Dev versions unavailable"
            } finally {
                isLoadingDev = false
            }
        }
    }
}

