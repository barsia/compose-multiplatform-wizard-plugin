package io.github.heisiar.composewizard.composer.features

import io.github.heisiar.composewizard.composer.PlatformModule
import io.github.heisiar.composewizard.composer.ProjectFeature
import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ModularResourceCopier
import java.io.File

class TestsFeature : ProjectFeature {
    
    private val resourceCopier = ModularResourceCopier()
    
    override val featureId: String = "tests"
    override val displayName: String = "Tests"
    override val defaultEnabled: Boolean = false
    
    override fun apply(targetPath: String, config: ProjectConfig, modules: List<PlatformModule>) {
        val srcPath = "templates/modular/features/tests/src"
        
        val classLoader = ModularResourceCopier::class.java.classLoader
        val resourceUrl = classLoader.getResource(srcPath)
        
        if (resourceUrl != null) {
            val commonTestPath = "$srcPath/commonTest"
            
            when (resourceUrl.protocol) {
                "jar" -> {
                    resourceCopier.copyModuleSrcFromJar(resourceUrl, commonTestPath, targetPath)
                }
                "file" -> {
                    val commonTestDir = File(resourceUrl.toURI()).resolve("commonTest")
                    if (commonTestDir.exists()) {
                        val targetDir = File(targetPath, "composeApp/src/commonTest")
                        targetDir.parentFile?.mkdirs()
                        commonTestDir.copyRecursively(targetDir, overwrite = true)
                    }
                }
            }
        }
    }
    
    override fun getSourceSetFragments(config: ProjectConfig): List<String> {
        val fragment = resourceCopier.readResourceFile("templates/modular/features/tests/sourceset.fragment")
        return listOf(fragment)
    }
    
    override fun getLibraryVersions(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
    
    override fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
}

