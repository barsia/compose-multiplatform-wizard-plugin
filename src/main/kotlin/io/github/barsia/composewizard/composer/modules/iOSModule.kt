package io.github.barsia.composewizard.composer.modules

import io.github.barsia.composewizard.composer.PlatformModule
import io.github.barsia.composewizard.generator.ProjectConfig
import io.github.barsia.composewizard.generator.xcode.XcodeProjectGenerator
import io.github.barsia.composewizard.shared.ModularResourceCopier
import java.io.File

class iOSModule : PlatformModule {
    
    private val resourceCopier = ModularResourceCopier()
    
    override val platformId: String = "ios"
    override val displayName: String = "iOS"
    
    override fun getImports(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/ios/import.fragment")
        return if (fragment.isNotBlank()) listOf(fragment.trim()) else emptyList()
    }
    
    override fun getPlugins(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/ios/plugin.fragment")
        return if (fragment.isNotBlank()) {
            fragment.trim().lines().filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
    
    override fun getRootPlugins(config: ProjectConfig): List<String> {
        return emptyList()
    }
    
    override fun getTargetFragment(config: ProjectConfig): String {
        return resourceCopier.readResourceFile("templates/modular/modules/ios/target.fragment")
    }
    
    override fun getSourceSetFragment(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/ios/sourceset.fragment")
        return fragment
    }
    
    override fun getConfigFragment(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/ios/config.fragment")
        return fragment
    }
    
    override fun getDependencies(config: ProjectConfig): List<String> {
        return emptyList()
    }
    
    override fun copyFiles(targetPath: String, config: ProjectConfig, isMultiplatform: Boolean) {
        val srcPath = "templates/modular/modules/ios/src"
        val iosAppPath = "templates/modular/modules/ios/iosApp"
        
        copyKotlinFiles(targetPath, config, srcPath, isMultiplatform)
        
        if (!isMultiplatform) {
            copyCommonFilesToIosMain(targetPath, config)
            copyComposeResourcesToIosMain(targetPath)
        }
        
        copyXcodeProject(targetPath, config, iosAppPath)
    }
    
    private fun copyKotlinFiles(targetPath: String, config: ProjectConfig, srcPath: String, isMultiplatform: Boolean) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val iosMainPath = "$srcPath/iosMain"
            
            if (isMultiplatform) {
                val exclusions = listOf("**/App.kt", "**/Greeting.kt", "**/Platform.kt", "**/Platform.ios.single.kt")
                
                when (resourceUrl.protocol) {
                    "jar" -> {
                        resourceCopier.copyFromJarWithExclusions(
                            resourceUrl,
                            iosMainPath,
                            "$targetPath/composeApp/src",
                            exclusions
                        )
                    }
                    "file" -> {
                        val iosMainDir = File(resourceUrl.toURI()).resolve("iosMain")
                        if (iosMainDir.exists()) {
                            iosMainDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    val relativePath = file.relativeTo(iosMainDir).path
                                    val shouldExclude = exclusions.any { pattern ->
                                        when {
                                            pattern.startsWith("**/") -> relativePath.endsWith(pattern.substring(3))
                                            else -> relativePath == pattern
                                        }
                                    }
                                    if (!shouldExclude) {
                                        val targetFile = File(targetPath, "composeApp/src/iosMain/$relativePath")
                                        targetFile.parentFile.mkdirs()
                                        file.copyTo(targetFile, overwrite = true)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val exclusions = listOf("**/Platform.ios.kt")
                val renameMap = mapOf("Platform.ios.single.kt" to "Platform.kt")
                
                when (resourceUrl.protocol) {
                    "jar" -> {
                        resourceCopier.copyFromJarWithExclusions(
                            resourceUrl,
                            iosMainPath,
                            "$targetPath/composeApp/src",
                            exclusions,
                            renameMap
                        )
                    }
                    "file" -> {
                        val iosMainDir = File(resourceUrl.toURI()).resolve("iosMain")
                        if (iosMainDir.exists()) {
                            iosMainDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    val relativePath = file.relativeTo(iosMainDir).path
                                    val shouldExclude = exclusions.any { pattern ->
                                        when {
                                            pattern.startsWith("**/") -> relativePath.endsWith(pattern.substring(3))
                                            else -> relativePath == pattern
                                        }
                                    }
                                    if (!shouldExclude) {
                                        var targetRelativePath = relativePath
                                        if (relativePath.endsWith("Platform.ios.single.kt")) {
                                            targetRelativePath = relativePath.replace("Platform.ios.single.kt", "Platform.kt")
                                        }
                                        val targetFile = File(targetPath, "composeApp/src/iosMain/$targetRelativePath")
                                        targetFile.parentFile.mkdirs()
                                        file.copyTo(targetFile, overwrite = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun copyXcodeProject(targetPath: String, config: ProjectConfig, iosAppPath: String) {
        resourceCopier.copyResourceDirectory(iosAppPath, "$targetPath/iosApp")
        
        val projectPbxprojPath = "$targetPath/iosApp/iosApp.xcodeproj/project.pbxproj"
        val projectFile = File(projectPbxprojPath)
        
        if (projectFile.exists()) {
            val uuidGenerator = io.github.barsia.composewizard.generator.xcode.SecureRandomUUIDGenerator()
            val xcodeGenerator = XcodeProjectGenerator(uuidGenerator)
            val templateContent = projectFile.readText()
            val generatedContent = xcodeGenerator.generate(config, templateContent)
            projectFile.writeText(generatedContent)
        }
    }
    
    override fun getReadmeSection(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/ios/readme.fragment")
        return if (fragment.isNotBlank()) fragment else ""
    }
    
    private fun copyCommonFilesToIosMain(targetPath: String, config: ProjectConfig) {
        val commonMainPath = "templates/modular/base/composeApp/src/commonMain/kotlin"
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(commonMainPath)
        
        if (resourceUrl != null) {
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, commonMainPath, targetPath)
                    
                    val commonKotlinDir = File(targetPath, "composeApp/src/commonMain/kotlin")
                    if (commonKotlinDir.exists()) {
                        val iosMainKotlinDir = File(targetPath, "composeApp/src/iosMain/kotlin")
                        
                        commonKotlinDir.walk().forEach { file ->
                            if (file.isFile && !file.name.equals("Platform.kt") && !file.name.equals("App.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir)
                                val targetFile = iosMainKotlinDir.resolve(relativePath)
                                targetFile.parentFile.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                            }
                        }
                        commonKotlinDir.deleteRecursively()
                    }
                }
                "file" -> {
                    val commonKotlinDir = File(resourceUrl.toURI())
                    if (commonKotlinDir.exists()) {
                        val iosMainKotlinDir = File(targetPath, "composeApp/src/iosMain/kotlin")
                        
                        commonKotlinDir.walk().forEach { file ->
                            if (file.isFile && !file.name.equals("Platform.kt") && !file.name.equals("App.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir)
                                val targetFile = iosMainKotlinDir.resolve(relativePath)
                                targetFile.parentFile.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                            }
                        }
                    }
                }
            }
        }
        
        val singlePlatformFile = resourceCopier.readResourceFile("templates/modular/modules/ios/src/iosMain/kotlin/org/example/project/Platform.ios.single.kt")
        val platformFile = File(targetPath, "composeApp/src/iosMain/kotlin/org/example/project/Platform.kt")
        platformFile.parentFile.mkdirs()
        platformFile.writeText(singlePlatformFile)
    }
    
    private fun copyComposeResourcesToIosMain(targetPath: String) {
        val resourcesPath = "templates/modular/base/composeApp/src/commonMain/composeResources"
        resourceCopier.copyResourceDirectory(resourcesPath, "$targetPath/composeApp/src/iosMain/composeResources")
    }
    
    override fun getLibraryVersions(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
    
    override fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
}

