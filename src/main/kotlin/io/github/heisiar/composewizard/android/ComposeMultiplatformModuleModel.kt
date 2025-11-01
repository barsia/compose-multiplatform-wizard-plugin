package io.github.heisiar.composewizard

import com.android.tools.idea.npw.model.ExistingProjectModelData
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleModel
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.Recipe
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.TemplatesUsage.TemplateComponent.WizardUiContext.NEW_MODULE
import com.intellij.openapi.project.Project
import io.github.heisiar.composewizard.android.recipes.generateComposeMultiplatformModule

class ComposeMultiplatformModuleModel(
  project: Project,
  moduleParent: String,
  projectSyncInvoker: ProjectSyncInvoker
) : ModuleModel(
  name = "composeApp",
  commandName = "New Compose Multiplatform Module",
  isLibrary = false,
  projectModelData = ExistingProjectModelData(project, projectSyncInvoker),
  moduleParent = moduleParent,
  wizardContext = NEW_MODULE
) {
  
  val enableWebTarget = BoolValueProperty(false)
  val enableDesktopTarget = BoolValueProperty(true)
  val enableIosTarget = BoolValueProperty(true)
  
  override val renderer = object : ModuleTemplateRenderer() {
    override val recipe: Recipe
      get() = { templateData ->
        val data = templateData as ModuleTemplateData
        generateComposeMultiplatformModule(
          data = data,
          enableAndroid = true,
          enableIos = enableIosTarget.get(),
          enableDesktop = enableDesktopTarget.get(),
          enableWeb = enableWebTarget.get()
        )
      }
  }
  
  override val loggingEvent: AndroidStudioEvent.TemplateRenderer
    get() = AndroidStudioEvent.TemplateRenderer.UNKNOWN_TEMPLATE_RENDERER
}
