package io.github.heisiar.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class ModularResourceCopier {
    
    fun readResourceFile(path: String): String {
        val classLoader = ModularTemplateProcessor::class.java.classLoader
        val stream = classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Resource not found: $path")
        return stream.bufferedReader().use { it.readText() }
    }
    
    fun readResourceFileOrEmpty(path: String): String {
        return try {
            readResourceFile(path)
        } catch (e: Exception) {
            ""
        }
    }
    
    fun copyResourceDirectory(resourcePath: String, targetPath: String) {
        val classLoader = ModularTemplateProcessor::class.java.classLoader
        val resourceUrl = classLoader.getResource(resourcePath) ?: return
        
        try {
            when (resourceUrl.protocol) {
                URLUtil.JAR_PROTOCOL -> {
                    copyFromJar(resourceUrl, resourcePath, targetPath)
                }
                URLUtil.FILE_PROTOCOL -> {
                    val resourceFile = File(resourceUrl.toURI())
                    FileUtil.copyDir(resourceFile, File(targetPath))
                }
                else -> {
                    val resourceFile = File(resourceUrl.toURI())
                    FileUtil.copyDir(resourceFile, File(targetPath))
                }
            }
        } catch (e: Exception) {
        }
    }
    
    fun copyResourceFile(resourcePath: String, targetPath: String) {
        val classLoader = ModularTemplateProcessor::class.java.classLoader
        val stream = classLoader.getResourceAsStream(resourcePath) ?: return
        
        val targetFile = File(targetPath)
        targetFile.parentFile?.mkdirs()
        stream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    
    private fun copyFromJar(jarUrl: URL, resourcePath: String, targetPath: String) {
        val splitJarPath = splitJarPath(jarUrl.file)
        val mayBeEscapedFile = java.net.URI(splitJarPath.first).toURL().file
        val file = URLUtil.unescapePercentSequences(mayBeEscapedFile)
        val jarFile = java.util.jar.JarFile(file)
        val prefix = splitJarPath.second
        
        val entries = jarFile.entries()
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
                }
            }
        }
    }
    
    fun copyModuleSrcFromJar(jarUrl: URL, modulePath: String, targetPath: String) {
        val splitJarPath = splitJarPath(jarUrl.file)
        val mayBeEscapedFile = java.net.URI(splitJarPath.first).toURL().file
        val file = URLUtil.unescapePercentSequences(mayBeEscapedFile)
        val jarFile = java.util.jar.JarFile(file)
        val prefix = if (splitJarPath.second.endsWith("/")) splitJarPath.second else "${splitJarPath.second}/"
        
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith(prefix) && entry.name != prefix) {
                val relativePath = entry.name.substring(prefix.length)
                val targetFile = File(targetPath, "composeApp/src/$relativePath")
                
                if (!entry.isDirectory) {
                    targetFile.parentFile?.mkdirs()
                    jarFile.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
    
    private fun splitJarPath(jarPath: String): Pair<String, String> {
        val index = jarPath.indexOf("!/")
        return if (index != -1) {
            Pair(jarPath.substring(0, index), jarPath.substring(index + 2))
        } else {
            Pair(jarPath, "")
        }
    }
    
    fun processAllFiles(dir: File, projectName: String, projectId: String, projectIdPath: String, composeVersion: String) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile && shouldProcessFile(file)) {
                processFile(file, projectName, projectId, projectIdPath, composeVersion)
            }
        }
    }
    
    private fun shouldProcessFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("kt", "kts", "xml", "swift", "xcconfig", "json", "html", "css", "md", "properties", "toml")
    }
    
    private fun processFile(file: File, projectName: String, projectId: String, projectIdPath: String, composeVersion: String) {
        try {
            val content = file.readText()
            var newContent = content
            
            newContent = newContent.replace("{{PROJECT_NAME}}", projectName)
            newContent = newContent.replace("KotlinProject", projectName)
            newContent = newContent.replace("kotlinproject", projectName.lowercase())
            
            newContent = newContent.replace("{{PROJECT_ID}}", projectId)
            newContent = newContent.replace("org.example.project", projectId)
            newContent = newContent.replace("org/example/project", projectIdPath)
            
            newContent = newContent.replace("{{COMPOSE_VERSION}}", composeVersion)
            
            if (newContent != content) {
                file.writeText(newContent)
            }
        } catch (e: Exception) {
        }
    }
    
    fun movePackageStructure(targetPath: String, projectIdPath: String) {
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
}
