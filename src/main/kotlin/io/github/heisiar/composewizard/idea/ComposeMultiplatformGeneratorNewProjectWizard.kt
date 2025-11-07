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

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        println("===== ComposeMultiplatformGeneratorNewProjectWizard: createStep() called =====")
        println("===== Using IntelliJ IDEA New Project Wizard (GeneratorNewProjectWizard) =====")
        return RootNewProjectWizardStep(context)
            .nextStep(::ComposeMultiplatformWizardNewStep)
    }

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
            }
            
            // Add hierarchy listener to re-validate when step becomes showing
            composeStep.component.addHierarchyListener { e ->
                val showingChanged = java.awt.event.HierarchyEvent.SHOWING_CHANGED.toLong()
                if ((e.changeFlags.toLong() and showingChanged) != 0L) {
                    if (composeStep.component.isShowing) {
                        println("ComposeMultiplatformWizardNewStep: component is now showing, triggering re-validation...")
                        javax.swing.SwingUtilities.invokeLater {
                            composeStep.triggerRevalidation()
                        }
                    }
                }
            }
        }

        override fun setupProject(project: Project) {
            println("ComposeMultiplatformWizardNewStep: setupProject() called")
            composeStep.updateDataModel()
        }
    }
}
