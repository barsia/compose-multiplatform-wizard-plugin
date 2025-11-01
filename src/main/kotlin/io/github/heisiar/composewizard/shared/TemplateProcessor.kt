package io.github.heisiar.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class TemplateProcessor(
    private val projectName: String,
    private val projectId: String,
    private val composeVersion: String,
    private val includeTests: Boolean,
    private val targetDesktop: Boolean,
    private val targetAndroid: Boolean,
    private val targetIOS: Boolean,
    private val targetWeb: Boolean,
    private val enableDevVersions: Boolean = false
) {
    
    private val projectIdPath = projectId.replace('.', '/')
    
    fun determineTemplate(): String {
        val selectedCount = listOf(targetDesktop, targetAndroid, targetIOS, targetWeb).count { it }
        
        return when {
            selectedCount == 4 -> "All"
            selectedCount == 1 && targetAndroid -> "Android"
            selectedCount == 1 && targetIOS -> "iOS"
            selectedCount == 1 && targetDesktop -> "Desktop"
            selectedCount == 1 && targetWeb -> "Web"
            else -> "All"
        }
    }
    
    fun copyTemplateToProject(targetPath: String) {
        val modularProcessor = ModularTemplateProcessor(
            projectName = projectName,
            projectId = projectId,
            composeVersion = composeVersion,
            includeTests = includeTests,
            targetDesktop = targetDesktop,
            targetAndroid = targetAndroid,
            targetIOS = targetIOS,
            targetWeb = targetWeb,
            enableDevVersions = enableDevVersions
        )
        
        modularProcessor.copyTemplateToProject(targetPath)
    }
    
    @Deprecated("Legacy method - use modular system instead")
    fun copyTemplateToProjectLegacy(targetPath: String) {
        val template = determineTemplate()
        val templateBase = "templates/$template"
        
        copyResourceDirectory(templateBase, targetPath)
        
        processAllFiles(File(targetPath))
        
        movePackageStructure(targetPath)
        
        cleanupUnusedPlatformFiles(targetPath)
        
        if (includeTests) {
            createTestDirectories(targetPath)
        }
    }
    
    private fun copyResourceDirectory(resourcePath: String, targetPath: String) {
        val classLoader = TemplateProcessor::class.java.classLoader
        val resourceUrl = classLoader.getResource(resourcePath)
        
        if (resourceUrl == null) {
            println("ERROR: Resource not found: $resourcePath")
            println("Trying to list available resources...")
            val testUrl = classLoader.getResource("templates")
            println("templates/ URL: $testUrl")
            return
        }
        
        println("Found resource: $resourcePath at $resourceUrl (protocol: ${resourceUrl.protocol})")
        
        try {
            when (resourceUrl.protocol) {
                URLUtil.JAR_PROTOCOL -> {
                    println("Copying from JAR...")
                    copyFromJar(resourceUrl, resourcePath, targetPath)
                }
                URLUtil.FILE_PROTOCOL -> {
                    val resourceFile = File(resourceUrl.toURI())
                    println("Copying from filesystem: ${resourceFile.absolutePath} to $targetPath")
                    FileUtil.copyDir(resourceFile, File(targetPath))
                }
                else -> {
                    println("Unknown protocol: ${resourceUrl.protocol}, trying direct file copy...")
                    val resourceFile = File(resourceUrl.toURI())
                    FileUtil.copyDir(resourceFile, File(targetPath))
                }
            }
        } catch (e: Exception) {
            println("ERROR copying resources: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun copyFromJar(jarUrl: URL, resourcePath: String, targetPath: String) {
        val splitJarPath = splitJarPath(jarUrl.file)
        val mayBeEscapedFile = URL(splitJarPath.first).file
        val file = URLUtil.unescapePercentSequences(mayBeEscapedFile)
        val jarFile = java.util.jar.JarFile(file)
        val prefix = splitJarPath.second
        
        println("JAR file: $file, prefix: $prefix")
        
        val entries = jarFile.entries()
        var copiedCount = 0
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith(prefix)) {
                val filename = entry.name.substring(prefix.length)
                if (filename.isEmpty()) continue
                
                val targetFile = File(targetPath, filename)
                
                if (!entry.isDirectory) {
                    targetFile.parentFile?.mkdirs()
                    jarFile.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    copiedCount++
                }
            }
        }
        println("Copied $copiedCount files from JAR")
    }
    
    private fun splitJarPath(jarPath: String): Pair<String, String> {
        val index = jarPath.indexOf("!/")
        return if (index != -1) {
            Pair(jarPath.substring(0, index), jarPath.substring(index + 2))
        } else {
            Pair(jarPath, "")
        }
    }
    
    private fun processAllFiles(dir: File) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile && shouldProcessFile(file)) {
                processFile(file)
            }
        }
    }
    
    private fun shouldProcessFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("kt", "kts", "xml", "swift", "xcconfig", "json", "html", "css", "md", "properties", "toml")
    }
    
    private fun processFile(file: File) {
        try {
            val content = file.readText()
            var newContent = content
            
            newContent = newContent.replace("KotlinProject", projectName)
            newContent = newContent.replace("kotlinproject", projectName.lowercase())
            
            newContent = newContent.replace("org.example.project", projectId)
            newContent = newContent.replace("org/example/project", projectIdPath)
            
            newContent = newContent.replace("{{COMPOSE_VERSION}}", composeVersion)
            
            if (newContent != content) {
                file.writeText(newContent)
            }
        } catch (e: Exception) {
            // Ignore binary files or files that can't be read as text
        }
    }
    
    private fun movePackageStructure(targetPath: String) {
        val oldPackagePath = "org/example/project"
        val newPackagePath = projectIdPath
        
        if (oldPackagePath == newPackagePath) return
        
        val composeAppSrc = File(targetPath, "composeApp/src")
        if (!composeAppSrc.exists()) return
        
        composeAppSrc.walkTopDown().forEach { dir ->
            if (dir.isDirectory && dir.path.contains(oldPackagePath)) {
                val oldPath = File(dir.path)
                val newPath = File(dir.path.replace(oldPackagePath, newPackagePath))
                
                if (oldPath.exists() && oldPath != newPath) {
                    newPath.parentFile?.mkdirs()
                    oldPath.renameTo(newPath)
                }
            }
        }
        
        cleanEmptyDirectories(composeAppSrc)
    }
    
    private fun cleanEmptyDirectories(dir: File) {
        dir.walkBottomUp().forEach { file ->
            if (file.isDirectory && file.listFiles()?.isEmpty() == true) {
                file.delete()
            }
        }
    }
    
    private fun cleanupUnusedPlatformFiles(targetPath: String) {
        val composeAppDir = File(targetPath, "composeApp")
        
        if (!targetWeb) {
            val webpackDir = File(composeAppDir, "webpack.config.d")
            if (webpackDir.exists()) {
                webpackDir.deleteRecursively()
                println("Removed webpack.config.d (Web not selected)")
            }
        }
        
        val srcDir = File(composeAppDir, "src")
        
        if (!targetDesktop) {
            File(srcDir, "jvmMain").deleteRecursively()
        }
        
        if (!targetAndroid) {
            File(srcDir, "androidMain").deleteRecursively()
        }
        
        if (!targetIOS) {
            File(srcDir, "iosMain").deleteRecursively()
        }
        
        if (!targetWeb) {
            File(srcDir, "jsMain").deleteRecursively()
            File(srcDir, "wasmJsMain").deleteRecursively()
            File(srcDir, "webMain").deleteRecursively()
        }
        
        if (!targetIOS) {
            File(targetPath, "iosApp").deleteRecursively()
        }
    }
    
    private fun createTestDirectories(targetPath: String) {
        val composeAppDir = File(targetPath, "composeApp")
        val srcDir = File(composeAppDir, "src")
        
        val commonTestDir = File(srcDir, "commonTest/kotlin/$projectIdPath")
        commonTestDir.mkdirs()
        
        val testFile = File(commonTestDir, "ExampleTest.kt")
        val testContent = """
            package $projectId
            
            import kotlin.test.Test
            import kotlin.test.assertTrue
            
            class ExampleTest {
                @Test
                fun testExample() {
                    assertTrue(true, "This test should pass")
                }
            }
        """.trimIndent()
        
        FileUtil.writeToFile(testFile, testContent)
        
        println("Created test directory: commonTest")
    }
}

