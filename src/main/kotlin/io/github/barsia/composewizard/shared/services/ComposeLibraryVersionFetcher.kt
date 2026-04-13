package io.github.barsia.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.barsia.composewizard.shared.LibraryType
import java.net.HttpURLConnection

object ComposeLibraryVersionFetcher {
    
    private val logger = Logger.getInstance(ComposeLibraryVersionFetcher::class.java)
    
    private const val CORE_TAG_WEB_URL = "https://github.com/JetBrains/compose-multiplatform-core/releases/tag"
    
    private val LIFECYCLE_PATTERN = Regex("""lifecycle-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val MATERIAL3_PATTERN = Regex("""material3\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val MATERIAL3_ADAPTIVE_PATTERN = Regex("""adaptive-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val NAVIGATION_PATTERN = Regex("""navigation-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val NAVIGATION3_PATTERN = Regex("""navigation3-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val NAVIGATION_EVENT_PATTERN = Regex("""navigationevent-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val SAVED_STATE_PATTERN = Regex("""savedstate\*?:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    private val WINDOW_PATTERN = Regex("""window-core:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    
    data class FetchResult(
        val lifecycle: String? = null,
        val isRateLimited: Boolean = false,
        val pageExists: Boolean = false
    )
    
    data class LibraryVersionsResult(
        val versions: Map<LibraryType, String> = emptyMap(),
        val isRateLimited: Boolean = false,
        val pageExists: Boolean = false
    )
    
    fun fetchLifecycleFromWebUIWithStatus(composeVersion: String): FetchResult {
        return try {
            val encodedVersion = java.net.URLEncoder.encode(composeVersion, "UTF-8")
            val url = "$CORE_TAG_WEB_URL/v$encodedVersion"
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = NetworkConfig.NETWORK_TIMEOUT_MS
            connection.readTimeout = NetworkConfig.NETWORK_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            val responseCode = connection.responseCode
            
            if (responseCode == 403 || responseCode == 429) {
                return FetchResult(lifecycle = null, isRateLimited = true, pageExists = false)
            }
            
            if (responseCode != 200) {
                return FetchResult(lifecycle = null, isRateLimited = false, pageExists = false)
            }
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            val match = LIFECYCLE_PATTERN.find(html)
            val lifecycleVersion = match?.groups?.get(1)?.value
            
            FetchResult(lifecycle = lifecycleVersion, isRateLimited = false, pageExists = true)
        } catch (e: Exception) {
            logger.info("Failed to fetch lifecycle from Web UI for $composeVersion: ${e.message}")
            FetchResult(lifecycle = null, isRateLimited = false, pageExists = false)
        }
    }
    
    fun fetchLibraryVersionsFromWebUI(composeVersion: String): LibraryVersionsResult {
        return try {
            val encodedVersion = java.net.URLEncoder.encode(composeVersion, "UTF-8")
            val url = "$CORE_TAG_WEB_URL/v$encodedVersion"
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = NetworkConfig.NETWORK_TIMEOUT_MS
            connection.readTimeout = NetworkConfig.NETWORK_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            val responseCode = connection.responseCode
            
            if (responseCode == 403 || responseCode == 429) {
                return LibraryVersionsResult(versions = emptyMap(), isRateLimited = true, pageExists = false)
            }
            
            if (responseCode != 200) {
                return LibraryVersionsResult(versions = emptyMap(), isRateLimited = false, pageExists = false)
            }
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Log relevant parts of HTML for debugging
            val relevantLines = html.lines().filter { line ->
                line.contains("lifecycle", ignoreCase = true) ||
                line.contains("material3", ignoreCase = true) ||
                line.contains("adaptive", ignoreCase = true) ||
                line.contains("navigation", ignoreCase = true) ||
                line.contains("savedstate", ignoreCase = true) ||
                line.contains("window", ignoreCase = true)
            }.take(20)
            if (relevantLines.isNotEmpty()) {
                logger.info("Sample HTML lines for $composeVersion:\n${relevantLines.joinToString("\n")}")
            }
            
            val versions = mutableMapOf<LibraryType, String>()
            
            LIFECYCLE_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.LIFECYCLE] = it
            }
            
            MATERIAL3_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.MATERIAL3] = it
            }
            
            MATERIAL3_ADAPTIVE_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.MATERIAL3_ADAPTIVE] = it
            }
            
            NAVIGATION_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.NAVIGATION] = it
            }
            
            NAVIGATION3_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.NAVIGATION3] = it
            }
            
            NAVIGATION_EVENT_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.NAVIGATION_EVENT] = it
            }
            
            SAVED_STATE_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.SAVED_STATE] = it
            }
            
            WINDOW_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.WINDOW] = it
            }
            
            logger.info("Parsed versions for $composeVersion: ${versions.map { "${it.key.displayName}=${it.value}" }.joinToString()}")
            
            LibraryVersionsResult(versions = versions, isRateLimited = false, pageExists = true)
        } catch (e: Exception) {
            logger.info("Failed to fetch library versions from Web UI for $composeVersion: ${e.message}")
            LibraryVersionsResult(versions = emptyMap(), isRateLimited = false, pageExists = false)
        }
    }
}

