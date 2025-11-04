package io.github.heisiar.composewizard.integration

import io.github.heisiar.composewizard.generator.ProjectGenerator
import io.github.heisiar.composewizard.testutils.FileTreeComparator
import io.github.heisiar.composewizard.testutils.ProjectFixtures
import io.github.heisiar.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class DesktopProjectGenerationTest {
    
    @Test
    fun `generate Desktop-only project with correct structure`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
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
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
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
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(
                libsVersions.contains("composeMultiplatform = \"${config.composeVersion}\""),
                "libs.versions.toml should contain Compose version: ${config.composeVersion}"
            )
        }
    }
    
    @Test
    fun `generated project has correct Kotlin version`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(
                libsVersions.contains("kotlin = \"${config.kotlinVersion}\""),
                "libs.versions.toml should contain Kotlin version: ${config.kotlinVersion}"
            )
        }
    }
    
    @Test
    fun `generated project has correct project name in settings gradle`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            FileTreeComparator.assertFileContains(
                tempDir,
                "settings.gradle.kts",
                "rootProject.name = \"${config.projectName}\""
            )
        }
    }
    
    @Test
    fun `generated Desktop project includes only jvm target`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("jvm()"), "Should contain Desktop target")
            assertTrue(!buildGradle.contains("androidTarget"), "Should not contain Android target")
            assertTrue(!buildGradle.contains("iosX64"), "Should not contain iOS target")
            assertTrue(!buildGradle.contains("wasmJs"), "Should not contain Web target")
        }
    }
    
    @Test
    fun `generated Desktop project includes hot reload plugin`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            FileTreeComparator.assertFileContains(
                tempDir,
                "composeApp/build.gradle.kts",
                "alias(libs.plugins.composeHotReload)"
            )
        }
    }
    
    @Test
    fun `generated Desktop project does not include tests`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(!buildGradle.contains("commonTest"), "Should not contain test dependencies")
            
            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(!libsVersions.contains("junit"), "Should not contain JUnit version")
        }
    }
    
    @Test
    fun `generated Desktop project does not initialize git`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            val gitDir = File(tempDir, ".git")
            assertTrue(!gitDir.exists(), "Should not create .git directory")
        }
    }
    
    @Test
    fun `compare generated Desktop project with fixture`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)
            
            generator.generateProject(tempDir.absolutePath)
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("desktop-only")
            
            // Full fixture comparison
            FileTreeComparator.compareDirectories(fixtureDir, tempDir, setOf("jar"))
        }
    }

    @Test
    fun `generated Desktop project does not contain system files`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)

            generator.generateProject(tempDir.absolutePath)

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
            val config = ProjectFixtures.DESKTOP_ONLY_CONFIG
            val generator = ProjectGenerator(config)

            generator.generateProject(tempDir.absolutePath)

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
            val config = ProjectFixtures.desktopConfig(includeTests = true)
            val generator = ProjectGenerator(config)

            generator.generateProject(tempDir.absolutePath)

            FileTreeComparator.assertTestsIncluded(tempDir)
        }
    }

    @Test
    fun `generated Desktop project initializes git when enabled`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.desktopConfig(initGit = true)
            val generator = ProjectGenerator(config)

            generator.generateProject(tempDir.absolutePath)

            FileTreeComparator.assertGitInitialized(tempDir)
        }
    }

    @Test
    fun `generated Desktop project with tests and git initialized`() {
        TemporaryProjectHelper.withTempProject("desktop-test-") { tempDir ->
            val config = ProjectFixtures.desktopConfig(includeTests = true, initGit = true)
            val generator = ProjectGenerator(config)

            generator.generateProject(tempDir.absolutePath)

            FileTreeComparator.assertTestsIncluded(tempDir)
            FileTreeComparator.assertGitInitialized(tempDir)
        }
    }

}
