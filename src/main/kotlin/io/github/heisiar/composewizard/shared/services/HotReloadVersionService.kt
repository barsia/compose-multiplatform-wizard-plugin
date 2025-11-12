package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class HotReloadVersionService {
    
    companion object {
        private const val MAVEN_METADATA_URL = "https://repo1.maven.org/maven2/org/jetbrains/compose/hot-reload/org.jetbrains.compose.hot-reload.gradle.plugin/maven-metadata.xml"
    }
    
    suspend fun fetchHotReloadVersions(): List<String> {
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
        bundledVersion: String? = null,
        maxCount: Int = 5
    ): List<String> {
        if (allVersions.isEmpty() || currentVersion.isEmpty()) {
            return listOf(currentVersion)
        }
        
        val result = mutableListOf<String>()
        val currentParsed = ComposeVersionComparator.parse(currentVersion)
        
        result.add(currentVersion)
        
        if (bundledVersion != null && bundledVersion != currentVersion) {
            result.add(bundledVersion)
        }
        
        val publishedVersions = allVersions.filter { version ->
            !version.contains("+dev", ignoreCase = true)
        }
        
        val stableVersions = publishedVersions.filter { version ->
            !version.contains("-alpha") && !version.contains("-beta") && !version.contains("-rc")
        }.filter { version ->
            version != currentVersion && version != bundledVersion
        }.filter { version ->
            val parsed = ComposeVersionComparator.parse(version)
            parsed < currentParsed
        }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        
        if (stableVersions.size < 2) {
            val unstableVersions = publishedVersions.filter { version ->
                version.contains("-alpha") || version.contains("-beta") || version.contains("-rc")
            }.filter { version ->
                version != currentVersion && version != bundledVersion
            }.filter { version ->
                val parsed = ComposeVersionComparator.parse(version)
                parsed < currentParsed
            }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
            
            result.addAll(unstableVersions.take(maxCount))
        } else {
            result.addAll(stableVersions.take(maxCount))
        }
        
        return result.distinct().take(maxCount + 2)
    }
    
    /**
     * Check if there are enough stable versions to show a dropdown.
     * Returns true if there are at least 2 stable versions (current + at least 1 lower).
     */
    fun hasMultipleStableVersions(
        allVersions: List<String>,
        currentVersion: String
    ): Boolean {
        if (allVersions.isEmpty() || currentVersion.isEmpty()) {
            return false
        }
        
        val currentParsed = ComposeVersionComparator.parse(currentVersion)
        
        val stableVersions = allVersions.filter { version ->
            !version.contains("-alpha", ignoreCase = true) &&
            !version.contains("-beta", ignoreCase = true) &&
            !version.contains("-rc", ignoreCase = true) &&
            !version.contains("-dev", ignoreCase = true) &&
            !version.contains("+dev", ignoreCase = true)
        }
        
        val lowerVersions = stableVersions.filter { version ->
            val parsed = ComposeVersionComparator.parse(version)
            parsed < currentParsed
        }
        
        return lowerVersions.isNotEmpty()
    }
}

