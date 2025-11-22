package io.github.heisiar.composewizard.shared.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class HotReloadVersionServiceTest {

    private val service = HotReloadVersionService()

    @Test
    fun `service extends LibraryVersionService`() {
        assertTrue(service is LibraryVersionService, "HotReloadVersionService should extend LibraryVersionService")
    }

    @Test
    fun `fetchVersions returns HotReload versions from Maven Central`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/hot-reload/org.jetbrains.compose.hot-reload.gradle.plugin/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        assertTrue(versions.isNotEmpty(), "Should fetch HotReload versions")
    }

    @Test
    fun `fetched versions follow semver format`() = runBlocking {
        val mavenUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/hot-reload/org.jetbrains.compose.hot-reload.gradle.plugin/maven-metadata.xml"
        val versions = service.fetchVersions(mavenUrl)
        
        val semverPattern = Regex("\\d+\\.\\d+\\.\\d+.*")
        assertTrue(
            versions.all { it.matches(semverPattern) },
            "All HotReload versions should follow semver format"
        )
    }

    @Test
    fun `service can be instantiated`() {
        val instance = HotReloadVersionService()
        assertTrue(instance is LibraryVersionService)
    }
}


