package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.barsia.composewizard.testutils.FileTreeComparator
import io.github.barsia.composewizard.testutils.ProjectFixtures
import io.github.barsia.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibrariesGenerationTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generated project with all libraries has correct dependencies`() {
        val builder = ComposeMultiplatformModuleBuilder().apply {
            projectName = "TestProject"
            projectId = "org.example.project"
            composeVersion = "1.10.0-beta01"
            kotlinVersion = "2.2.21"
            targetAndroid = true
            targetDesktop = true
            targetIOS = true
            includeTests = true
            
            lifecycleVersion = "2.10.0-alpha04"
            material3Version = "1.10.0-alpha04"
            material3AdaptiveVersion = "1.3.0-alpha01"
            navigation3Version = "1.0.0-alpha04"
            navigationEventVersion = "1.0.0-beta01"
            savedStateVersion = "1.4.0-beta01"
            windowVersion = "1.5.0-rc01"
            
            includeMaterial3 = true
            includeMaterial3Adaptive = true
            includeNavigation3 = true
            includeNavigationEvent = true
            includeSavedState = true
            includeWindow = true
        }

        ProjectCreator.createProjectStructure(
            projectPath = tempDir.toAbsolutePath().toString(),
            projectName = builder.projectName,
            builder = builder,
            useCache = false
        )

        val libsVersionsContent = tempDir.resolve("gradle/libs.versions.toml").toFile().readText()
        val buildGradleContent = tempDir.resolve("composeApp/build.gradle.kts").toFile().readText()

        // Check libs.versions.toml
        assertTrue(libsVersionsContent.contains("compose-material3 = \"1.10.0-alpha04\""), "material3 version should be correct")
        assertTrue(libsVersionsContent.contains("compose-material3-adaptive = \"1.3.0-alpha01\""), "material3-adaptive version should be correct")
        assertTrue(libsVersionsContent.contains("compose-navigation3-ui = \"1.0.0-alpha04\""), "navigation3-ui version should be correct")
        assertTrue(libsVersionsContent.contains("compose-navigationevent = \"1.0.0-beta01\""), "navigationevent version should be correct")
        assertTrue(libsVersionsContent.contains("androidx-savedstate = \"1.4.0-beta01\""), "savedstate version should be correct")
        assertTrue(libsVersionsContent.contains("androidx-window-core = \"1.5.0-rc01\""), "window-core version should be correct")
        
        assertTrue(libsVersionsContent.contains("""compose-material3 = { group = "org.jetbrains.compose.material3", name = "material3""""), "material3 library should use correct group/name")
        assertTrue(libsVersionsContent.contains("""compose-navigation3-ui = { group = "org.jetbrains.androidx.navigation3", name = "navigation3-ui""""), "navigation3-ui library should use correct group/name")
        assertTrue(libsVersionsContent.contains("""compose-navigationevent = { group = "org.jetbrains.androidx.navigationevent", name = "navigationevent-compose""""), "navigationevent library should use correct group/name")
        assertTrue(libsVersionsContent.contains("""androidx-window-core = { group = "org.jetbrains.androidx.window", name = "window-core""""), "window-core library should use correct group/name")
        assertTrue(libsVersionsContent.contains("""compose-material3-adaptive-nav3 = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive-navigation3""""), "adaptive-nav3 library should exist")

        // Check build.gradle.kts
        assertTrue(buildGradleContent.contains("implementation(libs.compose.material3)"), "should use compose.material3")
        assertTrue(buildGradleContent.contains("implementation(libs.compose.navigation3.ui)"), "should use compose.navigation3.ui")
        assertTrue(buildGradleContent.contains("implementation(libs.compose.material3.adaptive.nav3)"), "should use compose.material3.adaptive.nav3")
        assertTrue(buildGradleContent.contains("implementation(libs.compose.navigationevent)"), "should use compose.navigationevent")
        assertTrue(buildGradleContent.contains("implementation(libs.androidx.window.core)"), "should use androidx.window.core")
    }

    @Test
    fun `compare generated project with all libraries 1-10-beta01 with fixture`() {
        TemporaryProjectHelper.withTempProject("all-libraries-1-10-beta01-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = true,
                includeMaterial3 = true,
                includeMaterial3Adaptive = true,
                includeNavigation3 = true,
                includeNavigationEvent = true,
                includeSavedState = true,
                includeWindow = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("android-ios-desktop-all-libraries-tests-1.10-beta01")
            
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png"),
                excludeFiles = setOf("iosApp/iosApp.xcodeproj/project.pbxproj")
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test Material3 library inclusion`(includeMaterial3: Boolean) {
        TemporaryProjectHelper.withTempProject("material3-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = false,
                includeMaterial3 = includeMaterial3,
                includeMaterial3Adaptive = false,
                includeNavigation3 = false,
                includeNavigationEvent = false,
                includeSavedState = false,
                includeWindow = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeMaterial3) {
                // When Material3 is enabled, use libs catalog version
                assertTrue(buildGradle.contains("implementation(libs.compose.material3)"), 
                    "build.gradle.kts should contain libs.compose.material3 dependency when Material3 is enabled")
                assertTrue(libsVersions.contains("compose-material3 = \"1.10.0-alpha04\""), 
                    "libs.versions.toml should contain material3 version when Material3 is enabled")
                assertTrue(libsVersions.contains("""compose-material3 = { group = "org.jetbrains.compose.material3", name = "material3""""), 
                    "libs.versions.toml should contain material3 library definition when Material3 is enabled")
                
                // Should NOT contain basic compose.material3
                assertFalse(buildGradle.contains("implementation(compose.material3)"), 
                    "build.gradle.kts should not contain basic compose.material3 when Material3 library is enabled")
            } else {
                // When Material3 is disabled, use basic compose.material3 (from Compose BOM)
                assertTrue(buildGradle.contains("implementation(compose.material3)"), 
                    "build.gradle.kts should contain basic compose.material3 when Material3 library is disabled")
                
                // Should NOT contain libs catalog version
                assertFalse(buildGradle.contains("implementation(libs.compose.material3)"), 
                    "build.gradle.kts should not contain libs.compose.material3 when Material3 library is disabled")
                assertFalse(libsVersions.contains("compose-material3 = \"1.10.0-alpha04\""), 
                    "libs.versions.toml should not contain material3 version when Material3 library is disabled")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test Material3 Adaptive library inclusion`(includeMaterial3Adaptive: Boolean) {
        TemporaryProjectHelper.withTempProject("material3-adaptive-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = false,
                includeMaterial3 = false,
                includeMaterial3Adaptive = includeMaterial3Adaptive,
                includeNavigation3 = false,
                includeNavigationEvent = false,
                includeSavedState = false,
                includeWindow = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeMaterial3Adaptive) {
                assertTrue(buildGradle.contains("implementation(libs.compose.material3.adaptive)"), 
                    "build.gradle.kts should contain compose.material3.adaptive dependency")
                assertTrue(libsVersions.contains("compose-material3-adaptive = \"1.3.0-alpha01\""), 
                    "libs.versions.toml should contain material3-adaptive version")
                assertTrue(libsVersions.contains("""compose-material3-adaptive = { group = "org.jetbrains.compose.material3.adaptive", name = "adaptive""""), 
                    "libs.versions.toml should contain material3-adaptive library definition")
            } else {
                assertFalse(buildGradle.contains("implementation(libs.compose.material3.adaptive)"), 
                    "build.gradle.kts should not contain compose.material3.adaptive dependency")
                assertFalse(libsVersions.contains("compose-material3-adaptive = \"1.3.0-alpha01\""), 
                    "libs.versions.toml should not contain material3-adaptive version")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test Navigation3 library inclusion`(includeNavigation3: Boolean) {
        TemporaryProjectHelper.withTempProject("navigation3-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = false,
                includeMaterial3 = false,
                includeMaterial3Adaptive = false,
                includeNavigation3 = includeNavigation3,
                includeNavigationEvent = false,
                includeSavedState = false,
                includeWindow = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeNavigation3) {
                assertTrue(buildGradle.contains("implementation(libs.compose.navigation3.ui)"), 
                    "build.gradle.kts should contain compose.navigation3.ui dependency")
                assertTrue(libsVersions.contains("compose-navigation3-ui = \"1.0.0-alpha04\""), 
                    "libs.versions.toml should contain navigation3-ui version")
                assertTrue(libsVersions.contains("""compose-navigation3-ui = { group = "org.jetbrains.androidx.navigation3", name = "navigation3-ui""""), 
                    "libs.versions.toml should contain navigation3-ui library definition")
            } else {
                assertFalse(buildGradle.contains("implementation(libs.compose.navigation3.ui)"), 
                    "build.gradle.kts should not contain compose.navigation3.ui dependency")
                assertFalse(libsVersions.contains("compose-navigation3-ui = \"1.0.0-alpha04\""), 
                    "libs.versions.toml should not contain navigation3-ui version")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test NavigationEvent library inclusion`(includeNavigationEvent: Boolean) {
        TemporaryProjectHelper.withTempProject("navigationevent-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = false,
                includeMaterial3 = false,
                includeMaterial3Adaptive = false,
                includeNavigation3 = false,
                includeNavigationEvent = includeNavigationEvent,
                includeSavedState = false,
                includeWindow = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeNavigationEvent) {
                assertTrue(buildGradle.contains("implementation(libs.compose.navigationevent)"), 
                    "build.gradle.kts should contain compose.navigationevent dependency")
                assertTrue(libsVersions.contains("compose-navigationevent = \"1.0.0-beta01\""), 
                    "libs.versions.toml should contain navigationevent version")
                assertTrue(libsVersions.contains("""compose-navigationevent = { group = "org.jetbrains.androidx.navigationevent", name = "navigationevent-compose""""), 
                    "libs.versions.toml should contain navigationevent library definition")
            } else {
                assertFalse(buildGradle.contains("implementation(libs.compose.navigationevent)"), 
                    "build.gradle.kts should not contain compose.navigationevent dependency")
                assertFalse(libsVersions.contains("compose-navigationevent = \"1.0.0-beta01\""), 
                    "libs.versions.toml should not contain navigationevent version")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test SavedState library inclusion`(includeSavedState: Boolean) {
        TemporaryProjectHelper.withTempProject("savedstate-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = false,
                includeMaterial3 = false,
                includeMaterial3Adaptive = false,
                includeNavigation3 = false,
                includeNavigationEvent = false,
                includeSavedState = includeSavedState,
                includeWindow = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeSavedState) {
                assertTrue(buildGradle.contains("implementation(libs.androidx.savedstate)"), 
                    "build.gradle.kts should contain androidx.savedstate dependency")
                assertTrue(libsVersions.contains("androidx-savedstate = \"1.4.0-beta01\""), 
                    "libs.versions.toml should contain savedstate version")
                assertTrue(libsVersions.contains("""androidx-savedstate = { group = "org.jetbrains.androidx.savedstate", name = "savedstate""""), 
                    "libs.versions.toml should contain savedstate library definition")
            } else {
                assertFalse(buildGradle.contains("implementation(libs.androidx.savedstate)"), 
                    "build.gradle.kts should not contain androidx.savedstate dependency")
                assertFalse(libsVersions.contains("androidx-savedstate = \"1.4.0-beta01\""), 
                    "libs.versions.toml should not contain savedstate version")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test Window library inclusion`(includeWindow: Boolean) {
        TemporaryProjectHelper.withTempProject("window-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = false,
                includeMaterial3 = false,
                includeMaterial3Adaptive = false,
                includeNavigation3 = false,
                includeNavigationEvent = false,
                includeSavedState = false,
                includeWindow = includeWindow
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeWindow) {
                assertTrue(buildGradle.contains("implementation(libs.androidx.window.core)"), 
                    "build.gradle.kts should contain androidx.window.core dependency")
                assertTrue(libsVersions.contains("androidx-window-core = \"1.5.0-rc01\""), 
                    "libs.versions.toml should contain window-core version")
                assertTrue(libsVersions.contains("""androidx-window-core = { group = "org.jetbrains.androidx.window", name = "window-core""""), 
                    "libs.versions.toml should contain window-core library definition")
            } else {
                assertFalse(buildGradle.contains("implementation(libs.androidx.window.core)"), 
                    "build.gradle.kts should not contain androidx.window.core dependency")
                assertFalse(libsVersions.contains("androidx-window-core = \"1.5.0-rc01\""), 
                    "libs.versions.toml should not contain window-core version")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test Hot Reload inclusion`(includeHotReload: Boolean) {
        TemporaryProjectHelper.withTempProject("hotreload-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig(
                projectName = "HotReloadTest",
                packageName = "com.example.hotreloadtest"
            ).apply {
                this.includeHotReload = includeHotReload
                this.hotReloadVersion = if (includeHotReload) "1.0.0-rc02" else null
                this.bundledHotReloadVersion = null  // Compose < 1.10.0-beta01 (optional library)
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
            
            if (includeHotReload) {
                // When Hot Reload is enabled
                assertTrue(rootBuildGradle.contains("alias(libs.plugins.composeHotReload)"), 
                    "root build.gradle.kts should contain composeHotReload plugin when Hot Reload is enabled")
                assertTrue(composeAppBuildGradle.contains("alias(libs.plugins.composeHotReload)"), 
                    "composeApp/build.gradle.kts should contain composeHotReload plugin when Hot Reload is enabled")
                assertTrue(libsVersions.contains("composeHotReload = \"1.0.0-rc02\""), 
                    "libs.versions.toml should contain Hot Reload version when Hot Reload is enabled")
                assertTrue(libsVersions.contains("""composeHotReload = { id = "org.jetbrains.compose.hot-reload""""), 
                    "libs.versions.toml should contain Hot Reload plugin definition when Hot Reload is enabled")
            } else {
                // When Hot Reload is disabled
                assertFalse(rootBuildGradle.contains("alias(libs.plugins.composeHotReload)"), 
                    "root build.gradle.kts should NOT contain composeHotReload plugin when Hot Reload is disabled")
                assertFalse(composeAppBuildGradle.contains("alias(libs.plugins.composeHotReload)"), 
                    "composeApp/build.gradle.kts should NOT contain composeHotReload plugin when Hot Reload is disabled")
                assertFalse(libsVersions.contains("composeHotReload = \"1.0.0-rc02\""), 
                    "libs.versions.toml should NOT contain Hot Reload version when Hot Reload is disabled")
                assertFalse(libsVersions.contains("""composeHotReload = { id = "org.jetbrains.compose.hot-reload""""), 
                    "libs.versions.toml should NOT contain Hot Reload plugin definition when Hot Reload is disabled")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test sample tests inclusion`(includeTests: Boolean) {
        TemporaryProjectHelper.withTempProject("tests-inclusion-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosAllLibrariesConfig(
                includeTests = includeTests,
                includeMaterial3 = false,
                includeMaterial3Adaptive = false,
                includeNavigation3 = false,
                includeNavigationEvent = false,
                includeSavedState = false,
                includeWindow = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val commonTestDir = File(tempDir, "composeApp/src/commonTest")
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            
            if (includeTests) {
                assertTrue(commonTestDir.exists(), "commonTest directory should exist when tests are included")
                assertTrue(buildGradle.contains("commonTest.dependencies"), 
                    "build.gradle.kts should contain commonTest.dependencies block")
                assertTrue(libsVersions.contains("kotlin-test"), 
                    "libs.versions.toml should contain kotlin-test dependency")
                
                val testFiles = commonTestDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .toList()
                assertTrue(testFiles.isNotEmpty(), "commonTest should contain .kt test files")
            } else {
                assertFalse(commonTestDir.exists(), "commonTest directory should not exist when tests are not included")
                assertFalse(buildGradle.contains("commonTest.dependencies"), 
                    "build.gradle.kts should not contain commonTest.dependencies block")
            }
        }
    }
}
