package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.LibraryType

class ComposeLibraryVersionService {
    
    data class FetchResult(
        val lifecycle: String? = null,
        val isRateLimited: Boolean = false,
        val pageExists: Boolean = false
    )
    
    data class LibraryVersionsResult(
        val versions: Map<LibraryType, String> = emptyMap(),
        val isRateLimited: Boolean = false,
        val pageExists: Boolean = false
    )
    
    fun fetchLifecycleFromWebUI(composeVersion: String): String? {
        return fetchLifecycleFromWebUIWithStatus(composeVersion).lifecycle
    }
    
    fun fetchLifecycleFromWebUIWithStatus(composeVersion: String): FetchResult {
        val result = ComposeLibraryVersionFetcher.fetchLifecycleFromWebUIWithStatus(composeVersion)
        return FetchResult(
            lifecycle = result.lifecycle,
            isRateLimited = result.isRateLimited,
            pageExists = result.pageExists
        )
    }
    
    fun fetchLibraryVersionsFromWebUI(composeVersion: String): LibraryVersionsResult {
        val result = ComposeLibraryVersionFetcher.fetchLibraryVersionsFromWebUI(composeVersion)
        return LibraryVersionsResult(
            versions = result.versions,
            isRateLimited = result.isRateLimited,
            pageExists = result.pageExists
        )
    }
    
    fun fetchHotReloadVersion(composeVersion: String): String? {
        return ComposeHotReloadFetcher.fetchHotReloadVersion(composeVersion)
    }
    
    fun hasReleasePage(composeVersion: String): Boolean {
        return ComposeHotReloadFetcher.hasReleasePage(composeVersion)
    }
}
