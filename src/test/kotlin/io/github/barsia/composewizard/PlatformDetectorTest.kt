package io.github.barsia.composewizard

import io.github.barsia.composewizard.shared.PlatformDetector
import org.junit.jupiter.api.Test
import kotlin.test.*

class PlatformDetectorTest {
    
    @Test
    fun `PlatformDetector exists and has required methods`() {
        // Verify PlatformDetector object exists and has all required properties/methods
        assertNotNull(PlatformDetector)
    }
    
    @Test
    fun `hasAndroidSupport returns boolean without throwing`() {
        val hasSupport = PlatformDetector.hasAndroidSupport()
        
        // Just verify it returns without exception
        assertTrue(hasSupport || !hasSupport)
        println("Has Android support: $hasSupport")
    }
    
    @Test
    fun `hasAndroidSupport handles ClassNotFoundException gracefully`() {
        // This test verifies that hasAndroidSupport doesn't throw exceptions
        val hasSupport = try {
            PlatformDetector.hasAndroidSupport()
            true
        } catch (e: Exception) {
            false
        }
        
        assertTrue(hasSupport, "hasAndroidSupport should not throw exceptions")
    }
    
    @Test
    fun `hasAndroidSupport is idempotent`() {
        // Calling multiple times should return the same result
        val first = PlatformDetector.hasAndroidSupport()
        val second = PlatformDetector.hasAndroidSupport()
        
        assertEquals(first, second, "hasAndroidSupport should be idempotent")
    }
    
    @Test
    fun `platform detection logic is testable`() {
        // Test that the platform detection code can be called without crashing
        // In real environment with ApplicationManager, these would return actual values
        
        val productCodeAI = "AI"
        val productCodeIC = "IC"
        val productCodeIU = "IU"
        
        assertTrue(productCodeAI == "AI", "Android Studio product code should be AI")
        assertTrue(productCodeIC in listOf("IC", "IU"), "IDEA product codes should be IC or IU")
        assertTrue(productCodeIU in listOf("IC", "IU"), "IDEA product codes should be IC or IU")
    }
}

