package io.github.heisiar.composewizard.composer.modules

import io.github.heisiar.composewizard.composer.PlatformModule
import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ModularResourceCopier
import java.io.File

class AndroidModule : PlatformModule {
    
    private val resourceCopier = ModularResourceCopier()
    
    override val platformId: String = "android"
    override val displayName: String = "Android"
    
    override fun getImports(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/android/import.fragment")
        return if (fragment.isNotBlank()) listOf(fragment.trim()) else emptyList()
    }
    
    override fun getPlugins(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/android/plugin.fragment")
        return if (fragment.isNotBlank()) {
            fragment.trim().lines().filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
    
    override fun getRootPlugins(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/android/root-plugin.fragment")
        return if (fragment.isNotBlank()) {
            fragment.trim().split("\n").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
    
    override fun getTargetFragment(config: ProjectConfig): String {
        return resourceCopier.readResourceFile("templates/modular/modules/android/target.fragment")
    }
    
    override fun getSourceSetFragment(config: ProjectConfig): String {
        return resourceCopier.readResourceFile("templates/modular/modules/android/sourceset.fragment")
    }
    
    override fun getConfigFragment(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFile("templates/modular/modules/android/config.fragment")
        return fragment.replace("{{PROJECT_ID}}", config.projectId)
    }
    
    override fun getDependencies(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/android/dependency.fragment")
        return if (fragment.isNotBlank()) listOf(fragment.trim()) else emptyList()
    }
    
    override fun copyFiles(targetPath: String, config: ProjectConfig, isMultiplatform: Boolean) {
        val srcPath = "templates/modular/modules/android/src"
        
        if (isMultiplatform) {
            copyMultiplatformFiles(targetPath, config, srcPath)
        } else {
            copySinglePlatformFiles(targetPath, config, srcPath)
            copyCommonFilesToAndroidMain(targetPath, config)
            copyComposeResourcesToAndroidMain(targetPath)
        }
        
        createLocalProperties(targetPath)
    }
    
    private fun copyMultiplatformFiles(targetPath: String, config: ProjectConfig, srcPath: String) {
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val androidMainPath = "$srcPath/androidMain"
            val exclusions = listOf("**/App.kt", "**/Greeting.kt", "**/Platform.kt", "**/main.kt", "**/Platform.android.single.kt")
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyFromJarWithExclusions(
                        resourceUrl,
                        androidMainPath,
                        "$targetPath/composeApp/src",
                        exclusions
                    )
                }
                "file" -> {
                    val androidMainDir = File(resourceUrl.toURI()).resolve("androidMain")
                    if (androidMainDir.exists()) {
                        androidMainDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relativePath = file.relativeTo(androidMainDir).path
                                val shouldExclude = exclusions.any { pattern ->
                                    when {
                                        pattern.startsWith("**/") -> relativePath.endsWith(pattern.substring(3))
                                        else -> relativePath == pattern
                                    }
                                }
                                if (!shouldExclude) {
                                    val targetFile = File(targetPath, "composeApp/src/androidMain/$relativePath")
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
            val androidMainPath = "$srcPath/androidMain"
            val exclusions = listOf("**/Platform.android.kt")
            val renameMap = mapOf("Platform.android.single.kt" to "Platform.kt")
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyFromJarWithExclusions(
                        resourceUrl,
                        androidMainPath,
                        "$targetPath/composeApp/src",
                        exclusions,
                        renameMap
                    )
                }
                "file" -> {
                    val androidMainDir = File(resourceUrl.toURI()).resolve("androidMain")
                    if (androidMainDir.exists()) {
                        androidMainDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relativePath = file.relativeTo(androidMainDir).path
                                val shouldExclude = exclusions.any { pattern ->
                                    when {
                                        pattern.startsWith("**/") -> relativePath.endsWith(pattern.substring(3))
                                        else -> relativePath == pattern
                                    }
                                }
                                if (!shouldExclude) {
                                    var targetRelativePath = relativePath
                                    if (relativePath.endsWith("Platform.android.single.kt")) {
                                        targetRelativePath = relativePath.replace("Platform.android.single.kt", "Platform.kt")
                                    }
                                    val targetFile = File(targetPath, "composeApp/src/androidMain/$targetRelativePath")
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
    
    private fun createLocalProperties(targetPath: String) {
        val sdkPath = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: ""
        val localPropertiesContent = """
            ## This file must *NOT* be checked into Version Control Systems,
            # as it contains information specific to your local configuration.
            #
            # Location of the SDK. This is only used by Gradle.
            # For customization when using a Version Control System, please read the
            # header note.

            sdk.dir=$sdkPath
        """.trimIndent() + "\n"
        File(targetPath, "local.properties").writeText(localPropertiesContent)
    }
    
    override fun getReadmeSection(config: ProjectConfig): String {
        val fragment = resourceCopier.readResourceFileOrEmpty("templates/modular/modules/android/readme.fragment")
        return if (fragment.isNotBlank()) fragment else ""
    }
    
    private fun copyCommonFilesToAndroidMain(targetPath: String, config: ProjectConfig) {
        val commonMainPath = "templates/modular/base/composeApp/src/commonMain/kotlin"
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(commonMainPath)
        
        if (resourceUrl != null) {
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, commonMainPath, targetPath)
                    
                    val commonKotlinDir = File(targetPath, "composeApp/src/commonMain/kotlin")
                    if (commonKotlinDir.exists()) {
                        val androidMainKotlinDir = File(targetPath, "composeApp/src/androidMain/kotlin")
                        
                        commonKotlinDir.walk().forEach { file ->
                            if (file.isFile && !file.name.equals("Platform.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir)
                                val targetFile = androidMainKotlinDir.resolve(relativePath)
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
                        val androidMainKotlinDir = File(targetPath, "composeApp/src/androidMain/kotlin")
                        
                        commonKotlinDir.walk().forEach { file ->
                            if (file.isFile && !file.name.equals("Platform.kt")) {
                                val relativePath = file.relativeTo(commonKotlinDir)
                                val targetFile = androidMainKotlinDir.resolve(relativePath)
                                targetFile.parentFile.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                            }
                        }
                    }
                }
            }
        }
        
        val singlePlatformFile = resourceCopier.readResourceFile("templates/modular/modules/android/src/androidMain/kotlin/org/example/project/Platform.android.single.kt")
        val platformFile = File(targetPath, "composeApp/src/androidMain/kotlin/org/example/project/Platform.kt")
        platformFile.parentFile.mkdirs()
        platformFile.writeText(singlePlatformFile)
    }
    
    private fun copyComposeResourcesToAndroidMain(targetPath: String) {
        val resourcesPath = "templates/modular/base/composeApp/src/commonMain/composeResources"
        resourceCopier.copyResourceDirectory(resourcesPath, "$targetPath/composeApp/src/androidMain/composeResources")
    }
    
    override fun getLibraryVersions(config: ProjectConfig): Map<String, String> {
        return mapOf(
            "agp" to "8.11.2",
            "android-compileSdk" to "36",
            "android-minSdk" to "24",
            "android-targetSdk" to "36",
            "androidx-activity" to "1.11.0",
            "androidx-appcompat" to "1.7.1",
            "androidx-core" to "1.17.0"
        )
    }
    
    override fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> {
        return mapOf(
            "androidx-core-ktx" to "{ module = \"androidx.core:core-ktx\", version.ref = \"androidx-core\" }",
            "androidx-appcompat" to "{ module = \"androidx.appcompat:appcompat\", version.ref = \"androidx-appcompat\" }",
            "androidx-activity-compose" to "{ module = \"androidx.activity:activity-compose\", version.ref = \"androidx-activity\" }"
        )
    }
}

