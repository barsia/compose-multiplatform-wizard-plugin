package io.github.heisiar.composewizard.idea

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.ide.wizard.GitNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.wizard.newProjectWizardBaseStepWithoutGap
import com.intellij.ide.wizard.setupProjectFromBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeWizardStep
import javax.swing.Icon

class ComposeMultiplatformGeneratorNewProjectWizard : GeneratorNewProjectWizard {
    override val id: String = "COMPOSE_MULTIPLATFORM"
    override val name: String = "Compose Multiplatform"
    override val icon: Icon = IconLoader.getIcon("/META-INF/compose.svg", ComposeMultiplatformGeneratorNewProjectWizard::class.java)

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        System.err.println("!!!!! ComposeMultiplatformGeneratorNewProjectWizard: createStep() START !!!!!")
        System.err.println("!!!!! NEW APPROACH - Using RootNewProjectWizardStep like EmptyProject !!!!!")
        System.err.println("!!!!! context.projectBuilder: ${context.projectBuilder} !!!!!")
        
        // Use RootNewProjectWizardStep like EmptyProject does
        // Must include newProjectWizardBaseStepWithoutGap for project name/location
        // Include GitNewProjectWizardStep for proper Git initialization
        return RootNewProjectWizardStep(context)
            .nextStep(::newProjectWizardBaseStepWithoutGap)
            .nextStep(::GitNewProjectWizardStep)
            .nextStep(::ComposeMultiplatformWizardStep)
    }
    
    private class ComposeMultiplatformWizardStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
        private val builder = ComposeMultiplatformModuleBuilder()
        private val composeStep = ComposeWizardStep(builder)

        override fun setupUI(panelBuilder: com.intellij.ui.dsl.builder.Panel) {
            with(panelBuilder) {
                row {
                    cell(composeStep.component)
                        .align(com.intellij.ui.dsl.builder.AlignX.FILL)
                        .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                        .resizableColumn()
                }.resizableRow()
                    .topGap(com.intellij.ui.dsl.builder.TopGap.NONE)
            }
            
            // Add hierarchy listener to re-validate when step becomes showing
            composeStep.component.addHierarchyListener { e ->
                val showingChanged = java.awt.event.HierarchyEvent.SHOWING_CHANGED.toLong()
                if ((e.changeFlags.toLong() and showingChanged) != 0L) {
                    if (composeStep.component.isShowing) {
                        javax.swing.SwingUtilities.invokeLater {
                            composeStep.triggerRevalidation()
                        }
                    }
                }
            }
        }

        override fun setupProject(project: Project) {
            System.err.println("!!!!! ComposeMultiplatformWizardStep: setupProject() START !!!!!")
            System.err.println("!!!!!   project.name: ${project.name}")
            System.err.println("!!!!!   project.basePath: ${project.basePath}")
            System.err.println("!!!!!   context.projectName: ${context.projectName}")
            
            // CRITICAL: Override projectName from context (base wizard step)
            // In IntelliJ IDEA, the base wizard step sets context.projectName
            // and we must use it BEFORE calling updateDataModel()
            val projectName = context.projectName ?: project.name
            composeStep.setProjectName(projectName)
            System.err.println("!!!!! Set project name from context: $projectName !!!!!")
            
            // Update builder with UI values (now with correct project name)
            composeStep.updateDataModel()
            
            // CRITICAL: Override initGit from GitNewProjectWizardStep
            // In IntelliJ IDEA, Git checkbox is in GitNewProjectWizardStep, not in our UI
            val gitEnabled = gitData?.git ?: false
            builder.initGit = gitEnabled
            System.err.println("!!!!! Git enabled from GitNewProjectWizardStep: $gitEnabled !!!!!")
            
            // CRITICAL: Set contentEntryPath BEFORE calling setupProjectFromBuilder
            // This ensures the builder uses the correct project path from the wizard
            val projectPath = project.basePath ?: throw IllegalStateException("Project path is null")
            builder.contentEntryPath = projectPath
            System.err.println("!!!!! Set builder.contentEntryPath to: $projectPath !!!!!")
            
            // Use setupProjectFromBuilder like EmptyProject does
            System.err.println("!!!!! Calling setupProjectFromBuilder !!!!!")
            setupProjectFromBuilder(project, builder)
            System.err.println("!!!!! setupProjectFromBuilder completed !!!!!")
        }
    }
}
