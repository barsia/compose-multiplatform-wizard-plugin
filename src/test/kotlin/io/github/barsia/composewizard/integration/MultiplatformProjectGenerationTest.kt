package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.testutils.FileTreeComparator
import io.github.barsia.composewizard.testutils.ProjectFixtures
import io.github.barsia.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiplatformProjectGenerationTest {

    @Test
    fun `compare generated Android+Desktop project with fixture`() {
        TemporaryProjectHelper.withTempProject("android-desktop-compare-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_DESKTOP_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("android-desktop")
            
            // Full fixture comparison (excluding PNG files)
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png")
            )
        }
    }

    @Test
    fun `compare generated Android+iOS project with fixture`() {
        TemporaryProjectHelper.withTempProject("android-ios-compare-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_IOS_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("android-ios")
            
            // Full fixture comparison (excluding PNG files)
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png")
            )
        }
    }

    @Test
    fun `generate Desktop+Web project`() {
        TemporaryProjectHelper.withTempProject("multiplatform-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_WEB_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/jvmMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/jsMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/wasmJsMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/commonMain")

            val androidMainDir = File(tempDir, "composeApp/src/androidMain")
            val iosMainDir = File(tempDir, "composeApp/src/iosMain")
            assertFalse(androidMainDir.exists(), "Should not have androidMain for Desktop+Web")
            assertFalse(iosMainDir.exists(), "Should not have iosMain for Desktop+Web")
        }
    }

    @Test
    fun `generate Desktop+Web project has correct targets in build gradle`() {
        TemporaryProjectHelper.withTempProject("multiplatform-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_WEB_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("jvm()"), "Should contain Desktop target")
            assertTrue(buildGradle.contains("js {"), "Should contain JS target")
            assertTrue(buildGradle.contains("wasmJs {"), "Should contain WasmJS target")
            assertFalse(buildGradle.contains("androidTarget"), "Should not contain Android target")
            assertFalse(buildGradle.contains("iosArm64"), "Should not contain iOS targets")
        }
    }

    @Test
    fun `generate all-platforms project with complete structure`() {
        TemporaryProjectHelper.withTempProject("multiplatform-test-") { tempDir ->
            val builder = ProjectFixtures.ALL_PLATFORMS_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/androidMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/jvmMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/iosMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/jsMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/wasmJsMain")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/commonMain")

            FileTreeComparator.assertFileExists(tempDir, "iosApp/iosApp.xcodeproj/project.pbxproj")
        }
    }

    @Test
    fun `generate all-platforms project has all targets in build gradle`() {
        TemporaryProjectHelper.withTempProject("multiplatform-test-") { tempDir ->
            val builder = ProjectFixtures.ALL_PLATFORMS_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("androidTarget"), "Should contain androidTarget")
            assertTrue(buildGradle.contains("jvm()"), "Should contain Desktop target")
            assertTrue(buildGradle.contains("iosArm64"), "Should contain iOS targets")
            assertTrue(buildGradle.contains("js {"), "Should contain JS target")
            assertTrue(buildGradle.contains("wasmJs {"), "Should contain WasmJS target")
        }
    }

    @Test
    fun `generate all-platforms project has all plugins in root build gradle`() {
        TemporaryProjectHelper.withTempProject("multiplatform-test-") { tempDir ->
            val builder = ProjectFixtures.ALL_PLATFORMS_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val rootBuildGradle = File(tempDir, "build.gradle.kts").readText()
            assertTrue(rootBuildGradle.contains("composeMultiplatform"), "Should contain compose plugin")
            assertTrue(rootBuildGradle.contains("composeCompiler"), "Should contain compose compiler")
            assertTrue(rootBuildGradle.contains("kotlinMultiplatform"), "Should contain kotlin multiplatform")
            assertTrue(rootBuildGradle.contains("androidApplication"), "Should contain android plugin")
            assertTrue(rootBuildGradle.contains("composeHotReload"), "Should contain hot reload plugin")
        }
    }

    @Test
    fun `compare generated Desktop+Web project with fixture`() {
        TemporaryProjectHelper.withTempProject("desktop-web-compare-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_WEB_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("desktop-web")
            
            // Full fixture comparison (excluding PNG files)
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png")
            )
        }
    }

    @Test
    fun `compare generated All-platforms project with fixture`() {
        TemporaryProjectHelper.withTempProject("all-platforms-compare-") { tempDir ->
            val builder = ProjectFixtures.ALL_PLATFORMS_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("all-platforms")
            
            // Full fixture comparison (excluding PNG files)
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png")
            )
        }
    }

    @Test
    fun `compare generated Android+Desktop+iOS project with tests with fixture`() {
        TemporaryProjectHelper.withTempProject("android-desktop-ios-tests-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosConfig(includeTests = true)
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("android-ios-desktop-tests")
            
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png"),
                excludeFiles = setOf("iosApp/iosApp.xcodeproj/project.pbxproj")
            )
        }
    }

    @Test
    fun `compare generated Android+Web+iOS project with git with fixture`() {
        TemporaryProjectHelper.withTempProject("android-web-ios-git-") { tempDir ->
            val builder = ProjectFixtures.androidWebIosConfig(initGit = true)
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            FileTreeComparator.assertGitInitialized(tempDir)
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("android-web-ios-git")
            
            val gitFiles = tempDir.walkTopDown()
                .filter { it.isFile }
                .filter { it.relativeTo(tempDir).path.startsWith(".git/") }
                .map { it.relativeTo(tempDir).path }
                .toSet()
            
            FileTreeComparator.compareDirectories(
                fixtureDir,
                tempDir,
                excludeExtensions = setOf("png"),
                excludeFiles = setOf("iosApp/iosApp.xcodeproj/project.pbxproj") + gitFiles
            )
        }
    }

}

