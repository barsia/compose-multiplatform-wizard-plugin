package io.github.barsia.composewizard.composer

import io.github.barsia.composewizard.generator.ProjectConfig
import io.github.barsia.composewizard.shared.ModularResourceCopier
import java.io.File

/**
 * Main orchestrator for composing a project from modules and features.
 * 
 * Usage:
 * ```kotlin
 * val project = ProjectComposer(config)
 *     .addModule(AndroidModule())
 *     .addModule(DesktopModule())
 *     .addFeature(TestsFeature())
 *     .compose("/path/to/project")
 * ```
 */
class ProjectComposer(
    private val config: ProjectConfig
) {
    private val modules = mutableListOf<PlatformModule>()
    private val features = mutableListOf<ProjectFeature>()
    private val resourceCopier = ModularResourceCopier()
    
    /**
     * Add a platform module
     */
    fun addModule(module: PlatformModule): ProjectComposer {
        modules.add(module)
        return this
    }
    
    /**
     * Add an optional feature
     */
    fun addFeature(feature: ProjectFeature): ProjectComposer {
        features.add(feature)
        return this
    }
    
    /**
     * Compose the project at the target path
     */
    fun compose(targetPath: String) {
        validate()
        
        val isMultiplatform = modules.size > 1
        
        copyBaseStructure(targetPath)
        
        // Create .gitignore FIRST, before any other files
        // This ensures Git will properly ignore files like local.properties
        createGitignoreFile(targetPath)
        
        if (isMultiplatform) {
            copyCommonMainFiles(targetPath)
        }
        
        modules.forEach { module ->
            module.copyFiles(targetPath, config, isMultiplatform)
        }
        
        features.forEach { feature ->
            feature.apply(targetPath, config, modules)
        }
        
        generateBuildFiles(targetPath, isMultiplatform)
        
        generateLibsVersions(targetPath)
        
        generateReadme(targetPath)
        
        processPlaceholders(targetPath)
        
        if (!isMultiplatform) {
            val commonMainDir = File(targetPath, "composeApp/src/commonMain")
            if (commonMainDir.exists()) {
                commonMainDir.deleteRecursively()
            }
        }
    }
    
    private fun copyCommonMainFiles(targetPath: String) {
        val srcPath = "templates/modular/base/composeApp/src/commonMain/kotlin"
        val packageReplacement = "org/example/project" to config.projectIdPath
        resourceCopier.copyResourceDirectory(srcPath, "$targetPath/composeApp/src/commonMain/kotlin", packageReplacement)
        
        val composeResourcesPath = "templates/modular/base/composeApp/src/commonMain/composeResources"
        resourceCopier.copyResourceDirectory(composeResourcesPath, "$targetPath/composeApp/src/commonMain/composeResources")
    }
    
    private fun validate() {
        require(modules.isNotEmpty()) { "At least one platform module is required" }
        require(modules.map { it.platformId }.distinct().size == modules.size) {
            "Duplicate platform modules detected"
        }
    }
    
    private fun copyBaseStructure(targetPath: String) {
        resourceCopier.copyResourceFile("templates/gradlew", "$targetPath/gradlew")
        resourceCopier.copyResourceFile("templates/gradlew.bat", "$targetPath/gradlew.bat")
        
        val gradlewFile = File(targetPath, "gradlew")
        if (gradlewFile.exists()) {
            gradlewFile.setExecutable(true)
        }
        
        resourceCopier.copyResourceDirectory("templates/modular/base/gradle/wrapper", "$targetPath/gradle/wrapper")
        
        val gradlePropertiesTemplate = resourceCopier.readResourceFile("templates/modular/base/gradle.properties")
        var gradlePropertiesContent = gradlePropertiesTemplate
        
        if (!config.targetAndroid) {
            gradlePropertiesContent = gradlePropertiesContent.lines()
                .filterNot { line ->
                    line.trim().startsWith("#Android") ||
                    line.contains("android.nonTransitiveRClass") ||
                    line.contains("android.useAndroidX")
                }
                .joinToString("\n")
        }
        
        gradlePropertiesContent = gradlePropertiesContent.replace(Regex("\n{3,}"), "\n\n").trimEnd()
        File(targetPath, "gradle.properties").writeText(gradlePropertiesContent)
        
        val settingsTemplate = resourceCopier.readResourceFile("templates/modular/base/settings.gradle.kts")
        File(targetPath, "settings.gradle.kts").writeText(settingsTemplate)
    }
    
    private fun generateBuildFiles(targetPath: String, isMultiplatform: Boolean) {
        val buildComposer = BuildFileComposer(config, modules, features, resourceCopier)
        buildComposer.composeRootBuildFile(targetPath)
        buildComposer.composeAppBuildFile(targetPath, isMultiplatform)
        buildComposer.composeSettingsFile(targetPath)
    }
    
    private fun generateLibsVersions(targetPath: String) {
        val buildComposer = BuildFileComposer(config, modules, features, resourceCopier)
        buildComposer.composeLibsVersions(targetPath)
    }
    
    private fun generateReadme(targetPath: String) {
        val readmeTemplate = resourceCopier.readResourceFile("templates/modular/base/README.md")
        
        val isMultiplatform = modules.size > 1
        
        // iOS structure section (bullet point about /iosApp directory)
        val iosStructure = if (config.targetIOS) {
            resourceCopier.readResourceFileOrEmpty("templates/modular/modules/ios/readme-structure.fragment") + "\n"
        } else {
            ""
        }
        
        // Platform-specific build sections
        val platformSections = buildString {
            modules.forEachIndexed { index, module ->
                val section = module.getReadmeSection(config)
                if (section.isNotBlank()) {
                    append(section)
                    if (isMultiplatform && index < modules.size - 1) {
                        appendLine()
                    }
                }
            }
        }
        
        var readmeContent = readmeTemplate
        readmeContent = readmeContent.replace("{{IOS_STRUCTURE}}", iosStructure)
        readmeContent = readmeContent.replace("{{PLATFORM_SPECIFIC_SECTIONS}}", platformSections)
        
        File(targetPath, "README.md").writeText(readmeContent)
    }
    
    private fun processPlaceholders(targetPath: String) {
        val projectDir = File(targetPath)
        resourceCopier.processAllFiles(
            dir = projectDir,
            projectName = config.projectName,
            projectId = config.projectId,
            projectIdPath = config.projectIdPath,
            composeVersion = config.composeVersion,
            config = config
        )
        
        resourceCopier.movePackageStructure(targetPath, config.projectIdPath)
    }
    
    private fun createGitignoreFile(targetPath: String) {
        try {
            val gitignoreContent = """
                *.iml
                .kotlin
                .gradle
                **/build/
                xcuserdata
                !src/**/build/
                local.properties
                .idea
                .DS_Store
                captures
                .externalNativeBuild
                .cxx
                *.xcodeproj/*
                !*.xcodeproj/project.pbxproj
                !*.xcodeproj/xcshareddata/
                !*.xcodeproj/project.xcworkspace/
                !*.xcworkspace/contents.xcworkspacedata
                **/xcshareddata/WorkspaceSettings.xcsettings
                node_modules/
            """.trimIndent() + "\n"
            
            val gitignoreFile = File(targetPath, ".gitignore")
            gitignoreFile.writeText(gitignoreContent)
        } catch (e: Exception) {
        }
    }
}

