package io.github.heisiar.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class ModularTemplateProcessor(
    private val projectName: String,
    private val projectId: String,
    private val composeVersion: String,
    private val kotlinVersion: String,
    private val lifecycleVersion: String?,
    private val material3Version: String?,
    private val material3AdaptiveVersion: String?,
    private val navigationVersion: String?,
    private val navigationEventVersion: String?,
    private val savedStateVersion: String?,
    private val windowVersion: String?,
    private val hotReloadVersion: String?,
    private val includeTests: Boolean,
    private val targetDesktop: Boolean,
    private val targetAndroid: Boolean,
    private val targetIOS: Boolean,
    private val targetWeb: Boolean,
    private val enableDevVersions: Boolean = false,
    private val includeMaterial3: Boolean = false,
    private val includeMaterial3Adaptive: Boolean = false,
    private val includeNavigation: Boolean = false,
    private val includeNavigationEvent: Boolean = false,
    private val includeSavedState: Boolean = false,
    private val includeWindow: Boolean = false,
    private val includeHotReload: Boolean = false
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
            content = content.replace("{{KOTLIN_VERSION}}", kotlinVersion)
            content = content.replace("{{LIFECYCLE_VERSION}}", lifecycleVersion ?: "2.9.5")
            
            content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_VERSION_BLOCK", 
                                                         includeMaterial3, material3Version, "androidx-material3")
            content = replaceOptionalLibraryVersionBlock(content, "MATERIAL3_ADAPTIVE_VERSION_BLOCK",
                                                         includeMaterial3Adaptive, material3AdaptiveVersion, "androidx-material3-adaptive")
            content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_VERSION_BLOCK",
                                                         includeNavigation, navigationVersion, "androidx-navigation")
            content = replaceOptionalLibraryVersionBlock(content, "NAVIGATION_EVENT_VERSION_BLOCK",
                                                         includeNavigationEvent, navigationEventVersion, "androidx-navigation-event")
            content = replaceOptionalLibraryVersionBlock(content, "SAVED_STATE_VERSION_BLOCK",
                                                         includeSavedState, savedStateVersion, "androidx-savedstate")
            content = replaceOptionalLibraryVersionBlock(content, "WINDOW_VERSION_BLOCK",
                                                         includeWindow, windowVersion, "androidx-window")
            
            content = replaceOptionalLibraryLibrariesBlock(content, "MATERIAL3_LIBRARIES_BLOCK",
                                                           includeMaterial3, "androidx-material3")
            content = replaceOptionalLibraryLibrariesBlock(content, "MATERIAL3_ADAPTIVE_LIBRARIES_BLOCK",
                                                           includeMaterial3Adaptive, "androidx-material3-adaptive")
            content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION_LIBRARIES_BLOCK",
                                                           includeNavigation, "androidx-navigation")
            content = replaceOptionalLibraryLibrariesBlock(content, "NAVIGATION_EVENT_LIBRARIES_BLOCK",
                                                           includeNavigationEvent, "androidx-navigation-event")
            content = replaceOptionalLibraryLibrariesBlock(content, "SAVED_STATE_LIBRARIES_BLOCK",
                                                           includeSavedState, "androidx-savedstate")
            content = replaceOptionalLibraryLibrariesBlock(content, "WINDOW_LIBRARIES_BLOCK",
                                                           includeWindow, "androidx-window")
            
            libsVersionsFile.writeText(content)
            println("Updated libs.versions.toml with Compose version: $composeVersion, Kotlin: $kotlinVersion, Lifecycle: ${lifecycleVersion ?: "2.9.5"}")
        } catch (e: Exception) {
            println("ERROR updating library versions: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun replaceOptionalLibraryVersionBlock(content: String, placeholder: String, include: Boolean, version: String?, versionKey: String): String {
        val replacement = if (include && version != null) {
            "$versionKey = \"$version\""
        } else {
            ""
        }
        return content.replace("{{$placeholder}}", replacement)
    }
    
    private fun replaceOptionalLibraryLibrariesBlock(content: String, placeholder: String, include: Boolean, libraryPrefix: String): String {
        val replacement = if (include) {
            when (libraryPrefix) {
                "androidx-material3" -> """androidx-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "androidx-material3" }"""
                "androidx-material3-adaptive" -> """androidx-material3-adaptive = { module = "org.jetbrains.compose.material3.adaptive:adaptive", version.ref = "androidx-material3-adaptive" }
androidx-material3-adaptive-layout = { module = "org.jetbrains.compose.material3.adaptive:adaptive-layout", version.ref = "androidx-material3-adaptive" }
androidx-material3-adaptive-navigation = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation", version.ref = "androidx-material3-adaptive" }"""
                "androidx-navigation" -> """androidx-navigation = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "androidx-navigation" }"""
                "androidx-navigation-event" -> """androidx-navigation-event = { module = "org.jetbrains.androidx.navigationevent:navigationevent", version.ref = "androidx-navigation-event" }"""
                "androidx-savedstate" -> """androidx-savedstate = { module = "org.jetbrains.androidx.savedstate:savedstate", version.ref = "androidx-savedstate" }"""
                "androidx-window" -> """androidx-window = { module = "org.jetbrains.androidx.window:window-core", version.ref = "androidx-window" }"""
                else -> ""
            }
        } else {
            ""
        }
        return content.replace("{{$placeholder}}", replacement)
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
        
        val optionalLibrariesDependencies = generateOptionalLibrariesDependencies()
        
        val sourceSets = buildString {
            var commonMainFragment = readResourceFile("templates/modular/base/commonMain.sourceset.fragment")
            commonMainFragment = commonMainFragment.replace("{{OPTIONAL_LIBRARIES_DEPENDENCIES}}", optionalLibrariesDependencies)
            appendLine(commonMainFragment)
            appendLine(collectFragments("sourceset"))
            if (includeTests) {
                appendLine(readResourceFile("templates/modular/features/tests/sourceset.fragment"))
            }
        }
        val androidConfig = if (targetAndroid) collectFragments("config", listOf("android")) else ""
        val desktopConfig = if (targetDesktop) collectFragments("config", listOf("desktop")) else ""
        val dependencies = collectFragments("dependency")
        
        val hotReloadPlugin = if (includeHotReload) "    alias(libs.plugins.composeHotReload)" else ""
        
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
        if (!targetDesktop) {
            return false
        }
        
        // Hot Reload becomes part of Compose starting from 1.10.0-beta01
        // No need to add it as a separate dependency for versions >= 1.10.0-beta01
        return isComposeVersionLessThan(composeVersion, "1.10.0-beta01")
    }
    
    private fun isComposeVersionLessThan(version: String, threshold: String): Boolean {
        // Parse version: "1.9.2" or "1.10.0-beta01" or "1.10.0-beta01+dev3194"
        val versionBase = version.split("+").first() // Remove dev suffix
        val thresholdBase = threshold.split("+").first()
        
        // Split into numeric and qualifier parts
        val versionNumeric = versionBase.split("-").first()
        val versionQualifier = versionBase.substringAfter("-", "")
        
        val thresholdNumeric = thresholdBase.split("-").first()
        val thresholdQualifier = thresholdBase.substringAfter("-", "")
        
        // Compare numeric parts (1.9.2 vs 1.10.0)
        val versionParts = versionNumeric.split(".").map { it.toIntOrNull() ?: 0 }
        val thresholdParts = thresholdNumeric.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(versionParts.size, thresholdParts.size)) {
            val v = versionParts.getOrNull(i) ?: 0
            val t = thresholdParts.getOrNull(i) ?: 0
            if (v < t) return true
            if (v > t) return false
        }
        
        // Numeric parts are equal, compare qualifiers
        // If threshold has qualifier but version doesn't, version is greater (stable > beta)
        if (thresholdQualifier.isNotEmpty() && versionQualifier.isEmpty()) {
            return false
        }
        
        // If version has qualifier but threshold doesn't, version is less (beta < stable)
        if (versionQualifier.isNotEmpty() && thresholdQualifier.isEmpty()) {
            return true
        }
        
        // Both have qualifiers or both don't - compare lexicographically
        return versionQualifier < thresholdQualifier
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
    
    private fun generateOptionalLibrariesDependencies(): String {
        val dependencies = buildList {
            if (includeMaterial3 && material3Version != null) {
                add("            implementation(libs.androidx.material3)")
            }
            if (includeMaterial3Adaptive && material3AdaptiveVersion != null) {
                add("            implementation(libs.androidx.material3.adaptive)")
                add("            implementation(libs.androidx.material3.adaptive.layout)")
                add("            implementation(libs.androidx.material3.adaptive.navigation)")
            }
            if (includeNavigation && navigationVersion != null) {
                add("            implementation(libs.androidx.navigation)")
            }
            if (includeNavigationEvent && navigationEventVersion != null) {
                add("            implementation(libs.androidx.navigation.event)")
            }
            if (includeSavedState && savedStateVersion != null) {
                add("            implementation(libs.androidx.savedstate)")
            }
            if (includeWindow && windowVersion != null) {
                add("            implementation(libs.androidx.window)")
            }
        }
        
        return if (dependencies.isNotEmpty()) {
            "\n" + dependencies.joinToString("\n")
        } else {
            ""
        }
    }
}

