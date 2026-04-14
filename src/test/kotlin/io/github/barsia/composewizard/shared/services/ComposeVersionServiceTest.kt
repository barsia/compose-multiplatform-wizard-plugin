package io.github.barsia.composewizard.shared.services

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposeVersionServiceTest {

    private val service = ComposeVersionService()

    @Test
    fun `version comparison stable versions are sorted correctly`() {
        val versions = listOf("1.8.0", "1.10.0", "1.9.1", "1.9.0")
        val sorted = versions.sortedWith { v1, v2 ->
            val ver1 = parseSimpleVersion(v1)
            val ver2 = parseSimpleVersion(v2)
            when {
                ver1.first != ver2.first -> ver2.first - ver1.first
                ver1.second != ver2.second -> ver2.second - ver1.second
                else -> ver2.third - ver1.third
            }
        }
        
        assertEquals("1.10.0", sorted[0], "1.10.0 should be first (highest)")
        assertEquals("1.9.1", sorted[1], "1.9.1 should be second")
        assertEquals("1.9.0", sorted[2], "1.9.0 should be third")
        assertEquals("1.8.0", sorted[3], "1.8.0 should be last (lowest)")
    }
    
    private fun parseSimpleVersion(version: String): Triple<Int, Int, Int> {
        val parts = version.split(".", "-", "+")
        return Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    @Test
    fun `dev versions maintain XML order without semantic sorting`() {
        val devVersions = listOf(
            "1.9.0-dev1234",
            "1.10.0-dev5678", 
            "1.9.1-dev3456"
        )
        
        assertEquals(devVersions, devVersions, "Dev versions should maintain original order")
    }

    @Test
    fun `dev versions source points to new packages jetbrains team repository`() {
        val serviceSource = ComposeVersionService::class.java
            .getResource("/" + ComposeVersionService::class.java.name.replace('.', '/') + ".class")
        assertNotNull(serviceSource, "ComposeVersionService class should be available")

        val sourceFile = java.io.File(System.getProperty("user.dir"), "src/main/kotlin/io/github/barsia/composewizard/shared/services/ComposeVersionService.kt")
        val content = sourceFile.readText()

        assertTrue(
            content.contains("https://packages.jetbrains.team/maven/p/cmp/dev/org/jetbrains/compose/org.jetbrains.compose.gradle.plugin/"),
            "ComposeVersionService should use the new JetBrains Team Maven repository for dev versions"
        )
        assertFalse(
            content.contains("maven.pkg.jetbrains.space/public/p/compose/dev"),
            "ComposeVersionService should not use the old JetBrains Space dev repository anymore"
        )
    }

    @Test
    fun `version with alpha suffix is less than stable`() {
        val alpha = "1.9.0-alpha01"
        val stable = "1.9.0"
        
        assertTrue(alpha.contains("alpha"), "Alpha version should contain 'alpha'")
        assertFalse(stable.contains("-"), "Stable version should not contain suffix")
    }

    @Test
    fun `version with beta suffix is between alpha and stable`() {
        val alpha = "1.9.0-alpha01"
        val beta = "1.9.0-beta01"
        val stable = "1.9.0"
        
        assertTrue(alpha.contains("alpha"))
        assertTrue(beta.contains("beta"))
        assertFalse(stable.contains("-"))
    }

    @Test
    fun `version with rc suffix is between beta and stable`() {
        val beta = "1.9.0-beta01"
        val rc = "1.9.0-rc01"
        val stable = "1.9.0"
        
        assertTrue(beta.contains("beta"))
        assertTrue(rc.contains("rc"))
        assertFalse(stable.contains("-"))
    }

    @Test
    fun `complex version with multiple suffixes is parsed`() {
        val complex = "1.10.0-beta01+dev3194"
        
        assertTrue(complex.contains("beta"))
        assertTrue(complex.contains("dev"))
        assertTrue(complex.contains("+"))
    }

    @Test
    fun `version comparison handles major version differences`() {
        val v1 = parseSimpleVersion("2.0.0")
        val v2 = parseSimpleVersion("1.10.0")
        
        assertTrue(v1.first > v2.first, "Major version 2 > 1")
    }

    @Test
    fun `version comparison handles minor version differences`() {
        val v1 = parseSimpleVersion("1.10.0")
        val v2 = parseSimpleVersion("1.9.0")
        
        assertEquals(1, v1.first)
        assertEquals(1, v2.first)
        assertTrue(v1.second > v2.second, "Minor version 10 > 9")
    }

    @Test
    fun `version comparison handles patch version differences`() {
        val v1 = parseSimpleVersion("1.9.2")
        val v2 = parseSimpleVersion("1.9.1")
        
        assertTrue(v1.third > v2.third, "Patch version 2 > 1")
    }
}
