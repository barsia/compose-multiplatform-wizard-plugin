package io.github.heisiar.composewizard.testutils

import java.io.File
import java.nio.file.Files

object TemporaryProjectHelper {
    
    fun createTempProjectDir(prefix: String = "compose-test-"): File {
        val tempDir = Files.createTempDirectory(prefix).toFile()
        tempDir.deleteOnExit()
        return tempDir
    }
    
    fun cleanupTempDir(dir: File) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
    
    fun withTempProject(prefix: String = "compose-test-", block: (File) -> Unit) {
        val tempDir = createTempProjectDir(prefix)
        try {
            block(tempDir)
        } finally {
            cleanupTempDir(tempDir)
        }
    }
    
    fun getFixtureDir(fixtureName: String): File {
        val classLoader = TemporaryProjectHelper::class.java.classLoader
        val resource = classLoader.getResource("fixtures/$fixtureName")
            ?: throw IllegalArgumentException("Fixture not found: fixtures/$fixtureName")
        return File(resource.toURI())
    }
}

