package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Service for fetching Compose Multiplatform versions from Maven repositories.
 * 
 * Fetches versions from:
 * - **Stable versions**: Maven Central (https://repo1.maven.org) - filters to versions newer than first in LIBRARY_BUNDLES
 * - **Dev versions**: JetBrains Space (https://maven.pkg.jetbrains.space) - no filtering
 * 
 * Returns versions as-is from XML (no sorting).
 * Sorting happens at display time in ComposeVersionCache.getStableVersions().
 */
class ComposeVersionService {
    
    private val logger = Logger.getInstance(ComposeVersionService::class.java)
    
    init {
        // Quick test of comparator
        val testVersions = listOf("1.9.0-rc02", "1.10.0-beta02", "1.9.3", "1.8.2")
        val sorted = testVersions.sortedWith(compareByDescending { io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator.parse(it) })
        println("DEBUG ComposeVersionService INIT TEST:")
        println("  Original: $testVersions")
        println("  Sorted:   $sorted")
        testVersions.forEach { v ->
            io.github.heisiar.composewizard.shared.utils.ComposeVersionComparator.parse(v, debug = true)
        }
    }
    
    companion object {
        private const val STABLE_MAVEN_URL = "https://repo1.maven.org/maven2/org/jetbrains/compose/compose-gradle-plugin/"
        private const val DEV_MAVEN_URL = "https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/compose-gradle-plugin/"
    }
    
    suspend fun fetchAvailableVersions(includeDevVersions: Boolean): List<String> = withContext(Dispatchers.IO) {
        try {
            val mavenUrl = if (includeDevVersions) DEV_MAVEN_URL else STABLE_MAVEN_URL
            val versions = fetchVersionsFromMaven(mavenUrl, includeDevVersions)
            
            // Filter dev versions to only those with tag pages on GitHub
            if (includeDevVersions) {
                filterDevVersionsWithReleasePage(versions)
            } else {
                versions
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch Compose versions: ${e.message}")
            // For dev versions without internet: fallback to hardcoded versions
            ComposeVersions.STABLE_VERSIONS_HARDCODED
        }
    }
    
    private suspend fun filterDevVersionsWithReleasePage(versions: List<String>): List<String> = kotlinx.coroutines.coroutineScope {
        val libraryService = ComposeLibraryVersionService()
        
        println("DEBUG ComposeVersionService: Filtering ${versions.size} dev versions for GitHub tag page existence (parallel checks)...")
        val startTime = System.currentTimeMillis()
        
        // Check all versions in parallel using coroutines
        val deferredResults = versions.map { version ->
            async(Dispatchers.IO) {
                val hasPage = libraryService.hasReleasePage(version)
                if (!hasPage) {
                    println("DEBUG ComposeVersionService: ❌ Excluding $version (no tag page)")
                }
                version to hasPage
            }
        }
        
        val results = deferredResults.awaitAll()
        val filtered = results.filter { it.second }.map { it.first }
        val elapsed = System.currentTimeMillis() - startTime
        
        println("DEBUG ComposeVersionService: Filtered to ${filtered.size} dev versions with tag pages (took ${elapsed}ms)")
        filtered
    }
    
    private fun fetchVersionsFromMaven(mavenUrl: String, isDevMode: Boolean): List<String> {
        try {
            val metadataUrl = "${mavenUrl}maven-metadata.xml"
            val connection = java.net.URI(metadataUrl).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.setRequestProperty("User-Agent", "IntelliJ-Compose-Wizard")
            
            if (connection.responseCode == 200) {
                connection.inputStream.use { input ->
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(input)
                    doc.documentElement.normalize()
                    
                    val versionNodes = doc.getElementsByTagName("version")
                    val versions = mutableListOf<String>()
                    
                    for (i in 0 until versionNodes.length) {
                        val version = versionNodes.item(i).textContent
                        if (version.isNotBlank()) {
                            versions.add(version)
                        }
                    }
                    
                    val result = if (isDevMode) {
                        // Dev: Take FIRST 20 from XML as-is (no sorting, no filtering!)
                        val first20 = versions.take(20)
                        println("DEBUG ComposeVersionService: Dev versions (first 20 from XML, no sorting): ${first20.take(10)}")
                        first20
                    } else {
                        // Stable: Filter versions >= last stable version in LIBRARY_BUNDLES
                        val minVersion = ComposeVersions.LAST_STABLE_VERSION
                        val minVersionParsed = ComposeVersionComparator.parse(minVersion)
                        
                        val filtered = versions.filter { version ->
                            val parsed = ComposeVersionComparator.parse(version)
                            parsed >= minVersionParsed
                        }
                        
                        println("DEBUG ComposeVersionService: Stable versions from Maven (filtered >= $minVersion): ${filtered.size} versions")
                        println("DEBUG ComposeVersionService: Filtered stable versions sample: ${filtered.take(5)}")
                        filtered
                    }
                    return result
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch versions from $mavenUrl: ${e.message}")
        }
        
        return ComposeVersions.STABLE_VERSIONS_HARDCODED
    }
}

