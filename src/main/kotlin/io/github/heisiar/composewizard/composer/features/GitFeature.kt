package io.github.heisiar.composewizard.composer.features

import io.github.heisiar.composewizard.composer.PlatformModule
import io.github.heisiar.composewizard.composer.ProjectFeature
import io.github.heisiar.composewizard.generator.ProjectConfig
import java.io.File

class GitFeature : ProjectFeature {
    
    override val featureId: String = "git"
    override val displayName: String = "Git"
    override val defaultEnabled: Boolean = false
    
    override fun apply(targetPath: String, config: ProjectConfig, modules: List<PlatformModule>) {
        initializeGitRepository(targetPath)
    }
    
    private fun initializeGitRepository(targetPath: String): Boolean {
        return try {
            val projectDir = File(targetPath)
            
            val process = Runtime.getRuntime().exec(
                arrayOf("git", "init"),
                null,
                projectDir
            )
            val exitCode = process.waitFor()
            
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}
