package io.github.barsia.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.barsia.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Base class for library version services.
 * Provides common filtering logic for dropdown versions and Maven fetching.
 */
open class LibraryVersionService {
    
    private val logger = Logger.getInstance(LibraryVersionService::class.java)
    
    companion object {
        private const val TIMEOUT_MS = 5000
    }
    
    /**
     * Fetch all available versions from Maven Central for given library.
     * 
     * @param mavenMetadataUrl Full URL to maven-metadata.xml
     * @return Raw list from Maven (unsorted). Filtering and sorting should be done by caller.
     */
    suspend fun fetchVersions(mavenMetadataUrl: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URI(mavenMetadataUrl).toURL().openConnection() as HttpURLConnection
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
                        if (version.isNotBlank()) {
                            versions.add(version)
                        }
                    }
                    
                    versions
                }
            } else {
                logger.warn("Failed to fetch versions from $mavenMetadataUrl: HTTP ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch versions from $mavenMetadataUrl: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Filter and sort versions for display in dropdown.
     * 
     * Rules:
     * - Start with currentVersion (found from GitHub or fallback)
     * - Add bundledVersion if different from current
     * - Show all stable versions (no alpha/beta/rc/dev) from Maven
     * - If < 2 stable versions, add unstable versions
     * - Limited to maxCount + 2 versions total
     * - Sorted descending (newest first)
     * 
     * @param allVersions All available versions from Maven
     * @param originalVersion Current automatically detected version (will be first in list)
     * @param bundledVersion Version from LIBRARY_BUNDLES for current Compose version (optional)
     * @param maxItems Maximum number of versions to return (default: 5)
     * @return Filtered and sorted list with currentVersion first
     */
    open fun filterVersionsForDropdown(
        allVersions: List<String>,
        originalVersion: String,
        bundledVersion: String?,
        maxItems: Int
    ): List<String> {
        val result = mutableListOf<String>()
        
        val publishedVersions = allVersions.filter { version ->
            !version.contains("+dev", ignoreCase = true)
        }
        
        val currentParsed = ComposeVersionComparator.parse(originalVersion)
        val isCurrentStable = !originalVersion.contains("-alpha") && !originalVersion.contains("-beta") && 
                             !originalVersion.contains("-rc") && !originalVersion.contains("-dev")
        
        val allStableVersions = publishedVersions.filter { version ->
            !version.contains("-alpha") && !version.contains("-beta") && !version.contains("-rc") && !version.contains("-dev")
        }.filter { version ->
            version != originalVersion && version != bundledVersion
        }
        
        val newerStableVersions = allStableVersions.filter { version ->
            val parsed = ComposeVersionComparator.parse(version)
            parsed > currentParsed
        }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        
        val newerUnstableVersions = if (!isCurrentStable) {
            publishedVersions.filter { version ->
                version.contains("-alpha") || version.contains("-beta") || version.contains("-rc")
            }.filter { version ->
                version != originalVersion && version != bundledVersion
            }.filter { version ->
                val parsed = ComposeVersionComparator.parse(version)
                parsed > currentParsed
            }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        } else {
            emptyList()
        }
        
        val olderStableVersions = allStableVersions.filter { version ->
            val parsed = ComposeVersionComparator.parse(version)
            parsed < currentParsed
        }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
        
        val totalStableCount = allStableVersions.size + 1
        
        if (totalStableCount < 2) {
            result.addAll(newerUnstableVersions)
            result.add(originalVersion)
            
            if (bundledVersion != null && bundledVersion != originalVersion) {
                result.add(bundledVersion)
            }
            
            result.addAll(olderStableVersions)
            
            val remainingSlots = maxItems - newerUnstableVersions.size - 1 - (if (bundledVersion != null && bundledVersion != originalVersion) 1 else 0) - olderStableVersions.size
            
            if (remainingSlots > 0) {
                val unstableVersions = publishedVersions.filter { version ->
                    version.contains("-alpha") || version.contains("-beta") || version.contains("-rc")
                }.filter { version ->
                    version != originalVersion && version != bundledVersion
                }.filter { version ->
                    val parsed = ComposeVersionComparator.parse(version)
                    parsed < currentParsed
                }.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
                
                result.addAll(unstableVersions.take(remainingSlots))
            }
        } else {
            result.addAll(newerStableVersions)
            result.addAll(newerUnstableVersions)
            result.add(originalVersion)
            
            if (bundledVersion != null && bundledVersion != originalVersion) {
                result.add(bundledVersion)
            }
            
            val minOlderCount = 2
            val currentTotal = newerStableVersions.size + newerUnstableVersions.size + 1 + (if (bundledVersion != null && bundledVersion != originalVersion) 1 else 0)
            
            if (currentTotal + minOlderCount <= maxItems) {
                val olderToTake = maxOf(minOlderCount, maxItems - currentTotal)
                result.addAll(olderStableVersions.take(olderToTake))
            } else {
                result.addAll(olderStableVersions.take(minOlderCount))
            }
        }
        
        return result.distinct()
    }
}
