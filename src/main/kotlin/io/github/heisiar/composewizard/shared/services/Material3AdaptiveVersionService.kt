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
     * - Start with currentVersion (found from GitHub)
     * - Add bundledVersion if different from current
     * - Only stable versions (no alpha/beta/rc/dev), only < currentVersion
     * - If < 2 stable versions, add unstable versions
     * - Limited to maxCount + 2 versions total
     * - Sorted descending (newest first)
     * 
     * @param allVersions All available versions from Maven
     * @param currentVersion Current automatically detected version (will be first in list)
     * @param bundledVersion Version from LIBRARY_BUNDLES for current Compose version (optional)
     * @param maxCount Maximum number of versions to return (default: 5)
     * @return Filtered and sorted list with currentVersion first
     */
    fun filterVersionsForDropdown(
        allVersions: List<String>,
        currentVersion: String,
        bundledVersion: String? = null,
        maxCount: Int = 5
    ): List<String> {
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
}

