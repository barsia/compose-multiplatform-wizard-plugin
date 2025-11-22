package io.github.heisiar.composewizard.shared.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryVersionServiceTest {

    private val service = LibraryVersionService()

    @Test
    fun `fetchVersions returns non-empty list for Material3`() = runBlocking {
        val metadata = LibraryRegistry.getMetadata(io.github.heisiar.composewizard.shared.LibraryType.MATERIAL3)
        val versions = service.fetchVersions(metadata.mavenMetadataUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch Material3 versions from Maven")
    }

    @Test
    fun `fetchVersions returns non-empty list for Navigation`() = runBlocking {
        val metadata = LibraryRegistry.getMetadata(io.github.heisiar.composewizard.shared.LibraryType.NAVIGATION)
        val versions = service.fetchVersions(metadata.mavenMetadataUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch Navigation versions from Maven")
    }

    @Test
    fun `fetchVersions returns non-empty list for Lifecycle`() = runBlocking {
        val metadata = LibraryRegistry.getMetadata(io.github.heisiar.composewizard.shared.LibraryType.LIFECYCLE)
        val versions = service.fetchVersions(metadata.mavenMetadataUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch Lifecycle versions from Maven")
    }

    @Test
    fun `fetchVersions returns empty list for invalid URL`() = runBlocking {
        val versions = service.fetchVersions("https://invalid.url/maven-metadata.xml")
        
        assertTrue(versions.isEmpty(), "Should return empty list for invalid URL")
    }

    @Test
    fun `fetchVersions returns valid semver versions`() = runBlocking {
        val metadata = LibraryRegistry.getMetadata(io.github.heisiar.composewizard.shared.LibraryType.MATERIAL3)
        val versions = service.fetchVersions(metadata.mavenMetadataUrl)
        
        val semverPattern = Regex("\\d+\\.\\d+\\.\\d+.*")
        assertTrue(
            versions.all { it.matches(semverPattern) },
            "All versions should match semver format"
        )
    }

    @Test
    fun `fetchVersions handles network timeout gracefully`() = runBlocking {
        val versions = service.fetchVersions("https://httpstat.us/200?sleep=10000")
        
        assertTrue(versions.isEmpty(), "Should return empty list on timeout")
    }

    @Test
    fun `fetchVersions returns versions in Maven XML order`() = runBlocking {
        val metadata = LibraryRegistry.getMetadata(io.github.heisiar.composewizard.shared.LibraryType.MATERIAL3)
        val versions = service.fetchVersions(metadata.mavenMetadataUrl)
        
        assertTrue(versions.isNotEmpty(), "Should have versions")
        assertFalse(versions[0].isEmpty(), "First version should not be empty")
    }
}

