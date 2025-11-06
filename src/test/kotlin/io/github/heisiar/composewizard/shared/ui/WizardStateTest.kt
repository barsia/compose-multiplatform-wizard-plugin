package io.github.heisiar.composewizard.shared.ui

import org.junit.jupiter.api.Test
import kotlin.test.*

class WizardStateTest {

    @Test
    fun `WizardState class exists and is accessible`() {
        assertNotNull(WizardState::class)
    }

    @Test
    fun `WizardState can be instantiated`() {
        val state = WizardState()
        assertNotNull(state)
    }

    @Test
    fun `WizardState has mutable properties with default values`() {
        val state = WizardState()
        
        assertEquals("", state.projectName)
        assertEquals("", state.projectPath)
        assertEquals("", state.projectId)
        assertEquals("", state.composeVersion)
        assertEquals(false, state.desktop)
        assertEquals(false, state.android)
        assertEquals(false, state.ios)
        assertEquals(false, state.web)
        assertEquals(false, state.git)
        assertEquals(false, state.tests)
        assertEquals(false, state.enableDevVersions)
    }

    @Test
    fun `WizardState properties can be modified`() {
        val state = WizardState()
        
        state.projectName = "TestProject"
        state.desktop = true
        state.android = true
        
        assertEquals("TestProject", state.projectName)
        assertEquals(true, state.desktop)
        assertEquals(true, state.android)
    }

    @Test
    fun `WizardState hasNoTargets returns true when no platforms selected`() {
        val state = WizardState()
        assertTrue(state.hasNoTargets)
    }

    @Test
    fun `WizardState hasNoTargets returns false when at least one platform selected`() {
        val state = WizardState()
        state.desktop = true
        assertFalse(state.hasNoTargets)
    }
}

