package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Service for fetching Compose Multiplatform versions from Maven repositories.
 * 
 * Fetches versions from:
 * - **Stable versions**: Maven Central (https://repo1.maven.org)
 * - **Dev versions**: JetBrains Space (https://maven.pkg.jetbrains.space)
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
        private const val DEV_MAVEN_URL = "https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/"
    }
    
    suspend fun fetchAvailableVersions(includeDevVersions: Boolean): List<String> = withContext(Dispatchers.IO) {
        try {
            val mavenUrl = if (includeDevVersions) DEV_MAVEN_URL else STABLE_MAVEN_URL
            fetchVersionsFromMaven(mavenUrl)
        } catch (e: Exception) {
            logger.warn("Failed to fetch Compose versions: ${e.message}")
            // For dev versions without internet: fallback to stable versions
            ComposeVersions.STABLE_VERSIONS
        }
    }
    
    private fun fetchVersionsFromMaven(mavenUrl: String): List<String> {
        try {
            val metadataUrl = "${mavenUrl}maven-metadata.xml"
            val connection = java.net.URI(metadataUrl).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 2000  // Reduced from 5000ms to 2000ms
            connection.readTimeout = 2000     // Reduced from 5000ms to 2000ms
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
                    
                    // No sorting here - sorting happens on read (getStableVersions/getDevVersions)
                    // Just take first 20 versions as-is from Maven
                    println("DEBUG ComposeVersionService: Versions from Maven (first 10): ${versions.take(10)}")
                    return versions.take(20)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch versions from $mavenUrl: ${e.message}")
        }
        
        return ComposeVersions.STABLE_VERSIONS
    }
}

