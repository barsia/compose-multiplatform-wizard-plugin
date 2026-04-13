package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.testutils.FileTreeComparator
import io.github.barsia.composewizard.testutils.ProjectFixtures
import io.github.barsia.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class DesktopProjectGenerationTest {
    
    @Test
    fun `generate Desktop-only project with correct structure`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            FileTreeComparator.assertFileExists(tempDir, "build.gradle.kts")
            FileTreeComparator.assertFileExists(tempDir, "settings.gradle.kts")
            FileTreeComparator.assertFileExists(tempDir, "gradle.properties")
            FileTreeComparator.assertFileExists(tempDir, "gradle/libs.versions.toml")
            FileTreeComparator.assertFileExists(tempDir, "gradle/wrapper/gradle-wrapper.properties")
            FileTreeComparator.assertFileExists(tempDir, "gradlew")
            FileTreeComparator.assertFileExists(tempDir, "gradlew.bat")
            
            FileTreeComparator.assertFileExists(tempDir, "composeApp/build.gradle.kts")
            FileTreeComparator.assertFileExists(
                tempDir,
                "composeApp/src/jvmMain/kotlin/org/example/project/App.kt"
            )
            FileTreeComparator.assertFileExists(
                tempDir,
                "composeApp/src/jvmMain/kotlin/org/example/project/Greeting.kt"
            )
            FileTreeComparator.assertFileExists(
                tempDir,
                "composeApp/src/jvmMain/kotlin/org/example/project/Platform.kt"
            )
            FileTreeComparator.assertFileExists(
                tempDir,
                "composeApp/src/jvmMain/kotlin/org/example/project/main.kt"
            )
            FileTreeComparator.assertFileExists(
                tempDir,
                "composeApp/src/jvmMain/composeResources/drawable/compose-multiplatform.xml"
            )
        }
    }
    
    @Test
    fun `generated project has correct package name`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            FileTreeComparator.assertFileContains(
                tempDir,
                "composeApp/src/jvmMain/kotlin/org/example/project/App.kt",
                "package org.example.project"
            )
            FileTreeComparator.assertFileContains(
                tempDir,
                "composeApp/src/jvmMain/kotlin/org/example/project/main.kt",
                "package org.example.project"
            )
        }
    }
    
    @Test
    fun `generated project has correct Compose version`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(
                libsVersions.contains("composeMultiplatform = \"${builder.composeVersion}\""),
                "libs.versions.toml should contain Compose version: ${builder.composeVersion}"
            )
        }
    }
    
    @Test
    fun `generated project has correct Kotlin version`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(
                libsVersions.contains("kotlin = \"${builder.kotlinVersion}\""),
                "libs.versions.toml should contain Kotlin version: ${builder.kotlinVersion}"
            )
        }
    }
    
    @Test
    fun `generated project has correct project name in settings gradle`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
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
    fun `generated Desktop project includes only jvm target`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("jvm()"), "Should contain Desktop target")
            assertTrue(!buildGradle.contains("androidTarget"), "Should not contain Android target")
            assertTrue(!buildGradle.contains("iosX64"), "Should not contain iOS target")
            assertTrue(!buildGradle.contains("wasmJs"), "Should not contain Web target")
        }
    }
    
    @Test
    fun `generated Desktop project does not include hot reload plugin by default`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(!buildGradle.contains("alias(libs.plugins.composeHotReload)"), "Should not include hot reload plugin by default")
        }
    }
    
    @Test
    fun `generated Desktop project does not include tests`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(!buildGradle.contains("commonTest"), "Should not contain test dependencies")
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(!libsVersions.contains("junit"), "Should not contain JUnit version")
        }
    }
    
    @Test
    fun `generated Desktop project does not initialize git`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val gitDir = File(tempDir, ".git")
            assertTrue(!gitDir.exists(), "Should not create .git directory")
        }
    }
    
    @Test
    fun `compare generated Desktop project with fixture`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("desktop-only")
            
            // Full fixture comparison
            FileTreeComparator.compareDirectories(
                fixtureDir, 
                tempDir, 
                excludeExtensions = setOf("jar"),
                excludeFiles = setOf("gradle/.properties")
            )
        }
    }

    @Test
    fun `generated Desktop project does not contain system files`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
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
    fun `generated Desktop project includes gitignore`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.DESKTOP_ONLY_CONFIG
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
    fun `generated Desktop project includes tests when enabled`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig(includeTests = true)
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
    fun `generated Desktop project initializes git when enabled`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig(initGit = true)
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            FileTreeComparator.assertGitInitialized(tempDir)
        }
    }

    @Test
    fun `generated Desktop project with tests and git initialized`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig(includeTests = true, initGit = true)
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            FileTreeComparator.assertTestsIncluded(tempDir)
            FileTreeComparator.assertGitInitialized(tempDir)
        }
    }

    @Test
    fun `generated Desktop project in Release mode has correct settings gradle repositories`() {
        TemporaryProjectHelper.withTempProject("desktop-release-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig().apply {
                enableDevVersions = false
            }
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val settingsGradle = File(tempDir, "settings.gradle.kts").readText()
            
            assertTrue(
                settingsGradle.contains("google {"),
                "Release mode should have google with mavenContent block"
            )
            assertTrue(
                settingsGradle.contains("includeGroupAndSubgroups(\"androidx\")"),
                "Release mode should have includeGroupAndSubgroups for androidx"
            )
            assertTrue(
                settingsGradle.contains("includeGroupAndSubgroups(\"com.android\")"),
                "Release mode should have includeGroupAndSubgroups for com.android"
            )
            assertTrue(
                !settingsGradle.contains("maven.pkg.jetbrains.space"),
                "Release mode should not contain dev repository"
            )
        }
    }

    @Test
    fun `generated Desktop project in Dev mode has correct settings gradle repositories`() {
        TemporaryProjectHelper.withTempProject("desktop-dev-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig().apply {
                enableDevVersions = true
            }
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val settingsGradle = File(tempDir, "settings.gradle.kts").readText()
            
            assertTrue(
                settingsGradle.contains("google()"),
                "Dev mode should have google() without mavenContent block"
            )
            assertTrue(
                !settingsGradle.contains("includeGroupAndSubgroups"),
                "Dev mode should not have includeGroupAndSubgroups"
            )
            assertTrue(
                settingsGradle.contains("maven(\"https://maven.pkg.jetbrains.space/public/p/compose/dev\")"),
                "Dev mode should contain compose dev repository"
            )
            assertTrue(
                settingsGradle.contains("maven(\"https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev\")"),
                "Dev mode should contain kotlin dev repository"
            )
        }
    }

}
