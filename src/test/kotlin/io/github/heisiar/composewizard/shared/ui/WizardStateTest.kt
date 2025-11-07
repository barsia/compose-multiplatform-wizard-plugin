package io.github.heisiar.composewizard.shared.ui

import org.junit.jupiter.api.Test
import kotlin.test.*

class WizardStateTest {

    @Test
    fun `WizardState data class exists`() {
        // Verify WizardState class exists and is accessible
        assertNotNull(WizardState::class)
    }

    
    @Test
    fun `WizardState has all required properties`() {
        // Verify WizardState has all expected properties via reflection
        val properties = WizardState::class.java.declaredFields.map { it.name }.filter { !it.startsWith("$") }
        
        val expectedProperties = listOf(
            "projectName", "projectPath", "projectId", "composeVersion",
            "targetDesktop", "targetAndroid", "targetIOS", "targetWeb",
            "initGit", "includeTests", "enableDevVersions"
        )
        
        expectedProperties.forEach { property ->
            assertTrue(properties.contains(property), "WizardState should have property: $property")
        }
    }
    
    @Test
    fun `WizardState has toMap method`() {
        val methods = WizardState::class.java.methods.map { it.name }
        assertTrue(methods.contains("toMap"), "WizardState should have toMap method")
    }
    
    @Test
    fun `WizardState has getPlatformsCount method`() {
        val methods = WizardState::class.java.methods.map { it.name }
        assertTrue(methods.contains("getPlatformsCount"), "WizardState should have getPlatformsCount method")
    }
    
    @Test
    fun `WizardState has getFullProjectPath method`() {
        val methods = WizardState::class.java.methods.map { it.name }
        assertTrue(methods.contains("getFullProjectPath"), "WizardState should have getFullProjectPath method")
    }
    
    @Test
    fun `WizardState is a Kotlin data class`() {
        // Verify it has copy method (characteristic of data classes)
        val methods = WizardState::class.java.methods.map { it.name }
        assertTrue(methods.contains("copy"), "WizardState should have copy method (data class)")
    }
}

