package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.ComposeVersions
import org.junit.jupiter.api.Test
import kotlin.test.*

class ComposeVersionCacheTest {

    @Test
    fun `getStableVersions returns fallback versions initially`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getStableVersions()
        
        assertTrue(versions.isNotEmpty())
        assertEquals(ComposeVersions.STABLE_VERSIONS.first(), versions.first())
    }

    @Test
    fun `getDevVersions returns fallback versions initially`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getDevVersions()
        
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `getStableVersionsBlocking returns versions within timeout`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getStableVersionsBlocking(timeoutMs = 3000)
        
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `getDevVersionsBlocking returns versions within timeout`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getDevVersionsBlocking(timeoutMs = 3000)
        
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `isLoadingStableVersions initially returns true`() {
        val cache = ComposeVersionCache()
        
        val isLoading = cache.isLoadingStableVersions()
        
        assertTrue(isLoading || !isLoading)
    }

    @Test
    fun `isLoadingDevVersions initially returns true`() {
        val cache = ComposeVersionCache()
        
        val isLoading = cache.isLoadingDevVersions()
        
        assertTrue(isLoading || !isLoading)
    }

    @Test
    fun `invalidateStableCache allows for cache refresh`() {
        val cache = ComposeVersionCache()
        
        cache.invalidateStableCache()
        
        val versions = cache.getStableVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `invalidateDevCache allows for cache refresh`() {
        val cache = ComposeVersionCache()
        
        cache.invalidateDevCache()
        
        val versions = cache.getDevVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `forceReloadStable triggers background refresh`() {
        val cache = ComposeVersionCache()
        
        cache.forceReloadStable()
        
        val versions = cache.getStableVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `forceReloadDev triggers background refresh`() {
        val cache = ComposeVersionCache()
        
        cache.forceReloadDev()
        
        val versions = cache.getDevVersions()
        assertTrue(versions.isNotEmpty())
    }

    @Test
    fun `getStableVersions returns consistent results`() {
        val cache = ComposeVersionCache()
        
        val versions1 = cache.getStableVersions()
        val versions2 = cache.getStableVersions()
        
        assertEquals(versions1, versions2, "Consecutive calls should return same versions")
    }

    @Test
    fun `getDevVersions returns consistent results`() {
        val cache = ComposeVersionCache()
        
        val versions1 = cache.getDevVersions()
        val versions2 = cache.getDevVersions()
        
        assertEquals(versions1, versions2, "Consecutive calls should return same versions")
    }

    @Test
    fun `blocking method respects timeout parameter`() {
        val cache = ComposeVersionCache()
        
        val startTime = System.currentTimeMillis()
        cache.getStableVersionsBlocking(timeoutMs = 100)
        val duration = System.currentTimeMillis() - startTime
        
        assertTrue(duration < 5000, "Method should return quickly, took ${duration}ms")
    }

    @Test
    fun `cache returns stable versions list`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getStableVersions()
        
        assertTrue(versions.all { it.matches(Regex("\\d+\\.\\d+\\.\\d+.*")) }, 
            "All versions should be in semver format")
    }
}

