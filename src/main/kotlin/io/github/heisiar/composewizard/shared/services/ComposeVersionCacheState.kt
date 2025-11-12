package io.github.heisiar.composewizard.shared.services

data class ComposeVersionCacheState(
    var stableVersions: List<String> = emptyList(),
    var stableLastLoadTime: Long = 0L,
    var devVersions: List<String> = emptyList(),
    var devLastLoadTime: Long = 0L,
    
    var lifecycleAvailableVersions: List<String> = emptyList(),
    var lifecycleAvailableLastLoadTime: Long = 0L,
    
    var material3AvailableVersions: List<String> = emptyList(),
    var material3AvailableLastLoadTime: Long = 0L,
    
    var material3AdaptiveAvailableVersions: List<String> = emptyList(),
    var material3AdaptiveAvailableLastLoadTime: Long = 0L,
    
    var navigationAvailableVersions: List<String> = emptyList(),
    var navigationAvailableLastLoadTime: Long = 0L,
    
    var navigation3AvailableVersions: List<String> = emptyList(),
    var navigation3AvailableLastLoadTime: Long = 0L,
    
    var windowAvailableVersions: List<String> = emptyList(),
    var windowAvailableLastLoadTime: Long = 0L,
    
    var savedStateAvailableVersions: List<String> = emptyList(),
    var savedStateAvailableLastLoadTime: Long = 0L,
    
    var navigationEventAvailableVersions: List<String> = emptyList(),
    var navigationEventAvailableLastLoadTime: Long = 0L,
    
    var hotReloadAvailableVersions: List<String> = emptyList(),
    var hotReloadAvailableLastLoadTime: Long = 0L,
    
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
