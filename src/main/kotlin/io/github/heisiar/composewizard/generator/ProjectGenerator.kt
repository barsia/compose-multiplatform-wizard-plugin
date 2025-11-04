package io.github.heisiar.composewizard.generator

import com.intellij.openapi.util.io.FileUtil
import io.github.heisiar.composewizard.generator.xcode.SecureRandomUUIDGenerator
import io.github.heisiar.composewizard.generator.xcode.XcodeProjectGenerator
import java.io.File
import java.util.jar.JarFile

class ProjectGenerator(private val config: ProjectConfig) {
    
    private val buildGradleGenerator = BuildGradleGenerator()
    private val settingsGradleGenerator = SettingsGradleGenerator()
    private val libsVersionsGenerator = LibsVersionsGenerator()
    
    fun generateProject(projectPath: String) {
        val projectDir = File(projectPath)
        projectDir.mkdirs()
        
        copyGradleWrapper(projectDir)
        
        copyBaseFiles(projectDir)
        
        generateBuildFiles(projectDir)
        
        createComposeAppModule(projectDir)
        
        if (config.initGit) {
            initializeGitRepository(projectDir)
        }
    }
    
    private fun initializeGitRepository(projectDir: File) {
        try {
            val processBuilder = ProcessBuilder("git", "init")
            processBuilder.directory(projectDir)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().readText()
                System.err.println("WARNING: Git init failed with exit code $exitCode: $output")
                System.err.println("Project created without git initialization. You can run 'git init' manually later.")
            }
        } catch (e: Exception) {
            System.err.println("WARNING: Failed to initialize git repository: ${e.message}")
            System.err.println("Project created without git initialization. You can run 'git init' manually later.")
        }
    }
    
    private fun copyGradleWrapper(projectDir: File) {
        val targetGradleDir = File(projectDir, "gradle/wrapper")
        targetGradleDir.mkdirs()
        
        copyResourceFile(
            "templates/gradle/wrapper/gradle-wrapper.properties",
            File(targetGradleDir, "gradle-wrapper.properties")
        )
        
        copyResourceFile(
            "templates/gradle/wrapper/gradle-wrapper.jar",
            File(targetGradleDir, "gradle-wrapper.jar")
        )
        
        listOf("gradlew", "gradlew.bat").forEach { script ->
            copyResourceFile("templates/$script", File(projectDir, script))?.also {
                it.setExecutable(true)
            }
        }
    }
    
    private fun copyBaseFiles(projectDir: File) {
        // Copy platform-specific or generic gradle.properties
        val gradlePropertiesTemplate = when {
            config.targetAndroid && config.targetIOS -> "templates/android-ios/gradle.properties"  // Android+iOS needs 4GB + Android props
            config.targetAndroid -> "templates/android/gradle.properties"  // Android-only needs 3GB + Android props
            config.targetIOS -> "templates/ios/gradle.properties"  // iOS-only needs 4GB (no Android props)
            else -> "templates/base/gradle.properties"
        }
        copyResourceFile(gradlePropertiesTemplate, File(projectDir, "gradle.properties"))
        copyResourceFile("templates/base/gitignore", File(projectDir, ".gitignore"))  // Copy gitignore as .gitignore
        
        // Generate dynamic README based on selected platforms
        val readmeContent = ReadmeGenerator.generate(config)
        File(projectDir, "README.md").writeText(readmeContent)
        
        // local.properties is only needed for Android projects
        if (config.targetAndroid) {
            copyResourceFile("templates/base/local.properties", File(projectDir, "local.properties"))
        }
    }
    
    private fun generateBuildFiles(projectDir: File) {
        File(projectDir, "build.gradle.kts").writeText(
            buildGradleGenerator.generateRootBuildGradle(config)
        )
        
        File(projectDir, "settings.gradle.kts").writeText(
            settingsGradleGenerator.generate(config)
        )
        
        val gradleDir = File(projectDir, "gradle")
        gradleDir.mkdirs()
        File(gradleDir, "libs.versions.toml").writeText(
            libsVersionsGenerator.generate(config)
        )
    }
    
    private fun createComposeAppModule(projectDir: File) {
        val composeAppDir = File(projectDir, "composeApp")
        composeAppDir.mkdirs()
        
        File(composeAppDir, "build.gradle.kts").writeText(
            buildGradleGenerator.generateComposeAppBuildGradle(config)
        )
        
        if (config.selectedPlatforms.size > 1) {
            copyCommonSources(composeAppDir)
        }
        
        if (Platform.DESKTOP in config.selectedPlatforms) {
            copyDesktopSources(composeAppDir)
        }
        
        if (Platform.ANDROID in config.selectedPlatforms) {
            copyAndroidSources(composeAppDir)
        }
        
        if (Platform.IOS in config.selectedPlatforms) {
            copyIOSSources(composeAppDir)
        }
        
        if (Platform.WEB in config.selectedPlatforms) {
            copyWebSources(composeAppDir)
        }
        
        if (config.includeTests) {
            copyTestSources(composeAppDir)
        }
        
        processSourceFiles(composeAppDir)
        processREADME(projectDir)
        
        if (Platform.IOS in config.selectedPlatforms) {
            generateIOSApp(projectDir)
        }
    }
    
    private fun copyTestSources(composeAppDir: File) {
        val commonTestDir = File(composeAppDir, "src/commonTest/kotlin/${config.projectIdPath}")
        commonTestDir.mkdirs()
        
        copyResourceFile(
            "templates/modular/features/tests/src/commonTest/kotlin/org/example/project/ComposeAppCommonTest.kt",
            File(commonTestDir, "ComposeAppCommonTest.kt")
        )
    }
    
    private fun copyCommonSources(composeAppDir: File) {
        val commonMainDir = File(composeAppDir, "src/commonMain/kotlin/${config.projectIdPath}")
        commonMainDir.mkdirs()
        
        listOf("App.kt", "Greeting.kt").forEach { fileName ->
            copyResourceFile(
                "templates/sources/desktop/kotlin/org/example/project/$fileName",
                File(commonMainDir, fileName)
            )
        }
        
        copyResourceFile(
            "templates/sources/common/kotlin/org/example/project/Platform.kt",
            File(commonMainDir, "Platform.kt")
        )
        
        val resourcesDir = File(composeAppDir, "src/commonMain/composeResources/drawable")
        resourcesDir.mkdirs()
        copyResourceFile(
            "templates/sources/desktop/composeResources/drawable/compose-multiplatform.xml",
            File(resourcesDir, "compose-multiplatform.xml")
        )
    }
    
    private fun copyIOSSources(composeAppDir: File) {
        val iosMainDir = File(composeAppDir, "src/iosMain/kotlin/${config.projectIdPath}")
        iosMainDir.mkdirs()
        
        val isMultiplatform = config.selectedPlatforms.size > 1
        
        if (isMultiplatform) {
            copyResourceFile(
                "templates/sources/ios/kotlin/org/example/project/MainViewController.kt",
                File(iosMainDir, "MainViewController.kt")
            )
            copyResourceFile(
                "templates/sources/ios-actual/kotlin/org/example/project/Platform.ios.kt",
                File(iosMainDir, "Platform.ios.kt")
            )
        } else {
            listOf("App.kt", "Greeting.kt", "Platform.kt", "MainViewController.kt").forEach { fileName ->
                copyResourceFile(
                    "templates/sources/ios/kotlin/org/example/project/$fileName",
                    File(iosMainDir, fileName)
                )
            }
            
            val resourcesDir = File(composeAppDir, "src/iosMain/composeResources/drawable")
            resourcesDir.mkdirs()
            copyResourceFile(
                "templates/sources/ios/composeResources/drawable/compose-multiplatform.xml",
                File(resourcesDir, "compose-multiplatform.xml")
            )
        }
    }
    
    private fun generateIOSApp(projectDir: File) {
        val iosAppDir = File(projectDir, "iosApp")
        iosAppDir.mkdirs()
        
        copyResourceDirectory("templates/iosApp/iosApp", File(iosAppDir, "iosApp"))
        copyResourceDirectory("templates/iosApp/Configuration", File(iosAppDir, "Configuration"))
        
        val classLoader = ProjectGenerator::class.java.classLoader
        val templateContent = classLoader.getResourceAsStream("templates/iosApp/iosApp.xcodeproj/project.pbxproj")
            ?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Cannot find project.pbxproj template")
        
        val xcodeGenerator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val pbxprojContent = xcodeGenerator.generate(config, templateContent)
        
        val xcodeProjectDir = File(iosAppDir, "iosApp.xcodeproj")
        xcodeProjectDir.mkdirs()
        File(xcodeProjectDir, "project.pbxproj").writeText(pbxprojContent)
        
        copyResourceFile(
            "templates/iosApp/iosApp.xcodeproj/project.xcworkspace/contents.xcworkspacedata",
            File(xcodeProjectDir, "project.xcworkspace/contents.xcworkspacedata")
        )
        
        iosAppDir.walkTopDown()
            .filter { it.isFile && isTextFile(it) }
            .forEach { file ->
                var content = file.readText()
                content = content
                    .replace("\$PROJECT_NAME\$", config.projectName)
                    .replace("\$PACKAGE_ID\$", config.projectId)
                file.writeText(content)
            }
    }
    
    private fun isTextFile(file: File): Boolean {
        val textExtensions = setOf("swift", "xcconfig", "pbxproj", "plist", "json", "xcworkspacedata")
        return file.extension in textExtensions
    }
    
    private fun copyDesktopSources(composeAppDir: File) {
        val jvmMainDir = File(composeAppDir, "src/jvmMain/kotlin/${config.projectIdPath}")
        jvmMainDir.mkdirs()
        
        val isMultiplatform = config.selectedPlatforms.size > 1
        
        if (isMultiplatform) {
            copyResourceFile(
                "templates/sources/desktop/kotlin/org/example/project/main.kt",
                File(jvmMainDir, "main.kt")
            )
            copyResourceFile(
                "templates/sources/desktop-actual/kotlin/org/example/project/Platform.jvm.kt",
                File(jvmMainDir, "Platform.jvm.kt")
            )
        } else {
            copyResourceDirectory("templates/sources/desktop/kotlin/org/example/project", jvmMainDir)
            
            val resourcesDir = File(composeAppDir, "src/jvmMain/composeResources")
            resourcesDir.mkdirs()
            copyResourceDirectory("templates/sources/desktop/composeResources", resourcesDir)
        }
    }
    
    private fun copyAndroidSources(composeAppDir: File) {
        val androidMainDir = File(composeAppDir, "src/androidMain")
        androidMainDir.mkdirs()
        
        val isMultiplatform = config.selectedPlatforms.size > 1
        
        if (isMultiplatform) {
            val kotlinDir = File(androidMainDir, "kotlin/${config.projectIdPath}")
            kotlinDir.mkdirs()
            
            copyResourceFile(
                "templates/sources/android/kotlin/org/example/android/MainActivity.kt",
                File(kotlinDir, "MainActivity.kt")
            )
            copyResourceFile(
                "templates/sources/android-actual/kotlin/org/example/project/Platform.android.kt",
                File(kotlinDir, "Platform.android.kt")
            )
            
            copyResourceFile(
                "templates/sources/android/AndroidManifest.xml",
                File(androidMainDir, "AndroidManifest.xml")
            )
            
            val resDir = File(androidMainDir, "res")
            resDir.mkdirs()
            copyResourceDirectory("templates/sources/android/res", resDir)
        } else {
            copyResourceDirectory("templates/sources/android", androidMainDir)
        }
    }
    
    private fun copyWebSources(composeAppDir: File) {
        // For multiplatform: only copy webMain/main.kt and resources
        if (config.selectedPlatforms.size > 1) {
            // webMain - only main.kt for multiplatform
            val webMainDir = File(composeAppDir, "src/webMain/kotlin/${config.projectIdPath}")
            webMainDir.mkdirs()
            copyResourceFile("templates/web/main.kt", File(webMainDir, "main.kt"))
        } else {
            // For single-platform Web: copy full sources to webMain
            val webMainDir = File(composeAppDir, "src/webMain/kotlin/${config.projectIdPath}")
            webMainDir.mkdirs()
            copyResourceDirectory("templates/sources/web/kotlin/org/example/project", webMainDir)
            
            // webMain - compose resources for single-platform
            val webMainResourcesDir = File(composeAppDir, "src/webMain/composeResources")
            webMainResourcesDir.mkdirs()
            copyResourceDirectory("templates/sources/web/composeResources", webMainResourcesDir)
        }
        
        // webMain - resources (HTML, CSS)
        val webResourcesDir = File(composeAppDir, "src/webMain/resources")
        webResourcesDir.mkdirs()
        copyResourceDirectory("templates/sources/web/resources", webResourcesDir)
        
        // jsMain - JS-specific platform implementation
        val jsMainDir = File(composeAppDir, "src/jsMain/kotlin/${config.projectIdPath}")
        jsMainDir.mkdirs()
        copyResourceDirectory("templates/sources/web-js/kotlin/org/example/project", jsMainDir)
        
        // wasmJsMain - WasmJS-specific platform implementation
        val wasmJsMainDir = File(composeAppDir, "src/wasmJsMain/kotlin/${config.projectIdPath}")
        wasmJsMainDir.mkdirs()
        copyResourceDirectory("templates/sources/web-wasmjs/kotlin/org/example/project", wasmJsMainDir)
        
        // Copy webpack configuration
        val webpackConfigDir = File(composeAppDir, "webpack.config.d")
        webpackConfigDir.mkdirs()
        copyResourceFile("templates/web/webpack.config.d/watch.js", File(webpackConfigDir, "watch.js"))
    }
    
    private fun processSourceFiles(composeAppDir: File) {
        composeAppDir.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml" || it.extension == "html") }
            .filter { it.name != "build.gradle.kts" }  // Skip build.gradle.kts - already has correct values
            .forEach { file ->
                var content = file.readText()
                // Replace placeholders with actual values
                content = content
                    .replace("\$PACKAGE_ID\$", config.projectId)
                    .replace("\$PACKAGE_PATH\$", config.projectIdPath)
                    .replace("\$PROJECT_NAME\$", config.projectName)
                    .replace("\$PROJECT_NAME_LOWERCASE\$", config.projectName.lowercase())
                    .replace("\$KOTLIN_VERSION\$", config.kotlinVersion)
                    .replace("\$COMPOSE_VERSION\$", config.composeVersion)
                file.writeText(content)
            }
    }
    
    private fun processREADME(projectDir: File) {
        val readmeFile = File(projectDir, "README.md")
        if (readmeFile.exists()) {
            var content = readmeFile.readText()
            
            // Single-platform projects only need PROJECT_NAME replacement
            if (config.selectedPlatforms.size == 1) {
                content = content.replace("\$PROJECT_NAME\$", config.projectName)
                readmeFile.writeText(content)
                return
            }
            
            // Build targets string
            val targets = buildList {
                if (config.targetAndroid) add("Android")
                if (config.targetIOS) add("iOS")
                if (config.targetDesktop) add("Desktop")
                if (config.targetWeb) add("Web")
            }.joinToString(", ")
            
            // Build project structure string
            val projectStructure = buildString {
                appendLine("* `/composeApp` - the code shared by all platforms")
                if (config.targetIOS) {
                    appendLine("* `/iosApp` - the entry point for iOS application")
                }
            }.trimEnd()
            
            content = content
                .replace("\$PROJECT_NAME\$", config.projectName)
                .replace("\$TARGETS\$", targets)
                .replace("\$PROJECT_STRUCTURE\$", projectStructure)
                .replace("\$KOTLIN_VERSION\$", config.kotlinVersion)
                .replace("\$COMPOSE_VERSION\$", config.composeVersion)
            readmeFile.writeText(content)
        }
    }
    
    private fun getResourceAsFile(resourcePath: String): File? {
        val classLoader = ProjectGenerator::class.java.classLoader
        val url = classLoader.getResource(resourcePath) ?: return null
        return try {
            File(url.toURI())
        } catch (e: Exception) {
            null
        }
    }
    
    private fun copyResourceFile(resourcePath: String, targetFile: File): File? {
        val classLoader = ProjectGenerator::class.java.classLoader
        val stream = classLoader.getResourceAsStream(resourcePath) ?: return null
        
        targetFile.parentFile?.mkdirs()
        stream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }
    
    private fun copyResourceDirectory(resourcePath: String, targetDir: File) {
        val classLoader = ProjectGenerator::class.java.classLoader
        val resourceUrl = classLoader.getResource(resourcePath) ?: return

        when (resourceUrl.protocol) {
            "jar" -> {
                val jarPath = resourceUrl.path.substring(5, resourceUrl.path.indexOf("!"))
                val jarFile = JarFile(jarPath)
                val entries = jarFile.entries()
                val prefix = resourcePath + "/"

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith(prefix) && !entry.isDirectory) {
                        val relativePath = entry.name.substring(prefix.length)
                        if (shouldCopyFile(relativePath)) {
                            val targetFile = File(targetDir, relativePath)
                            copyResourceFile(entry.name, targetFile)
                        }
                    }
                }
            }
            "file" -> {
                val sourceDir = File(resourceUrl.toURI())
                sourceDir.walkTopDown().forEach { sourceFile ->
                    if (sourceFile.isFile) {
                        val relativePath = sourceFile.relativeTo(sourceDir).path
                        if (shouldCopyFile(relativePath)) {
                            val targetFile = File(targetDir, relativePath)
                            sourceFile.copyTo(targetFile, overwrite = true)
                        }
                    }
                }
            }
        }
    }
    
    private fun shouldCopyFile(relativePath: String): Boolean {
        val fileName = File(relativePath).name
        val pathParts = relativePath.split("/", "\\")
        
        // Exclude system/temporary hidden files
        if (fileName in setOf(".DS_Store", ".gitkeep", ".gitattributes")) return false
        
        // Exclude OS-specific files
        if (fileName in setOf("Thumbs.db", "desktop.ini")) return false
        
        // Exclude temporary files
        if (fileName.endsWith("~") || fileName.endsWith(".tmp") || fileName.endsWith(".bak")) return false
        
        // Exclude IDE metadata directories
        if (pathParts.any { it in setOf(".idea", ".vscode", "node_modules", "__pycache__") }) return false
        
        return true
    }
}

