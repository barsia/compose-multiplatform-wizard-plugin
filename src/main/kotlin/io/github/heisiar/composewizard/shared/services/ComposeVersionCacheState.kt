package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.LibraryType

data class ComposeVersionCacheState(
    var stableVersions: List<String> = emptyList(),
    var stableLastLoadTime: Long = 0L,
    var devVersions: List<String> = emptyList(),
    var devLastLoadTime: Long = 0L,
    
    var libraryAvailableVersions: MutableMap<String, List<String>> = mutableMapOf(),
    var libraryAvailableLastLoadTime: MutableMap<String, Long> = mutableMapOf(),
    
    var libraryVersions: MutableMap<String, LinkedHashMap<String, String>> = mutableMapOf(),
    var libraryIsFromBundle: MutableMap<String, LinkedHashMap<String, Boolean>> = mutableMapOf(),
    
    var hotReloadGithubVersions: LinkedHashMap<String, String> = linkedMapOf()
) {
    fun getAvailableVersions(type: LibraryType): List<String> {
        return libraryAvailableVersions[type.name] ?: emptyList()
    }
    
    fun setAvailableVersions(type: LibraryType, versions: List<String>) {
        libraryAvailableVersions[type.name] = versions
    }
    
    fun getAvailableLastLoadTime(type: LibraryType): Long {
        return libraryAvailableLastLoadTime[type.name] ?: 0L
    }
    
    fun setAvailableLastLoadTime(type: LibraryType, time: Long) {
        libraryAvailableLastLoadTime[type.name] = time
    }
    
    fun getLibraryVersions(type: LibraryType): LinkedHashMap<String, String> {
        return libraryVersions.getOrPut(type.name) { linkedMapOf() }
    }
    
    fun getLibraryIsFromBundle(type: LibraryType): LinkedHashMap<String, Boolean> {
        return libraryIsFromBundle.getOrPut(type.name) { linkedMapOf() }
    }
}
