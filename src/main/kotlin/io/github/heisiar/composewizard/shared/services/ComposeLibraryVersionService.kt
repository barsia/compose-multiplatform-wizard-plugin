package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.ComposeVersions
import java.net.HttpURLConnection

/**
 * Service for fetching Lifecycle versions from GitHub Web UI.
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
        
        // Captures full version including qualifiers (alpha, beta, rc) and dev suffix
        // Examples: 2.10.0-alpha04+dev3224, 2.9.5, 2.10.0-beta01
        private val LIFECYCLE_PATTERN = Regex("""lifecycle-\*:((\d+\.\d+\.\d+)(?:[-+][a-zA-Z0-9.]+)*)""")
    }
    
    data class FetchResult(
        val lifecycle: String? = null,
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
     * Generate fallback versions for given base Compose version.
     * 
     * For "1.10.0-beta02" generates:
     * - 1.10.0-beta01
     * - 1.10.0-alpha08, alpha07, ..., alpha01
     * - 1.9.3, 1.9.2, 1.9.1, ...
     * - 1.8.0, 1.7.1, ...
     */
    fun generateFallbackVersions(baseVersion: String): List<String> {
        val versions = mutableListOf<String>()
        
        // Start with known Bundle versions (fast checks first!)
        versions.addAll(ComposeVersions.LIBRARY_BUNDLES.keys)
        
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
                
                // If beta, add alphas for same patch
                if (qualifierType == "beta") {
                    for (i in 8 downTo 1) {
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
