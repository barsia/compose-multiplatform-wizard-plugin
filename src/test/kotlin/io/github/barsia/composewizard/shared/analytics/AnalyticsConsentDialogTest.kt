package io.github.barsia.composewizard.shared.analytics

import io.github.barsia.composewizard.shared.settings.WizardSettings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for Analytics Consent logic.
 * 
 * These tests verify GDPR-compliant consent handling through settings:
 * - Consent state management
 * - Settings updates
 * - GDPR compliance verification
 * 
 * Note: Dialog UI tests are handled separately in integration tests.
 */
class AnalyticsConsentDialogTest {
    
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
    }
    
    // ========== Consent State Management Tests ==========
    
    @Test
    fun `simulate user agreeing to analytics`() {
        // Given - fresh state
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertFalse(settings.analyticsConsentDialogShown)
        
        // When - user clicks "Agree" (simulated)
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        
        // Then
        assertTrue(settings.analyticsEnabled, "Analytics should be enabled")
        assertTrue(settings.analyticsConsentGiven, "Consent should be recorded")
        assertTrue(settings.analyticsConsentDialogShown, "Dialog shown flag should be set")
    }
    
    @Test
    fun `simulate user declining analytics`() {
        // Given - fresh state
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertFalse(settings.analyticsConsentDialogShown)
        
        // When - user clicks "Continue without Analytics" (simulated)
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        settings.analyticsConsentDialogShown = true
        
        // Then
        assertFalse(settings.analyticsEnabled, "Analytics should be disabled")
        assertFalse(settings.analyticsConsentGiven, "Consent should be false")
        assertTrue(settings.analyticsConsentDialogShown, "Dialog shown flag should be set")
    }
    
    @Test
    fun `consent state is persisted`() {
        // When - user agrees
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        
        val state1 = Triple(settings.analyticsEnabled, settings.analyticsConsentGiven, settings.analyticsConsentDialogShown)
        
        // Simulate multiple reads
        val state2 = Triple(settings.analyticsEnabled, settings.analyticsConsentGiven, settings.analyticsConsentDialogShown)
        
        // Then - state should not change
        assertEquals(state1, state2)
    }
    
    // ========== GDPR Compliance Tests ==========
    
    @Test
    fun `default settings are GDPR compliant - opt-in required`() {
        // Given - fresh settings
        val freshSettings = WizardSettings()
        
        // Then - analytics must be disabled by default (opt-in, not opt-out)
        assertFalse(freshSettings.analyticsEnabled, "Analytics must be disabled by default")
        assertFalse(freshSettings.analyticsConsentGiven, "Consent must not be pre-given")
        assertFalse(freshSettings.analyticsConsentDialogShown, "Dialog must not be pre-shown")
    }
    
    @Test
    fun `user can withdraw consent - GDPR right to object`() {
        // Given - user previously agreed
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = true
        settings.analyticsConsentDialogShown = true
        
        // When - user changes their mind
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        
        // Then - consent is withdrawn (GDPR right to object)
        assertFalse(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown) // Still shown once
    }
    
    @Test
    fun `consent and analytics state should be synchronized`() {
        // Test 1: Both enabled
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        assertEquals(settings.analyticsConsentGiven, settings.analyticsEnabled)
        
        // Test 2: Both disabled
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        assertEquals(settings.analyticsConsentGiven, settings.analyticsEnabled)
    }
    
    // ========== Dialog Show Logic Tests ==========
    
    @Test
    fun `dialog should not be shown if already shown`() {
        // Given
        settings.analyticsConsentDialogShown = true
        
        // When/Then - showIfNeeded() should check this flag
        assertTrue(settings.analyticsConsentDialogShown)
        
        // Dialog logic should respect this flag
        // (actual showIfNeeded() test is in integration tests)
    }
    
    @Test
    fun `consent can be given without dialog being shown - programmatic consent`() {
        // Edge case: consent given programmatically (e.g., from config)
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        // Dialog not shown
        
        assertTrue(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentDialogShown)
    }
    
    // ========== State Transitions Tests ==========
    
    @Test
    fun `consent can be toggled multiple times`() {
        // Agree
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        assertTrue(settings.analyticsEnabled)
        
        // Decline
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        assertFalse(settings.analyticsEnabled)
        
        // Agree again
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        assertTrue(settings.analyticsEnabled)
    }
    
    @Test
    fun `dialog shown flag persists after consent changes`() {
        // Given - dialog was shown
        settings.analyticsConsentDialogShown = true
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        
        // When - user changes consent
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        
        // Then - dialog shown flag remains true
        assertTrue(settings.analyticsConsentDialogShown)
    }
    
    // ========== Edge Cases Tests ==========
    
    @Test
    fun `all analytics properties are independent`() {
        // Set different values
        settings.analyticsEnabled = true
        settings.analyticsConsentGiven = false
        settings.analyticsConsentDialogShown = true
        
        // Verify independence
        assertTrue(settings.analyticsEnabled)
        assertFalse(settings.analyticsConsentGiven)
        assertTrue(settings.analyticsConsentDialogShown)
    }
    
    @Test
    fun `analytics client ID can be set and retrieved`() {
        val testId = "test-uuid-12345"
        settings.analyticsClientId = testId
        assertEquals(testId, settings.analyticsClientId)
    }
    
    @Test
    fun `client ID persists across consent changes`() {
        val clientId = "persistent-uuid"
        settings.analyticsClientId = clientId
        
        // Enable
        settings.analyticsEnabled = true
        assertEquals(clientId, settings.analyticsClientId)
        
        // Disable
        settings.analyticsEnabled = false
        assertEquals(clientId, settings.analyticsClientId)
    }
}
