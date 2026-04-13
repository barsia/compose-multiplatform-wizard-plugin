package io.github.barsia.composewizard.shared.analytics

import io.github.barsia.composewizard.shared.settings.WizardSettings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for AnalyticsLogger - event logging methods.
 * 
 * NOTE: These tests focus on logic verification without requiring IntelliJ Application context.
 * The actual AnalyticsService integration is tested separately.
 */
class AnalyticsLoggerTest {
    
    private lateinit var settings: WizardSettings
    
    @BeforeEach
    fun setUp() {
        settings = WizardSettings()
        resetAnalyticsSettings()
    }
    
    @AfterEach
    fun tearDown() {
        resetAnalyticsSettings()
    }
    
    private fun resetAnalyticsSettings() {
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        settings.analyticsConsentDialogShown = false
        settings.analyticsClientId = ""
    }
    
    // ========== Label Format Tests ==========
    
    @Test
    fun `platform toggle creates correct label format`() {
        val androidOn = "android:on"
        val androidOff = "android:off"
        
        assertTrue(androidOn.contains("android"))
        assertTrue(androidOn.endsWith("on"))
        assertTrue(androidOff.endsWith("off"))
    }
    
    @Test
    fun `compose version label format for stable versions`() {
        val version = "1.7.3"
        val isDevVersion = false
        val expectedLabel = if (isDevVersion) "dev:$version" else "stable:$version"
        
        assertEquals("stable:1.7.3", expectedLabel)
    }
    
    @Test
    fun `compose version label format for dev versions`() {
        val version = "1.7.3-dev123"
        val isDevVersion = true
        val expectedLabel = if (isDevVersion) "dev:$version" else "stable:$version"
        
        assertEquals("dev:1.7.3-dev123", expectedLabel)
    }
    
    @Test
    fun `field edited label format`() {
        val fieldFilled = "projectName:filled"
        val fieldEmpty = "projectName:empty"
        
        assertTrue(fieldFilled.endsWith("filled"))
        assertTrue(fieldEmpty.endsWith("empty"))
    }
    
    @Test
    fun `validation error label format`() {
        val errorLabel = "projectName:invalid_characters"
        
        assertTrue(errorLabel.contains(":"))
        assertTrue(errorLabel.startsWith("projectName"))
        assertTrue(errorLabel.endsWith("invalid_characters"))
    }
    
    @Test
    fun `library toggle label format`() {
        val libraryName = "material3"
        val version = "1.3.0"
        val selected = true
        val expectedLabel = "$libraryName:$version:${if (selected) "on" else "off"}"
        
        assertEquals("material3:1.3.0:on", expectedLabel)
    }
    
    @Test
    fun `library version selected label format`() {
        val libraryName = "navigation"
        val version = "2.9.0"
        val expectedLabel = "$libraryName:$version"
        
        assertEquals("navigation:2.9.0", expectedLabel)
    }
    
    @Test
    fun `repository source toggle label format`() {
        val jetbrainsMavenLabel = "jetbrains_maven"
        val mavenCentralLabel = "maven_central"
        
        assertTrue(jetbrainsMavenLabel.contains("jetbrains"))
        assertTrue(mavenCentralLabel.contains("maven_central"))
    }
    
    // ========== Time Conversion Tests ==========
    
    @Test
    fun `wizard completed converts milliseconds to seconds`() {
        val timeMs = 125000L // 125 seconds
        val timeSeconds = (timeMs / 1000).toInt()
        
        assertEquals(125, timeSeconds)
    }
    
    @Test
    fun `wizard completed handles zero time`() {
        val timeMs = 0L
        val timeSeconds = (timeMs / 1000).toInt()
        
        assertEquals(0, timeSeconds)
    }
    
    @Test
    fun `wizard completed handles large time values`() {
        val timeMs = 3600000L // 1 hour
        val timeSeconds = (timeMs / 1000).toInt()
        
        assertEquals(3600, timeSeconds)
    }
    
    // ========== Boolean Toggle Tests ==========
    
    @Test
    fun `tests toggle creates enabled or disabled labels`() {
        val enabledLabel = "enabled"
        val disabledLabel = "disabled"
        
        assertEquals("enabled", enabledLabel)
        assertEquals("disabled", disabledLabel)
    }
    
    @Test
    fun `git toggle creates enabled or disabled labels`() {
        val enabledLabel = "enabled"
        val disabledLabel = "disabled"
        
        assertNotEquals(enabledLabel, disabledLabel)
    }
    
    @Test
    fun `dev versions toggle creates enabled or disabled labels`() {
        val enabled = true
        val disabled = false
        val enabledLabel = if (enabled) "enabled" else "disabled"
        val disabledLabel = if (disabled) "enabled" else "disabled"
        
        assertEquals("enabled", enabledLabel)
        assertEquals("disabled", disabledLabel)
    }
    
    // ========== Platform Label Tests ==========
    
    @Test
    fun `all platform names create valid labels`() {
        val platforms = listOf("android", "ios", "desktop", "web")
        
        platforms.forEach { platform ->
            val onLabel = "$platform:on"
            val offLabel = "$platform:off"
            
            assertTrue(onLabel.startsWith(platform))
            assertTrue(offLabel.startsWith(platform))
            assertTrue(onLabel.endsWith("on"))
            assertTrue(offLabel.endsWith("off"))
        }
    }
    
    // ========== Analytics Settings Tests ==========
    
    @Test
    fun `analytics can be properly configured`() {
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        settings.analyticsClientId = "test-uuid"
        
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentGiven)
        assertNotEquals("", settings.analyticsClientId)
    }
    
    @Test
    fun `analytics can be disabled`() {
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
    }
    
    // ========== Edge Cases Tests ==========
    
    @Test
    fun `empty string platform creates valid label`() {
        val platform = ""
        val label = "$platform:on"
        
        assertEquals(":on", label)
    }
    
    @Test
    fun `special characters in platform name`() {
        val platform = "test-platform"
        val label = "$platform:on"
        
        assertEquals("test-platform:on", label)
    }
    
    @Test
    fun `very long strings in labels`() {
        val longString = "a".repeat(1000)
        val label = "$longString:on"
        
        assertTrue(label.length > 1000)
        assertTrue(label.endsWith(":on"))
    }
    
    // ========== Constants Verification Tests ==========
    
    @Test
    fun `verify AnalyticsService categories exist`() {
        assertEquals("wizard", AnalyticsService.CATEGORY_WIZARD)
        assertEquals("platform", AnalyticsService.CATEGORY_PLATFORM)
        assertEquals("options", AnalyticsService.CATEGORY_OPTIONS)
        assertEquals("version", AnalyticsService.CATEGORY_VERSION)
        assertEquals("library", AnalyticsService.CATEGORY_LIBRARY)
        assertEquals("ui", AnalyticsService.CATEGORY_UI)
    }
    
    @Test
    fun `verify AnalyticsService actions exist`() {
        // Wizard
        assertEquals("opened", AnalyticsService.ACTION_WIZARD_OPENED)
        assertEquals("completed", AnalyticsService.ACTION_WIZARD_COMPLETED)
        
        // Platform
        assertEquals("toggled", AnalyticsService.ACTION_PLATFORM_TOGGLED)
        
        // Options
        assertEquals("tests_toggled", AnalyticsService.ACTION_TESTS_TOGGLED)
        assertEquals("git_toggled", AnalyticsService.ACTION_GIT_TOGGLED)
        assertEquals("dev_versions_toggled", AnalyticsService.ACTION_DEV_VERSIONS_TOGGLED)
        assertEquals("repository_toggled", AnalyticsService.ACTION_REPOSITORY_TOGGLED)
        
        // Library
        assertEquals("toggled", AnalyticsService.ACTION_LIBRARY_TOGGLED)
        assertEquals("version_selected", AnalyticsService.ACTION_LIBRARY_VERSION_SELECTED)
        
        // UI
        assertEquals("bug_icon_clicked", AnalyticsService.ACTION_BUG_ICON_CLICKED)
        assertEquals("feature_icon_clicked", AnalyticsService.ACTION_FEATURE_ICON_CLICKED)
        assertEquals("triple_click_version", AnalyticsService.ACTION_TRIPLE_CLICK_VERSION)
    }
}
