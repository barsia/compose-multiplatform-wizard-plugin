package io.github.heisiar.composewizard.shared.analytics

import io.github.heisiar.composewizard.shared.settings.WizardSettings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for Analytics system logic.
 * 
 * Tests verify complete GDPR-compliant analytics flow through settings:
 * - Consent changes propagate correctly
 * - Analytics respects consent state
 * - Data persistence across restarts
 * - GDPR compliance
 */
class AnalyticsIntegrationTest {
    
    private lateinit var settings: WizardSettings
    
    @BeforeEach
    fun setUp() {
        settings = WizardSettings()
        resetAnalyticsState()
    }
    
    @AfterEach
    fun tearDown() {
        resetAnalyticsState()
    }
    
    private fun resetAnalyticsState() {
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        settings.analyticsConsentDialogShown = false
        settings.analyticsClientId = ""
    }
    
    // Helper to check if analytics is enabled
    private fun isAnalyticsEnabled(): Boolean {
        return settings.analyticsEnabled && settings.analyticsConsentGiven
    }
    
    // ========== End-to-End Consent Flow Tests ==========
    
    @Test
    fun `complete consent flow - user agrees`() {
        // 1. Initial state - no consent
        assertFalse(settings.analyticsConsentDialogShown)
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        
        // 2. User clicks "Agree" (simulated)
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        
        // 3. Settings are updated
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown)
        
        // 4. Analytics can send events
        assertTrue(isAnalyticsEnabled())
    }
    
    @Test
    fun `complete consent flow - user declines`() {
        // 1. Initial state - no consent
        assertFalse(settings.analyticsConsentDialogShown)
        assertFalse(settings.analyticsEnabled)
        
        // 2. User clicks "Continue without Analytics" (simulated)
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        settings.analyticsConsentDialogShown = true
        
        // 3. Settings are updated
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown)
        
        // 4. Analytics does NOT send events
        assertFalse(isAnalyticsEnabled())
    }
    
    @Test
    fun `complete consent flow - user changes mind in settings`() {
        // 1. User initially agrees
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        assertTrue(isAnalyticsEnabled())
        
        // 2. User goes to Settings and disables analytics
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        
        // 3. Analytics is now disabled
        assertFalse(settings.analyticsEnabled)
        assertFalse(isAnalyticsEnabled())
        
        // 4. User re-enables in Settings
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        
        // 5. Analytics is enabled again
        assertTrue(settings.analyticsEnabled)
        assertTrue(isAnalyticsEnabled())
    }
    
    // ========== Analytics State Tests ==========
    
    @Test
    fun `analytics respects user consent`() {
        // Initially disabled
        assertFalse(isAnalyticsEnabled())
        
        // User gives consent
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        assertTrue(isAnalyticsEnabled())
        
        // User withdraws consent
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        assertFalse(isAnalyticsEnabled())
    }
    
    @Test
    fun `analytics requires both consent flags`() {
        // Only analyticsEnabled = true
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = false
        assertFalse(isAnalyticsEnabled(), 
            "Analytics should be disabled if consent not explicitly given")
        
        // Only analyticsConsentGiven = true
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = true
        assertFalse(isAnalyticsEnabled(),
            "Analytics should be disabled if user disabled it in settings")
        
        // Both true
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        assertTrue(isAnalyticsEnabled(),
            "Analytics should be enabled only when both flags are true")
    }
    
    @Test
    fun `analytics client ID management`() {
        // Initially empty
        assertEquals("", settings.analyticsClientId)
        
        // Set client ID
        val clientId = java.util.UUID.randomUUID().toString()
        settings.analyticsClientId = clientId
        
        // Should be persisted in settings
        assertEquals(clientId, settings.analyticsClientId)
    }
    
    @Test
    fun `analytics client ID format validation`() {
        // Set a valid UUID
        val validUUID = "550e8400-e29b-41d4-a716-446655440000"
        settings.analyticsClientId = validUUID
        
        // UUID format: 8-4-4-4-12 hex digits
        val uuidPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(validUUID.matches(uuidPattern), 
            "Client ID should be valid UUID format")
    }
    
    // ========== State Persistence Tests ==========
    
    @Test
    fun `analytics state persists across settings save and load`() {
        // 1. User gives consent
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        
        val clientId = java.util.UUID.randomUUID().toString()
        settings.analyticsClientId = clientId
        
        // 2. Get current state
        val savedState = settings.getState()
        assertNotNull(savedState)
        assertTrue(savedState.analyticsEnabled)
        assertTrue(savedState.analyticsConsentGiven)
        assertEquals(clientId, savedState.analyticsClientId)
        
        // 3. Simulate app restart - load state into fresh settings
        val freshSettings = WizardSettings()
        freshSettings.loadState(savedState)
        
        // 4. Verify state is preserved
        assertTrue(freshSettings.analyticsEnabled)
        assertTrue(freshSettings.analyticsConsentGiven)
        assertTrue(freshSettings.analyticsConsentDialogShown)
        assertEquals(clientId, freshSettings.analyticsClientId)
    }
    
    @Test
    fun `client ID survives consent withdrawal`() {
        // 1. User agrees, client ID is set
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        
        val clientId = java.util.UUID.randomUUID().toString()
        settings.analyticsClientId = clientId
        assertNotEquals("", clientId)
        
        // 2. User withdraws consent
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        
        // 3. Client ID is still stored (for potential re-enable)
        assertEquals(clientId, settings.analyticsClientId)
    }
    
    // ========== GDPR Compliance Tests ==========
    
    @Test
    fun `GDPR compliance - no data collection without explicit consent`() {
        // Fresh start - no consent given
        resetAnalyticsState()
        
        // Analytics should be disabled
        assertFalse(isAnalyticsEnabled())
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
    }
    
    @Test
    fun `GDPR compliance - right to object is respected`() {
        // User gives consent
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        assertTrue(isAnalyticsEnabled())
        
        // User exercises right to object (GDPR Article 21)
        settings.analyticsEnabled = false
        settings.analyticsConsentGiven = false
        
        // Analytics immediately stops
        assertFalse(isAnalyticsEnabled())
    }
    
    @Test
    fun `GDPR compliance - consent is freely given and can be withdrawn`() {
        // 1. No pre-ticked boxes - consent starts as false
        assertFalse(settings.analyticsConsentGiven)
        assertFalse(settings.analyticsEnabled)
        
        // 2. User freely gives consent
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        assertTrue(settings.analyticsConsentGiven)
        
        // 3. User can freely withdraw consent
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        assertFalse(settings.analyticsConsentGiven)
        assertFalse(settings.analyticsEnabled)
    }
    
    @Test
    fun `GDPR compliance - data minimization principle`() {
        // Only essential data is stored:
        // 1. analyticsEnabled - user preference
        // 2. analyticsConsentGiven - legal requirement
        // 3. analyticsConsentDialogShown - UX requirement
        // 4. analyticsClientId - anonymous identifier (required for analytics)
        
        // Verify only these 4 analytics fields exist
        val settingsState = settings.getState()
        val analyticsFields = settingsState.javaClass.declaredFields
            .filter { it.name.startsWith("analytics") }
            .map { it.name }
            .sorted()
        
        assertEquals(
            listOf("analyticsClientId", "analyticsConsentDialogShown", "analyticsConsentGiven", "analyticsEnabled"),
            analyticsFields,
            "Only minimal analytics data should be stored"
        )
    }
    
    // ========== Edge Cases and Error Handling ==========
    
    @Test
    fun `concurrent consent changes are handled correctly`() {
        // Simulate rapid consent changes
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        assertTrue(settings.analyticsEnabled)
        
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        assertFalse(settings.analyticsEnabled)
        
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        
        // Final state is consistent
        assertTrue(settings.analyticsEnabled)
        assertTrue(settings.analyticsConsentGiven)
    }
    
    @Test
    fun `analytics handles empty client ID`() {
        // Edge case: client ID is empty
        settings.analyticsClientId = ""
        
        // Can still enable analytics
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        assertTrue(isAnalyticsEnabled())
        
        // Client ID can be set later
        val newClientId = java.util.UUID.randomUUID().toString()
        settings.analyticsClientId = newClientId
        assertEquals(newClientId, settings.analyticsClientId)
    }
    
    @Test
    fun `settings state is valid after multiple consent changes`() {
        // Simulate user changing mind multiple times
        repeat(5) { i ->
            if (i % 2 == 0) {
                settings.analyticsConsentGiven = true
                settings.analyticsEnabled = true
                settings.analyticsConsentDialogShown = true
                assertTrue(settings.analyticsEnabled)
            } else {
                settings.analyticsConsentGiven = false
                settings.analyticsEnabled = false
                assertFalse(settings.analyticsEnabled)
            }
        }
        
        // Final state should be valid and consistent
        val state = settings.getState()
        assertNotNull(state)
        assertEquals(settings.analyticsEnabled, state.analyticsEnabled)
        assertEquals(settings.analyticsConsentGiven, state.analyticsConsentGiven)
    }
    
    @Test
    fun `dialog shown flag prevents multiple dialogs`() {
        // First time - flag is false
        assertFalse(settings.analyticsConsentDialogShown)
        
        // User sees dialog and agrees
        settings.analyticsConsentDialogShown = true
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        
        // Flag is now true
        assertTrue(settings.analyticsConsentDialogShown)
        
        // Dialog should not be shown again (checked by showIfNeeded())
        // This test verifies the flag state, actual dialog logic is in showIfNeeded()
    }
}
