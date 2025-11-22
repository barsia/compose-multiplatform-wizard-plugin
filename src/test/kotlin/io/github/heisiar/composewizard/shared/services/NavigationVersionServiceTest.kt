package io.github.heisiar.composewizard.shared.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NavigationVersionServiceTest {

    private val service = NavigationVersionService()

    @Test
    fun `service extends LibraryVersionService`() {
        assertTrue(service is LibraryVersionService, "NavigationVersionService should extend LibraryVersionService")
    }

    @Test
    fun `fetchVersions returns Navigation versions from Maven Central`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation/navigation-compose/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch Navigation versions")
    }

    @Test
    fun `fetched versions follow semver format`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation/navigation-compose/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        val semverPattern = Regex("\\d+\\.\\d+\\.\\d+.*")
        assertTrue(
            versions.all { it.matches(semverPattern) },
            "All Navigation versions should follow semver format"
        )
    }

    @Test
    fun `service can be instantiated`() {
        val instance = NavigationVersionService()
        assertTrue(instance is LibraryVersionService)
    }
}


