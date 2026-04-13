package io.github.barsia.composewizard.shared.ui

import org.junit.jupiter.api.Test
import kotlin.test.*

class WizardStateTest {

    @Test
    fun `WizardState class exists`() {
        // Verify WizardState class exists and is accessible
        assertNotNull(WizardState::class)
    }

    @Test
    fun `WizardState can be instantiated`() {
        // Verify WizardState can be created
        val state = WizardState()
        assertNotNull(state)
    }
    
    @Test
    fun `WizardState has all required basic properties`() {
        // Verify WizardState has all expected basic properties by accessing them
        val state = WizardState()
        
        // Test that we can access these properties without exceptions
        assertNotNull(state.projectName)
        assertNotNull(state.projectPath)
        assertNotNull(state.projectId)
        assertNotNull(state.composeVersion)
    }
    
    @Test
    fun `WizardState has all required platform properties`() {
        // Verify WizardState has all platform target properties by accessing them
        val state = WizardState()
        
        // Test that we can access these properties without exceptions
        state.desktop
        state.android
        state.ios
        state.web
        
        // All checks passed
        assertTrue(true)
    }
    
    @Test
    fun `WizardState has all required option properties`() {
        // Verify WizardState has all option properties by accessing them
        val state = WizardState()
        
        // Test that we can access these properties without exceptions
        state.git
        state.tests
        state.enableDevVersions
        state.isLocationSynced
        
        // All checks passed
        assertTrue(true)
    }
    
    @Test
    fun `WizardState has all required validation properties`() {
        // Verify WizardState has all validation error properties by accessing them
        val state = WizardState()
        
        // Test that we can access these properties without exceptions
        state.projectNameError
        state.projectPathError
        state.projectLocationWarning
        state.projectIdError
        
        // All checks passed
        assertTrue(true)
    }
    
    @Test
    fun `WizardState has hasNoTargets computed property`() {
        // Verify hasNoTargets property exists by accessing it
        val state = WizardState()
        
        // Test that we can access this property without exception
        val hasNoTargets = state.hasNoTargets
        assertTrue(hasNoTargets is Boolean, "hasNoTargets should be Boolean")
    }
    
    @Test
    fun `WizardState hasNoTargets returns true when no platforms selected`() {
        // Test hasNoTargets logic
        val state = WizardState()
        state.desktop = false
        state.android = false
        state.ios = false
        state.web = false
        
        assertTrue(state.hasNoTargets, "hasNoTargets should return true when no platforms are selected")
    }
    
    @Test
    fun `WizardState hasNoTargets returns false when at least one platform selected`() {
        // Test hasNoTargets logic
        val state = WizardState()
        state.desktop = true
        state.android = false
        state.ios = false
        state.web = false
        
        assertFalse(state.hasNoTargets, "hasNoTargets should return false when at least one platform is selected")
    }
    
    @Test
    fun `WizardState default values are correct`() {
        // Verify default values
        val state = WizardState()
        
        assertEquals("", state.projectName)
        assertEquals("", state.projectPath)
        assertEquals("", state.projectId)
        assertEquals("", state.composeVersion)
        
        assertFalse(state.desktop)
        assertFalse(state.android)
        assertFalse(state.ios)
        assertFalse(state.web)
        
        assertFalse(state.git)
        assertFalse(state.tests)
        assertFalse(state.enableDevVersions)
        
        assertTrue(state.isLocationSynced)
        
        assertNull(state.projectNameError)
        assertNull(state.projectPathError)
        assertNull(state.projectLocationWarning)
        assertNull(state.projectIdError)
    }
    
    @Test
    fun `WizardState properties can be modified`() {
        // Verify properties are mutable
        val state = WizardState()
        
        state.projectName = "TestProject"
        state.projectPath = "/test/path"
        state.projectId = "com.example.test"
        state.composeVersion = "1.6.0"
        
        state.desktop = true
        state.android = true
        
        state.git = true
        state.tests = true
        
        assertEquals("TestProject", state.projectName)
        assertEquals("/test/path", state.projectPath)
        assertEquals("com.example.test", state.projectId)
        assertEquals("1.6.0", state.composeVersion)
        
        assertTrue(state.desktop)
        assertTrue(state.android)
        assertFalse(state.ios)
        assertFalse(state.web)
        
        assertTrue(state.git)
        assertTrue(state.tests)
    }
}

