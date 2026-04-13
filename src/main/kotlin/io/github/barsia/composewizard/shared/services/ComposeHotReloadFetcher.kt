package io.github.barsia.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import java.net.HttpURLConnection

object ComposeHotReloadFetcher {
    
    private val logger = Logger.getInstance(ComposeHotReloadFetcher::class.java)
    
    private const val COMPOSE_REPO_RAW_URL = "https://raw.githubusercontent.com/JetBrains/compose-multiplatform"
    private const val CORE_TAG_WEB_URL = "https://github.com/JetBrains/compose-multiplatform-core/releases/tag"
    private const val TIMEOUT_MS = 5000
    private const val CHECK_TIMEOUT_MS = 3000
    
    fun fetchHotReloadVersion(composeVersion: String): String? {
        return try {
            val encodedVersion = java.net.URLEncoder.encode(composeVersion, "UTF-8")
            val url = "$COMPOSE_REPO_RAW_URL/v$encodedVersion/gradle-plugins/gradle/libs.versions.toml"
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            val responseCode = connection.responseCode
            
            if (responseCode != 200) {
                return null
            }
            
            val tomlContent = connection.inputStream.bufferedReader().use { it.readText() }
            
            val preferMatch = Regex("""plugin-hot-reload\s*=\s*\{\s*prefer\s*=\s*"([\d.]+(?:-[\w\d.]+)?)"\s*\}""").find(tomlContent)
            if (preferMatch != null) {
                return preferMatch.groupValues[1]
            }
            
            val directMatch = Regex("""compose-hot-reload\s*=\s*"([\d.]+(?:-[\w\d.]+)?)"""").find(tomlContent)
            if (directMatch != null) {
                return directMatch.groupValues[1]
            }
            
            null
        } catch (e: Exception) {
            logger.info("Failed to fetch hot reload version from libs.versions.toml for $composeVersion: ${e.message}")
            null
        }
    }
    
    fun hasReleasePage(composeVersion: String): Boolean {
        return try {
            val encodedVersion = java.net.URLEncoder.encode(composeVersion, "UTF-8")
            val url = "$CORE_TAG_WEB_URL/v$encodedVersion"
            
            val connection = java.net.URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = CHECK_TIMEOUT_MS
            connection.readTimeout = CHECK_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (IntelliJ Compose Wizard)")
            
            connection.responseCode == 200
        } catch (e: Exception) {
            logger.info("Failed to check release page for $composeVersion: ${e.message}")
            false
        }
    }
}

