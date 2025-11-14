package io.github.heisiar.composewizard.composer.modules

import io.github.heisiar.composewizard.composer.PlatformModule
import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ModularResourceCopier
import java.io.File

class DesktopModule : PlatformModule {
    
    private val resourceCopier = ModularResourceCopier()
    
    override val platformId: String = "desktop"
    override val displayName: String = "Desktop"
    
    override fun getImports(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/desktop/import.fragment")
        return if (fragment.isNotBlank()) listOf(fragment.trim()) else emptyList()
    }
    
    override fun getPlugins(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/desktop/plugin.fragment")
        return if (fragment.isNotBlank()) {
            fragment.trim().lines().filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
    
    override fun getRootPlugins(config: ProjectConfig): List<String> {
        if (!config.includeHotReload) {
            return emptyList()
        }
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/desktop/root-plugin.fragment")
        return if (fragment.isNotBlank()) listOf(fragment.trim()) else emptyList()
    }
    
    override fun getTargetFragment(config: ProjectConfig): String {
        return resourceCopier.readResourceFile("templates/modular/modules/desktop/target.fragment")
    }
    
    override fun getSourceSetFragment(config: ProjectConfig): String {
        return resourceCopier.readResourceFile("templates/modular/modules/desktop/sourceset.fragment")
    }
    
    override fun getConfigFragment(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFile("templates/modular/modules/desktop/config.fragment")
        return fragment.replace("{{PROJECT_ID}}", config.projectId)
    }
    
    override fun getDependencies(config: ProjectConfig): List<String> {
        return emptyList()
    }
    
    override fun copyFiles(targetPath: String, config: ProjectConfig, isMultiplatform: Boolean) {
        val srcPath = "templates/modular/modules/desktop/src"
        
        if (isMultiplatform) {
            copyMultiplatformFiles(targetPath, config, srcPath)
        } else {
            copySinglePlatformFiles(targetPath, config, srcPath)
            copyCommonFilesToJvmMain(targetPath, config)
            copyComposeResourcesToJvmMain(targetPath)
        }
    }
    
    private fun copyMultiplatformFiles(targetPath: String, config: ProjectConfig, srcPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val jvmMainPath = "$srcPath/jvmMain"
            val exclusions = listOf("**/App.kt", "**/Greeting.kt", "**/Platform.kt", "**/Platform.jvm.single.kt")
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyFromJarWithExclusions(
                        resourceUrl, 
                        jvmMainPath,
                        "$targetPath/composeApp/src",
                        exclusions
                    )
                }
                "file" -> {
                    val jvmMainDir = File(resourceUrl.toURI()).resolve("jvmMain")
                    if (jvmMainDir.exists()) {
                        jvmMainDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relativePath = file.relativeTo(jvmMainDir).path
                                val shouldExclude = exclusions.any { pattern ->
                                    when {
                                        pattern.startsWith("**/") -> relativePath.endsWith(pattern.substring(3))
                                        else -> relativePath == pattern
                                    }
                                }
                                if (!shouldExclude) {
                                    val targetFile = File(targetPath, "composeApp/src/jvmMain/$relativePath")
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
    
    private fun copySinglePlatformFiles(targetPath: String, config: ProjectConfig, srcPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val jvmMainPath = "$srcPath/jvmMain"
            val exclusions = listOf("**/Platform.jvm.kt", "**/Platform.jvm.single.kt")
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyFromJarWithExclusions(resourceUrl, jvmMainPath, "$targetPath/composeApp/src", exclusions)
                }
                "file" -> {
                    val jvmMainDir = File(resourceUrl.toURI()).resolve("jvmMain")
                    if (jvmMainDir.exists()) {
                        jvmMainDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relativePath = file.relativeTo(jvmMainDir).path
                                val shouldExclude = exclusions.any { pattern ->
                                    when {
                                        pattern.startsWith("**/") -> relativePath.endsWith(pattern.substring(3))
                                        else -> relativePath == pattern
                                    }
                                }
                                if (!shouldExclude) {
                                    val targetFile = File(targetPath, "composeApp/src/jvmMain/$relativePath")
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
    
    override fun getReadmeSection(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/desktop/readme.fragment")
        return if (fragment.isNotBlank()) fragment else ""
    }
    
    private fun copyCommonFilesToJvmMain(targetPath: String, config: ProjectConfig) {
        val commonMainPath = "templates/modular/base/composeApp/src/commonMain/kotlin"
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(commonMainPath)
        
        if (resourceUrl != null) {
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, commonMainPath, targetPath)
                    
                    val commonKotlinDir = File(targetPath, "composeApp/src/commonMain/kotlin")
                    if (commonKotlinDir.exists()) {
                        val jvmMainKotlinDir = File(targetPath, "composeApp/src/jvmMain/kotlin")
                        
                        commonKotlinDir.walk().forEach { file ->
                            if (file.isFile && !file.name.equals("Platform.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir)
                                val targetFile = jvmMainKotlinDir.resolve(relativePath)
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
                        val jvmMainKotlinDir = File(targetPath, "composeApp/src/jvmMain/kotlin")
                        
                        commonKotlinDir.walk().forEach { file ->
                            if (file.isFile && !file.name.equals("Platform.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir)
                                val targetFile = jvmMainKotlinDir.resolve(relativePath)
                                targetFile.parentFile.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                            }
                        }
                    }
                }
            }
        }
        
        val singlePlatformFile = resourceCopier.readResourceFile("templates/modular/modules/desktop/src/jvmMain/kotlin/org/example/project/Platform.jvm.single.kt")
        val platformFile = File(targetPath, "composeApp/src/jvmMain/kotlin/org/example/project/Platform.kt")
        platformFile.parentFile.mkdirs()
        platformFile.writeText(singlePlatformFile)
    }
    
    private fun copyComposeResourcesToJvmMain(targetPath: String) {
        val resourcesPath = "templates/modular/base/composeApp/src/commonMain/composeResources"
        resourceCopier.copyResourceDirectory(resourcesPath, "$targetPath/composeApp/src/jvmMain/composeResources")
    }
    
    override fun getLibraryVersions(config: ProjectConfig): Map<String, String> {
        return mapOf(
            "kotlinx-coroutines" to "1.10.1"
        )
    }
    
    override fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> {
        return mapOf(
            "kotlinx-coroutinesSwing" to "{ module = \"org.jetbrains.kotlinx:kotlinx-coroutines-swing\", version.ref = \"kotlinx-coroutines\" }"
        )
    }
}

