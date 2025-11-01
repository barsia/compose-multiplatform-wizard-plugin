package io.github.heisiar.composewizard.android.actions

import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.wizard.model.ModelWizard
import com.android.tools.idea.wizard.ui.SimpleStudioWizardLayout
import com.android.tools.idea.wizard.ui.StudioWizardDialogBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.impl.welcomeScreen.NewWelcomeScreen
import io.github.heisiar.composewizard.android.wizard.ComposeMultiplatformProjectSetupStep

/**
 * Action for creating new Compose Multiplatform projects.
 * This appears in:
 * - File → New → Compose Multiplatform Project...
 * - Welcome Screen → New Project
 */
class CreateComposeMultiplatformProjectAction : AnAction(
  "Compose Multiplatform Project...",
  "Create a new Compose Multiplatform project for Android, iOS, Desktop, and Web",
  null
), DumbAware {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    if (NewWelcomeScreen.isNewWelcomeScreen(e)) {
      NewWelcomeScreen.updateNewProjectIconIfWelcomeScreen(e)
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val projectModel = NewProjectModel()
    
    val wizard = ModelWizard.Builder()
      .addStep(ComposeMultiplatformProjectSetupStep(projectModel))
      .build()
    
    val layout = SimpleStudioWizardLayout()
    
    StudioWizardDialogBuilder(wizard, "Create Compose Multiplatform Project")
      .build(layout)
      .show()
  }
}

