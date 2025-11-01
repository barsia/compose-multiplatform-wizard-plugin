package io.github.heisiar.composewizard.idea

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class ComposeMultiplatformGeneratorNewProjectWizard : GeneratorNewProjectWizard {
    override val id: String = "COMPOSE_MULTIPLATFORM"
    override val name: String = "Compose Multiplatform"
    override val icon: Icon = IconLoader.getIcon(
        "/META-INF/pluginIcon.svg", 
        ComposeMultiplatformGeneratorNewProjectWizard::class.java
    )

    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep(::ComposeMultiplatformWizardStep)

    private class ComposeMultiplatformWizardStep(parent: NewProjectWizardStep) 
        : AbstractNewProjectWizardStep(parent) {
        
        override fun setupUI(builder: com.intellij.ui.dsl.builder.Panel) {
            with(builder) {
                row {
                    label("Compose Multiplatform Project Wizard")
                        .bold()
                }
                row {
                    label("TODO: Implement wizard UI")
                }
            }
        }
        
        override fun setupProject(project: com.intellij.openapi.project.Project) {
            super.setupProject(project)
            // TODO: Implement project setup
        }
    }
}

