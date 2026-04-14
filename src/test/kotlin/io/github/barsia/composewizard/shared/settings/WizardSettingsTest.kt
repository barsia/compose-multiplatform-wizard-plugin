package io.github.barsia.composewizard.shared.settings

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WizardSettingsTest {

    private lateinit var settings: WizardSettings

    @BeforeEach
    fun setup() {
        settings = WizardSettings()
    }

    @Test
    fun `default settings remain analytics free`() {
        assertFalse(settings.enableDevVersions)
        assertFalse(settings.enableDevVersionsSetByUser)
        assertFalse(settings.devCheckboxVisibleByUser)
        assertFalse(settings.welcomeTooltipShown)
        assertNull(settings.isLibrariesExpanded)
        assertEquals("", settings.lastPluginVersion)
        assertEquals("", settings.githubToken)
    }

    @Test
    fun `getState returns same instance`() {
        assertSame(settings, settings.getState())
    }

    @Test
    fun `loadState copies non analytics properties`() {
        val source = WizardSettings().apply {
            enableDevVersions = true
            enableDevVersionsSetByUser = true
            devCheckboxVisibleByUser = true
            welcomeTooltipShown = true
            isLibrariesExpanded = true
            lastPluginVersion = "1.2.3"
            githubToken = "test-token"
        }

        settings.loadState(source)

        assertTrue(settings.enableDevVersions)
        assertTrue(settings.enableDevVersionsSetByUser)
        assertTrue(settings.devCheckboxVisibleByUser)
        assertTrue(settings.welcomeTooltipShown)
        assertEquals(true, settings.isLibrariesExpanded)
        assertEquals("1.2.3", settings.lastPluginVersion)
        assertEquals("test-token", settings.githubToken)
    }
}
