package io.github.barsia.composewizard.shared.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeVersionComparatorTest {

    @Test
    fun `parse stable version correctly`() {
        val version = ComposeVersionComparator.parse("1.9.1")
        
        assertEquals(1, version.major)
        assertEquals(9, version.minor)
        assertEquals(1, version.patch)
        assertEquals("zzz", version.suffix)
        assertEquals(0, version.suffixNum)
        assertEquals(0, version.devNum)
    }

    @Test
    fun `parse alpha version correctly`() {
        val version = ComposeVersionComparator.parse("1.9.0-alpha03")
        
        assertEquals(1, version.major)
        assertEquals(9, version.minor)
        assertEquals(0, version.patch)
        assertEquals("alpha", version.suffix)
        assertEquals(3, version.suffixNum)
        assertEquals(0, version.devNum)
    }

    @Test
    fun `parse beta version correctly`() {
        val version = ComposeVersionComparator.parse("1.10.0-beta01")
        
        assertEquals(1, version.major)
        assertEquals(10, version.minor)
        assertEquals(0, version.patch)
        assertEquals("beta", version.suffix)
        assertEquals(1, version.suffixNum)
        assertEquals(0, version.devNum)
    }

    @Test
    fun `parse rc version correctly`() {
        val version = ComposeVersionComparator.parse("1.9.0-rc02")
        
        assertEquals(1, version.major)
        assertEquals(9, version.minor)
        assertEquals(0, version.patch)
        assertEquals("rc", version.suffix)
        assertEquals(2, version.suffixNum)
        assertEquals(0, version.devNum)
    }

    @Test
    fun `parse dev version correctly`() {
        val version = ComposeVersionComparator.parse("1.9.0+dev2970")
        
        assertEquals(1, version.major)
        assertEquals(9, version.minor)
        assertEquals(0, version.patch)
        assertEquals("dev", version.suffix)
        assertEquals(2970, version.suffixNum)
        assertEquals(0, version.devNum)
    }

    @Test
    fun `parse beta with dev suffix correctly`() {
        val version = ComposeVersionComparator.parse("1.10.0-beta01+dev3194")
        
        assertEquals(1, version.major)
        assertEquals(10, version.minor)
        assertEquals(0, version.patch)
        assertEquals("beta", version.suffix)
        assertEquals(1, version.suffixNum)
        assertEquals(3194, version.devNum)
    }

    @Test
    fun `stable version is greater than alpha`() {
        val stable = ComposeVersionComparator.parse("1.9.0")
        val alpha = ComposeVersionComparator.parse("1.9.0-alpha03")
        
        assertTrue(stable > alpha, "Stable version should be greater than alpha")
    }

    @Test
    fun `stable version is greater than beta`() {
        val stable = ComposeVersionComparator.parse("1.9.0")
        val beta = ComposeVersionComparator.parse("1.9.0-beta01")
        
        assertTrue(stable > beta, "Stable version should be greater than beta")
    }

    @Test
    fun `stable version is greater than rc`() {
        val stable = ComposeVersionComparator.parse("1.9.0")
        val rc = ComposeVersionComparator.parse("1.9.0-rc02")
        
        assertTrue(stable > rc, "Stable version should be greater than rc")
    }

    @Test
    fun `rc is greater than beta`() {
        val rc = ComposeVersionComparator.parse("1.9.0-rc02")
        val beta = ComposeVersionComparator.parse("1.9.0-beta01")
        
        assertTrue(rc > beta, "RC version should be greater than beta")
    }

    @Test
    fun `beta is greater than alpha`() {
        val beta = ComposeVersionComparator.parse("1.9.0-beta01")
        val alpha = ComposeVersionComparator.parse("1.9.0-alpha03")
        
        assertTrue(beta > alpha, "Beta version should be greater than alpha")
    }

    @Test
    fun `higher major version wins`() {
        val v2 = ComposeVersionComparator.parse("2.0.0")
        val v1 = ComposeVersionComparator.parse("1.10.0")
        
        assertTrue(v2 > v1, "Higher major version should be greater")
    }

    @Test
    fun `higher minor version wins`() {
        val v10 = ComposeVersionComparator.parse("1.10.0")
        val v9 = ComposeVersionComparator.parse("1.9.0")
        
        assertTrue(v10 > v9, "Higher minor version should be greater")
    }

    @Test
    fun `higher patch version wins`() {
        val v2 = ComposeVersionComparator.parse("1.9.2")
        val v1 = ComposeVersionComparator.parse("1.9.1")
        
        assertTrue(v2 > v1, "Higher patch version should be greater")
    }

    @Test
    fun `higher suffix number wins for same suffix`() {
        val beta02 = ComposeVersionComparator.parse("1.9.0-beta02")
        val beta01 = ComposeVersionComparator.parse("1.9.0-beta01")
        
        assertTrue(beta02 > beta01, "Higher suffix number should be greater")
    }

    @Test
    fun `higher dev number wins for same version and suffix`() {
        val dev3194 = ComposeVersionComparator.parse("1.10.0-beta01+dev3194")
        val dev3000 = ComposeVersionComparator.parse("1.10.0-beta01+dev3000")
        
        assertTrue(dev3194 > dev3000, "Higher dev number should be greater")
    }

    @Test
    fun `version comparison is consistent`() {
        val versions = listOf(
            "1.8.0",
            "1.9.0-alpha01",
            "1.9.0-beta01",
            "1.9.0-rc01",
            "1.9.0",
            "1.9.1",
            "1.10.0-alpha01",
            "1.10.0-beta01",
            "1.10.0",
            "2.0.0"
        )
        
        val parsed = versions.map { ComposeVersionComparator.parse(it) }
        val sorted = parsed.sorted()
        
        assertEquals(parsed, sorted, "Versions should already be in ascending order")
    }

    @Test
    fun `version comparison handles edge cases`() {
        val v1 = ComposeVersionComparator.parse("1.0.0")
        val v2 = ComposeVersionComparator.parse("1.0.0")
        
        assertEquals(0, v1.compareTo(v2), "Same versions should be equal")
    }

    @Test
    fun `parse caches results`() {
        val version1 = ComposeVersionComparator.parse("1.9.1")
        val version2 = ComposeVersionComparator.parse("1.9.1")
        
        assertTrue(version1 === version2, "Parser should cache and return same instance")
    }

    @Test
    fun `parse handles invalid version gracefully`() {
        val invalid = ComposeVersionComparator.parse("invalid.version")
        
        assertEquals(0, invalid.major)
        assertEquals(0, invalid.minor)
        assertEquals(0, invalid.patch)
    }

    @Test
    fun `version with only major and minor is parsed correctly`() {
        val version = ComposeVersionComparator.parse("1.9")
        
        assertEquals(1, version.major)
        assertEquals(9, version.minor)
        assertEquals(0, version.patch)
    }

    @Test
    fun `version sorting works correctly for mixed versions`() {
        val versions = listOf(
            "1.10.0-beta01",
            "1.9.1",
            "1.10.0",
            "1.9.0-alpha03",
            "1.9.0"
        )
        
        val sorted = versions
            .map { ComposeVersionComparator.parse(it) }
            .sorted()
            .map { "${it.major}.${it.minor}.${it.patch}" + 
                   if (it.suffix != "zzz") "-${it.suffix}${it.suffixNum.toString().padStart(2, '0')}" else "" }
        
        assertEquals("1.9.0-alpha03", sorted[0])
        assertEquals("1.9.0", sorted[1])
        assertEquals("1.9.1", sorted[2])
        assertEquals("1.10.0-beta01", sorted[3])
        assertEquals("1.10.0", sorted[4])
    }

    @Test
    fun `dev versions are compared correctly`() {
        val dev1 = ComposeVersionComparator.parse("1.9.0+dev1000")
        val dev2 = ComposeVersionComparator.parse("1.9.0+dev2000")
        
        assertTrue(dev2 > dev1, "Higher dev number should be greater")
    }

    @Test
    fun `alpha with higher number is greater than alpha with lower number`() {
        val alpha10 = ComposeVersionComparator.parse("1.9.0-alpha10")
        val alpha03 = ComposeVersionComparator.parse("1.9.0-alpha03")
        
        assertTrue(alpha10 > alpha03, "alpha10 should be greater than alpha03")
    }

    @Test
    fun `version equality works correctly`() {
        val v1 = ComposeVersionComparator.parse("1.9.1")
        val v2 = ComposeVersionComparator.parse("1.9.1")
        
        assertEquals(v1, v2, "Same versions should be equal")
    }
}


