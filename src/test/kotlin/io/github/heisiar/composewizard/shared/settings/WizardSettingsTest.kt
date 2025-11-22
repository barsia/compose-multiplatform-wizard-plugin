package io.github.heisiar.composewizard.shared.settings

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Tests for WizardSettings.
 * 
 * PRIORITY: HIGH - Settings have 0% coverage
 * These tests verify settings persistence and state management.
 */
class WizardSettingsTest {
    
    private lateinit var settings: WizardSettings
    
    @BeforeEach
    fun setup() {
        settings = WizardSettings()
    }
    
    // ========== Default Values Tests ==========
    
    @Test
    fun `default enableDevVersions is false`() {
        assertEquals(false, settings.enableDevVersions)
    }
    
    @Test
    fun `default enableDevVersionsSetByUser is false`() {
        assertEquals(false, settings.enableDevVersionsSetByUser)
    }
    
    @Test
    fun `default devCheckboxVisibleByUser is false`() {
        assertEquals(false, settings.devCheckboxVisibleByUser)
    }
    
    @Test
    fun `default welcomeTooltipShown is false`() {
        assertEquals(false, settings.welcomeTooltipShown)
    }
    
    @Test
    fun `default isLibrariesExpanded is null`() {
        assertNull(settings.isLibrariesExpanded)
    }
    
    @Test
    fun `default lastPluginVersion is empty string`() {
        assertEquals("", settings.lastPluginVersion)
    }
    
    @Test
    fun `default githubToken is empty string`() {
        assertEquals("", settings.githubToken)
    }
    
    // ========== State Modification Tests ==========
    
    @Test
    fun `can set enableDevVersions to true`() {
        settings.enableDevVersions = true
        assertTrue(settings.enableDevVersions)
    }
    
    @Test
    fun `can set enableDevVersionsSetByUser to true`() {
        settings.enableDevVersionsSetByUser = true
        assertTrue(settings.enableDevVersionsSetByUser)
    }
    
    @Test
    fun `can set devCheckboxVisibleByUser to true`() {
        settings.devCheckboxVisibleByUser = true
        assertTrue(settings.devCheckboxVisibleByUser)
    }
    
    @Test
    fun `can set welcomeTooltipShown to true`() {
        settings.welcomeTooltipShown = true
        assertTrue(settings.welcomeTooltipShown)
    }
    
    @Test
    fun `can set isLibrariesExpanded to true`() {
        settings.isLibrariesExpanded = true
        assertEquals(true, settings.isLibrariesExpanded)
    }
    
    @Test
    fun `can set isLibrariesExpanded to false`() {
        settings.isLibrariesExpanded = false
        assertEquals(false, settings.isLibrariesExpanded)
    }
    
    @Test
    fun `can set lastPluginVersion`() {
        settings.lastPluginVersion = "1.2.3"
        assertEquals("1.2.3", settings.lastPluginVersion)
    }
    
    @Test
    fun `can set githubToken`() {
        settings.githubToken = "ghp_test123"
        assertEquals("ghp_test123", settings.githubToken)
    }
    
    // ========== State Persistence Tests ==========
    
    @Test
    fun `getState returns self`() {
        val state = settings.getState()
        assertSame(settings, state)
    }
    
    @Test
    fun `loadState copies properties from source`() {
        // Given
        val sourceSettings = WizardSettings().apply {
            enableDevVersions = true
            enableDevVersionsSetByUser = true
            devCheckboxVisibleByUser = true
            welcomeTooltipShown = true
            isLibrariesExpanded = true
            lastPluginVersion = "1.0.0"
            githubToken = "test_token"
        }
        
        // When
        settings.loadState(sourceSettings)
        
        // Then
        assertTrue(settings.enableDevVersions)
        assertTrue(settings.enableDevVersionsSetByUser)
        assertTrue(settings.devCheckboxVisibleByUser)
        assertTrue(settings.welcomeTooltipShown)
        assertEquals(true, settings.isLibrariesExpanded)
        assertEquals("1.0.0", settings.lastPluginVersion)
        assertEquals("test_token", settings.githubToken)
    }
    
    // ========== Multiple State Changes Tests ==========
    
    @Test
    fun `can toggle enableDevVersions multiple times`() {
        settings.enableDevVersions = true
        assertTrue(settings.enableDevVersions)
        
        settings.enableDevVersions = false
        assertFalse(settings.enableDevVersions)
        
        settings.enableDevVersions = true
        assertTrue(settings.enableDevVersions)
    }
    
    @Test
    fun `can update lastPluginVersion multiple times`() {
        settings.lastPluginVersion = "1.0.0"
        assertEquals("1.0.0", settings.lastPluginVersion)
        
        settings.lastPluginVersion = "1.1.0"
        assertEquals("1.1.0", settings.lastPluginVersion)
        
        settings.lastPluginVersion = "2.0.0"
        assertEquals("2.0.0", settings.lastPluginVersion)
    }
    
    // ========== Edge Cases Tests ==========
    
    @Test
    fun `can set empty githubToken`() {
        settings.githubToken = "test"
        settings.githubToken = ""
        assertEquals("", settings.githubToken)
    }
    
    @Test
    fun `can set isLibrariesExpanded back to null`() {
        settings.isLibrariesExpanded = true
        settings.isLibrariesExpanded = null
        assertNull(settings.isLibrariesExpanded)
    }
    
    @Test
    fun `loadState with same instance does not throw`() {
        // When/Then - should not throw
        settings.loadState(settings)
        assertNotNull(settings)
    }
    
    @Test
    fun `multiple loadState calls work correctly`() {
        // Given
        val state1 = WizardSettings().apply {
            enableDevVersions = true
            lastPluginVersion = "1.0.0"
        }
        
        val state2 = WizardSettings().apply {
            enableDevVersions = false
            lastPluginVersion = "2.0.0"
        }
        
        // When
        settings.loadState(state1)
        assertTrue(settings.enableDevVersions)
        assertEquals("1.0.0", settings.lastPluginVersion)
        
        settings.loadState(state2)
        assertFalse(settings.enableDevVersions)
        assertEquals("2.0.0", settings.lastPluginVersion)
    }
}
