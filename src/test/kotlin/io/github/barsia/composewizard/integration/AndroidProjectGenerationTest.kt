package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.testutils.FileTreeComparator
import io.github.barsia.composewizard.testutils.ProjectFixtures
import io.github.barsia.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidProjectGenerationTest {

    @Test
    fun `generate Android-only project with correct structure`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG

            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            // Verify project structure
            assertTrue(File(tempDir, "build.gradle.kts").exists())
            assertTrue(File(tempDir, "settings.gradle.kts").exists())
            assertTrue(File(tempDir, "gradle/libs.versions.toml").exists())
            assertTrue(File(tempDir, "composeApp/build.gradle.kts").exists())
            
            // Verify Android-specific files
            assertTrue(File(tempDir, "composeApp/src/androidMain").exists())
            assertTrue(File(tempDir, "composeApp/src/androidMain/AndroidManifest.xml").exists())
            assertTrue(File(tempDir, "composeApp/src/androidMain/kotlin/org/example/android").exists())
            assertTrue(File(tempDir, "composeApp/src/androidMain/res").exists())
        }
    }

    @Test
    fun `generated project has correct project name in settings gradle`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            FileTreeComparator.assertFileContains(
                tempDir,
                "settings.gradle.kts",
                "rootProject.name = \"${builder.projectName}\""
            )
        }
    }

    @Test
    fun `generated project has correct package name`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val mainActivity = File(tempDir, "composeApp/src/androidMain/kotlin/org/example/android/MainActivity.kt")
            assertTrue(mainActivity.exists(), "MainActivity.kt should exist")
            val content = mainActivity.readText()
            assertTrue(content.contains("package org.example.android"))
        }
    }

    @Test
    fun `generated project has correct Compose version`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val versionsToml = File(tempDir, "gradle/libs.versions.toml")
            val content = versionsToml.readText()
            assertTrue(content.contains("composeMultiplatform = \"${builder.composeVersion}\""))
        }
    }

    @Test
    fun `generated project has correct Kotlin version`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val versionsToml = File(tempDir, "gradle/libs.versions.toml")
            val content = versionsToml.readText()
            assertTrue(content.contains("kotlin = \"${builder.kotlinVersion}\""))
        }
    }

    @Test
    fun `generated Android project does not include tests`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertFalse(buildGradle.contains("commonTest.dependencies"))
        }
    }

    @Test
    fun `generated Android project does not initialize git`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val gitDir = File(tempDir, ".git")
            assertFalse(gitDir.exists(), ".git directory should not exist")
        }
    }

    @Test
    fun `generated Android project includes only androidTarget`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val composeAppBuildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(composeAppBuildGradle.contains("androidTarget"))
            assertFalse(composeAppBuildGradle.contains("jvm()"))
            assertFalse(composeAppBuildGradle.contains("js("))
            assertFalse(composeAppBuildGradle.contains("wasmJs"))
            assertFalse(composeAppBuildGradle.contains("iosX64"))
            assertFalse(composeAppBuildGradle.contains("iosArm64"))
        }
    }

    @Test
    fun `generated Android project includes AndroidManifest`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val manifest = File(tempDir, "composeApp/src/androidMain/AndroidManifest.xml")
            assertTrue(manifest.exists(), "AndroidManifest.xml should exist")
            val content = manifest.readText()
            assertTrue(content.contains("android:name=\".MainActivity\""))
            
            // Package определяется через namespace в build.gradle.kts (современный Android)
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("namespace = \"org.example.android\""))
        }
    }

    @Test
    fun `generated Android project includes google repositories`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val settingsGradleContent = File(tempDir, "settings.gradle.kts").readText()
            assertTrue(settingsGradleContent.contains("google {"))
        }
    }

    @Test
    fun `compare generated Android project with fixture`() {
        TemporaryProjectHelper.withTempProject("android-compare-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("android-only")

            // Full fixture comparison
            FileTreeComparator.compareDirectories(fixtureDir, tempDir, setOf("jar"))
        }
    }

    @Test
    fun `generated Android project does not contain system files`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val allFiles = tempDir.walkTopDown().filter { it.isFile }.map { it.name }.toList()
            
            // Should not contain hidden/system files
            assertTrue(!allFiles.contains(".DS_Store"), "Should not contain .DS_Store")
            assertTrue(!allFiles.contains(".gitkeep"), "Should not contain .gitkeep")
            assertTrue(!allFiles.contains("Thumbs.db"), "Should not contain Thumbs.db")
            assertTrue(!allFiles.contains("desktop.ini"), "Should not contain desktop.ini")
            
            // Should not contain temporary files
            assertTrue(allFiles.none { it.endsWith("~") }, "Should not contain backup files (~)")
            assertTrue(allFiles.none { it.endsWith(".tmp") }, "Should not contain .tmp files")
            assertTrue(allFiles.none { it.endsWith(".bak") }, "Should not contain .bak files")
        }
    }

    @Test
    fun `generated Android project includes gitignore`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.ANDROID_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val gitignore = File(tempDir, ".gitignore")
            assertTrue(gitignore.exists(), ".gitignore should exist")
            val content = gitignore.readText()
            assertTrue(content.contains(".gradle"), ".gitignore should contain .gradle")
            assertTrue(content.contains(".idea"), ".gitignore should contain .idea")
        }
    }

    @Test
    fun `generated Android project includes tests when enabled`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(includeTests = true)
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            FileTreeComparator.assertTestsIncluded(tempDir)
        }
    }

    @Test
    fun `generated Android project initializes git when enabled`() {
        TemporaryProjectHelper.withTempProject("android-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(initGit = true)
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            FileTreeComparator.assertGitInitialized(tempDir)
        }
    }
}

