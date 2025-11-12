package io.github.heisiar.composewizard.idea

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GeneratorNewProjectWizard
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
        return RootNewProjectWizardStep(context)
            .nextStep(::newProjectWizardBaseStepWithoutGap)
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
            
            // Update builder with UI values
            composeStep.updateDataModel()
            
            // Use setupProjectFromBuilder like EmptyProject does
            System.err.println("!!!!! Calling setupProjectFromBuilder !!!!!")
            setupProjectFromBuilder(project, builder)
            System.err.println("!!!!! setupProjectFromBuilder completed !!!!!")
        }
    }
}
