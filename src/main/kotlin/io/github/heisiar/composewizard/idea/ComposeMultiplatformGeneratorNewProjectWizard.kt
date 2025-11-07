package io.github.heisiar.composewizard.idea

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeWizardStep
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
        
        private val composeStep = ComposeWizardStep(ComposeMultiplatformModuleBuilder())

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
                return
            }
            
            val projectName = composeStep.getProjectName()
            val projectId = composeStep.getProjectId()
            
            moduleBuilder.projectName = projectName
            moduleBuilder.projectId = projectId
            
            // Use project.basePath which is already set by the platform based on context.projectDirectory
            val projectPath = project.basePath ?: context.projectDirectory.toString()
            println("Creating project at: $projectPath (from project.basePath)")
            
            // Use unified wizard integration
            val integration = IdeaWizardIntegration(project)
            integration.createAndOpenProject(projectPath, projectName, moduleBuilder)
                }
    }
}

