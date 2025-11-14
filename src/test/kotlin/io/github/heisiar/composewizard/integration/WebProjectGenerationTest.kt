package io.github.heisiar.composewizard.integration

import io.github.heisiar.composewizard.shared.ProjectCreator
import io.github.heisiar.composewizard.testutils.FileTreeComparator
import io.github.heisiar.composewizard.testutils.ProjectFixtures
import io.github.heisiar.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class WebProjectGenerationTest {

    @Test
    fun `generate Web-only project with correct structure`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
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
            FileTreeComparator.assertFileExists(tempDir, "gradle/wrapper/gradle-wrapper.jar")
            FileTreeComparator.assertFileExists(tempDir, "gradlew")
            FileTreeComparator.assertFileExists(tempDir, "gradlew.bat")

            FileTreeComparator.assertFileExists(tempDir, "composeApp/build.gradle.kts")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain/kotlin/org/example/web/App.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain/kotlin/org/example/web/Greeting.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain/kotlin/org/example/web/Platform.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain/kotlin/org/example/web/main.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/jsMain/kotlin/org/example/web/Platform.js.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/wasmJsMain/kotlin/org/example/web/Platform.wasmJs.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain/resources/index.html")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/webMain/resources/styles.css")

            val jvmMainDir = File(tempDir, "composeApp/src/jvmMain")
            val androidMainDir = File(tempDir, "composeApp/src/androidMain")
            val iosMainDir = File(tempDir, "composeApp/src/iosMain")
            val commonTestDir = File(tempDir, "composeApp/src/commonTest")
            
            assertTrue(!jvmMainDir.exists(), "Should not have jvmMain")
            assertTrue(!androidMainDir.exists(), "Should not have androidMain")
            assertTrue(!iosMainDir.exists(), "Should not have iosMain")
            assertTrue(!commonTestDir.exists(), "Should not have commonTest")
        }
    }

    @Test
    fun `generated project has correct project name in settings gradle`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val settingsGradleContent = File(tempDir, "settings.gradle.kts").readText()
            assertTrue(settingsGradleContent.contains("rootProject.name = \"${builder.projectName}\""))
        }
    }

    @Test
    fun `generated project has correct package name`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val appKtContent = File(tempDir, "composeApp/src/webMain/kotlin/org/example/web/App.kt").readText()
            assertTrue(appKtContent.contains("package ${builder.projectId}"))
        }
    }

    @Test
    fun `generated project has correct Compose version`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val libsVersionsContent = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(libsVersionsContent.contains("composeMultiplatform = \"${builder.composeVersion}\""))
        }
    }

    @Test
    fun `generated project has correct Kotlin version`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val libsVersionsContent = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(libsVersionsContent.contains("kotlin = \"${builder.kotlinVersion}\""))
        }
    }

    @Test
    fun `generated Web project does not include tests`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val commonTestDir = File(tempDir, "composeApp/src/commonTest")
            assertTrue(!commonTestDir.exists(), "Should not have commonTest")
        }
    }

    @Test
    fun `generated Web project does not initialize git`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
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
    fun `generated Web project includes js and wasmJs targets`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val composeAppBuildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(composeAppBuildGradle.contains("js {"))
            assertTrue(composeAppBuildGradle.contains("wasmJs {"))
            assertTrue(!composeAppBuildGradle.contains("jvm()"))
            assertTrue(!composeAppBuildGradle.contains("androidTarget"))
            assertTrue(!composeAppBuildGradle.contains("iosX64"))
        }
    }

    @Test
    fun `generated Web project includes ExperimentalWasmDsl import`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val composeAppBuildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(composeAppBuildGradle.contains("import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl"))
        }
    }

    @Test
    fun `generated Web project does not contain system files`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
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
    fun `generated Web project includes google repositories in settings gradle`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
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
    fun `compare generated Web project with fixture`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.WEB_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val fixtureDir = TemporaryProjectHelper.getFixtureDir("web-only")

            // Full fixture comparison
            FileTreeComparator.compareDirectories(fixtureDir, tempDir, setOf("jar"))
        }
    }

    @Test
    fun `generated Web project includes tests when enabled`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.webConfig(includeTests = true)
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
    fun `generated Web project initializes git when enabled`() {
        TemporaryProjectHelper.withTempProject("web-test-") { tempDir ->
            val builder = ProjectFixtures.webConfig(initGit = true)
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

