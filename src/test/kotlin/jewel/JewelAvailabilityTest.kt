package jewel

import org.junit.Test
import org.jetbrains.jewel.foundation.theme.JewelTheme

class JewelAvailabilityTest {
    @Test
    fun testJewelAvailable() {
        // Just check if Jewel classes are accessible
        val themeClass = JewelTheme::class.java
        println("Jewel is available: ${themeClass.name}")
    }
}
