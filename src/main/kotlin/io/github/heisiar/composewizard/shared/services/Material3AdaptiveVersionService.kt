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
        
        // Filter: stable versions only (no qualifiers), and < currentVersion
        val filtered = allVersions
            .filter { version ->
                // No dev suffix
                if (version.contains("+dev")) return@filter false
                
                // No alpha/beta/rc
                if (version.contains("-alpha") || version.contains("-beta") || version.contains("-rc")) {
                    return@filter false
                }
                
                // Must be < currentVersion semantically
                val parsed = ComposeVersionComparator.parse(version)
                parsed < currentParsed
            }
        
        // Sort descending and take top N
        val sorted = filtered.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        val limited = sorted.take(maxCount)
        
        
        // Return with currentVersion first
        return listOf(currentVersion) + limited
    }
}

