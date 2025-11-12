package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.LibraryType
import java.net.HttpURLConnection

object ComposeLibraryVersionFetcher {
    
    private val logger = Logger.getInstance(ComposeLibraryVersionFetcher::class.java)
    
    private const val CORE_TAG_WEB_URL = "https://github.com/JetBrains/compose-multiplatform-core/releases/tag"
    private const val TIMEOUT_MS = 5000
    
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
            
            println("DEBUG: 🌐 Fetching lifecycle from GitHub Web UI: $composeVersion")
            println("DEBUG: URL: $url")
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            val responseCode = connection.responseCode
            println("DEBUG: Response code: $responseCode")
            
            if (responseCode == 403 || responseCode == 429) {
                println("DEBUG: ⚠️ Rate limited on GitHub Web UI (code: $responseCode)")
                return FetchResult(lifecycle = null, isRateLimited = true, pageExists = false)
            }
            
            if (responseCode != 200) {
                println("DEBUG: ❌ Tag page does not exist (code: $responseCode)")
                return FetchResult(lifecycle = null, isRateLimited = false, pageExists = false)
            }
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            val match = LIFECYCLE_PATTERN.find(html)
            val lifecycleVersion = match?.groups?.get(1)?.value
            
            if (lifecycleVersion != null) {
                println("DEBUG: ✅ Found lifecycle: $lifecycleVersion (tag page exists)")
            } else {
                println("DEBUG: ⚠️ Tag page exists but lifecycle not published")
            }
            
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
            
            println("DEBUG: 🌐 Fetching library versions from GitHub Web UI: $composeVersion")
            println("DEBUG: 🔗 URL: $url")
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            val responseCode = connection.responseCode
            
            if (responseCode == 403 || responseCode == 429) {
                println("DEBUG: ⚠️ Rate limited on GitHub Web UI (code: $responseCode)")
                return LibraryVersionsResult(versions = emptyMap(), isRateLimited = true, pageExists = false)
            }
            
            if (responseCode != 200) {
                println("DEBUG: ❌ Tag page does not exist for $composeVersion (HTTP $responseCode)")
                return LibraryVersionsResult(versions = emptyMap(), isRateLimited = false, pageExists = false)
            }
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            val versions = mutableMapOf<LibraryType, String>()
            
            LIFECYCLE_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.LIFECYCLE] = it
                println("DEBUG: ✅ Found lifecycle: $it")
            }
            
            MATERIAL3_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.MATERIAL3] = it
                println("DEBUG: ✅ Found material3: $it")
            }
            
            MATERIAL3_ADAPTIVE_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.MATERIAL3_ADAPTIVE] = it
                println("DEBUG: ✅ Found material3-adaptive: $it")
            }
            
            NAVIGATION_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.NAVIGATION] = it
                println("DEBUG: ✅ Found navigation: $it")
            }
            
            NAVIGATION3_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.NAVIGATION3] = it
                println("DEBUG: ✅ Found navigation3: $it (from GitHub for $composeVersion)")
            } ?: run {
                println("DEBUG: ⚠️ Navigation3 NOT found in GitHub HTML for $composeVersion")
            }
            
            NAVIGATION_EVENT_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.NAVIGATION_EVENT] = it
                println("DEBUG: ✅ Found navigationEvent: $it")
            }
            
            SAVED_STATE_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.SAVED_STATE] = it
                println("DEBUG: ✅ Found savedState: $it")
            }
            
            WINDOW_PATTERN.find(html)?.groups?.get(1)?.value?.let {
                versions[LibraryType.WINDOW] = it
                println("DEBUG: ✅ Found window: $it")
            }
            
            println("DEBUG: Parsed ${versions.size} library versions from tag page (page exists)")
            
            LibraryVersionsResult(versions = versions, isRateLimited = false, pageExists = true)
        } catch (e: Exception) {
            logger.info("Failed to fetch library versions from Web UI for $composeVersion: ${e.message}")
            LibraryVersionsResult(versions = emptyMap(), isRateLimited = false, pageExists = false)
        }
    }
}

