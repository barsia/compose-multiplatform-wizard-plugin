package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for fetching library versions for Compose Multiplatform from GitHub.
 * 
 * Strategy:
 * - For release versions: Git Tag → Release Notes (optional) → Previous stable release (fallback)
 * - For dev versions: Git Tag → Previous stable release (fallback)
 * 
 * Cache is permanent since git tags are immutable.
 */
class ComposeLibraryVersionService {
    
    private val logger = Logger.getInstance(ComposeLibraryVersionService::class.java)
    
    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/JetBrains/compose-multiplatform"
    }
    
    private val cache = ConcurrentHashMap<String, Map<String, String>>()
    
    /**
     * Fetch library versions for a specific Compose Multiplatform version.
     * Returns a map of library identifiers to their versions.
     */
    suspend fun fetchLibraryVersions(composeVersion: String): Map<String, String> = withContext(Dispatchers.IO) {
        cache[composeVersion]?.let {
            logger.info("Using cached versions for $composeVersion")
            return@withContext it
        }
        
        try {
            val isDevVersion = composeVersion.contains("+dev")
            
            val versions = if (isDevVersion) {
                fetchVersionsForDevBuild(composeVersion)
            } else {
                fetchVersionsForRelease(composeVersion)
            }
            
            cache[composeVersion] = versions
            versions
        } catch (e: Exception) {
            logger.warn("Failed to fetch library versions for $composeVersion: ${e.message}")
            mapOf("compose-multiplatform" to composeVersion)
        }
    }
    
    private suspend fun fetchVersionsForRelease(composeVersion: String): Map<String, String> {
        val versions = mutableMapOf<String, String>()
        
        // 1. ALWAYS get tag (created immediately with release)
        try {
            val tagVersions = fetchFromTagMessage("v$composeVersion", composeVersion)
            versions.putAll(tagVersions)
            logger.info("Got ${tagVersions.size} versions from tag for $composeVersion")
        } catch (e: Exception) {
            logger.warn("Tag not found for $composeVersion: ${e.message}")
        }
        
        // 2. Supplement from Release Notes (if already published)
        try {
            val releaseVersions = fetchFromRelease("v$composeVersion", composeVersion)
            // Add only missing ones (tag has priority)
            releaseVersions.forEach { (key, value) ->
                if (key !in versions) {
                    versions[key] = value
                }
            }
            logger.info("Supplemented with Release Notes for $composeVersion")
        } catch (e: Exception) {
            logger.info("Release Notes not yet published for $composeVersion (normal for fresh releases)")
        }
        
        // 3. Fallback: supplement missing from previous stable release
        if (versions.size < 3) {
            logger.info("Insufficient data (${versions.size} libraries), fetching from previous stable release")
            val baseVersions = fetchFromPreviousStableRelease(composeVersion)
            return baseVersions + versions
        }
        
        return versions
    }
    
    private suspend fun fetchVersionsForDevBuild(composeVersion: String): Map<String, String> {
        val versions = mutableMapOf<String, String>()
        
        // 1. ALWAYS get tag (only source for dev)
        try {
            val tagVersions = fetchFromTagMessage("v$composeVersion", composeVersion)
            versions.putAll(tagVersions)
            logger.info("Got ${tagVersions.size} versions from dev tag for $composeVersion")
        } catch (e: Exception) {
            logger.warn("Dev tag not found for $composeVersion: ${e.message}")
            throw e
        }
        
        // 2. Supplement missing from previous stable release
        if (versions.size < 3) {
            logger.info("Dev build has only ${versions.size} libraries, fetching base from stable release")
            val baseVersion = extractBaseVersion(composeVersion)
            val baseVersions = fetchFromPreviousStableRelease(baseVersion)
            return baseVersions + versions
        }
        
        return versions
    }
    
    private suspend fun fetchFromPreviousStableRelease(targetVersion: String): Map<String, String> {
        val (major, minor) = parseVersion(targetVersion)
        
        val releasesUrl = "$GITHUB_API/releases?per_page=50"
        val connection = URL(releasesUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "IntelliJ-Compose-Wizard")
        
        if (connection.responseCode != 200) {
            return emptyMap()
        }
        
        val json = connection.inputStream.bufferedReader().use { it.readText() }
        
        val releaseRegex = """"tag_name"\s*:\s*"v([^"]+)"""".toRegex()
        val stableReleases = releaseRegex.findAll(json)
            .map { it.groupValues[1] }
            .filter { version ->
                !version.contains("-alpha") && 
                !version.contains("-beta") && 
                !version.contains("-rc") && 
                !version.contains("-dev") &&
                !version.contains("+")
            }
            .filter { candidateVersion ->
                val (candMajor, candMinor) = parseVersion(candidateVersion)
                when {
                    candMajor < major -> true
                    candMajor == major && candMinor <= minor -> true
                    else -> false
                }
            }
            .toList()
        
        val previousStable = stableReleases.firstOrNull()
        
        if (previousStable != null) {
            logger.info("Using $previousStable as base for $targetVersion")
            return try {
                fetchFromRelease("v$previousStable", previousStable)
            } catch (e: Exception) {
                logger.warn("Failed to fetch base from $previousStable")
                emptyMap()
            }
        }
        
        return emptyMap()
    }
    
    private fun extractBaseVersion(devVersion: String): String {
        return devVersion.substringBefore("+dev")
    }
    
    private fun parseVersion(version: String): Pair<Int, Int> {
        val parts = version.split(".", "-", "+")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return major to minor
    }
    
    private fun fetchFromRelease(tag: String, composeVersion: String): Map<String, String> {
        val url = "$GITHUB_API/releases/tags/$tag"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "IntelliJ-Compose-Wizard")
        
        if (connection.responseCode == 200) {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Simple regex parsing instead of JSON library
            val bodyMatch = """"body"\s*:\s*"([^"]*(?:\\"[^"]*)*)"""".toRegex().find(json)
            if (bodyMatch != null) {
                val body = bodyMatch.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\t", "\t")
                
                if (body.contains("Dependencies") || body.contains("org.jetbrains")) {
                    return parseVersionsFromReleaseNotes(body, composeVersion)
                }
            }
        }
        
        throw Exception("Release not found or has no body")
    }
    
    private fun fetchFromTagMessage(tag: String, composeVersion: String): Map<String, String> {
        val tagRefUrl = "$GITHUB_API/git/refs/tags/$tag"
        val connection = URL(tagRefUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "IntelliJ-Compose-Wizard")
        
        if (connection.responseCode == 200) {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Simple regex parsing for nested "object": { "url": "..." }
            val urlMatch = """"object"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""".toRegex().find(json)
            if (urlMatch != null) {
                val tagUrl = urlMatch.groupValues[1]
                
                val tagConnection = URL(tagUrl).openConnection() as HttpURLConnection
                tagConnection.connectTimeout = 5000
                tagConnection.readTimeout = 5000
                tagConnection.setRequestProperty("User-Agent", "IntelliJ-Compose-Wizard")
                
                if (tagConnection.responseCode == 200) {
                    val tagJson = tagConnection.inputStream.bufferedReader().use { it.readText() }
                    
                    // Parse message field
                    val messageMatch = """"message"\s*:\s*"([^"]*(?:\\"[^"]*)*)"""".toRegex().find(tagJson)
                    if (messageMatch != null) {
                        val message = messageMatch.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\t", "\t")
                        
                        return parseVersionsFromTagMessage(message, composeVersion)
                    }
                }
            }
        }
        
        throw Exception("Tag not found")
    }
    
    private fun parseVersionsFromReleaseNotes(body: String, composeVersion: String): Map<String, String> {
        val versions = mutableMapOf("compose-multiplatform" to composeVersion)
        
        val versionRegex = """org\.jetbrains\.([\w.]+):([\w-]+)\*?:([^\s`]+)""".toRegex()
        
        versionRegex.findAll(body).forEach { match ->
            val group = match.groupValues[1]
            val artifact = match.groupValues[2]
            val version = match.groupValues[3].trim('`', '.', ',', ')')
            
            val key = when {
                group.contains("lifecycle") -> "androidx-lifecycle"
                group.contains("navigation") && !group.contains("navigation3") -> "androidx-navigation"
                group.contains("savedstate") -> "androidx-savedstate"
                group.contains("window") -> "androidx-window"
                group == "compose.material3" -> {
                    when {
                        artifact.contains("adaptive") -> "compose-material3-adaptive"
                        else -> "compose-material3"
                    }
                }
                else -> null
            }
            
            if (key != null && version.isNotBlank()) {
                versions[key] = version
            }
        }
        
        return versions
    }
    
    private fun parseVersionsFromTagMessage(message: String, composeVersion: String): Map<String, String> {
        val versions = mutableMapOf("compose-multiplatform" to composeVersion)
        
        val versionRegex = """org\.jetbrains\.([\w.]+):([\w-]+)\*?:(\S+)""".toRegex()
        
        versionRegex.findAll(message).forEach { match ->
            val group = match.groupValues[1]
            val artifact = match.groupValues[2]
            val version = match.groupValues[3]
            
            val key = when {
                group.contains("lifecycle") -> "androidx-lifecycle"
                group.contains("navigation3") -> "androidx-navigation3"
                group.contains("navigationevent") -> "androidx-navigationevent"
                group.contains("navigation") -> "androidx-navigation"
                group.contains("savedstate") -> "androidx-savedstate"
                group.contains("window") -> "androidx-window"
                group == "compose.material3" -> {
                    when {
                        artifact.contains("adaptive") -> "compose-material3-adaptive"
                        else -> "compose-material3"
                    }
                }
                else -> null
            }
            
            if (key != null && version.isNotBlank()) {
                versions[key] = version
            }
        }
        
        return versions
    }
    
    fun clearCache() {
        cache.clear()
    }
}

