package io.github.heisiar.composewizard.integration

import io.github.heisiar.composewizard.shared.ProjectCreator
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for Hot Reload behavior with Compose >= 1.10.0-beta01 (bundled Hot Reload)
 */
class BundledHotReloadTest {
    
    @Test
    fun `Compose 1-10-0-beta01 with DEFAULT bundled Hot Reload version should NOT override in libs-versions-toml`() {
        TemporaryProjectHelper.withTempProject("bundled-default-") { tempDir ->
            // User selects Compose 1.10.0-beta01
            // Hot Reload is bundled, default version from GitHub is loaded
            // User does NOT change dropdown → should use bundled version (no override in libs.versions.toml)
            val githubVersion = "1.0.0-rc02"
            
            val builder = ComposeMultiplatformModuleBuilder().apply {
                projectName = "BundledTest"
                projectId = "com.example.test"
                composeVersion = "1.10.0-beta01"  // >= 1.10.0-beta01 → bundled
                targetDesktop = true
                targetAndroid = false
                targetIOS = false
                targetWeb = false
                includeHotReload = true  // Always true for bundled (checkbox disabled)
                hotReloadVersion = githubVersion  // Same as bundled (user did NOT change)
                bundledHotReloadVersion = githubVersion  // GitHub version for this Compose
            }
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val rootBuildGradle = File(tempDir, "build.gradle.kts").readText()
            val composeAppBuildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            // VERIFY: Hot Reload should NOT be ANYWHERE (uses bundled version)
            assertFalse(libsVersions.contains("composeHotReload = \""),
                "❌ FAILED: User did NOT override bundled Hot Reload, but libs.versions.toml contains Hot Reload version! " +
                "When hotReloadVersion == bundledHotReloadVersion, should use bundled version without override.")
            assertFalse(libsVersions.contains("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\""),
                "❌ FAILED: User did NOT override bundled Hot Reload, but libs.versions.toml contains Hot Reload plugin!")
            assertFalse(rootBuildGradle.contains("composeHotReload"),
                "❌ FAILED: User did NOT override bundled Hot Reload, but root build.gradle.kts contains composeHotReload! " +
                "Bundled Hot Reload should not appear in any build files.")
            assertFalse(composeAppBuildGradle.contains("composeHotReload"),
                "❌ FAILED: User did NOT override bundled Hot Reload, but composeApp/build.gradle.kts contains composeHotReload! " +
                "Bundled Hot Reload should not appear in any build files.")
        }
    }
    
    @Test
    fun `Compose 1-10-0-beta01 with OVERRIDDEN Hot Reload version should add to libs-versions-toml`() {
        TemporaryProjectHelper.withTempProject("bundled-override-") { tempDir ->
            // User selects Compose 1.10.0-beta01
            // Hot Reload is bundled, default version from GitHub is "1.0.0-rc02"
            // User CHANGES dropdown to "1.0.0-rc01" → should override bundled version
            val githubVersion = "1.0.0-rc02"
            val userSelectedVersion = "1.0.0-rc01"
            
            val builder = ComposeMultiplatformModuleBuilder().apply {
                projectName = "BundledOverrideTest"
                projectId = "com.example.test"
                composeVersion = "1.10.0-beta01"  // >= 1.10.0-beta01 → bundled
                targetDesktop = true
                targetAndroid = false
                targetIOS = false
                targetWeb = false
                includeHotReload = true  // Always true for bundled (checkbox disabled)
                hotReloadVersion = userSelectedVersion  // User selected different version
                bundledHotReloadVersion = githubVersion  // GitHub version for this Compose
            }
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val rootBuildGradle = File(tempDir, "build.gradle.kts").readText()
            val composeAppBuildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            // VERIFY: Hot Reload MUST be EVERYWHERE (user override)
            assertTrue(libsVersions.contains("composeHotReload = \"$userSelectedVersion\""),
                "❌ FAILED: User overrode bundled Hot Reload with $userSelectedVersion, but libs.versions.toml does NOT contain it!")
            assertTrue(libsVersions.contains("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\""),
                "❌ FAILED: User overrode bundled Hot Reload, but libs.versions.toml does NOT contain Hot Reload plugin!")
            assertTrue(rootBuildGradle.contains("alias(libs.plugins.composeHotReload)"),
                "❌ FAILED: User overrode bundled Hot Reload, but root build.gradle.kts does NOT contain composeHotReload plugin!")
            assertTrue(composeAppBuildGradle.contains("alias(libs.plugins.composeHotReload)"),
                "❌ FAILED: User overrode bundled Hot Reload, but composeApp/build.gradle.kts does NOT contain composeHotReload plugin!")
        }
    }
    
    @Test
    fun `Compose 1-9-3 with enabled Hot Reload should always add to libs-versions-toml`() {
        TemporaryProjectHelper.withTempProject("optional-enabled-") { tempDir ->
            // User selects Compose 1.9.3 (< 1.10.0-beta01)
            // Hot Reload is OPTIONAL, user enables checkbox
            // Should always add to libs.versions.toml
            val builder = ComposeMultiplatformModuleBuilder().apply {
                projectName = "OptionalEnabledTest"
                projectId = "com.example.test"
                composeVersion = "1.9.3"  // < 1.10.0-beta01 → optional
                targetDesktop = true
                targetAndroid = false
                targetIOS = false
                targetWeb = false
                includeHotReload = true  // User enabled checkbox
                hotReloadVersion = "1.0.0-rc02"
                bundledHotReloadVersion = null  // Not bundled for < 1.10.0-beta01
            }
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            // VERIFY: Hot Reload MUST be in libs.versions.toml (optional library enabled)
            assertTrue(libsVersions.contains("composeHotReload = \"1.0.0-rc02\""),
                "❌ FAILED: User enabled optional Hot Reload, but libs.versions.toml does NOT contain version!")
            assertTrue(libsVersions.contains("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\""),
                "❌ FAILED: User enabled optional Hot Reload, but libs.versions.toml does NOT contain plugin!")
        }
    }
}

