package io.github.barsia.composewizard.shared.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class LifecycleVersionServiceTest {

    private val service = LifecycleVersionService()

    @Test
    fun `service extends LibraryVersionService`() {
        assertTrue(service is LibraryVersionService, "LifecycleVersionService should extend LibraryVersionService")
    }

    @Test
    fun `fetchVersions returns Lifecycle versions from Maven Central`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch Lifecycle versions")
    }

    @Test
    fun `fetched versions follow semver format`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        val semverPattern = Regex("\\d+\\.\\d+\\.\\d+.*")
        assertTrue(
            versions.all { it.matches(semverPattern) },
            "All Lifecycle versions should follow semver format"
        )
    }

    @Test
    fun `service can be instantiated`() {
        val instance = LifecycleVersionService()
        assertTrue(instance is LibraryVersionService)
    }
}


