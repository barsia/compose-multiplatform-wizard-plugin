package io.github.heisiar.composewizard.shared.ui

import org.junit.jupiter.api.Test
import kotlin.test.*

class ValidationStateTest {

    @Test
    fun `validation should detect errors in initial state`() {
        val state = WizardState()
        
        // Set up state with errors
        state.projectName = ""  // Empty name - should be invalid
        state.projectNameError = "Project name cannot be empty"
        state.desktop = true  // At least one platform selected
        
        // Validation should detect the error
        val isValid = state.projectNameError == null && 
                     state.projectPathError == null && 
                     state.projectIdError == null &&
                     !state.hasNoTargets
        
        assertFalse(isValid, "Form should be invalid when projectNameError is set")
    }

    @Test
    fun `validation should detect missing platforms`() {
        val state = WizardState()
        
        // Set up state with no platforms selected
        state.projectName = "MyProject"
        state.desktop = false
        state.android = false
        state.ios = false
        state.web = false
        
        assertTrue(state.hasNoTargets, "hasNoTargets should be true when no platforms selected")
        
        // Validation should detect missing platforms
        val isValid = state.projectNameError == null && 
                     state.projectPathError == null && 
                     state.projectIdError == null &&
                     !state.hasNoTargets
        
        assertFalse(isValid, "Form should be invalid when no platforms are selected")
    }

    @Test
    fun `validation should pass when all fields are valid`() {
        val state = WizardState()
        
        // Set up valid state
        state.projectName = "MyProject"
        state.projectPath = "/Users/test/Projects"
        state.projectId = "com.example.myproject"
        state.desktop = true
        state.projectNameError = null
        state.projectPathError = null
        state.projectIdError = null
        
        // Validation should pass
        val isValid = state.projectNameError == null && 
                     state.projectPathError == null && 
                     state.projectIdError == null &&
                     !state.hasNoTargets
        
        assertTrue(isValid, "Form should be valid when all fields are correct")
    }

    @Test
    fun `validation should detect projectPath errors`() {
        val state = WizardState()
        
        // Set up state with path error
        state.projectName = "MyProject"
        state.projectPath = "/invalid/path"
        state.projectPathError = "Path is not writable"
        state.desktop = true
        
        // Validation should detect the error
        val isValid = state.projectNameError == null && 
                     state.projectPathError == null && 
                     state.projectIdError == null &&
                     !state.hasNoTargets
        
        assertFalse(isValid, "Form should be invalid when projectPathError is set")
    }

    @Test
    fun `validation should detect projectId errors`() {
        val state = WizardState()
        
        // Set up state with ID error
        state.projectName = "MyProject"
        state.projectId = "invalid package"
        state.projectIdError = "Invalid package name format"
        state.desktop = true
        
        // Validation should detect the error
        val isValid = state.projectNameError == null && 
                     state.projectPathError == null && 
                     state.projectIdError == null &&
                     !state.hasNoTargets
        
        assertFalse(isValid, "Form should be invalid when projectIdError is set")
    }

    @Test
    fun `validation should handle multiple errors`() {
        val state = WizardState()
        
        // Set up state with multiple errors
        state.projectName = ""
        state.projectNameError = "Project name cannot be empty"
        state.projectPath = "/invalid/path"
        state.projectPathError = "Path is not writable"
        state.projectId = "invalid"
        state.projectIdError = "Invalid package name"
        // No platforms selected
        
        // Validation should detect all errors
        val hasErrors = state.projectNameError != null || 
                       state.projectPathError != null || 
                       state.projectIdError != null ||
                       state.hasNoTargets
        
        assertTrue(hasErrors, "Form should detect multiple errors")
        
        val isValid = state.projectNameError == null && 
                     state.projectPathError == null && 
                     state.projectIdError == null &&
                     !state.hasNoTargets
        
        assertFalse(isValid, "Form should be invalid when multiple errors exist")
    }

    @Test
    fun `validation state should persist across wizard reopening`() {
        // Simulate wizard with error
        val state1 = WizardState()
        state1.projectName = ""
        state1.projectNameError = "Project name cannot be empty"
        state1.desktop = true
        
        val isValid1 = state1.projectNameError == null && 
                      state1.projectPathError == null && 
                      state1.projectIdError == null &&
                      !state1.hasNoTargets
        
        assertFalse(isValid1, "Initial state should be invalid")
        
        // Simulate reopening wizard with same values
        val state2 = WizardState()
        state2.projectName = ""  // Same empty name
        state2.projectNameError = "Project name cannot be empty"  // Error should be set
        state2.desktop = true
        
        val isValid2 = state2.projectNameError == null && 
                      state2.projectPathError == null && 
                      state2.projectIdError == null &&
                      !state2.hasNoTargets
        
        assertFalse(isValid2, "Reopened wizard should still be invalid")
    }
}

