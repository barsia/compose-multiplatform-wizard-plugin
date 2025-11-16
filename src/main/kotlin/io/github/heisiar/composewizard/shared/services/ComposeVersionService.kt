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
            if (includeDevVersions) {
                emptyList()
            } else {
                ComposeVersions.STABLE_VERSIONS_HARDCODED
            }
        }
    }
    
    private suspend fun filterDevVersionsWithReleasePage(versions: List<String>): List<String> = kotlinx.coroutines.coroutineScope {
        val libraryService = ComposeLibraryVersionService()
        
        val startTime = System.currentTimeMillis()
        
        // Check all versions in parallel using coroutines
        val deferredResults = versions.map { version ->
            async(Dispatchers.IO) {
                val hasPage = libraryService.hasReleasePage(version)
                if (!hasPage) {
                }
                version to hasPage
            }
        }
        
        val results = deferredResults.awaitAll()
        val filtered = results.filter { it.second }.map { it.first }
        val elapsed = System.currentTimeMillis() - startTime
        
        filtered
    }
    
    private fun fetchVersionsFromMaven(mavenUrl: String, isDevMode: Boolean): List<String> {
        var lastException: Exception? = null
        
        repeat(5) { attempt ->
            try {
                val metadataUrl = "${mavenUrl}maven-metadata.xml"
                val connection = java.net.URI(metadataUrl).toURL().openConnection() as HttpURLConnection
                val timeout = if (attempt == 0) 3000 else 1500
                connection.connectTimeout = timeout
                connection.readTimeout = timeout
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
                            versions.take(20)
                        } else {
                            val minVersion = ComposeVersions.LAST_STABLE_VERSION
                            val minVersionParsed = ComposeVersionComparator.parse(minVersion)
                            
                            val sortedVersions = versions.sortedWith(compareByDescending { ComposeVersionComparator.parse(it) })
                            
                            val filtered = sortedVersions.filter { version ->
                                val parsed = ComposeVersionComparator.parse(version)
                                parsed >= minVersionParsed
                            }
                            
                            if (filtered.isEmpty()) {
                                emptyList()
                            } else {
                                val lastPublished = filtered.first()
                                val lastStable = filtered.firstOrNull { version ->
                                    !version.contains("-alpha") && !version.contains("-beta") && !version.contains("-rc")
                                }
                                
                                when {
                                    lastStable == null -> filtered
                                    lastPublished == lastStable -> filtered.take(6)
                                    else -> {
                                        val lastStableIndex = filtered.indexOf(lastStable)
                                        filtered.subList(0, lastStableIndex + 1)
                                    }
                                }
                            }
                        }
                        return result
                    }
                }
            } catch (e: Exception) {
                lastException = e
                logger.warn("Attempt ${attempt + 1}/5 failed to fetch versions from $mavenUrl: ${e.message}")
                
                if (attempt < 4) {
                    Thread.sleep(1000)
                }
            }
        }
        
        logger.warn("All 5 attempts failed to fetch versions from $mavenUrl. Last error: ${lastException?.message}")
        throw java.io.IOException("Failed to fetch versions from Maven after 5 attempts", lastException)
    }
}

