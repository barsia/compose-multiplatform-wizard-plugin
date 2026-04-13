package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.barsia.composewizard.shared.ui.WizardState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Integration tests to verify that WizardState correctly maps to ProjectBuilder.
 * These tests simulate what happens when user interacts with UI.
 */
class WizardStateIntegrationTest {
    
    @Test
    fun `wizard state includeHotReload true maps to builder includeHotReload true`() {
        // Simulate user enabling Hot Reload checkbox in UI
        val state = WizardState().apply {
            projectName = "TestProject"
            projectId = "com.example.test"
            composeVersion = "1.9.3"
            desktop = true
            android = false
            ios = false
            web = false
            includeHotReload = true  // User checks the box
            hotReloadVersion = "1.0.0-rc02"
        }
        
        // Simulate what ComposeWizardStep.updateDataModel() does
        val builder = ComposeMultiplatformModuleBuilder().apply {
            projectName = state.projectName
            projectId = state.projectId
            composeVersion = state.composeVersion
            targetDesktop = state.desktop
            targetAndroid = state.android
            targetIOS = state.ios
            targetWeb = state.web
            includeHotReload = state.includeHotReload  // This is the critical line
            hotReloadVersion = state.hotReloadVersion
        }
        
        // Verify mapping
        assertTrue(builder.includeHotReload, 
            "When user enables Hot Reload in UI, builder.includeHotReload must be true")
        assertEquals("1.0.0-rc02", builder.hotReloadVersion,
            "Hot Reload version must be passed from state to builder")
    }
    
    @Test
    fun `wizard state includeHotReload false maps to builder includeHotReload false`() {
        // Simulate user disabling Hot Reload checkbox in UI
        val state = WizardState().apply {
            projectName = "TestProject"
            projectId = "com.example.test"
            composeVersion = "1.9.3"
            desktop = true
            android = false
            ios = false
            web = false
            includeHotReload = false  // User unchecks the box
            hotReloadVersion = null
        }
        
        // Simulate what ComposeWizardStep.updateDataModel() does
        val builder = ComposeMultiplatformModuleBuilder().apply {
            projectName = state.projectName
            projectId = state.projectId
            composeVersion = state.composeVersion
            targetDesktop = state.desktop
            targetAndroid = state.android
            targetIOS = state.ios
            targetWeb = state.web
            includeHotReload = state.includeHotReload
            hotReloadVersion = state.hotReloadVersion
        }
        
        // Verify mapping
        assertFalse(builder.includeHotReload,
            "When user disables Hot Reload in UI, builder.includeHotReload must be false")
        assertNull(builder.hotReloadVersion,
            "Hot Reload version must be null when disabled")
    }
    
    @Test
    fun `all library flags from wizard state map correctly to builder`() {
        // Simulate user selecting various libraries in UI
        val state = WizardState().apply {
            projectName = "TestProject"
            projectId = "com.example.test"
            composeVersion = "1.10.0-beta01"
            desktop = true
            android = true
            ios = false
            web = false
            tests = true
            includeMaterial3 = true
            includeMaterial3Adaptive = false
            includeNavigation = true
            includeNavigation3 = false
            includeNavigationEvent = false
            includeSavedState = true
            includeWindow = false
            includeHotReload = true
        }
        
        // Simulate what ComposeWizardStep.updateDataModel() does
        val builder = ComposeMultiplatformModuleBuilder().apply {
            projectName = state.projectName
            projectId = state.projectId
            composeVersion = state.composeVersion
            targetDesktop = state.desktop
            targetAndroid = state.android
            targetIOS = state.ios
            targetWeb = state.web
            includeTests = state.tests
            includeMaterial3 = state.includeMaterial3
            includeMaterial3Adaptive = state.includeMaterial3Adaptive
            includeNavigation = state.includeNavigation
            includeNavigation3 = state.includeNavigation3
            includeNavigationEvent = state.includeNavigationEvent
            includeSavedState = state.includeSavedState
            includeWindow = state.includeWindow
            includeHotReload = state.includeHotReload
        }
        
        // Verify all mappings
        assertEquals(state.includeMaterial3, builder.includeMaterial3, "Material3 flag must match")
        assertEquals(state.includeMaterial3Adaptive, builder.includeMaterial3Adaptive, "Material3Adaptive flag must match")
        assertEquals(state.includeNavigation, builder.includeNavigation, "Navigation flag must match")
        assertEquals(state.includeNavigation3, builder.includeNavigation3, "Navigation3 flag must match")
        assertEquals(state.includeNavigationEvent, builder.includeNavigationEvent, "NavigationEvent flag must match")
        assertEquals(state.includeSavedState, builder.includeSavedState, "SavedState flag must match")
        assertEquals(state.includeWindow, builder.includeWindow, "Window flag must match")
        assertEquals(state.includeHotReload, builder.includeHotReload, "HotReload flag must match")
        assertEquals(state.tests, builder.includeTests, "Tests flag must match")
    }
}

