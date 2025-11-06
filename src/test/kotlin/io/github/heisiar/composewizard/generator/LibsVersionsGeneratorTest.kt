package io.github.heisiar.composewizard.generator

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibsVersionsGeneratorTest {
    
    private val generator = LibsVersionsGenerator()
    
    @Test
    fun `libs versions should include Hot Reload version for Compose 1_7_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generate(config)
        
        assertTrue(result.contains("composeHotReload = "), 
            "libs.versions.toml should include composeHotReload version for Compose 1.7.0")
        assertTrue(result.contains("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\""), 
            "libs.versions.toml should include composeHotReload plugin for Compose 1.7.0")
    }
    
    @Test
    fun `libs versions should NOT include Hot Reload for Compose 1_10_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generate(config)
        
        assertFalse(result.contains("composeHotReload = "), 
            "libs.versions.toml should NOT include composeHotReload version for Compose 1.10.0")
        assertFalse(result.contains("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\""), 
            "libs.versions.toml should NOT include composeHotReload plugin for Compose 1.10.0")
    }
    
    @Test
    fun `libs versions should NOT include Hot Reload for Compose 2_0_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "2.0.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generate(config)
        
        assertFalse(result.contains("composeHotReload"), 
            "libs.versions.toml should NOT include composeHotReload for Compose 2.0.0")
    }
    
    @Test
    fun `libs versions should NOT include Hot Reload for Android-only project`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = false
        )
        
        val result = generator.generate(config)
        
        assertFalse(result.contains("composeHotReload"), 
            "libs.versions.toml should NOT include composeHotReload for Android-only project")
    }
    
    @Test
    fun `libs versions should include Hot Reload for Desktop+Android with old Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.9.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = true
        )
        
        val result = generator.generate(config)
        
        assertTrue(result.contains("composeHotReload = "), 
            "libs.versions.toml should include composeHotReload version for Desktop+Android with Compose 1.9.0")
        assertTrue(result.contains("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\""), 
            "libs.versions.toml should include composeHotReload plugin for Desktop+Android with Compose 1.9.0")
    }
    
    @Test
    fun `libs versions should NOT include Hot Reload for Desktop+Android with new Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.11.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = true
        )
        
        val result = generator.generate(config)
        
        assertFalse(result.contains("composeHotReload"), 
            "libs.versions.toml should NOT include composeHotReload for Desktop+Android with Compose 1.11.0")
    }
    
    @Test
    fun `libs versions should NOT include Hot Reload for Android+iOS project`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.9.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetIOS = true,
            targetDesktop = false
        )
        
        val result = generator.generate(config)
        
        assertFalse(result.contains("composeHotReload"), 
            "libs.versions.toml should NOT include composeHotReload for Android+iOS project (no Desktop)")
    }
    
    @Test
    fun `libs versions should always include compose and kotlin versions`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        val result = generator.generate(config)
        
        assertTrue(result.contains("composeMultiplatform = \"1.10.0\""), 
            "libs.versions.toml should include compose version")
        assertTrue(result.contains("kotlin = \"2.1.0\""), 
            "libs.versions.toml should include kotlin version")
    }
}

