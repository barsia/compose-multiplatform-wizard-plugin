package io.github.barsia.composewizard.integration

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.testutils.ProjectFixtures
import io.github.barsia.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitInitializationTest {
    
    @Test
    fun `git repository is initialized when initGit is true`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Verify .git directory exists
            val gitDir = File(tempDir, ".git")
            assertTrue(gitDir.exists() && gitDir.isDirectory, ".git directory should exist")
            
            // Verify .git/HEAD exists (basic git structure)
            val gitHead = File(gitDir, "HEAD")
            assertTrue(gitHead.exists(), ".git/HEAD should exist")
        }
    }
    
    @Test
    fun `gitignore file is created`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val gitignore = File(tempDir, ".gitignore")
            assertTrue(gitignore.exists(), ".gitignore should exist")
            
            val content = gitignore.readText()
            assertTrue(content.contains("*.iml"), ".gitignore should contain *.iml")
            assertTrue(content.contains(".gradle"), ".gitignore should contain .gradle")
            assertTrue(content.contains("local.properties"), ".gitignore should contain local.properties")
            assertTrue(content.contains("build/"), ".gitignore should contain build/")
        }
    }
    
    @Test
    fun `required configuration files are staged in git`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Add all files to git to test that gitignore works correctly
            val addProcess = ProcessBuilder("git", "add", ".")
                .directory(tempDir)
                .start()
            addProcess.waitFor()
            
            // Run git ls-files to get staged files
            val process = ProcessBuilder("git", "ls-files")
                .directory(tempDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            val stagedFiles = output.lines().filter { it.isNotBlank() }.toSet()
            
            // Debug: print first 20 files to understand structure
            if (stagedFiles.isEmpty()) {
                throw AssertionError("No files staged! Git might not be initialized properly")
            }
            
            // Verify root configuration files
            assertTrue(".gitignore" in stagedFiles, ".gitignore must be in git. Staged files: ${stagedFiles.take(10)}")
            assertTrue("build.gradle.kts" in stagedFiles, "build.gradle.kts must be in git. Staged files: ${stagedFiles.take(10)}")
            assertTrue("settings.gradle.kts" in stagedFiles, "settings.gradle.kts must be in git")
            assertTrue("gradle.properties" in stagedFiles, "gradle.properties must be in git")
            assertTrue("README.md" in stagedFiles, "README.md must be in git")
            assertTrue("gradle/libs.versions.toml" in stagedFiles, "gradle/libs.versions.toml must be in git")
            assertTrue("gradle/wrapper/gradle-wrapper.properties" in stagedFiles, "gradle-wrapper.properties must be in git")
            assertTrue("gradlew" in stagedFiles, "gradlew must be in git")
            assertTrue("gradlew.bat" in stagedFiles, "gradlew.bat must be in git")
            
            // Verify composeApp configuration
            assertTrue("composeApp/build.gradle.kts" in stagedFiles, "composeApp/build.gradle.kts must be in git")
            
            // Verify source files from different platforms exist
            val kotlinFiles = stagedFiles.filter { it.endsWith(".kt") }
            assertTrue(kotlinFiles.isNotEmpty(), "Kotlin source files must be in git")
            
            // Verify commonMain sources
            assertTrue(stagedFiles.any { it.contains("src/commonMain") && it.endsWith(".kt") }, 
                "commonMain Kotlin files must be in git")
            
            // Verify androidMain sources
            assertTrue(stagedFiles.any { it.contains("src/androidMain") && it.endsWith(".kt") }, 
                "androidMain Kotlin files must be in git")
            assertTrue("composeApp/src/androidMain/AndroidManifest.xml" in stagedFiles, 
                "AndroidManifest.xml must be in git")
            
            // Verify jvmMain (Desktop) sources
            assertTrue(stagedFiles.any { it.contains("src/jvmMain") && it.endsWith(".kt") }, 
                "jvmMain (Desktop) Kotlin files must be in git")
            
            // Verify iosMain sources  
            assertTrue(stagedFiles.any { it.contains("src/iosMain") && it.endsWith(".kt") }, 
                "iosMain Kotlin files must be in git")
            
            // Verify iosApp Xcode project
            assertTrue(stagedFiles.any { it.startsWith("iosApp/") }, 
                "iosApp directory files must be in git")
        }
    }
    
    @Test
    fun `local properties is not in git`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Create local.properties file (simulating AS creating it)
            val localProperties = File(tempDir, "local.properties")
            localProperties.writeText("sdk.dir=/path/to/sdk")
            
            // Run git add . to try adding all files
            val addProcess = ProcessBuilder("git", "add", ".")
                .directory(tempDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            addProcess.waitFor()
            
            // Run git ls-files to check staged files
            val lsProcess = ProcessBuilder("git", "ls-files")
                .directory(tempDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            
            val output = lsProcess.inputStream.bufferedReader().readText()
            lsProcess.waitFor()
            
            val stagedFiles = output.lines().filter { it.isNotBlank() }
            
            // Verify local.properties is NOT staged (respected .gitignore)
            assertFalse(
                stagedFiles.any { it.contains("local.properties") },
                "local.properties should NOT be staged (should be ignored)"
            )
        }
    }
    
    @Test
    fun `git repository is not initialized when initGit is false`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = false
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Verify .git directory does NOT exist
            val gitDir = File(tempDir, ".git")
            assertFalse(gitDir.exists(), ".git directory should NOT exist when initGit is false")
            
            // But .gitignore should still exist for future use
            val gitignore = File(tempDir, ".gitignore")
            assertTrue(gitignore.exists(), ".gitignore should exist even when initGit is false")
        }
    }
    
    @Test
    fun `build and ide files are not in git`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Create some files that should be ignored
            File(tempDir, "build").mkdirs()
            File(tempDir, "build/test.txt").writeText("test")
            File(tempDir, ".gradle").mkdirs()
            File(tempDir, ".gradle/test.txt").writeText("test")
            File(tempDir, "composeApp/build").mkdirs()
            File(tempDir, "composeApp/build/test.txt").writeText("test")
            File(tempDir, "test.iml").writeText("test")
            
            // Run git add . to try adding everything
            val addProcess = ProcessBuilder("git", "add", ".")
                .directory(tempDir)
                .start()
            addProcess.waitFor()
            
            // Get staged files
            val lsProcess = ProcessBuilder("git", "ls-files")
                .directory(tempDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
            
            val output = lsProcess.inputStream.bufferedReader().readText()
            lsProcess.waitFor()
            
            val stagedFiles = output.lines().filter { it.isNotBlank() }
            
            // Verify ignored files are NOT staged
            assertFalse(stagedFiles.any { it.contains("build/") }, "build/ directories should NOT be in git")
            assertFalse(stagedFiles.any { it.contains(".gradle/") }, ".gradle/ directories should NOT be in git")
            assertFalse(stagedFiles.any { it.endsWith(".iml") }, ".iml files should NOT be in git")
        }
    }
    
    @Test
    fun `all multiplatform source sets are staged in git`() {
        TemporaryProjectHelper.withTempProject("git-test-") { tempDir ->
            val builder = ProjectFixtures.androidWebIosConfig(
                projectName = "GitTest",
                packageName = "com.example.gittest",
                initGit = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Run git ls-files to get staged files
            val process = ProcessBuilder("git", "ls-files")
                .directory(tempDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            val stagedFiles = output.lines().filter { it.isNotBlank() }
            
            // Verify multiple platform source sets are staged
            assertTrue(stagedFiles.any { it.contains("src/commonMain") }, "commonMain should be staged")
            assertTrue(stagedFiles.any { it.contains("src/androidMain") }, "androidMain should be staged")
            assertTrue(stagedFiles.any { it.contains("src/iosMain") }, "iosMain should be staged")
            
            // Verify we have multiple .kt files from different platforms
            val kotlinFiles = stagedFiles.filter { it.endsWith(".kt") }
            assertTrue(kotlinFiles.size >= 5, "At least 5 Kotlin files should be staged, got ${kotlinFiles.size}")
        }
    }
}

