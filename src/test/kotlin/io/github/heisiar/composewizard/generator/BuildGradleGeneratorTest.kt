package io.github.heisiar.composewizard.generator

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildGradleGeneratorTest {
    
    private val generator = BuildGradleGenerator()
    
    @Test
    fun `root build gradle should include Hot Reload for Compose 1_7_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generateRootBuildGradle(config)
        
        assertTrue(result.contains("composeHotReload"), "Root build.gradle should include composeHotReload for Compose 1.7.0")
    }
    
    @Test
    fun `root build gradle should NOT include Hot Reload for Compose 1_10_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generateRootBuildGradle(config)
        
        assertFalse(result.contains("composeHotReload"), "Root build.gradle should NOT include composeHotReload for Compose 1.10.0")
    }
    
    @Test
    fun `composeApp build gradle should include Hot Reload plugin for Compose 1_7_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generateComposeAppBuildGradle(config)
        
        assertTrue(result.contains("alias(libs.plugins.composeHotReload)"), 
            "composeApp build.gradle should include composeHotReload plugin for Compose 1.7.0")
    }
    
    @Test
    fun `composeApp build gradle should NOT include Hot Reload plugin for Compose 1_10_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generateComposeAppBuildGradle(config)
        
        assertFalse(result.contains("alias(libs.plugins.composeHotReload)"), 
            "composeApp build.gradle should NOT include composeHotReload plugin for Compose 1.10.0")
    }
    
    @Test
    fun `composeApp build gradle should NOT include Hot Reload for Android-only project`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = false
        )
        
        val result = generator.generateComposeAppBuildGradle(config)
        
        assertFalse(result.contains("composeHotReload"), 
            "composeApp build.gradle should NOT include composeHotReload for Android-only project")
    }
    
    @Test
    fun `build gradle should include Hot Reload for Desktop+Android with old Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.9.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = true
        )
        
        val rootResult = generator.generateRootBuildGradle(config)
        val appResult = generator.generateComposeAppBuildGradle(config)
        
        assertTrue(rootResult.contains("composeHotReload"), 
            "Root build.gradle should include composeHotReload for Desktop+Android with Compose 1.9.0")
        assertTrue(appResult.contains("alias(libs.plugins.composeHotReload)"), 
            "composeApp build.gradle should include composeHotReload plugin for Desktop+Android with Compose 1.9.0")
    }
    
    @Test
    fun `build gradle should NOT include Hot Reload for Desktop+Android with new Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = true
        )
        
        val rootResult = generator.generateRootBuildGradle(config)
        val appResult = generator.generateComposeAppBuildGradle(config)
        
        assertFalse(rootResult.contains("composeHotReload"), 
            "Root build.gradle should NOT include composeHotReload for Desktop+Android with Compose 1.10.0")
        assertFalse(appResult.contains("alias(libs.plugins.composeHotReload)"), 
            "composeApp build.gradle should NOT include composeHotReload plugin for Desktop+Android with Compose 1.10.0")
    }
    
    @Test
    fun `build gradle should NOT include Hot Reload for Android+iOS project`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.9.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetIOS = true,
            targetDesktop = false
        )
        
        val rootResult = generator.generateRootBuildGradle(config)
        val appResult = generator.generateComposeAppBuildGradle(config)
        
        assertFalse(rootResult.contains("composeHotReload"), 
            "Root build.gradle should NOT include composeHotReload for Android+iOS project (no Desktop)")
        assertFalse(appResult.contains("alias(libs.plugins.composeHotReload)"), 
            "composeApp build.gradle should NOT include composeHotReload plugin for Android+iOS project (no Desktop)")
    }
}

