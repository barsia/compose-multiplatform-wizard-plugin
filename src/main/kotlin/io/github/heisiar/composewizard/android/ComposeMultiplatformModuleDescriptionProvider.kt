package io.github.heisiar.composewizard

import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.openapi.project.Project
import icons.StudioIcons
import javax.swing.Icon

class ComposeMultiplatformModuleDescriptionProvider : ModuleDescriptionProvider {
  override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> {
    return listOf(ComposeMultiplatformModuleGalleryEntry())
  }
  
  private class ComposeMultiplatformModuleGalleryEntry : ModuleGalleryEntry {
    override val name: String = "Compose Multiplatform Module"
    override val description: String = "Create a new Compose Multiplatform module for Android, iOS, Desktop, and Web"
    override val icon: Icon = StudioIcons.Compose.Editor.COMPOSABLE_FUNCTION
    
    override fun createStep(
      project: Project, 
      moduleParent: String, 
      projectSyncInvoker: ProjectSyncInvoker
    ): SkippableWizardStep<*> {
      val model = ComposeMultiplatformModuleModel(project, moduleParent, projectSyncInvoker)
      return ConfigureComposeMultiplatformModuleStep(model, name)
    }
  }
}

