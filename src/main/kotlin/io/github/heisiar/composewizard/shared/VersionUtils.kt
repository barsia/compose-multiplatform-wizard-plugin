package io.github.heisiar.composewizard.shared

/**
 * Utility for comparing semantic versions
 */
object VersionUtils {
    
    /**
     * Compare two semantic versions (e.g., "1.7.0", "1.10.0")
     * Returns:
     *  - negative if version1 < version2
     *  - zero if version1 == version2
     *  - positive if version1 > version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val parts1 = parseVersion(version1)
        val parts2 = parseVersion(version2)
        
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0
            
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        
        return 0
    }
    
    /**
     * Check if version1 is less than version2
     */
    fun isVersionLessThan(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) < 0
    }
    
    /**
     * Check if version1 is greater than or equal to version2
     */
    fun isVersionGreaterOrEqual(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) >= 0
    }
    
    /**
     * Parse version string into list of integers
     * Examples:
     *  - "1.7.0" -> [1, 7, 0]
     *  - "1.10.0-alpha01" -> [1, 10, 0]
     *  - "1.10.0-beta01" -> [1, 10, 0]
     *  - "1.10.0-rc01" -> [1, 10, 0]
     *  - "2.0.0-dev1234" -> [2, 0, 0]
     */
    private fun parseVersion(version: String): List<Int> {
        // Remove suffix like -alpha01, -beta01, -rc01, -dev1234
        val cleanVersion = version.split("-", "+").first()
        
        return cleanVersion.split(".")
            .mapNotNull { it.toIntOrNull() }
    }
    
    /**
     * Check if Compose Hot Reload should be added as a separate dependency
     * 
     * For Compose < 1.10.0: Hot Reload is a separate plugin
     * For Compose >= 1.10.0: Hot Reload is built-in, no need for separate dependency
     */
    fun needsHotReloadPlugin(composeVersion: String): Boolean {
        return isVersionLessThan(composeVersion, "1.10.0")
    }
}

