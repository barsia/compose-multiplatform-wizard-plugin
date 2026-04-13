package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.testutils.FileTreeComparator
import io.github.barsia.composewizard.testutils.ProjectFixtures
import io.github.barsia.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class IOSProjectGenerationTest {

    @Test
    fun `generate iOS-only project with correct structure`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
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
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/iosMain/kotlin/${builder.projectId.replace(".", "/")}/App.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/iosMain/kotlin/${builder.projectId.replace(".", "/")}/Greeting.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/iosMain/kotlin/${builder.projectId.replace(".", "/")}/Platform.kt")
            FileTreeComparator.assertFileExists(tempDir, "composeApp/src/iosMain/kotlin/${builder.projectId.replace(".", "/")}/MainViewController.kt")
            
            FileTreeComparator.assertFileExists(tempDir, "iosApp/iosApp.xcodeproj/project.pbxproj")
            FileTreeComparator.assertFileExists(tempDir, "iosApp/iosApp/iOSApp.swift")
            FileTreeComparator.assertFileExists(tempDir, "iosApp/Configuration/Config.xcconfig")

            val jvmMainDir = File(tempDir, "composeApp/src/jvmMain")
            val androidMainDir = File(tempDir, "composeApp/src/androidMain")
            val webMainDir = File(tempDir, "composeApp/src/webMain")
            val commonTestDir = File(tempDir, "composeApp/src/commonTest")

            assertTrue(!jvmMainDir.exists(), "Should not have jvmMain")
            assertTrue(!androidMainDir.exists(), "Should not have androidMain")
            assertTrue(!webMainDir.exists(), "Should not have webMain")
            assertTrue(!commonTestDir.exists(), "Should not have commonTest")
        }
    }

    @Test
    fun `generated project has correct project name in settings gradle`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val settingsGradle = File(tempDir, "settings.gradle.kts").readText()
            assertTrue(settingsGradle.contains("rootProject.name = \"${builder.projectName}\""))
        }
    }

    @Test
    fun `generated project has correct package name`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val appKt = File(tempDir, "composeApp/src/iosMain/kotlin/${builder.projectId.replace(".", "/")}/App.kt").readText()
            assertTrue(appKt.contains("package ${builder.projectId}"))
        }
    }

    @Test
    fun `generated project has correct Compose version`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(libsVersions.contains("composeMultiplatform = \"${builder.composeVersion}\""))
        }
    }

    @Test
    fun `generated project has correct Kotlin version`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(libsVersions.contains("kotlin = \"${builder.kotlinVersion}\""))
        }
    }

    @Test
    fun `generated iOS project does not initialize git`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val gitDir = File(tempDir, ".git")
            assertTrue(!gitDir.exists(), "Should not initialize Git repository when initGit is false")
        }
    }

    @Test
    fun `generated iOS project does not include tests`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(!buildGradle.contains("commonTest"), "Should not have commonTest when tests are disabled")

            val libsVersions = File(tempDir, "gradle/libs.versions.toml").readText()
            assertTrue(!libsVersions.contains("junit"), "Should not have junit dependency")
            assertTrue(!libsVersions.contains("kotlin-test"), "Should not have kotlin-test dependency")
        }
    }

    @Test
    fun `generated iOS project includes iosArm64 and iosSimulatorArm64 targets`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("iosArm64()"))
            assertTrue(buildGradle.contains("iosSimulatorArm64()"))
            assertTrue(!buildGradle.contains("iosX64()"), "Should not have iosX64 target")
        }
    }

    @Test
    fun `generated iOS project includes ComposeApp framework configuration`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val buildGradle = File(tempDir, "composeApp/build.gradle.kts").readText()
            assertTrue(buildGradle.contains("baseName = \"ComposeApp\""))
            assertTrue(buildGradle.contains("isStatic = true"))
        }
    }

    @Test
    fun `generated iOS project includes google repositories`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
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
    fun `generated iosApp has correct bundle identifier`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val xcconfig = File(tempDir, "iosApp/Configuration/Config.xcconfig").readText()
            assertTrue(xcconfig.contains(builder.projectId))
        }
    }

    @Test
    fun `compare generated iOS project with fixture`() {
        TemporaryProjectHelper.withTempProject("ios-compare-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val fixtureDir = TemporaryProjectHelper.getFixtureDir("ios-only")

            // Full fixture comparison (excluding JAR files)
            FileTreeComparator.compareDirectories(
                fixtureDir, 
                tempDir, 
                excludeExtensions = setOf("jar")
            )
        }
    }

    @Test
    fun `generated iOS project does not contain system files`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
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
    fun `generated iOS project includes tests when enabled`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.iosConfig(includeTests = true)
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
    fun `generated iOS project initializes git when enabled`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.iosConfig(initGit = true)
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
    fun `generated project pbxproj has valid UUIDs`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val pbxprojFile = File(tempDir, "iosApp/iosApp.xcodeproj/project.pbxproj")
            assertTrue(pbxprojFile.exists(), "project.pbxproj should exist")
            
            val content = pbxprojFile.readText()
            val uuidPattern = Regex("[0-9A-F]{24}")
            val foundUuids = uuidPattern.findAll(content).map { it.value }.toList()
            
            assertTrue(foundUuids.isNotEmpty(), "Should have UUIDs in project.pbxproj")
            
            val uniqueUuids = foundUuids.toSet()
            assertTrue(uniqueUuids.size >= 15, "Should have at least 15 different UUIDs")
            
            foundUuids.forEach { uuid ->
                assertTrue(uuid.length == 24, "Each UUID should be 24 characters")
                assertTrue(uuid.all { it in "0123456789ABCDEF" }, "Each UUID should be valid hex")
            }
        }
    }

    @Test
    fun `generated project pbxproj contains project name`() {
        TemporaryProjectHelper.withTempProject("ios-test-") { tempDir ->
            val builder = ProjectFixtures.IOS_ONLY_CONFIG
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )

            val pbxprojFile = File(tempDir, "iosApp/iosApp.xcodeproj/project.pbxproj")
            val content = pbxprojFile.readText()
            
            assertTrue(content.contains("${builder.projectName}.app"), 
                "project.pbxproj should contain project name in .app reference")
        }
    }

    @Test
    fun `multiple generations produce different UUIDs`() {
        TemporaryProjectHelper.withTempProject("ios-test-1-") { tempDir1 ->
            TemporaryProjectHelper.withTempProject("ios-test-2-") { tempDir2 ->
                val builder = ProjectFixtures.IOS_ONLY_CONFIG

                ProjectCreator.createProjectStructure(
                    projectPath = tempDir1.absolutePath,
                    projectName = builder.projectName,
                    builder = builder,
                    useCache = false
                )
                
                ProjectCreator.createProjectStructure(
                    projectPath = tempDir2.absolutePath,
                    projectName = builder.projectName,
                    builder = builder,
                    useCache = false
                )

                val pbxproj1 = File(tempDir1, "iosApp/iosApp.xcodeproj/project.pbxproj").readText()
                val pbxproj2 = File(tempDir2, "iosApp/iosApp.xcodeproj/project.pbxproj").readText()
                
                val uuids1 = Regex("[0-9A-F]{24}").findAll(pbxproj1).map { it.value }.toSet()
                val uuids2 = Regex("[0-9A-F]{24}").findAll(pbxproj2).map { it.value }.toSet()
                
                val commonUuids = uuids1.intersect(uuids2)
                assertTrue(commonUuids.isEmpty(), 
                    "Different project generations should have completely different UUIDs")
            }
        }
    }
}

