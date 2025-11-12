package io.github.heisiar.composewizard.idea

import com.intellij.ide.IdeBundle
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.GitRepositoryInitializer
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.observation.launchTracked
import com.intellij.platform.ide.progress.withBackgroundProgress
import io.github.heisiar.composewizard.shared.ProjectCreator
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.wizard.AbstractWizardIntegration
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.nio.file.Path

/**
 * IntelliJ IDEA specific wizard integration.
 * 
 * Handles IDEA-specific project setup:
 * - Linking Gradle project automatically
 * - Using GitRepositoryInitializer API for Git initialization
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
        val gitRepositoryInitializer = GitRepositoryInitializer.getInstance()
        
        if (gitRepositoryInitializer != null) {
            runAfterOpened(project) { proj ->
                proj.service<CoroutineScopeService>().coroutineScope.launchTracked {
                    setupProjectSafe(proj, "Error initializing Git repository") {
                        initRepository(proj, projectPath, gitRepositoryInitializer)
                    }
                }
            }
        } else {
            ProjectCreator.initializeGitRepository(projectPath)
        }
    }
    
    @Suppress("UnstableApiUsage")
    private fun linkGradleProject(project: Project, projectPath: String) {
        TrustedProjects.setProjectTrusted(Path.of(projectPath), true)
        
        val projectSettings = GradleProjectSettings()
        projectSettings.externalProjectPath = projectPath
        
        val settings = GradleSettings.getInstance(project)
        settings.linkProject(projectSettings)
        
        ExternalProjectsManagerImpl.setupCreatedProject(project)
        
        StartupManager.getInstance(project).runAfterOpened {
            ExternalSystemUtil.refreshProjects(
                ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            )
        }
    }
    
    private suspend fun initRepository(
        project: Project,
        projectPath: String,
        gitRepositoryInitializer: GitRepositoryInitializer
    ) {
        withBackgroundProgress(project, IdeBundle.message("progress.title.creating.git.repository")) {
            Path.of(projectPath).refreshAndFindVirtualDirectory()?.let { rootDirectory ->
                gitRepositoryInitializer.initRepository(project, rootDirectory, true)
            }
        }
    }
    
    @Service(Service.Level.PROJECT)
    private class CoroutineScopeService(cs: CoroutineScope) {
        val coroutineScope: CoroutineScope = cs
    }
    
    /**
     * Runs an action after the project is opened.
     */
    @Suppress("UnstableApiUsage")
    private fun runAfterOpened(project: Project, action: (Project) -> Unit) {
        StartupManager.getInstance(project).runAfterOpened {
            action(project)
        }
    }
    
    /**
     * Safely runs a suspend project setup action with error handling.
     */
    private suspend fun setupProjectSafe(
        project: Project,
        errorMessage: String,
        action: suspend () -> Unit
    ) {
        try {
            action()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

