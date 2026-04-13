package io.github.barsia.composewizard.idea

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.barsia.composewizard.shared.wizard.AbstractWizardIntegration
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.nio.file.Path

/**
 * IntelliJ IDEA specific wizard integration.
 * 
 * Handles IDEA-specific project setup:
 * - Linking Gradle project automatically
 * - Using GitRepositoryInitializer API for proper Git initialization
 * - Project is already opened by the platform, no need to explicitly open it
 */
class IdeaWizardIntegration(
    private val project: Project
) : AbstractWizardIntegration() {
    
    override fun performIdeSpecificSetup(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    ) {
        linkGradleProject(project, projectPath)
    }
    
    override fun openProject(projectPath: String) {
    }
    
    override fun initializeGit(projectPath: String) {
        // Git initialization is now handled in ComposeMultiplatformGeneratorNewProjectWizard
        // for the new IntelliJ IDEA wizard API. This method is kept for compatibility
        // but is not used in the current implementation.
    }
    
    @Suppress("UnstableApiUsage")
    private fun linkGradleProject(project: Project, projectPath: String) {
        TrustedProjects.setProjectTrusted(Path.of(projectPath), true)
        
        val projectSettings = GradleProjectSettings()
        projectSettings.externalProjectPath = projectPath
        
        val settings = GradleSettings.getInstance(project)
        settings.linkProject(projectSettings)
        
        ExternalProjectsManagerImpl.setupCreatedProject(project)
        
        // Refresh Gradle project asynchronously using invokeLater
        ApplicationManager.getApplication().invokeLater({
            if (!project.isDisposed) {
                ExternalSystemUtil.refreshProjects(
                    ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                )
            }
        }, project.disposed)
    }
}

