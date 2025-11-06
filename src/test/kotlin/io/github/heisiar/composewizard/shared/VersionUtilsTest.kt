package io.github.heisiar.composewizard.shared

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionUtilsTest {
    
    @Test
    fun `compareVersions should handle equal versions`() {
        assertEquals(0, VersionUtils.compareVersions("1.7.0", "1.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.10.0", "1.10.0"))
        assertEquals(0, VersionUtils.compareVersions("2.0.0", "2.0.0"))
    }
    
    @Test
    fun `compareVersions should handle major version differences`() {
        assertTrue(VersionUtils.compareVersions("2.0.0", "1.10.0") > 0)
        assertTrue(VersionUtils.compareVersions("1.10.0", "2.0.0") < 0)
    }
    
    @Test
    fun `compareVersions should handle minor version differences`() {
        assertTrue(VersionUtils.compareVersions("1.10.0", "1.7.0") > 0)
        assertTrue(VersionUtils.compareVersions("1.7.0", "1.10.0") < 0)
        assertTrue(VersionUtils.compareVersions("1.9.0", "1.10.0") < 0)
    }
    
    @Test
    fun `compareVersions should handle patch version differences`() {
        assertTrue(VersionUtils.compareVersions("1.7.1", "1.7.0") > 0)
        assertTrue(VersionUtils.compareVersions("1.7.0", "1.7.1") < 0)
    }
    
    @Test
    fun `compareVersions should handle versions with suffixes`() {
        // Suffixes should be ignored for comparison
        assertEquals(0, VersionUtils.compareVersions("1.7.0-alpha01", "1.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.7.0-beta01", "1.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.7.0-rc01", "1.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.7.0-dev1234", "1.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.7.0-alpha01", "1.7.0-beta01"))
        assertEquals(0, VersionUtils.compareVersions("1.7.0-dev1234", "1.7.0-rc01"))
        assertTrue(VersionUtils.compareVersions("1.10.0-beta01", "1.7.0-rc01") > 0)
        assertTrue(VersionUtils.compareVersions("1.10.0-alpha01", "1.7.0-rc01") > 0)
    }
    
    @Test
    fun `isVersionLessThan should work correctly`() {
        assertTrue(VersionUtils.isVersionLessThan("1.7.0", "1.10.0"))
        assertTrue(VersionUtils.isVersionLessThan("1.9.0", "1.10.0"))
        assertFalse(VersionUtils.isVersionLessThan("1.10.0", "1.10.0"))
        assertFalse(VersionUtils.isVersionLessThan("1.11.0", "1.10.0"))
        assertFalse(VersionUtils.isVersionLessThan("2.0.0", "1.10.0"))
    }
    
    @Test
    fun `isVersionGreaterOrEqual should work correctly`() {
        assertTrue(VersionUtils.isVersionGreaterOrEqual("1.10.0", "1.10.0"))
        assertTrue(VersionUtils.isVersionGreaterOrEqual("1.11.0", "1.10.0"))
        assertTrue(VersionUtils.isVersionGreaterOrEqual("2.0.0", "1.10.0"))
        assertFalse(VersionUtils.isVersionGreaterOrEqual("1.7.0", "1.10.0"))
        assertFalse(VersionUtils.isVersionGreaterOrEqual("1.9.0", "1.10.0"))
    }
    
    @Test
    fun `needsHotReloadPlugin should return true for versions less than 1_10_0`() {
        // Hot Reload is needed as separate plugin for old versions
        assertTrue(VersionUtils.needsHotReloadPlugin("1.7.0"))
        assertTrue(VersionUtils.needsHotReloadPlugin("1.7.1"))
        assertTrue(VersionUtils.needsHotReloadPlugin("1.8.0"))
        assertTrue(VersionUtils.needsHotReloadPlugin("1.9.0"))
        assertTrue(VersionUtils.needsHotReloadPlugin("1.9.99"))
        assertTrue(VersionUtils.needsHotReloadPlugin("1.7.0-beta01"))
        assertTrue(VersionUtils.needsHotReloadPlugin("1.9.0-dev1234"))
    }
    
    @Test
    fun `needsHotReloadPlugin should return false for versions 1_10_0 and above`() {
        // Hot Reload is built-in for Compose 1.10.0+
        assertFalse(VersionUtils.needsHotReloadPlugin("1.10.0"))
        assertFalse(VersionUtils.needsHotReloadPlugin("1.10.1"))
        assertFalse(VersionUtils.needsHotReloadPlugin("1.11.0"))
        assertFalse(VersionUtils.needsHotReloadPlugin("2.0.0"))
        assertFalse(VersionUtils.needsHotReloadPlugin("1.10.0-beta01"))
        assertFalse(VersionUtils.needsHotReloadPlugin("2.0.0-dev1234"))
    }
    
    @Test
    fun `compareVersions should handle different length versions`() {
        assertEquals(0, VersionUtils.compareVersions("1.7", "1.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.7.0", "1.7"))
        assertTrue(VersionUtils.compareVersions("1.7.1", "1.7") > 0)
        assertTrue(VersionUtils.compareVersions("1.7", "1.7.1") < 0)
    }
    
    @Test
    fun `compareVersions should handle edge cases`() {
        // Single digit versions
        assertTrue(VersionUtils.compareVersions("2", "1") > 0)
        assertTrue(VersionUtils.compareVersions("1", "2") < 0)
        
        // Many parts
        assertTrue(VersionUtils.compareVersions("1.7.0.1", "1.7.0.0") > 0)
    }
}

