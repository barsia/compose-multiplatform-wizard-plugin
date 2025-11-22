package io.github.heisiar.composewizard.shared.statistics

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * Tests for ComposeWizardUsageCollector.
 * 
 * PRIORITY: HIGH - Statistics have 6.2% coverage
 * These tests verify that logging methods don't throw exceptions.
 */
class ComposeWizardUsageCollectorTest {
    
    // ========== Basic Method Call Tests ==========
    
    @Test
    fun `logWizardOpened does not throw`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logWizardOpened()
    }
    
    @Test
    fun `logWizardCompleted does not throw`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = 2,
            includeTests = true,
            includeGit = true,
            usedDevVersions = false,
            timeSpentMs = 5000L
        )
    }
    
    @Test
    fun `logPlatformToggled does not throw for Android`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logPlatformToggled("Android", true)
    }
    
    @Test
    fun `logPlatformToggled does not throw for iOS`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logPlatformToggled("iOS", false)
    }
    
    @Test
    fun `logPlatformToggled does not throw for Desktop`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logPlatformToggled("Desktop", true)
    }
    
    @Test
    fun `logPlatformToggled does not throw for Web`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logPlatformToggled("Web", false)
    }
    
    @Test
    fun `logTestsToggled does not throw when enabled`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logTestsToggled(true)
    }
    
    @Test
    fun `logTestsToggled does not throw when disabled`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logTestsToggled(false)
    }
    
    @Test
    fun `logGitToggled does not throw when enabled`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logGitToggled(true)
    }
    
    @Test
    fun `logGitToggled does not throw when disabled`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logGitToggled(false)
    }
    
    @Test
    fun `logDevVersionsUnlocked does not throw`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logDevVersionsUnlocked()
    }
    
    @Test
    fun `logDevVersionsVisibilityToggled does not throw`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logDevVersionsVisibilityToggled(true, 1)
    }
    
    @Test
    fun `logDevVersionsToggled does not throw when enabled`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logDevVersionsToggled(true)
    }
    
    @Test
    fun `logDevVersionsToggled does not throw when disabled`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logDevVersionsToggled(false)
    }
    
    @Test
    fun `logVersionDropdownOpened does not throw`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logVersionDropdownOpened()
    }
    
    @Test
    fun `logVersionRefreshClicked does not throw`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logVersionRefreshClicked()
    }
    
    @Test
    fun `logComposeVersionSelected does not throw for stable version`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logComposeVersionSelected("1.6.0", false)
    }
    
    @Test
    fun `logComposeVersionSelected does not throw for dev version`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logComposeVersionSelected("1.7.0-beta01", true)
    }
    
    @Test
    fun `logFieldEdited does not throw for project_name`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logFieldEdited("project_name", true)
    }
    
    @Test
    fun `logFieldEdited does not throw for project_location`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logFieldEdited("project_location", false)
    }
    
    @Test
    fun `logFieldEdited does not throw for project_id`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logFieldEdited("project_id", true)
    }
    
    @Test
    fun `logValidationError does not throw for empty error`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logValidationError("project_name", "empty")
    }
    
    @Test
    fun `logValidationError does not throw for invalid_chars error`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logValidationError("project_name", "invalid_chars")
    }
    
    @Test
    fun `logValidationError does not throw for no_platform_selected error`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logValidationError("platforms", "no_platform_selected")
    }
    
    // ========== Multiple Calls Tests ==========
    
    @Test
    fun `can call logWizardOpened multiple times`() {
        // When/Then - should not throw
        repeat(5) {
            ComposeWizardUsageCollector.logWizardOpened()
        }
    }
    
    @Test
    fun `can toggle platforms multiple times`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logPlatformToggled("Android", true)
        ComposeWizardUsageCollector.logPlatformToggled("Android", false)
        ComposeWizardUsageCollector.logPlatformToggled("iOS", true)
        ComposeWizardUsageCollector.logPlatformToggled("Desktop", true)
    }
    
    @Test
    fun `can log multiple field edits`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logFieldEdited("project_name", true)
        ComposeWizardUsageCollector.logFieldEdited("project_location", true)
        ComposeWizardUsageCollector.logFieldEdited("project_id", true)
    }
    
    // ========== Edge Cases Tests ==========
    
    @Test
    fun `logWizardCompleted handles zero platforms`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = 0,
            includeTests = false,
            includeGit = false,
            usedDevVersions = false,
            timeSpentMs = 0L
        )
    }
    
    @Test
    fun `logWizardCompleted handles all platforms`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = 4,
            includeTests = true,
            includeGit = true,
            usedDevVersions = true,
            timeSpentMs = 120000L
        )
    }
    
    @Test
    fun `logDevVersionsVisibilityToggled handles high toggle count`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logDevVersionsVisibilityToggled(true, 100)
    }
    
    @Test
    fun `logComposeVersionSelected handles empty version string`() {
        // When/Then - should not throw
        ComposeWizardUsageCollector.logComposeVersionSelected("", false)
    }
    
    @Test
    fun `getGroup returns non-null EventLogGroup`() {
        // When
        val group = ComposeWizardUsageCollector.getGroup()
        
        // Then
        assertNotNull(group)
    }
}
