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
 * Uses different strategies for different version types:
 * - **Stable versions** (Maven Central): Semantic sorting by version number (1.10.0 > 1.9.1)
 * - **Dev versions** (JetBrains Space): Chronological order from XML (newest build first)
 */
class ComposeVersionService {
    
    private val logger = Logger.getInstance(ComposeVersionService::class.java)
    
    companion object {
        private const val STABLE_MAVEN_URL = "https://repo1.maven.org/maven2/org/jetbrains/compose/compose-gradle-plugin/"
        private const val DEV_MAVEN_URL = "https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/"
    }
    
    suspend fun fetchAvailableVersions(includeDevVersions: Boolean): List<String> = withContext(Dispatchers.IO) {
        try {
            val mavenUrl = if (includeDevVersions) DEV_MAVEN_URL else STABLE_MAVEN_URL
            fetchVersionsFromMaven(mavenUrl, sortSemantrically = !includeDevVersions)
        } catch (e: Exception) {
            logger.warn("Failed to fetch Compose versions: ${e.message}")
            // For dev versions without internet: fallback to stable versions
            ComposeVersions.STABLE_VERSIONS
        }
    }
    
    private fun fetchVersionsFromMaven(mavenUrl: String, sortSemantrically: Boolean): List<String> {
        try {
            val metadataUrl = "${mavenUrl}maven-metadata.xml"
            val connection = URL(metadataUrl).openConnection() as HttpURLConnection
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
                    
                    return if (sortSemantrically) {
                        // For stable versions: semantic sorting (newest by version number)
                        versions
                            .sortedWith(compareByDescending { parseVersion(it) })
                            .take(20)
                    } else {
                        // For dev versions: chronological order from XML (as-is, newest first)
                        versions.take(20)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch versions from $mavenUrl: ${e.message}")
        }
        
        return ComposeVersions.STABLE_VERSIONS
    }
    
    private fun parseVersion(version: String): Comparable<*> {
        // Parse version strings like "1.9.1", "1.9.0-alpha03", "1.9.0+dev2970", "1.10.0-beta01+dev3194"
        return try {
            // Split by '.', '-', or '+' to handle both stable and dev versions
            val parts = version.split(".", "-", "+")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            
            // Extract primary suffix (alpha, beta, rc, or dev)
            val suffixPart = parts.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: "zzz"
            val suffixName = suffixPart.takeWhile { !it.isDigit() }.ifEmpty { suffixPart }
            val suffixNum = suffixPart.filter { it.isDigit() }.toIntOrNull() ?: 0
            
            // Extract secondary dev suffix if present (for versions like "1.10.0-beta01+dev3194")
            val devPart = parts.getOrNull(4)?.takeIf { it.isNotEmpty() } ?: ""
            val devNum = if (devPart.startsWith("dev")) {
                devPart.filter { it.isDigit() }.toIntOrNull() ?: 0
            } else {
                0
            }
            
            VersionComparable(major, minor, patch, suffixName, suffixNum, devNum)
        } catch (e: Exception) {
            VersionComparable(0, 0, 0, version, 0, 0)
        }
    }
    
    private data class VersionComparable(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val suffix: String,
        val suffixNum: Int,
        val devNum: Int
    ) : Comparable<VersionComparable> {
        override fun compareTo(other: VersionComparable): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            if (patch != other.patch) return patch.compareTo(other.patch)
            
            // "zzz" means no suffix (stable), which is higher than any suffix
            // Lexicographic comparison: "zzz" > "rc" > "dev" > "beta" > "alpha"
            val suffixCompare = suffix.compareTo(other.suffix)
            if (suffixCompare != 0) return suffixCompare
            
            // For same suffix, compare by number (higher is newer)
            if (suffixNum != other.suffixNum) return suffixNum.compareTo(other.suffixNum)
            
            // For same suffix and number, compare by dev number (higher dev is newer)
            return devNum.compareTo(other.devNum)
        }
    }
}

