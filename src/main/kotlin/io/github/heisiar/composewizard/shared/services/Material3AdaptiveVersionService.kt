package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Service for fetching Material3 Adaptive library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/
 */
class Material3AdaptiveVersionService {
    
    private val logger = Logger.getInstance(Material3AdaptiveVersionService::class.java)
    
    companion object {
        private const val MAVEN_URL = "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/"
        private const val TIMEOUT_MS = 5000
    }
    
    /**
     * Fetch all available Material3 Adaptive versions from Maven Central.
     * 
     * Returns raw list from Maven (unsorted).
     * Filtering and sorting should be done by caller.
     */
    suspend fun fetchMaterial3AdaptiveVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val metadataUrl = "${MAVEN_URL}maven-metadata.xml"
            
            val connection = java.net.URI(metadataUrl).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "IntelliJ-Compose-Wizard")
            
            if (connection.responseCode == 200) {
                connection.inputStream.use { input ->
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(input)
                    doc.documentElement.normalize()
                    
                    val versionList = doc.getElementsByTagName("version")
                    val versions = mutableListOf<String>()
                    
                    for (i in 0 until versionList.length) {
                        val version = versionList.item(i).textContent
                        versions.add(version)
                    }
                    
                    versions
                }
            } else {
                logger.warn("Failed to fetch Material3 Adaptive versions: HTTP ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch Material3 Adaptive versions: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Filter and sort versions for display in dropdown.
     * 
     * Rules:
     * - Only stable versions (no alpha/beta/rc/dev)
     * - Only versions < currentVersion (semantically)
     * - Limited to maxCount versions
     * - Sorted descending (newest first)
     * 
     * @param allVersions All available versions from Maven
     * @param currentVersion Current automatically detected version (will be first in list)
     * @param maxCount Maximum number of versions to return (default: 5)
     * @return Filtered and sorted list with currentVersion first
     */
    fun filterVersionsForDropdown(
        allVersions: List<String>,
        currentVersion: String,
        maxCount: Int = 5
    ): List<String> {
        val currentParsed = ComposeVersionComparator.parse(currentVersion)
        
        // Filter out only +dev versions from Maven
        val publishedVersions = allVersions.filter { version ->
            !version.contains("+dev", ignoreCase = true)
        }
        
        // Try to find stable versions (no alpha/beta/rc) < currentVersion
        val stableVersions = publishedVersions.filter { version ->
            !version.contains("-alpha") && !version.contains("-beta") && !version.contains("-rc")
        }.filter { version ->
            val parsed = ComposeVersionComparator.parse(version)
            parsed < currentParsed
        }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
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
        
        val rc = versions.filter { it.contains("-rc") }
            .maxWithOrNull { a, b -> ComposeVersionComparator.parse(a).compareTo(ComposeVersionComparator.parse(b)) }
        if (rc != null) return rc
        
        val beta = versions.filter { it.contains("-beta") }
            .maxWithOrNull { a, b -> ComposeVersionComparator.parse(a).compareTo(ComposeVersionComparator.parse(b)) }
        if (beta != null) return beta
        
        val alpha = versions.filter { it.contains("-alpha") }
            .maxWithOrNull { a, b -> ComposeVersionComparator.parse(a).compareTo(ComposeVersionComparator.parse(b)) }
        return alpha
    }
}

