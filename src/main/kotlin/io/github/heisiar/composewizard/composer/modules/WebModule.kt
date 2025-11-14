package io.github.heisiar.composewizard.composer.modules

import io.github.heisiar.composewizard.composer.PlatformModule
import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ModularResourceCopier
import java.io.File

class WebModule : PlatformModule {
    
    private val resourceCopier = ModularResourceCopier()
    
    override val platformId: String = "web"
    override val displayName: String = "Web"
    
    override fun getImports(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/web/import.fragment")
        return if (fragment.isNotBlank()) listOf(fragment.trim()) else emptyList()
    }
    
    override fun getPlugins(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/web/plugin.fragment")
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
        return resourceCopier.readResourceFile("templates/modular/modules/web/target.fragment")
    }
    
    override fun getSourceSetFragment(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/web/sourceset.fragment")
        return fragment
    }
    
    override fun getConfigFragment(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/web/config.fragment")
        return fragment
    }
    
    override fun getDependencies(config: ProjectConfig): List<String> {
        return emptyList()
    }
    
    override fun copyFiles(targetPath: String, config: ProjectConfig, isMultiplatform: Boolean) {
        val srcPath = "templates/modular/modules/web/src"
        
        copyWebMainFiles(targetPath, config, srcPath)
        copyWasmJsMainFiles(targetPath, config, srcPath)
        copyJsMainFiles(targetPath, config, srcPath)
        
        if (!isMultiplatform) {
            copyCommonFilesToWebMain(targetPath, config)
            copyComposeResourcesToWebMain(targetPath)
        }
        
        copyWebpackConfig(targetPath)
    }
    
    private fun copyWebMainFiles(targetPath: String, config: ProjectConfig, srcPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val webMainPath = "$srcPath/webMain"
            val isMultiplatform = listOf(config.targetAndroid, config.targetDesktop, config.targetIOS, config.targetWeb).count { it } > 1
            
            if (isMultiplatform) {
                // For multiplatform, exclude common files that should be in commonMain
                val exclusions = listOf("**/App.kt", "**/Greeting.kt", "**/Platform.kt")
                when (resourceUrl.protocol) {
                    "jar" -> {
                        resourceCopier.copyFromJarWithExclusions(resourceUrl, webMainPath, "$targetPath/composeApp/src", exclusions)
                    }
                    "file" -> {
                        val webMainDir = File(resourceUrl.toURI()).resolve("webMain")
                        if (webMainDir.exists()) {
                            val targetDir = File(targetPath, "composeApp/src/webMain")
                            webMainDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    val shouldExclude = exclusions.any { pattern ->
                                        val regex = pattern.replace("**/", "").replace("**", ".*")
                                        file.name.matches(Regex(regex))
                                    }
                                    if (!shouldExclude) {
                                        val relativePath = file.relativeTo(webMainDir).path
                                        val targetFile = File(targetDir, relativePath)
                                        targetFile.parentFile.mkdirs()
                                        file.copyTo(targetFile, overwrite = true)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // For single-platform, copy everything
                when (resourceUrl.protocol) {
                    "jar" -> {
                        resourceCopier.copyModuleSrcFromJar(resourceUrl, webMainPath, targetPath)
                    }
                    "file" -> {
                        val webMainDir = File(resourceUrl.toURI()).resolve("webMain")
                        if (webMainDir.exists()) {
                            val targetDir = File(targetPath, "composeApp/src/webMain")
                            webMainDir.copyRecursively(targetDir, overwrite = true)
                        }
                    }
                }
            }
        }
    }
    
    private fun copyWasmJsMainFiles(targetPath: String, config: ProjectConfig, srcPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val wasmJsMainPath = "$srcPath/wasmJsMain"
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, wasmJsMainPath, targetPath)
                }
                "file" -> {
                    val wasmJsMainDir = File(resourceUrl.toURI()).resolve("wasmJsMain")
                    if (wasmJsMainDir.exists()) {
                        val targetDir = File(targetPath, "composeApp/src/wasmJsMain")
                        wasmJsMainDir.copyRecursively(targetDir, overwrite = true)
                    }
                }
            }
        }
    }
    
    private fun copyJsMainFiles(targetPath: String, config: ProjectConfig, srcPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val jsMainPath = "$srcPath/jsMain"
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, jsMainPath, targetPath)
                }
                "file" -> {
                    val jsMainDir = File(resourceUrl.toURI()).resolve("jsMain")
                    if (jsMainDir.exists()) {
                        val targetDir = File(targetPath, "composeApp/src/jsMain")
                        jsMainDir.copyRecursively(targetDir, overwrite = true)
                    }
                }
            }
        }
    }
    
    private fun copyWebpackConfig(targetPath: String) {
        resourceCopier.copyResourceDirectory(
            "templates/modular/modules/web/webpack.config.d",
            "$targetPath/composeApp/webpack.config.d"
        )
    }
    
    override fun getReadmeSection(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/web/readme.fragment")
        return if (fragment.isNotBlank()) fragment else ""
    }
    
    private fun copyCommonFilesToWebMain(targetPath: String, config: ProjectConfig) {
        val commonMainPath = "templates/modular/base/composeApp/src/commonMain/kotlin"
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(commonMainPath)
        
        if (resourceUrl != null) {
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, commonMainPath, targetPath)
                    
                    val commonKotlinDir = File(targetPath, "composeApp/src/commonMain/kotlin")
                    if (commonKotlinDir.exists()) {
                        val webMainKotlinDir = File(targetPath, "composeApp/src/webMain/kotlin")
                        commonKotlinDir.walkTopDown().forEach { file ->
                            if (file.isFile && !file.name.endsWith("App.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir).path
                                val targetFile = File(webMainKotlinDir, relativePath)
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
                        val webMainKotlinDir = File(targetPath, "composeApp/src/webMain/kotlin")
                        commonKotlinDir.walkTopDown().forEach { file ->
                            if (file.isFile && !file.name.endsWith("App.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir).path
                                val targetFile = File(webMainKotlinDir, relativePath)
                                targetFile.parentFile.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun copyComposeResourcesToWebMain(targetPath: String) {
        val resourcesPath = "templates/modular/base/composeApp/src/commonMain/composeResources"
        resourceCopier.copyResourceDirectory(resourcesPath, "$targetPath/composeApp/src/webMain/composeResources")
    }
    
    override fun getLibraryVersions(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
    
    override fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
}

