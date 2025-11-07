package io.github.heisiar.composewizard.idea

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class ComposeProjectWizard : GeneratorNewProjectWizard {
    override val id: String = "COMPOSE_MULTIPLATFORM"
    override val name: String = "Compose Multiplatform"
    override val icon: Icon = IconLoader.getIcon("/META-INF/compose.svg", ComposeProjectWizard::class.java)

    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep(::ComposeWizardStep)
}
