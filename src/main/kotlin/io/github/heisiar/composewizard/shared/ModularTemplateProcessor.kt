package io.github.heisiar.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File

class ModularTemplateProcessor(
    private val projectName: String,
    private val projectId: String,
    private val composeVersion: String,
    private val kotlinVersion: String,
    private val lifecycleVersion: String?,
    private val material3Version: String?,
    private val material3AdaptiveVersion: String?,
    private val navigationVersion: String?,
    private val navigation3Version: String?,
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
    private val includeNavigation3: Boolean = false,
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
    
    private val resourceCopier = ModularResourceCopier()
    private val buildGenerator = ModularBuildGenerator(
        resourceCopier, selectedPlatforms, targetAndroid, targetDesktop, includeTests,
        includeHotReload, includeMaterial3, includeMaterial3Adaptive, includeNavigation,
        includeNavigation3, includeNavigationEvent, includeSavedState, includeWindow,
        material3Version, material3AdaptiveVersion, navigationVersion, navigation3Version,
        navigationEventVersion, savedStateVersion, windowVersion
    )
    
    fun copyTemplateToProject(targetPath: String) {
        copyBaseTemplate(targetPath)
        
        for (platform in selectedPlatforms) {
            copyPlatformModule(platform, targetPath)
        }
        
        if (includeTests) {
            copyTestsFeature(targetPath)
        }
        
        buildGenerator.generateBuildFiles(targetPath)
        generateReadme(targetPath)
        
        resourceCopier.processAllFiles(File(targetPath), projectName, projectId, projectIdPath, composeVersion)
        resourceCopier.movePackageStructure(targetPath, projectIdPath)
    }
    
    private fun copyBaseTemplate(targetPath: String) {
        val basePath = "templates/modular/base"
        
        File(targetPath).mkdirs()
        
        resourceCopier.copyResourceDirectory("$basePath/gradle", "$targetPath/gradle")
        
        buildGenerator.updateLibraryVersions(targetPath, composeVersion, kotlinVersion, lifecycleVersion)
        
        resourceCopier.copyResourceFile("templates/gradlew", "$targetPath/gradlew")
        resourceCopier.copyResourceFile("templates/gradlew.bat", "$targetPath/gradlew.bat")
        resourceCopier.copyResourceFile("$basePath/gradle.properties", "$targetPath/gradle.properties")
        resourceCopier.copyResourceFile("$basePath/local.properties", "$targetPath/local.properties")
        
        var settingsContent = resourceCopier.readResourceFile("$basePath/settings.gradle.kts")
        
        val devMavenRepo = if (enableDevVersions) {
            """maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")"""
        } else {
            ""
        }
        settingsContent = settingsContent.replace("{{DEV_MAVEN_REPO}}", devMavenRepo)
        
        File(targetPath, "settings.gradle.kts").writeText(settingsContent)
        
        resourceCopier.copyResourceDirectory("$basePath/composeApp/src/commonMain", "$targetPath/composeApp/src/commonMain")
    }
    
    private fun copyPlatformModule(platform: String, targetPath: String) {
        val modulePath = "templates/modular/modules/$platform"
        
        val classLoader = ModularTemplateProcessor::class.java.classLoader
        val srcResourceUrl = classLoader.getResource("$modulePath/src")
        
        if (srcResourceUrl != null) {
            when (srcResourceUrl.protocol) {
                URLUtil.JAR_PROTOCOL -> {
                    resourceCopier.copyModuleSrcFromJar(srcResourceUrl, modulePath, targetPath)
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
            resourceCopier.copyResourceDirectory("$modulePath/iosApp", "$targetPath/iosApp")
        }
        
        if (platform == "web") {
            resourceCopier.copyResourceDirectory("$modulePath/webpack.config.d", "$targetPath/composeApp/webpack.config.d")
        }
    }
    
    private fun copyTestsFeature(targetPath: String) {
        val testsPath = "templates/modular/features/tests"
        resourceCopier.copyResourceDirectory("$testsPath/src/commonTest", "$targetPath/composeApp/src/commonTest")
    }
    
    private fun generateReadme(targetPath: String) {
        val baseReadme = resourceCopier.readResourceFile("templates/modular/base/README.md")
        
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
                val section = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/$platform/readme.fragment")
                if (section.isNotBlank()) {
                    appendLine(section)
                    appendLine()
                }
            }
        }
        
        val webFeedback = if (targetWeb) {
            resourceCopier.readResourceFileOrEmpty("templates/modular/modules/web/readme-footer.fragment")
        } else {
            ""
        }
        
        val content = baseReadme
            .replace("{{PLATFORMS_LIST}}", platformsList)
            .replace("{{PLATFORM_SPECIFIC_SECTIONS}}", platformSections)
            .replace("{{WEB_FEEDBACK}}", webFeedback)
        
        File(targetPath, "README.md").writeText(content)
    }
}
