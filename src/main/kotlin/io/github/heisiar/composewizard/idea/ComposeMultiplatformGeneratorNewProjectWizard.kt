package io.github.heisiar.composewizard.idea

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
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
                            
                            // Add hand cursor to Git repository checkbox in IntelliJ IDEA
                            addHandCursorToGitCheckbox()
                        }
                    }
                }
            }
        }
        
        private fun addHandCursorToGitCheckbox() {
            // Find Git checkbox in parent hierarchy and add hand cursor
            javax.swing.SwingUtilities.invokeLater {
                var parent = composeStep.component.parent
                while (parent != null) {
                    findAndModifyGitCheckbox(parent)
                    parent = parent.parent
                }
            }
        }
        
        private fun findAndModifyGitCheckbox(container: java.awt.Container) {
            for (component in container.components) {
                if (component is javax.swing.JCheckBox) {
                    val text = component.text
                    if (text != null && text.contains("Git", ignoreCase = true)) {
                        component.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                    }
                }
                if (component is java.awt.Container) {
                    findAndModifyGitCheckbox(component)
                }
            }
        }

        override fun setupProject(project: Project) {
            // Override projectName from context (base wizard step)
            val projectName = context.projectName ?: project.name
            composeStep.setProjectName(projectName)
            
            // Update builder with UI values
            composeStep.updateDataModel()
            
            // Override initGit from GitNewProjectWizardStep
            val gitEnabled = gitData?.git ?: false
            builder.initGit = gitEnabled
            
            // Set contentEntryPath before calling setupProjectFromBuilder
            val projectPath = project.basePath ?: throw IllegalStateException("Project path is null")
            builder.contentEntryPath = projectPath
            
            setupProjectFromBuilder(project, builder)
        }
    }
}
