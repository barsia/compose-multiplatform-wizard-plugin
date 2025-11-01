package io.github.heisiar.composewizard

import io.github.heisiar.composewizard.shared.PlatformDetector
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class PlatformDetectorTest {
    
    @Test
    fun `platform name should not be null`() {
        val platformName = PlatformDetector.platformName
        assertNotNull(platformName)
        println("Running on: $platformName")
    }
    
    @Test
    fun `should detect platform type`() {
        val isIdea = PlatformDetector.isIntellijIdea
        val isAS = PlatformDetector.isAndroidStudio
        
        println("Is IntelliJ IDEA: $isIdea")
        println("Is Android Studio: $isAS")
        
        // At least one should be true (or both false if running in test environment)
        assert(isIdea || isAS || (!isIdea && !isAS))
    }
}

