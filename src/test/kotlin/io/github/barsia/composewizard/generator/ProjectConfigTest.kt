package io.github.barsia.composewizard.generator

import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectConfigTest {

    @Test
    fun `projectIdPath is computed correctly`() {
        val config = ProjectConfig(
            projectName = "Test",
            projectId = "com.example.app",
            composeVersion = "1.6.0",
            kotlinVersion = "2.0.0"
        )
        
        assertEquals("com/example/app", config.projectIdPath)
    }
    
    @Test
    fun `projectIdPath handles single segment id`() {
        val config = ProjectConfig(
            projectName = "Test",
            projectId = "app",
            composeVersion = "1.6.0",
            kotlinVersion = "2.0.0"
        )
        
        assertEquals("app", config.projectIdPath)
    }
    
    @Test
    fun `selectedPlatforms returns correct set`() {
        val config = ProjectConfig(
            projectName = "Test",
            projectId = "app",
            composeVersion = "1.6.0",
            kotlinVersion = "2.0.0",
            targetAndroid = true,
            targetDesktop = true,
            targetIOS = false,
            targetWeb = false
        )
        
        val platforms = config.selectedPlatforms
        assertEquals(2, platforms.size)
        assertTrue(platforms.contains(Platform.ANDROID))
        assertTrue(platforms.contains(Platform.DESKTOP))
    }
    
    @Test
    fun `from builder creates correct config`() {
        val builder = mockk<ComposeMultiplatformModuleBuilder>()
        every { builder.projectName } returns "TestProject"
        every { builder.projectId } returns "com.test"
        every { builder.composeVersion } returns "1.6.0"
        every { builder.kotlinVersion } returns "2.0.0"
        every { builder.targetDesktop } returns true
        every { builder.targetAndroid } returns false
        every { builder.targetIOS } returns false
        every { builder.targetWeb } returns false
        every { builder.includeTests } returns true
        every { builder.includeAgentsMd } returns false
        every { builder.initGit } returns true
        every { builder.enableDevVersions } returns false
        every { builder.lifecycleVersion } returns null
        every { builder.material3Version } returns null
        every { builder.material3AdaptiveVersion } returns null
        every { builder.navigationVersion } returns null
        every { builder.navigation3Version } returns null
        every { builder.navigationEventVersion } returns null
        every { builder.savedStateVersion } returns null
        every { builder.windowVersion } returns null
        every { builder.hotReloadVersion } returns null
        every { builder.bundledHotReloadVersion } returns null
        every { builder.includeMaterial3 } returns false
        every { builder.includeMaterial3Adaptive } returns false
        every { builder.includeNavigation } returns false
        every { builder.includeNavigation3 } returns false
        every { builder.includeNavigationEvent } returns false
        every { builder.includeSavedState } returns false
        every { builder.includeWindow } returns false
        every { builder.includeHotReload } returns false
        
        val config = ProjectConfig.from(builder)
        
        assertEquals("TestProject", config.projectName)
        assertEquals("com.test", config.projectId)
        assertTrue(config.targetDesktop)
        assertTrue(config.includeTests)
        assertTrue(config.initGit)
    }
}
