package io.github.heisiar.composewizard.generator

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Hot Reload plugin inclusion based on Compose version
 */
class HotReloadVersionTest {
    
    @Test
    fun `Hot Reload should be included for Compose 1_7_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertTrue(config.needsHotReloadPlugin, "Hot Reload should be included for Compose 1.7.0")
    }
    
    @Test
    fun `Hot Reload should be included for Compose 1_9_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.9.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertTrue(config.needsHotReloadPlugin, "Hot Reload should be included for Compose 1.9.0")
    }
    
    @Test
    fun `Hot Reload should NOT be included for Compose 1_10_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Compose 1.10.0 (built-in)")
    }
    
    @Test
    fun `Hot Reload should NOT be included for Compose 1_11_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.11.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Compose 1.11.0 (built-in)")
    }
    
    @Test
    fun `Hot Reload should NOT be included for Compose 2_0_0 with Desktop`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "2.0.0",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Compose 2.0.0 (built-in)")
    }
    
    @Test
    fun `Hot Reload should NOT be included for Android-only project even with old Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = false
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Android-only project")
    }
    
    @Test
    fun `Hot Reload should be included for Desktop+Android project with old Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = true
        )
        
        assertTrue(config.needsHotReloadPlugin, "Hot Reload should be included for Desktop+Android project with Compose 1.7.0")
    }
    
    @Test
    fun `Hot Reload should NOT be included for Desktop+Android project with new Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Desktop+Android project with Compose 1.10.0 (built-in)")
    }
    
    @Test
    fun `Hot Reload should NOT be included for Android+iOS project even with old Compose`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0",
            kotlinVersion = "2.1.0",
            targetAndroid = true,
            targetIOS = true,
            targetDesktop = false
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Android+iOS project (no Desktop)")
    }
    
    @Test
    fun `Hot Reload should handle alpha versions correctly - old version`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0-alpha01",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertTrue(config.needsHotReloadPlugin, "Hot Reload should be included for Compose 1.7.0-alpha01")
    }
    
    @Test
    fun `Hot Reload should handle alpha versions correctly - new version`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0-alpha01",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Compose 1.10.0-alpha01 (built-in)")
    }
    
    @Test
    fun `Hot Reload should handle beta versions correctly - old version`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.7.0-beta01",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertTrue(config.needsHotReloadPlugin, "Hot Reload should be included for Compose 1.7.0-beta01")
    }
    
    @Test
    fun `Hot Reload should handle beta versions correctly - new version`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0-beta01",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Compose 1.10.0-beta01 (built-in)")
    }
    
    @Test
    fun `Hot Reload should handle rc versions correctly - old version`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.9.0-rc01",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertTrue(config.needsHotReloadPlugin, "Hot Reload should be included for Compose 1.9.0-rc01")
    }
    
    @Test
    fun `Hot Reload should handle rc versions correctly - new version`() {
        val config = ProjectConfig(
            projectName = "TestApp",
            projectId = "com.test.app",
            composeVersion = "1.10.0-rc01",
            kotlinVersion = "2.1.0",
            targetDesktop = true
        )
        
        assertFalse(config.needsHotReloadPlugin, "Hot Reload should NOT be included for Compose 1.10.0-rc01 (built-in)")
    }
}

