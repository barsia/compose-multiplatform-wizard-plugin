package io.github.heisiar.composewizard.shared.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class Material3VersionServiceTest {

    private val service = Material3VersionService()

    @Test
    fun `service extends LibraryVersionService`() {
        assertTrue(service is LibraryVersionService, "Material3VersionService should extend LibraryVersionService")
    }

    @Test
    fun `fetchVersions returns Material3 versions from Maven Central`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch Material3 versions")
    }

    @Test
    fun `fetched versions follow semver format`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        val semverPattern = Regex("\\d+\\.\\d+\\.\\d+.*")
        assertTrue(
            versions.all { it.matches(semverPattern) },
            "All Material3 versions should follow semver format"
        )
    }

    @Test
    fun `service can be instantiated`() {
        val instance = Material3VersionService()
        assertTrue(instance is LibraryVersionService)
    }
}


