package io.github.heisiar.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class ModularTemplateProcessor(
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
    private val selectedPlatforms = buildList {
        if (targetAndroid) add("android")
        if (targetIOS) add("ios")
        if (targetDesktop) add("desktop")
        if (targetWeb) add("web")
    }
    
    fun copyTemplateToProject(targetPath: String) {
        copyBaseTemplate(targetPath)
        
        for (platform in selectedPlatforms) {
            copyPlatformModule(platform, targetPath)
        }
        
        if (includeTests) {
            copyTestsFeature(targetPath)
        }
        
        generateBuildFiles(targetPath)
        generateReadme(targetPath)
        
        processAllFiles(File(targetPath))
        movePackageStructure(targetPath)
    }
    
    private fun copyBaseTemplate(targetPath: String) {
        val basePath = "templates/modular/base"
        
        File(targetPath).mkdirs()
        
        copyResourceDirectory("$basePath/gradle", "$targetPath/gradle")
        
        updateLibraryVersions(targetPath)
        
        // Use shared gradlew scripts from templates root
        copyResourceFile("templates/gradlew", "$targetPath/gradlew")
        copyResourceFile("templates/gradlew.bat", "$targetPath/gradlew.bat")
        copyResourceFile("$basePath/gradle.properties", "$targetPath/gradle.properties")
        copyResourceFile("$basePath/local.properties", "$targetPath/local.properties")
        
        var settingsContent = readResourceFile("$basePath/settings.gradle.kts")
        
        val devMavenRepo = if (enableDevVersions) {
            """maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")"""
        } else {
            ""
        }
        settingsContent = settingsContent.replace("{{DEV_MAVEN_REPO}}", devMavenRepo)
        
        File(targetPath, "settings.gradle.kts").writeText(settingsContent)
        
        copyResourceDirectory("$basePath/composeApp/src/commonMain", "$targetPath/composeApp/src/commonMain")
    }
    
    private fun updateLibraryVersions(targetPath: String) {
        val libsVersionsFile = File(targetPath, "gradle/libs.versions.toml")
        if (!libsVersionsFile.exists()) {
            println("WARNING: libs.versions.toml not found at ${libsVersionsFile.absolutePath}")
            return
        }
        
        try {
            var content = libsVersionsFile.readText()
            content = content.replace("{{COMPOSE_VERSION}}", composeVersion)
            libsVersionsFile.writeText(content)
            println("Updated libs.versions.toml with Compose version: $composeVersion")
        } catch (e: Exception) {
            println("ERROR updating library versions: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun copyPlatformModule(platform: String, targetPath: String) {
        val modulePath = "templates/modular/modules/$platform"
        
        val classLoader = ModularTemplateProcessor::class.java.classLoader
        val srcResourceUrl = classLoader.getResource("$modulePath/src")
        
        if (srcResourceUrl != null) {
            when (srcResourceUrl.protocol) {
                URLUtil.JAR_PROTOCOL -> {
                    copyModuleSrcFromJar(srcResourceUrl, modulePath, targetPath)
                }
                URLUtil.FILE_PROTOCOL -> {
                    val srcDir = File(srcResourceUrl.toURI())
                    srcDir.listFiles()?.forEach { sourceSetDir ->
                        if (sourceSetDir.isDirectory) {
                            val targetSrcDir = File(targetPath, "composeApp/src/${sourceSetDir.name}")
                            FileUtil.copyDir(sourceSetDir, targetSrcDir)
                        }
                    }
                }
            }
        }
        
        if (platform == "ios") {
            copyResourceDirectory("$modulePath/iosApp", "$targetPath/iosApp")
        }
        
        if (platform == "web") {
            copyResourceDirectory("$modulePath/webpack.config.d", "$targetPath/composeApp/webpack.config.d")
        }
    }
    
    private fun copyModuleSrcFromJar(jarUrl: URL, modulePath: String, targetPath: String) {
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
    
    private fun copyTestsFeature(targetPath: String) {
        val testsPath = "templates/modular/features/tests"
        copyResourceDirectory("$testsPath/src/commonTest", "$targetPath/composeApp/src/commonTest")
    }
    
    private fun generateBuildFiles(targetPath: String) {
        generateRootBuildFile(targetPath)
        generateComposeAppBuildFile(targetPath)
    }
    
    private fun generateRootBuildFile(targetPath: String) {
        val template = readResourceFile("templates/modular/base/build.gradle.kts.template")
        
        val rootPlugins = buildString {
            for (platform in selectedPlatforms) {
                val fragment = readResourceFileOrEmpty("templates/modular/modules/$platform/root-plugin.fragment")
                if (fragment.isNotBlank()) {
                    appendLine(fragment)
                }
            }
        }
        
        val content = template.replace("{{ROOT_PLUGINS}}", rootPlugins)
        
        File(targetPath, "build.gradle.kts").writeText(content)
    }
    
    private fun generateComposeAppBuildFile(targetPath: String) {
        val template = readResourceFile("templates/modular/base/composeApp/build.gradle.kts.template")
        
        val imports = collectFragments("import")
        val plugins = collectFragments("plugin")
        val targets = collectFragments("target")
        val sourceSets = buildString {
            appendLine(readResourceFile("templates/modular/base/commonMain.sourceset.fragment"))
            appendLine(collectFragments("sourceset"))
            if (includeTests) {
                appendLine(readResourceFile("templates/modular/features/tests/sourceset.fragment"))
            }
        }
        val androidConfig = if (targetAndroid) collectFragments("config", listOf("android")) else ""
        val desktopConfig = if (targetDesktop) collectFragments("config", listOf("desktop")) else ""
        val dependencies = collectFragments("dependency")
        
        val hotReloadPlugin = if (needsHotReload()) "    alias(libs.plugins.composeHotReload)" else ""
        
        var content = template
            .replace("{{IMPORTS}}", imports)
            .replace("{{PLUGINS}}", plugins)
            .replace("{{HOT_RELOAD_PLUGIN}}", hotReloadPlugin)
            .replace("{{TARGETS}}", targets)
            .replace("{{SOURCE_SETS}}", sourceSets)
            .replace("{{ANDROID_CONFIG}}", androidConfig)
            .replace("{{DESKTOP_CONFIG}}", desktopConfig)
            .replace("{{DEPENDENCIES}}", dependencies)
        
        content = content.lines()
            .filter { it.isNotBlank() || it.trim().isEmpty() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
        
        File(targetPath, "composeApp/build.gradle.kts").writeText(content)
    }
    
    private fun collectFragments(fragmentType: String, platforms: List<String>? = null): String {
        val platformsToUse = platforms ?: selectedPlatforms
        return buildString {
            for (platform in platformsToUse) {
                val fragment = readResourceFileOrEmpty("templates/modular/modules/$platform/$fragmentType.fragment")
                if (fragment.isNotBlank()) {
                    appendLine(fragment)
                }
            }
        }
    }
    
    private fun needsHotReload(): Boolean {
        return targetDesktop || selectedPlatforms.size > 1
    }
    
    private fun generateReadme(targetPath: String) {
        val baseReadme = readResourceFile("templates/modular/base/README.md")
        
        val platformsList = selectedPlatforms.joinToString(", ") { platform ->
            when (platform) {
                "android" -> "Android"
                "ios" -> "iOS"
                "desktop" -> "Desktop (JVM)"
                "web" -> "Web"
                else -> platform
            }
        }
        
        val platformSections = buildString {
            for (platform in selectedPlatforms) {
                val section = readResourceFileOrEmpty("templates/modular/modules/$platform/readme.fragment")
                if (section.isNotBlank()) {
                    appendLine(section)
                    appendLine()
                }
            }
        }
        
        val webFeedback = if (targetWeb) {
            readResourceFileOrEmpty("templates/modular/modules/web/readme-footer.fragment")
        } else {
            ""
        }
        
        val content = baseReadme
            .replace("{{PLATFORMS_LIST}}", platformsList)
            .replace("{{PLATFORM_SPECIFIC_SECTIONS}}", platformSections)
            .replace("{{WEB_FEEDBACK}}", webFeedback)
        
        File(targetPath, "README.md").writeText(content)
    }
    
    private fun readResourceFile(path: String): String {
        val classLoader = ModularTemplateProcessor::class.java.classLoader
        val stream = classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Resource not found: $path")
        return stream.bufferedReader().use { it.readText() }
    }
    
    private fun readResourceFileOrEmpty(path: String): String {
        return try {
            readResourceFile(path)
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun copyResourceDirectory(resourcePath: String, targetPath: String) {
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
            println("ERROR copying $resourcePath: ${e.message}")
        }
    }
    
    private fun copyResourceFile(resourcePath: String, targetPath: String) {
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
}

