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
    
    // ========== Analytics Settings Tests (GDPR) ==========
    
    @Test
    fun `default analyticsEnabled is false - GDPR opt-in required`() {
        // GDPR requires opt-in, not opt-out
        assertFalse(settings.analyticsEnabled)
    }
    
    @Test
    fun `default analyticsConsentGiven is false`() {
        assertFalse(settings.analyticsConsentGiven)
    }
    
    @Test
    fun `default analyticsConsentDialogShown is false`() {
        assertFalse(settings.analyticsConsentDialogShown)
    }
    
    @Test
    fun `default analyticsClientId is empty string`() {
        assertEquals("", settings.analyticsClientId)
    }
    
    @Test
    fun `can enable analytics`() {
        settings.analyticsEnabled = true
        assertTrue(settings.analyticsEnabled)
    }
    
    @Test
    fun `can give analytics consent`() {
        settings.analyticsConsentGiven = true
        assertTrue(settings.analyticsConsentGiven)
    }
    
    @Test
    fun `can mark consent dialog as shown`() {
        settings.analyticsConsentDialogShown = true
        assertTrue(settings.analyticsConsentDialogShown)
    }
    
    @Test
    fun `can set analytics client ID`() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        settings.analyticsClientId = uuid
        assertEquals(uuid, settings.analyticsClientId)
    }
    
    @Test
    fun `can disable analytics after enabling`() {
        settings.analyticsEnabled = true
        assertTrue(settings.analyticsEnabled)
        
        settings.analyticsEnabled = false
        assertFalse(settings.analyticsEnabled)
    }
    
    @Test
    fun `can withdraw consent after giving it - GDPR right to object`() {
        // GDPR Article 21 - Right to object
        settings.analyticsConsentGiven = true
        assertTrue(settings.analyticsConsentGiven)
        
        settings.analyticsConsentGiven = false
        assertFalse(settings.analyticsConsentGiven)
    }
    
    @Test
    fun `loadState copies all analytics properties`() {
        // Given
        val sourceSettings = WizardSettings().apply {
            analyticsEnabled = true
            analyticsConsentGiven = true
            analyticsConsentDialogShown = true
            analyticsClientId = "test-uuid-123"
        }
        
        // When
        settings.loadState(sourceSettings)
        
        // Then
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown)
        assertEquals("test-uuid-123", settings.analyticsClientId)
    }
    
    @Test
    fun `analytics settings can be toggled multiple times`() {
        // Simulate user changing consent multiple times
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentGiven)
        
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentGiven)
    }
    
    @Test
    fun `all four analytics properties can be set independently`() {
        // Test that all properties are independent
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = false
        settings.analyticsConsentDialogShown = true
        settings.analyticsClientId = "uuid"
        
        assertTrue(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown)
        assertEquals("uuid", settings.analyticsClientId)
    }
    
    @Test
    fun `analyticsClientId can be empty string`() {
        settings.analyticsClientId = "test"
        settings.analyticsClientId = ""
        assertEquals("", settings.analyticsClientId)
    }
    
    @Test
    fun `analyticsClientId can store valid UUID format`() {
        val validUUID = "123e4567-e89b-12d3-a456-426614174000"
        settings.analyticsClientId = validUUID
        assertEquals(validUUID, settings.analyticsClientId)
    }
    
    // ========== Analytics Integration Tests ==========
    
    @Test
    fun `typical user consent flow - agree`() {
        // Simulate user agreeing to analytics
        assertFalse(settings.analyticsConsentDialogShown)
        
        // User sees dialog and agrees
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        settings.analyticsClientId = "generated-uuid"
        
        // Verify state
        assertTrue(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentDialogShown)
        assertEquals("generated-uuid", settings.analyticsClientId)
    }
    
    @Test
    fun `typical user consent flow - decline`() {
        // Simulate user declining analytics
        assertFalse(settings.analyticsConsentDialogShown)
        
        // User sees dialog and declines
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        settings.analyticsConsentDialogShown = true
        // clientId remains empty
        
        // Verify state
        assertFalse(settings.analyticsConsentGiven)
        assertFalse(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentDialogShown)
        assertEquals("", settings.analyticsClientId)
    }
    
    @Test
    fun `typical user consent flow - change mind later`() {
        // User initially agrees
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        settings.analyticsClientId = "uuid"
        
        // User changes mind in settings
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        
        // Verify state - dialog shown flag stays true, clientId preserved
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown)
        assertEquals("uuid", settings.analyticsClientId) // UUID preserved for potential re-enable
    }
    
    @Test
    fun `loadState preserves complete analytics state across restarts`() {
        // Simulate app restart with persisted state
        val persistedState = WizardSettings().apply {
            analyticsEnabled = true
            analyticsConsentGiven = true
            analyticsConsentDialogShown = true
            analyticsClientId = "persistent-uuid"
        }
        
        // Fresh settings instance (simulating app restart)
        val freshSettings = WizardSettings()
        assertFalse(freshSettings.analyticsEnabled)
        assertFalse(freshSettings.analyticsConsentGiven)
        
        // Load persisted state
        freshSettings.loadState(persistedState)
        
        // Verify all analytics state is restored
        assertTrue(freshSettings.analyticsEnabled)
        assertTrue(freshSettings.analyticsConsentGiven)
        assertTrue(freshSettings.analyticsConsentDialogShown)
        assertEquals("persistent-uuid", freshSettings.analyticsClientId)
    }
}
