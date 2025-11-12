package io.github.heisiar.composewizard.shared.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class Navigation3VersionService {
    
    companion object {
        private const val MAVEN_METADATA_URL = "https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml"
    }
    
    suspend fun fetchNavigation3Versions(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val xml = URL(MAVEN_METADATA_URL).readText()
                val versionRegex = Regex("<version>([^<]+)</version>")
                versionRegex.findAll(xml)
                    .map { it.groupValues[1] }
                    .toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    fun filterVersionsForDropdown(
        allVersions: List<String>,
        currentVersion: String,
        maxCount: Int = 5
    ): List<String> {
        if (allVersions.isEmpty() || currentVersion.isEmpty()) {
            return listOf(currentVersion)
        }
        
        // Filter out only +dev versions from Maven
        val publishedVersions = allVersions.filter { version ->
            !version.contains("+dev", ignoreCase = true)
        }
        
        // Try to find stable versions (no alpha/beta/rc) < currentVersion
        val stableVersions = publishedVersions.filter { version ->
            !version.contains("-alpha") && !version.contains("-beta") && !version.contains("-rc")
        }.filter { version ->
            compareVersions(version, currentVersion) < 0
        }.sortedWith { a, b -> compareVersions(b, a) }
            .take(maxCount)
        
        if (stableVersions.isNotEmpty()) {
            return listOf(currentVersion) + stableVersions
        }
        
        // No stable versions found - find best unstable version
        val bestUnstable = findBestPublishedVersion(publishedVersions)
        
        // If best unstable is different from current, show both
        if (bestUnstable != null && bestUnstable != currentVersion) {
            return listOf(currentVersion, bestUnstable)
        }
        
        // Otherwise show only current version
        return listOf(currentVersion)
    }
    
    private fun findBestPublishedVersion(versions: List<String>): String? {
        if (versions.isEmpty()) return null
        
        val rc = versions.filter { it.contains("-rc") }.maxWithOrNull { a, b -> compareVersions(a, b) }
        if (rc != null) return rc
        
        val beta = versions.filter { it.contains("-beta") }.maxWithOrNull { a, b -> compareVersions(a, b) }
        if (beta != null) return beta
        
        val alpha = versions.filter { it.contains("-alpha") }.maxWithOrNull { a, b -> compareVersions(a, b) }
        return alpha
    }
    
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(Regex("[.\\-+]")).mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(Regex("[.\\-+]")).mapNotNull { it.toIntOrNull() }
        
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }
}

