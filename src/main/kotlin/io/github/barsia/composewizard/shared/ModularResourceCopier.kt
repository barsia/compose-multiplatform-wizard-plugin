package io.github.barsia.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class ModularResourceCopier {
    
    fun readResourceFile(path: String): String {
        val classLoader = ModularResourceCopier::class.java.classLoader
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
    
    fun copyResourceDirectory(resourcePath: String, targetPath: String, packagePathReplacement: Pair<String, String>? = null) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(resourcePath) ?: return
        
        try {
            when (resourceUrl.protocol) {
                URLUtil.JAR_PROTOCOL -> {
                    copyFromJarWithPackageReplacement(resourceUrl, resourcePath, targetPath, packagePathReplacement)
                }
                URLUtil.FILE_PROTOCOL -> {
                    val resourceFile = File(resourceUrl.toURI())
                    if (packagePathReplacement != null) {
                        copyDirWithPackageReplacement(resourceFile, File(targetPath), packagePathReplacement)
                    } else {
                        FileUtil.copyDir(resourceFile, File(targetPath))
                    }
                }
                else -> {
                    val resourceFile = File(resourceUrl.toURI())
                    if (packagePathReplacement != null) {
                        copyDirWithPackageReplacement(resourceFile, File(targetPath), packagePathReplacement)
                    } else {
                        FileUtil.copyDir(resourceFile, File(targetPath))
                    }
                }
            }
        } catch (e: Exception) {
        }
    }
    
    private fun copyDirWithPackageReplacement(source: File, target: File, packageReplacement: Pair<String, String>) {
        source.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = file.relativeTo(source).path
                val newPath = relativePath.replace(packageReplacement.first, packageReplacement.second)
                val targetFile = File(target, newPath)
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }
    
    private fun copyFromJarWithPackageReplacement(jarUrl: URL, resourcePath: String, targetPath: String, packageReplacement: Pair<String, String>?) {
        if (packageReplacement == null) {
            copyFromJar(jarUrl, resourcePath, targetPath)
            return
        }
        
        val splitJarPath = splitJarPath(jarUrl.file)
        val mayBeEscapedFile = java.net.URI(splitJarPath.first).toURL().file
        val file = URLUtil.unescapePercentSequences(mayBeEscapedFile)
        val jarFile = java.util.jar.JarFile(file)
        val prefix = splitJarPath.second
        
        jarFile.entries().asIterator().forEach { entry ->
            if (!entry.isDirectory && entry.name.startsWith(prefix)) {
                val relativePath = entry.name.substring(prefix.length)
                if (relativePath.isNotEmpty()) {
                    val newPath = relativePath.replace(packageReplacement.first, packageReplacement.second)
                    val targetFile = File(targetPath, newPath)
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
    
    fun copyResourceFile(resourcePath: String, targetPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
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
        val prefix = if (modulePath.endsWith("/")) modulePath else "$modulePath/"
        
        val relativeToBase = when {
            modulePath.startsWith("templates/modular/base/") -> {
                modulePath.substring("templates/modular/base/".length)
            }
            modulePath.startsWith("templates/modular/modules/") -> {
                val afterModules = modulePath.substring("templates/modular/modules/".length)
                val pathParts = afterModules.split("/")
                if (pathParts.size >= 3 && pathParts[1] == "src") {
                    "composeApp/src/${pathParts[2]}"
                } else {
                    "composeApp/src"
                }
            }
            modulePath.startsWith("templates/modular/features/") -> {
                val afterFeatures = modulePath.substring("templates/modular/features/".length)
                val pathParts = afterFeatures.split("/")
                if (pathParts.size >= 3 && pathParts[1] == "src") {
                    "composeApp/src/${pathParts[2]}"
                } else {
                    "composeApp/src"
                }
            }
            else -> {
                modulePath
            }
        }
        
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith(prefix) && entry.name != prefix) {
                val relativePath = entry.name.substring(prefix.length)
                val targetFile = File(targetPath, "$relativeToBase/$relativePath")
                
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
    
    fun copyFromJarWithExclusions(jarUrl: URL, sourcePath: String, targetPath: String, excludePatterns: List<String> = emptyList(), renameMap: Map<String, String> = emptyMap()) {
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
                
                // Check if this entry matches any exclusion pattern
                val shouldExclude = excludePatterns.any { pattern ->
                    when {
                        pattern == "**/Platform.*.kt" -> {
                            // Special case: exclude Platform.xxx.kt but not Platform.kt
                            val fileName = relativePath.substringAfterLast('/')
                            fileName.matches(Regex("Platform\\..+\\.kt"))
                        }
                        pattern == "**/Platform.kt" -> {
                            relativePath.endsWith("/Platform.kt") || relativePath == "Platform.kt"
                        }
                        pattern.matches(Regex("\\*\\*/.*?/\\*\\*")) -> {
                            // Pattern like **/kotlin/** - match any path containing /kotlin/
                            val dirName = pattern.substring(3, pattern.length - 3) // Extract "kotlin" from "**/kotlin/**"
                            relativePath.contains("/$dirName/") || relativePath.startsWith("$dirName/")
                        }
                        pattern.startsWith("**/") -> {
                            val suffix = pattern.substring(3)
                            relativePath.endsWith(suffix) || relativePath.contains("/$suffix")
                        }
                        pattern.startsWith("**") -> relativePath.endsWith(pattern.substring(2))
                        else -> relativePath.endsWith(pattern)
                    }
                }
                
                if (shouldExclude) continue
                
                var targetRelativePath = relativePath
                renameMap.forEach { (from, to) ->
                    if (relativePath.endsWith(from)) {
                        targetRelativePath = relativePath.replace(from, to)
                    }
                }
                
                val targetFile = File(targetPath, targetRelativePath)
                
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
    
    fun processAllFiles(dir: File, projectName: String, projectId: String, projectIdPath: String, composeVersion: String, config: io.github.barsia.composewizard.generator.ProjectConfig? = null) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile && shouldProcessFile(file)) {
                processFile(file, projectName, projectId, projectIdPath, composeVersion, config)
            }
        }
    }
    
    private fun shouldProcessFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("kt", "kts", "xml", "swift", "xcconfig", "json", "html", "css", "md", "properties", "toml")
    }
    
    private fun processFile(file: File, projectName: String, projectId: String, projectIdPath: String, composeVersion: String, config: io.github.barsia.composewizard.generator.ProjectConfig?) {
        try {
            val content = file.readText()
            var newContent = content
            
            // For code files (.kt, .swift), replace hardcoded package names
            // For config files (.kts, .xml, .xcconfig), only replace placeholders
            val ext = file.extension.lowercase()
            val isCodeFile = ext in setOf("kt", "swift")
            
            if (isCodeFile) {
                // Replace hardcoded package names in code (without placeholders)
                newContent = newContent.replace("org.example.project", projectId)
                newContent = newContent.replace("org/example/project", projectIdPath)
            }
            
            // Replace placeholders in all files
            newContent = newContent.replace("\$PROJECT_NAME\$", projectName)
            newContent = newContent.replace("{{PROJECT_NAME}}", projectName)
            
            newContent = newContent.replace("\$PROJECT_NAME_LOWERCASE\$", projectName.lowercase())
            
            newContent = newContent.replace("\$PACKAGE_ID\$", projectId)
            newContent = newContent.replace("{{PROJECT_ID}}", projectId)
            
            newContent = newContent.replace("\$PACKAGE_NAME\$", projectId)
            
            newContent = newContent.replace("{{COMPOSE_VERSION}}", composeVersion)
            
            if (config != null) {
                val platforms = mutableListOf<String>()
                if (config.targetAndroid) platforms.add("Android")
                if (config.targetIOS) platforms.add("iOS")
                if (config.targetDesktop) platforms.add("Desktop (JVM)")
                if (config.targetWeb) platforms.add("Web")
                newContent = newContent.replace("{{PLATFORMS_LIST}}", platforms.joinToString(", "))
                
                val webFeedback = if (config.targetWeb) {
                    "\nWe would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).\nIf you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).\n"
                } else {
                    ""
                }
                newContent = newContent.replace("{{WEB_FEEDBACK}}", webFeedback)
                
                newContent = newContent.replace("{{GRADLE_XMX}}", "-Xmx4096M")
                
                val androidGradleProperties = if (config.targetAndroid) {
                    """

#Android
android.nonTransitiveRClass=true
android.useAndroidX=true"""
                } else {
                    ""
                }
                newContent = newContent.replace("{{ANDROID_GRADLE_PROPERTIES}}", androidGradleProperties)
                
                val foojayPlugin = if (config.targetDesktop) {
                    """
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

"""
                } else {
                    "\n"
                }
                newContent = newContent.replace("{{FOOJAY_PLUGIN}}", foojayPlugin)
            }
            
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
        
        // Collect all files in old package structure (but skip already moved files)
        val filesToMove = mutableListOf<Pair<File, File>>()
        composeAppSrc.walkTopDown().forEach { file ->
            if (file.isFile && file.path.contains("/$oldPackagePath/") && !file.path.contains("/$newPackagePath/")) {
                val newPath = File(file.path.replace("/$oldPackagePath/", "/$newPackagePath/"))
                filesToMove.add(file to newPath)
            }
        }
        
        // Move files to new package structure
        filesToMove.forEach { (oldFile, newFile) ->
            newFile.parentFile?.mkdirs()
            oldFile.copyTo(newFile, overwrite = true)
            oldFile.delete()
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

