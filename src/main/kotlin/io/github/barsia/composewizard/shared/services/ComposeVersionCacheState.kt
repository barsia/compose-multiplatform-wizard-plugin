package io.github.barsia.composewizard.shared.services

import io.github.barsia.composewizard.shared.LibraryType

data class ComposeVersionCacheState(
    var stableVersions: List<String>? = emptyList(),
    var stableLastLoadTime: Long = 0L,
    var devVersions: List<String>? = emptyList(),
    var devLastLoadTime: Long = 0L,
    
    var libraryAvailableVersions: MutableMap<String, List<String>>? = mutableMapOf(),
    var libraryAvailableLastLoadTime: MutableMap<String, Long>? = mutableMapOf(),
    
    var libraryVersions: MutableMap<String, LinkedHashMap<String, String>>? = mutableMapOf(),
    var libraryIsFromBundle: MutableMap<String, LinkedHashMap<String, Boolean>>? = mutableMapOf(),
    
    var hotReloadGithubVersions: LinkedHashMap<String, String>? = linkedMapOf()
) {
    fun getAvailableVersions(type: LibraryType): List<String> {
        return libraryAvailableVersions?.get(type.name) ?: emptyList()
    }
    
    fun setAvailableVersions(type: LibraryType, versions: List<String>) {
        if (libraryAvailableVersions == null) {
            libraryAvailableVersions = mutableMapOf()
        }
        libraryAvailableVersions!![type.name] = versions
    }
    
    fun getAvailableLastLoadTime(type: LibraryType): Long {
        return libraryAvailableLastLoadTime?.get(type.name) ?: 0L
    }
    
    fun setAvailableLastLoadTime(type: LibraryType, time: Long) {
        if (libraryAvailableLastLoadTime == null) {
            libraryAvailableLastLoadTime = mutableMapOf()
        }
        libraryAvailableLastLoadTime!![type.name] = time
    }
    
    fun getLibraryVersions(type: LibraryType): LinkedHashMap<String, String> {
        if (libraryVersions == null) {
            libraryVersions = mutableMapOf()
        }
        return libraryVersions!!.getOrPut(type.name) { linkedMapOf() }
    }
    
    fun getLibraryIsFromBundle(type: LibraryType): LinkedHashMap<String, Boolean> {
        if (libraryIsFromBundle == null) {
            libraryIsFromBundle = mutableMapOf()
        }
        return libraryIsFromBundle!!.getOrPut(type.name) { linkedMapOf() }
    }
}
