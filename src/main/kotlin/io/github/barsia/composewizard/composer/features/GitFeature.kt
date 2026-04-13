package io.github.barsia.composewizard.composer.features

import io.github.barsia.composewizard.composer.PlatformModule
import io.github.barsia.composewizard.composer.ProjectFeature
import io.github.barsia.composewizard.generator.ProjectConfig
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
            
            // Step 1: Initialize Git repository
            val initProcess = Runtime.getRuntime().exec(
                arrayOf("git", "init"),
                null,
                projectDir
            )
            if (initProcess.waitFor() != 0) {
                return false
            }
            
            // Step 2: Add all files to staging
            // Files will be staged (green in IDE), user can commit when ready
            val addProcess = Runtime.getRuntime().exec(
                arrayOf("git", "add", "."),
                null,
                projectDir
            )
            addProcess.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
