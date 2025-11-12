package io.github.heisiar.composewizard.shared.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class WindowVersionService {
    
    companion object {
        private const val MAVEN_METADATA_URL = "https://repo1.maven.org/maven2/org/jetbrains/androidx/window/window-core/maven-metadata.xml"
    }
    
    suspend fun fetchWindowVersions(): List<String> {
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
        
        // Filter to stable versions (no alpha, beta, rc, dev postfixes)
        val stableVersions = allVersions.filter { version ->
            !version.contains("-alpha", ignoreCase = true) &&
            !version.contains("-beta", ignoreCase = true) &&
            !version.contains("-rc", ignoreCase = true) &&
            !version.contains("-dev", ignoreCase = true) &&
            !version.contains("+dev", ignoreCase = true)
        }
        
        // Find versions less than current (compare as version strings)
        val lowerVersions = stableVersions.filter { version ->
            compareVersions(version, currentVersion) < 0
        }.sortedWith { a, b -> compareVersions(b, a) } // Sort descending
        
        // Take current version + maxCount lower versions
        return listOf(currentVersion) + lowerVersions.take(maxCount)
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

