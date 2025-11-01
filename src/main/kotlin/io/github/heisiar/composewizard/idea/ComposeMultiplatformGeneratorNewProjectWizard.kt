package io.github.heisiar.composewizard.idea

import com.intellij.ide.IdeBundle
import com.intellij.ide.impl.TrustedPaths
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.openapi.GitRepositoryInitializer
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.observation.launchTracked
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeMultiplatformWizardStep
import java.nio.file.Path
import javax.swing.Icon

class ComposeMultiplatformGeneratorNewProjectWizard : GeneratorNewProjectWizard {
    override val id: String = "COMPOSE_MULTIPLATFORM"
    override val name: String = "Compose Multiplatform"
    override val icon: Icon = IconLoader.getIcon("/META-INF/compose.svg", ComposeMultiplatformGeneratorNewProjectWizard::class.java)

    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep(::ComposeMultiplatformWizardNewStep)

    private class ComposeMultiplatformWizardNewStep(parent: NewProjectWizardStep) 
        : AbstractNewProjectWizardStep(parent) {
        
        private val composeStep = ComposeMultiplatformWizardStep(ComposeMultiplatformModuleBuilder())

        override fun setupUI(builder: com.intellij.ui.dsl.builder.Panel) {
            with(builder) {
                row {
                    cell(composeStep.component)
                        .align(com.intellij.ui.dsl.builder.AlignX.FILL)
                        .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                        .resizableColumn()
                }.resizableRow()
                    .topGap(com.intellij.ui.dsl.builder.TopGap.NONE)
                
                onApply {
                    val projectName = composeStep.getProjectName()
                    val projectPath = composeStep.getProjectPath()
                    
                    context.projectName = projectName
                    context.setProjectFileDirectory(java.nio.file.Path.of(projectPath).resolve(projectName), false)
                }
            }
        }

        override fun setupProject(project: Project) {
            val moduleBuilder = composeStep.getBuilder()
            
            composeStep.updateDataModel()
            
            // Validate before creating - if validation fails, skip project creation
            if (!composeStep.validate()) {
                println("WARNING: Validation failed, skipping project creation")
                return  // Don't create project if validation fails
            }
            
            val projectName = composeStep.getProjectName()
            val projectId = composeStep.getProjectId()
            val initGit = moduleBuilder.initGit
            
            moduleBuilder.projectName = projectName
            moduleBuilder.projectId = projectId
            
            // Use project.basePath which is already set by the platform based on context.projectDirectory
            val projectPath = project.basePath ?: context.projectDirectory.toString()
            println("Creating project at: $projectPath (from project.basePath)")
            
            moduleBuilder.createProjectStructure(projectPath, projectName)
            
            // Link and import Gradle project automatically
            linkGradleProject(project, projectPath)
            
            // Initialize Git repository if requested (after project files are created)
            if (initGit) {
                val gitRepositoryInitializer = GitRepositoryInitializer.getInstance()
                if (gitRepositoryInitializer != null) {
                    runAfterOpened(project) { proj ->
                        proj.service<CoroutineScopeService>().coroutineScope.launchTracked {
                            setupProjectSafe(proj, "Error initializing Git repository") {
                                initRepository(proj, projectPath, gitRepositoryInitializer)
                            }
                        }
                    }
                }
            }
        }
        
        private fun linkGradleProject(project: Project, projectPath: String) {
            // Trust the project path first
            @Suppress("DEPRECATION")
            TrustedPaths.getInstance().setProjectPathTrusted(Path.of(projectPath), true)
            
            // Create Gradle project settings
            val projectSettings = GradleProjectSettings()
            projectSettings.externalProjectPath = projectPath
            
            // Link the Gradle project
            val settings = GradleSettings.getInstance(project)
            settings.linkProject(projectSettings)
            
            // Setup as newly created project
            ExternalProjectsManagerImpl.setupCreatedProject(project)
            
            // Schedule automatic import after project is opened
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
        private class CoroutineScopeService(val coroutineScope: CoroutineScope)
    }
}

