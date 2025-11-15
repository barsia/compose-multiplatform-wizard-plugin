package io.github.heisiar.composewizard.testutils

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail

object FileTreeComparator {
    
    data class ComparisonResult(
        val passed: Boolean,
        val missingFiles: List<String> = emptyList(),
        val extraFiles: List<String> = emptyList(),
        val differentContent: List<String> = emptyList()
    )
    
    fun compareStructure(expected: File, actual: File, ignoreFiles: Set<String> = setOf()): ComparisonResult {
        val missingFiles = mutableListOf<String>()
        val extraFiles = mutableListOf<String>()
        
        val expectedFiles = getRelativeFilePaths(expected, ignoreFiles)
        val actualFiles = getRelativeFilePaths(actual, ignoreFiles)
        
        expectedFiles.forEach { path ->
            if (path !in actualFiles) {
                missingFiles.add(path)
            }
        }
        
        actualFiles.forEach { path ->
            if (path !in expectedFiles) {
                extraFiles.add(path)
            }
        }
        
        val passed = missingFiles.isEmpty() && extraFiles.isEmpty()
        return ComparisonResult(passed, missingFiles, extraFiles)
    }
    
    fun compareFileContent(expected: File, actual: File, strict: Boolean = true): Boolean {
        if (!expected.exists() || !actual.exists()) return false
        if (expected.isDirectory || actual.isDirectory) return false
        
        val expectedContent = expected.readText()
        val actualContent = actual.readText()
        
        return if (strict) {
            expectedContent == actualContent
        } else {
            normalizeContent(expectedContent) == normalizeContent(actualContent)
        }
    }
    
    fun assertFileExists(projectDir: File, relativePath: String) {
        val file = File(projectDir, relativePath)
        assertTrue(file.exists(), "File should exist: $relativePath")
    }
    
    fun assertFileContains(projectDir: File, relativePath: String, expectedContent: String) {
        val file = File(projectDir, relativePath)
        assertTrue(file.exists(), "File should exist: $relativePath")
        
        val content = file.readText()
        assertTrue(
            content.contains(expectedContent),
            "File $relativePath should contain: $expectedContent"
        )
    }
    
    fun assertFileMatches(projectDir: File, relativePath: String, regex: Regex) {
        val file = File(projectDir, relativePath)
        assertTrue(file.exists(), "File should exist: $relativePath")
        
        val content = file.readText()
        assertTrue(
            regex.containsMatchIn(content),
            "File $relativePath should match regex: ${regex.pattern}"
        )
    }
    
    fun assertFilesEqual(expected: File, actual: File, message: String? = null) {
        if (!expected.exists()) fail("Expected file does not exist: ${expected.path}")
        if (!actual.exists()) fail("Actual file does not exist: ${actual.path}")
        
        val expectedContent = expected.readText()
        val actualContent = actual.readText()
        
        if (expectedContent != actualContent) {
            // Show diff for better debugging
            val expectedLines = expectedContent.lines()
            val actualLines = actualContent.lines()
            
            val maxLines = maxOf(expectedLines.size, actualLines.size)
            var diffCount = 0
            val maxDiffsToShow = 20 // Limit output for readability
            
            val diffMessage = buildString {
                appendLine(message ?: "Files should be equal: ${expected.name}")
                appendLine("Expected ${expectedLines.size} lines, got ${actualLines.size} lines")
                appendLine("\nDifferences found:")
                
                for (i in 0 until maxLines) {
                    val expLine = expectedLines.getOrNull(i)
                    val actLine = actualLines.getOrNull(i)
                    
                    if (expLine != actLine) {
                        diffCount++
                        if (diffCount <= maxDiffsToShow) {
                            appendLine("\n  Line ${i + 1}:")
                            appendLine("    Expected: ${expLine?.take(100) ?: "<missing>"}")
                            appendLine("    Actual:   ${actLine?.take(100) ?: "<missing>"}")
                        }
                    }
                }
                
                if (diffCount > maxDiffsToShow) {
                    appendLine("\n... and ${diffCount - maxDiffsToShow} more differences (showing first $maxDiffsToShow)")
                }

                appendLine("\nTotal differences: $diffCount")
            }

            if (diffCount > 0) {
                fail(diffMessage)
            }
        }
    }
    
    fun assertGitInitialized(projectDir: File) {
        val gitDir = File(projectDir, ".git")
        assertTrue(gitDir.exists(), ".git directory should exist when initGit is true")
        assertTrue(gitDir.isDirectory, ".git should be a directory")
        
        val gitConfig = File(gitDir, "config")
        assertTrue(gitConfig.exists(), ".git/config should exist")
        
        val gitHead = File(gitDir, "HEAD")
        assertTrue(gitHead.exists(), ".git/HEAD should exist")
        
        val gitRefs = File(gitDir, "refs")
        assertTrue(gitRefs.exists(), ".git/refs directory should exist")
        
        val gitObjects = File(gitDir, "objects")
        assertTrue(gitObjects.exists(), ".git/objects directory should exist")
        
        // Check that files are staged (added but not committed)
        val process = Runtime.getRuntime().exec(
            arrayOf("git", "status", "--short"),
            null,
            projectDir
        )
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        assertTrue(
            output.lines().any { it.startsWith("A ") },
            "Git repository should have staged files (marked with 'A')"
        )
    }
    
    fun assertTestsIncluded(projectDir: File) {
        val buildGradle = File(projectDir, "composeApp/build.gradle.kts")
        assertTrue(buildGradle.exists(), "composeApp/build.gradle.kts should exist")
        
        val buildGradleContent = buildGradle.readText()
        assertTrue(
            buildGradleContent.contains("commonTest.dependencies"),
            "build.gradle.kts should contain commonTest.dependencies block"
        )
        
        val libsVersions = File(projectDir, "gradle/libs.versions.toml")
        assertTrue(libsVersions.exists(), "gradle/libs.versions.toml should exist")
        
        val libsContent = libsVersions.readText()
        assertTrue(
            libsContent.contains("kotlin-test"),
            "libs.versions.toml should contain kotlin-test dependency"
        )
        
        val commonTestDir = File(projectDir, "composeApp/src/commonTest")
        assertTrue(commonTestDir.exists(), "commonTest directory should exist when tests are included")
        
        val testFiles = commonTestDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertTrue(testFiles.isNotEmpty(), "commonTest should contain .kt test files")
    }
    
    fun compareDirectories(
        expected: File, 
        actual: File, 
        excludeExtensions: Set<String> = emptySet(),
        excludeFiles: Set<String> = emptySet()
    ) {
        if (!expected.exists()) fail("Expected directory does not exist: ${expected.path}")
        if (!actual.exists()) fail("Actual directory does not exist: ${actual.path}")
        
        // Helper to map fixture file names to actual file names (gitignore → .gitignore)
        fun normalizeFileName(path: String): String {
            return path.replace("/gitignore", "/.gitignore").replace("^gitignore$".toRegex(), ".gitignore")
        }
        
        val expectedFiles = expected.walkTopDown()
            .onEnter { true }  // Include all directories, including hidden ones
            .filter { it.isFile }
            .filter { it.extension !in excludeExtensions }
            .filter { it.relativeTo(expected).path !in excludeFiles }
            .map { normalizeFileName(it.relativeTo(expected).path) }
            .toSet()
        
        val actualFiles = actual.walkTopDown()
            .onEnter { true }  // Include all directories, including hidden ones
            .filter { it.isFile }
            .filter { it.extension !in excludeExtensions }
            .filter { it.relativeTo(actual).path !in excludeFiles }
            .map { it.relativeTo(actual).path }
            .toSet()
        
        // Check for missing files
        val missingFiles = expectedFiles - actualFiles
        if (missingFiles.isNotEmpty()) {
            fail("Missing files in generated project:\n${missingFiles.joinToString("\n") { "  - $it" }}")
        }
        
        // Check for extra files
        val extraFiles = actualFiles - expectedFiles
        if (extraFiles.isNotEmpty()) {
            fail("Extra files in generated project:\n${extraFiles.joinToString("\n") { "  - $it" }}")
        }
        
        // Compare content of each file
        expectedFiles.forEach { normalizedPath ->
            // Map back: .gitignore in expected path → gitignore in fixture
            val fixtureRelativePath = normalizedPath.replace("/.gitignore", "/gitignore").replace("^\\.gitignore$".toRegex(), "gitignore")
            val expectedFile = File(expected, fixtureRelativePath)
            val actualFile = File(actual, normalizedPath)
            
            if (normalizedPath.endsWith("project.pbxproj")) {
                assertPbxprojFilesEqualIgnoringUUIDs(expectedFile, actualFile, "File should match fixture: $normalizedPath")
            } else {
                assertFilesEqual(expectedFile, actualFile, "File should match fixture: $normalizedPath")
            }
        }
    }
    
    fun assertPbxprojFilesEqualIgnoringUUIDs(expected: File, actual: File, message: String? = null) {
        if (!expected.exists()) fail("Expected file does not exist: ${expected.path}")
        if (!actual.exists()) fail("Actual file does not exist: ${actual.path}")
        
        val expectedContent = expected.readText()
        val actualContent = actual.readText()
        
        val expectedNormalized = normalizeUUIDs(expectedContent)
        val actualNormalized = normalizeUUIDs(actualContent)
        
        if (expectedNormalized != actualNormalized) {
            val expectedLines = expectedNormalized.lines()
            val actualLines = actualNormalized.lines()
            
            val maxLines = maxOf(expectedLines.size, actualLines.size)
            var diffCount = 0
            val maxDiffsToShow = 20
            
            val diffMessage = buildString {
                appendLine(message ?: "Pbxproj files should be equal (ignoring UUIDs): ${expected.name}")
                appendLine("Expected ${expectedLines.size} lines, got ${actualLines.size} lines")
                appendLine("\nDifferences found:")
                
                for (i in 0 until maxLines) {
                    val expLine = expectedLines.getOrNull(i)
                    val actLine = actualLines.getOrNull(i)
                    
                    if (expLine != actLine) {
                        diffCount++
                        if (diffCount <= maxDiffsToShow) {
                            appendLine("\n  Line ${i + 1}:")
                            appendLine("    Expected: ${expLine?.take(100) ?: "<missing>"}")
                            appendLine("    Actual:   ${actLine?.take(100) ?: "<missing>"}")
                        }
                    }
                }
                
                if (diffCount > maxDiffsToShow) {
                    appendLine("\n... and ${diffCount - maxDiffsToShow} more differences (showing first $maxDiffsToShow)")
                }

                appendLine("\nTotal differences: $diffCount")
            }

            if (diffCount > 0) {
                fail(diffMessage)
            }
        }
    }
    
    private fun normalizeUUIDs(content: String): String {
        val uuidPattern = Regex("[0-9A-F]{24}")
        var normalized = content
        val foundUuids = uuidPattern.findAll(content).map { it.value }.distinct().toList()
        
        foundUuids.forEachIndexed { index, uuid ->
            val placeholder = "UUID${index.toString().padStart(20, '0')}"
            normalized = normalized.replace(uuid, placeholder)
        }
        
        return normalized
    }
    
    private fun getRelativeFilePaths(root: File, ignoreFiles: Set<String>): Set<String> {
        return root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val relativePath = file.relativeTo(root).path
                ignoreFiles.none { ignore -> relativePath.contains(ignore) }
            }
            .map { it.relativeTo(root).path }
            .toSet()
    }
    
    private fun normalizeContent(content: String): String {
        return content
            .trim()
            .replace("\\r\\n".toRegex(), "\n")
            .replace("\\s+$".toRegex(), "")
    }
}

