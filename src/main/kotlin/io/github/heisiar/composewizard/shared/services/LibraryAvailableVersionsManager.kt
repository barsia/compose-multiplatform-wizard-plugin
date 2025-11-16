package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.LibraryType

class LibraryAvailableVersionsManager(
    private val state: ComposeVersionCacheState,
    private val loader: LibraryAvailableVersionsLoader
) {
    
    private val logger = Logger.getInstance(LibraryAvailableVersionsManager::class.java)
    
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }
    
    fun initializeAvailableVersions() {
        for (type in LibraryRegistry.getAllTypes()) {
            if (isCacheExpired(type)) {
                loader.loadLibraryVersions(type)
            }
        }
    }
    
    fun getLibraryVersions(type: LibraryType): List<String> {
        if (isCacheExpired(type) && !loader.isLoading(type)) {
            loader.loadLibraryVersions(type)
        }
        return getVersionsFromState(type)
    }
    
    fun invalidateCache(type: LibraryType) {
        logger.info("${type.displayName} available cache manually invalidated")
        setLastLoadTime(type, 0L)
    }
    
    private fun isCacheExpired(type: LibraryType): Boolean {
        val lastLoadTime = getLastLoadTime(type)
        val versions = getVersionsFromState(type)
        
        if (lastLoadTime == 0L) return true
        if (versions.isEmpty()) return true
        
        val age = System.currentTimeMillis() - lastLoadTime
        return age > CACHE_TTL_MS
    }
    
    private fun getVersionsFromState(type: LibraryType): List<String> {
        return state.getAvailableVersions(type)
    }
    
    private fun getLastLoadTime(type: LibraryType): Long {
        return state.getAvailableLastLoadTime(type)
    }
    
    private fun setLastLoadTime(type: LibraryType, time: Long) {
        state.setAvailableLastLoadTime(type, time)
    }
}

