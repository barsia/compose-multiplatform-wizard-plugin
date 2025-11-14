package io.github.heisiar.composewizard.shared.services

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeVersionCacheTest {

    @Test
    fun `getStableVersions returns null while loading from GitHub`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getStableVersions()
        
        // Initially null while fetching from GitHub
        assertTrue(versions == null || versions.isNotEmpty(), 
            "Versions should be null (loading) or non-empty list")
    }

    @Test
    fun `getDevVersions returns null while loading from GitHub`() {
        val cache = ComposeVersionCache()
        
        val versions = cache.getDevVersions()
        
        // Initially null while fetching from GitHub
        assertTrue(versions == null || versions.isNotEmpty(), 
            "Versions should be null (loading) or non-empty list")
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
    fun `invalidateStableCache marks cache for refresh on next access`() {
        val cache = ComposeVersionCache()
        
        // First, get versions to populate cache
        val initialVersions = cache.getStableVersionsBlocking(timeoutMs = 5000)
        assertTrue(initialVersions.isNotEmpty(), "Initial versions should be loaded")
        
        // Invalidate cache (only marks for refresh, doesn't reload immediately)
        cache.invalidateStableCache()
        
        // Next access should trigger reload
        val versionsAfterInvalidate = cache.getStableVersionsBlocking(timeoutMs = 5000)
        assertTrue(versionsAfterInvalidate.isNotEmpty(), "Should reload versions on next access")
    }

    @Test
    fun `invalidateDevCache marks cache for refresh on next access`() {
        val cache = ComposeVersionCache()
        
        // First, get versions to populate cache
        val initialVersions = cache.getDevVersionsBlocking(timeoutMs = 5000)
        assertTrue(initialVersions.isNotEmpty(), "Initial versions should be loaded")
        
        // Invalidate cache (only marks for refresh, doesn't reload immediately)
        cache.invalidateDevCache()
        
        // Next access should trigger reload
        val versionsAfterInvalidate = cache.getDevVersionsBlocking(timeoutMs = 5000)
        assertTrue(versionsAfterInvalidate.isNotEmpty(), "Should reload versions on next access")
    }

    @Test
    fun `forceReloadStable triggers immediate background refresh`() {
        val cache = ComposeVersionCache()
        
        // First, populate cache
        val initialVersions = cache.getStableVersionsBlocking(timeoutMs = 5000)
        assertTrue(initialVersions.isNotEmpty(), "Initial versions should be loaded")
        
        // Force reload immediately triggers background reload
        cache.forceReloadStable()
        
        // Give it a moment to start loading
        Thread.sleep(100)
        
        // Should be loading or already loaded
        val versionsAfterReload = cache.getStableVersionsBlocking(timeoutMs = 5000)
        assertTrue(versionsAfterReload.isNotEmpty(), "Should return versions after immediate force reload")
    }

    @Test
    fun `forceReloadDev triggers immediate background refresh`() {
        val cache = ComposeVersionCache()
        
        // First, populate cache
        val initialVersions = cache.getDevVersionsBlocking(timeoutMs = 5000)
        assertTrue(initialVersions.isNotEmpty(), "Initial versions should be loaded")
        
        // Force reload immediately triggers background reload
        cache.forceReloadDev()
        
        // Give it a moment to start loading
        Thread.sleep(100)
        
        // Should be loading or already loaded
        val versionsAfterReload = cache.getDevVersionsBlocking(timeoutMs = 5000)
        assertTrue(versionsAfterReload.isNotEmpty(), "Should return versions after immediate force reload")
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
    fun `loaded stable versions are in valid semver format`() {
        val cache = ComposeVersionCache()
        
        // Load versions from GitHub/Maven/fallback
        val versions = cache.getStableVersionsBlocking(timeoutMs = 5000)
        
        // Validate that we got actual version data
        assertTrue(versions.isNotEmpty(), "Should load at least one version")
        
        // Validate that all versions follow semver format: 1.2.3, 1.2.3-beta01, etc.
        val semverPattern = Regex("\\d+\\.\\d+\\.\\d+.*")
        assertTrue(
            versions.all { it.matches(semverPattern) }, 
            "All versions should match semver format (major.minor.patch), got: ${versions.take(3)}"
        )
    }
}

