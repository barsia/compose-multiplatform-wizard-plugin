package io.github.heisiar.composewizard.shared.analytics

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import io.github.heisiar.composewizard.shared.settings.WizardSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.util.*

/**
 * Analytics service using Google Analytics 4 Measurement Protocol (v2).
 * 
 * Privacy-first implementation:
 * - Requires explicit user consent
 * - No personal data collected
 * - Anonymous client ID (random UUID)
 * - IP anonymization enabled
 * - Can be disabled in settings at any time
 * 
 * Data collected:
 * - Plugin version
 * - IDE type (AI/IU/IC)
 * - Event names (wizard_opened, platform_toggled, etc.)
 * - Non-identifying usage patterns
 * 
 * Setup instructions:
 * 1. Create GA4 Property at https://analytics.google.com/
 * 2. Get Measurement ID (format: G-XXXXXXXXXX) from Admin → Data Streams
 * 3. Create API Secret: Admin → Data Streams → Measurement Protocol API secrets → Create
 * 4. Replace GA4_MEASUREMENT_ID and GA4_API_SECRET below
 * 5. (Optional) Configure Custom Dimensions in GA4 UI: plugin_version, ide_type
 */
@Service(Service.Level.APP)
class AnalyticsService {
    
    private val logger = Logger.getInstance(AnalyticsService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Google Analytics 4 Measurement Protocol endpoint
    private val GA4_ENDPOINT = "https://www.google-analytics.com/mp/collect"
    
    // GA4 credentials - loaded from analytics.properties (generated at build time from local.properties)
    // Add to local.properties (NOT committed to git):
    //   ga4.measurement.id=G-XXXXXXXXXX
    //   ga4.api.secret=your_api_secret_here
    private val GA4_MEASUREMENT_ID: String by lazy {
        val props = java.util.Properties()
        javaClass.classLoader.getResourceAsStream("analytics.properties")?.use { props.load(it) }
        props.getProperty("ga4.measurement.id", "G-XXXXXXXXXX")
    }
    
    private val GA4_API_SECRET: String by lazy {
        val props = java.util.Properties()
        javaClass.classLoader.getResourceAsStream("analytics.properties")?.use { props.load(it) }
        props.getProperty("ga4.api.secret", "your_api_secret_here")
    }
    
    // Anonymous client ID (generated once per installation)
    private val clientId: String by lazy {
        val settings = WizardSettings.getInstance()
        if (settings.analyticsClientId.isEmpty()) {
            settings.analyticsClientId = UUID.randomUUID().toString()
        }
        settings.analyticsClientId
    }
    
    /**
     * Log an event to Google Analytics.
     * Only sends if user has given consent.
     */
    fun logEvent(category: String, action: String, label: String? = null, value: Int? = null) {
        if (!isEnabled()) {
            return
        }
        
        scope.launch {
            try {
                sendToGA(category, action, label, value)
            } catch (e: Exception) {
                logger.warn("Failed to send analytics event: ${e.message}")
            }
        }
    }
    
    /**
     * Check if analytics is enabled (user consent + not disabled in settings).
     */
    private fun isEnabled(): Boolean {
        val settings = WizardSettings.getInstance()
        return settings.analyticsEnabled && settings.analyticsConsentGiven
    }
    
    /**
     * Send event to Google Analytics 4 using Measurement Protocol v2.
     * Uses JSON payload format (not URL-encoded).
     */
    private fun sendToGA(category: String, action: String, label: String?, value: Int?) {
        // Build event name from category and action
        val eventName = "${category}_${action}".replace("-", "_").replace(" ", "_")
        
        // Build event parameters
        val eventParams = buildMap<String, Any> {
            put("category", category)
            put("action", action)
            label?.let { put("label", it) }
            value?.let { put("value", it) }
            
            // Custom parameters
            put("plugin_version", getPluginVersion())
            put("ide_type", getIdeType())
        }
        
        // Build GA4 payload
        val payload = mapOf(
            "client_id" to clientId,
            "events" to listOf(
                mapOf(
                    "name" to eventName,
                    "params" to eventParams
                )
            )
        )
        
        // Convert to JSON manually (to avoid dependency on JSON library)
        val jsonPayload = buildJsonString(payload)
        
        // Build URL with query parameters
        val urlString = "$GA4_ENDPOINT?measurement_id=$GA4_MEASUREMENT_ID&api_secret=$GA4_API_SECRET"
        val connection = java.net.URI(urlString).toURL().openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "ComposeMultiplatformWizard/${getPluginVersion()}")
            
            connection.outputStream.use { it.write(jsonPayload.toByteArray(Charsets.UTF_8)) }
            
            val responseCode = connection.responseCode
            if (responseCode != 200 && responseCode != 204) {
                logger.warn("GA4 returned non-success response: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Build JSON string manually to avoid external dependencies.
     * Only handles the specific structure needed for GA4 events.
     */
    private fun buildJsonString(data: Map<String, Any>): String {
        val clientId = data["client_id"] as String
        val events = data["events"] as List<*>
        val event = (events[0] as Map<*, *>)
        val eventName = event["name"] as String
        val params = event["params"] as Map<*, *>
        
        val paramsJson = params.entries.joinToString(",") { (key, value) ->
            when (value) {
                is Number -> "\"$key\":$value"
                else -> "\"$key\":\"${escapeJson(value.toString())}\""
            }
        }
        
        return """{"client_id":"$clientId","events":[{"name":"$eventName","params":{$paramsJson}}]}"""
    }
    
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    private fun getPluginVersion(): String {
        return try {
            val plugin = PluginManagerCore.getPlugin(
                PluginId.getId("io.github.heisiar.compose-multiplatform-wizard")
            )
            plugin?.version ?: "unknown"
        } catch (e: Exception) {
            logger.warn("Failed to get plugin version: ${e.message}")
            "unknown"
        }
    }
    
    private fun getIdeType(): String {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            return ApplicationInfo.getInstance().build.productCode ?: "UNKNOWN"
        }
        return "TEST"
    }
    
    companion object {
        @JvmStatic
        fun getInstance(): AnalyticsService = service()
        
        // Event categories
        const val CATEGORY_WIZARD = "wizard"
        const val CATEGORY_PLATFORM = "platform"
        const val CATEGORY_OPTIONS = "options"
        const val CATEGORY_VERSION = "version"
        const val CATEGORY_VALIDATION = "validation"
        const val CATEGORY_FIELD = "field"
        const val CATEGORY_LIBRARY = "library"  // NEW: Library selection and versioning
        const val CATEGORY_UI = "ui"  // NEW: UI interactions (icons, links, etc.)
        
        // Wizard actions
        const val ACTION_WIZARD_OPENED = "opened"
        const val ACTION_WIZARD_COMPLETED = "completed"
        
        // Platform actions
        const val ACTION_PLATFORM_TOGGLED = "toggled"
        
        // Options actions
        const val ACTION_TESTS_TOGGLED = "tests_toggled"
        const val ACTION_GIT_TOGGLED = "git_toggled"
        const val ACTION_DEV_VERSIONS_UNLOCKED = "dev_versions_unlocked"
        const val ACTION_DEV_VERSIONS_TOGGLED = "dev_versions_toggled"
        const val ACTION_REPOSITORY_TOGGLED = "repository_toggled"  // NEW: JetBrains Maven vs Maven Central
        
        // Version actions
        const val ACTION_VERSION_DROPDOWN_OPENED = "dropdown_opened"
        const val ACTION_VERSION_REFRESH_CLICKED = "refresh_clicked"
        const val ACTION_VERSION_SELECTED = "selected"
        
        // Validation actions
        const val ACTION_VALIDATION_ERROR = "error"
        
        // Field actions
        const val ACTION_FIELD_EDITED = "edited"
        
        // NEW: Library actions
        const val ACTION_LIBRARY_TOGGLED = "toggled"  // Library checkbox on/off
        const val ACTION_LIBRARY_VERSION_SELECTED = "version_selected"  // Library version changed
        
        // NEW: UI interaction actions
        const val ACTION_BUG_ICON_CLICKED = "bug_icon_clicked"  // Bug icon in footer
        const val ACTION_FEATURE_ICON_CLICKED = "feature_icon_clicked"  // Feature icon in footer
        const val ACTION_BUG_LINK_CLICKED = "bug_link_clicked"  // Bug link in Settings -> Plugins
        const val ACTION_FEATURE_LINK_CLICKED = "feature_link_clicked"  // Feature link in Settings -> Plugins
        const val ACTION_TRIPLE_CLICK_VERSION = "triple_click_version"  // Dev/Release toggle discovery
    }
}

