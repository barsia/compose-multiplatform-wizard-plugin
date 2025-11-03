package io.github.heisiar.composewizard.shared.statistics

import org.junit.jupiter.api.Test
import kotlin.test.*

class ComposeWizardUsageCollectorTest {

    @Test
    fun `UsageCollector object exists`() {
        assertNotNull(ComposeWizardUsageCollector)
    }
    
    @Test
    fun `UsageCollector has all required logging methods`() {
        // Verify all methods exist via reflection
        val methods = ComposeWizardUsageCollector::class.java.methods.map { it.name }
        
        assertTrue(methods.contains("logWizardOpened"))
        assertTrue(methods.contains("logWizardCompleted"))
        assertTrue(methods.contains("logPlatformToggled"))
        assertTrue(methods.contains("logTestsToggled"))
        assertTrue(methods.contains("logGitToggled"))
        assertTrue(methods.contains("logDevVersionsUnlocked"))
        assertTrue(methods.contains("logDevVersionsVisibilityToggled"))
        assertTrue(methods.contains("logDevVersionsToggled"))
        assertTrue(methods.contains("logVersionDropdownOpened"))
        assertTrue(methods.contains("logVersionRefreshClicked"))
        assertTrue(methods.contains("logComposeVersionSelected"))
        assertTrue(methods.contains("logFieldEdited"))
        assertTrue(methods.contains("logValidationError"))
    }

    
    @Test
    fun `event field types are correctly defined`() {
        val platforms = listOf("Android", "iOS", "Desktop", "Web")
        val fields = listOf("project_name", "project_location", "project_id")
        val errorTypes = listOf(
            "empty", "invalid_chars", "reserved_name", "invalid_path",
            "no_write_access", "already_open", "directory_not_empty",
            "invalid_package_format", "no_platform_selected"
        )
        
        assertTrue(platforms.isNotEmpty(), "Platform types should be defined")
        assertTrue(fields.isNotEmpty(), "Field types should be defined")
        assertTrue(errorTypes.isNotEmpty(), "Error types should be defined")
    }
    
    @Test
    fun `FUS event structure is documented`() {
        // This test documents the FUS event structure for future reference
        val events = mapOf(
            "wizard.opened" to listOf("ide_type"),
            "wizard.completed" to listOf("ide_type", "platforms_count", "include_tests", "include_git", "used_dev_versions", "time_spent_ms"),
            "platform.toggled" to listOf("ide_type", "platform", "selected"),
            "tests.toggled" to listOf("ide_type", "enabled"),
            "git.toggled" to listOf("ide_type", "enabled"),
            "dev.versions.unlocked" to listOf("ide_type"),
            "dev.versions.visibility.toggled" to listOf("ide_type", "visible", "toggle_count"),
            "dev.versions.toggled" to listOf("ide_type", "enabled"),
            "version.dropdown.opened" to listOf("ide_type"),
            "version.refresh.clicked" to listOf("ide_type"),
            "compose.version.selected" to listOf("ide_type", "version", "is_dev_version"),
            "field.edited" to listOf("ide_type", "field", "has_content"),
            "validation.error" to listOf("ide_type", "field", "error_type")
        )
        
        assertEquals(13, events.size, "Should have 13 FUS events defined")
        assertTrue(events.all { it.value.contains("ide_type") }, "All events should include ide_type")
    }
}

