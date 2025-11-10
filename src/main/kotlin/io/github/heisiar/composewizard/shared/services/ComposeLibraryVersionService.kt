package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.LibraryType
import java.net.HttpURLConnection

/**
 * Service for fetching library versions from GitHub Web UI.
 * 
 * Uses HTML scraping from compose-multiplatform-core release pages
 * because this repository doesn't provide GitHub Releases API access.
 */
class ComposeLibraryVersionService {
    
    private val logger = Logger.getInstance(ComposeLibraryVersionService::class.java)
    
    companion object {
        // compose-multiplatform-core Web UI URL for tags
        private const val CORE_TAG_WEB_URL = "https://github.com/JetBrains/compose-multiplatform-core/releases/tag"
        private const val TIMEOUT_MS = 5000
        private const val MAX_FALLBACK_VERSIONS = 20  // Limit fallback depth
        
        // Regex patterns for all library types
        // Captures full version including qualifiers (alpha, beta, rc) and dev suffix
        private val LIFECYCLE_PATTERN = Regex("""lifecycle-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
        private val MATERIAL3_PATTERN = Regex("""material3\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
        private val MATERIAL3_ADAPTIVE_PATTERN = Regex("""adaptive-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
        private val NAVIGATION_PATTERN = Regex("""navigation-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
        private val NAVIGATION_EVENT_PATTERN = Regex("""navigationevent-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
        private val SAVED_STATE_PATTERN = Regex("""savedstate\*?:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
        private val WINDOW_PATTERN = Regex("""window-core:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    }
    
    data class FetchResult(
        val lifecycle: String? = null,
        val isRateLimited: Boolean = false
    )
    
    data class LibraryVersionsResult(
        val versions: Map<LibraryType, String> = emptyMap(),
        val isRateLimited: Boolean = false
    )
    
    /**
     * Fetch lifecycle version from GitHub Web UI for given Compose version.
     * Returns null if not found or error occurred.
     */
    fun fetchLifecycleFromWebUI(composeVersion: String): String? {
        return fetchLifecycleFromWebUIWithStatus(composeVersion).lifecycle
    }
    
    /**
     * Fetch lifecycle version from GitHub Web UI with rate limit status.
     * Returns FetchResult with lifecycle (if found) and rate limit flag.
     */
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
                return FetchResult(lifecycle = null, isRateLimited = true)
            }
            
            if (responseCode != 200) {
                println("DEBUG: ❌ Not found or error (code: $responseCode)")
                return FetchResult(lifecycle = null, isRateLimited = false)
            }
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Parse lifecycle version from HTML
            val match = LIFECYCLE_PATTERN.find(html)
            val lifecycleVersion = match?.groups?.get(1)?.value
            
            if (lifecycleVersion != null) {
                println("DEBUG: ✅ Found lifecycle: $lifecycleVersion")
            } else {
                println("DEBUG: ❌ Lifecycle pattern not found in HTML")
            }
            
            FetchResult(lifecycle = lifecycleVersion, isRateLimited = false)
        } catch (e: Exception) {
            logger.info("Failed to fetch lifecycle from Web UI for $composeVersion: ${e.message}")
            FetchResult(lifecycle = null, isRateLimited = false)
        }
    }
    
    /**
     * Fetch all library versions from GitHub Web UI for given Compose version.
     * Returns LibraryVersionsResult with versions map and rate limit status.
     */
    fun fetchLibraryVersionsFromWebUI(composeVersion: String): LibraryVersionsResult {
        return try {
            val encodedVersion = java.net.URLEncoder.encode(composeVersion, "UTF-8")
            val url = "$CORE_TAG_WEB_URL/v$encodedVersion"
            
            println("DEBUG: 🌐 Fetching library versions from GitHub Web UI: $composeVersion")
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            val responseCode = connection.responseCode
            
            if (responseCode == 403 || responseCode == 429) {
                println("DEBUG: ⚠️ Rate limited on GitHub Web UI (code: $responseCode)")
                return LibraryVersionsResult(versions = emptyMap(), isRateLimited = true)
            }
            
            if (responseCode != 200) {
                println("DEBUG: ❌ Not found or error (code: $responseCode)")
                return LibraryVersionsResult(versions = emptyMap(), isRateLimited = false)
            }
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Parse all library versions from HTML
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
            
            println("DEBUG: Parsed ${versions.size} library versions from GitHub")
            
            LibraryVersionsResult(versions = versions, isRateLimited = false)
        } catch (e: Exception) {
            logger.info("Failed to fetch library versions from Web UI for $composeVersion: ${e.message}")
            LibraryVersionsResult(versions = emptyMap(), isRateLimited = false)
        }
    }
    
    /**
     * Generate fallback versions for given base Compose version.
     * 
     * For "1.10.0-beta02" generates:
     * - 1.10.0-beta01
     * - 1.10.0-alpha08, alpha07, ..., alpha01
     * - 1.9.3, 1.9.2, 1.9.1, ...
     * - 1.8.0, 1.7.1, ...
     * 
     * For "1.9.0-rc02" generates:
     * - 1.9.0-rc01
     * - 1.9.0-beta08, beta07, ..., beta01
     * - 1.9.0-alpha08, alpha07, ..., alpha01
     * - 1.8.3, 1.8.2, ...
     */
    fun generateFallbackVersions(baseVersion: String): List<String> {
        val versions = mutableListOf<String>()
        
        // Start with known Bundle versions (fast checks first!), excluding dev versions
        versions.addAll(ComposeVersions.LIBRARY_BUNDLES.keys.filter { !it.contains("+dev") })
        
        val parts = baseVersion.split(".")
        
        if (parts.size < 3) return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        
        val major = parts[0].toIntOrNull() ?: return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        val minor = parts[1].toIntOrNull() ?: return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        val patchWithQualifier = parts[2]
        
        // Extract patch number and qualifier (e.g., "0-beta02" → patch=0, qualifier="beta02")
        val patchParts = patchWithQualifier.split("-")
        val patch = patchParts[0].toIntOrNull() ?: return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        val qualifier = if (patchParts.size > 1) patchParts[1] else ""
        
        // If has qualifier (beta/alpha), add decreasing qualifiers for same patch
        if (qualifier.isNotEmpty()) {
            val (qualifierType, qualifierNum) = parseQualifier(qualifier)
            
            if (qualifierType.isNotEmpty() && qualifierNum > 0) {
                // Add previous qualifiers of same type (beta02 → beta01, alpha05 → alpha04, ...)
                for (i in (qualifierNum - 1) downTo 1) {
                    versions.add("$major.$minor.$patch-$qualifierType${i.toString().padStart(2, '0')}")
                }
                
                // If rc, add betas and alphas for same patch
                if (qualifierType == "rc") {
                    for (i in 3 downTo 1) {
                        versions.add("$major.$minor.$patch-beta${i.toString().padStart(2, '0')}")
                    }
                    for (i in 3 downTo 1) {
                        versions.add("$major.$minor.$patch-alpha${i.toString().padStart(2, '0')}")
                    }
                }
                
                // If beta, add alphas for same patch
                if (qualifierType == "beta") {
                    for (i in 3 downTo 1) {
                        versions.add("$major.$minor.$patch-alpha${i.toString().padStart(2, '0')}")
                    }
                }
            }
        }
        
        // Add ONLY previous patch versions (don't generate "future" versions!)
        if (patch > 0) {
            for (p in (patch - 1) downTo maxOf(0, patch - 2)) {  // Max 2 previous patches
                // Add stable version first
                versions.add("$major.$minor.$p")
                // Add ONLY PREVIOUS qualifiers (don't generate beta08 if we're in beta02!)
                for (q in listOf("rc", "beta", "alpha")) {
                    for (num in 3 downTo 1) {  // Max 3 of each qualifier type
                        versions.add("$major.$minor.$p-$q${num.toString().padStart(2, '0')}")
                    }
                }
            }
        }
        
        // Add previous minor versions
        for (m in (minor - 1) downTo 0) {
            // Add few most recent patch versions for each minor
            for (p in 3 downTo 0) {
                versions.add("$major.$m.$p")
            }
        }
        
        return versions.distinct().take(MAX_FALLBACK_VERSIONS)
    }
    
    private fun parseQualifier(qualifier: String): Pair<String, Int> {
        val match = Regex("""(beta|alpha|rc)(\d+)""").find(qualifier)
        return if (match != null) {
            val type = match.groupValues[1]
            val num = match.groupValues[2].toIntOrNull() ?: 0
            type to num
        } else {
            "" to 0
        }
    }
}
