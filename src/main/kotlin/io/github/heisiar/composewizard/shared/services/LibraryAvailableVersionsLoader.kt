package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.LibraryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class LibraryAvailableVersionsLoader(
    private val state: ComposeVersionCacheState,
    private val scope: CoroutineScope,
    private val libraryVersionService: LibraryVersionService,
    private val onCacheInvalidated: suspend () -> Unit
) {
    
    private val logger = Logger.getInstance(LibraryAvailableVersionsLoader::class.java)
    
    private val loadingFlags = ConcurrentHashMap<LibraryType, Boolean>()
    
    fun isLoading(type: LibraryType): Boolean {
        return loadingFlags[type] == true
    }
    
    fun loadLibraryVersions(type: LibraryType) {
        synchronized(loadingFlags) {
            if (isLoading(type)) return
            loadingFlags[type] = true
        }
        
        scope.launch {
            try {
                val metadata = LibraryRegistry.getMetadata(type)
                val versions = libraryVersionService.fetchVersions(metadata.mavenMetadataUrl)
                
                setLibraryVersions(type, versions)
                setLibraryLastLoadTime(type, System.currentTimeMillis())
                
                onCacheInvalidated()
            } catch (e: Exception) {
                logger.warn("Failed to load ${type.displayName} available versions: ${e.message}")
                setLibraryVersions(type, emptyList())
            } finally {
                loadingFlags[type] = false
            }
        }
    }
    
    private fun setLibraryVersions(type: LibraryType, versions: List<String>) {
        state.setAvailableVersions(type, versions)
    }
    
    private fun setLibraryLastLoadTime(type: LibraryType, time: Long) {
        state.setAvailableLastLoadTime(type, time)
    }
}

