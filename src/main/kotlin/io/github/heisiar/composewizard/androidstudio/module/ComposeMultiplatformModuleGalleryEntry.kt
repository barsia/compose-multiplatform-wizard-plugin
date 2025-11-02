package io.github.heisiar.composewizard.androidstudio.module

import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Gallery entry for Compose Multiplatform module in New Module wizard.
 * 
 * This entry appears in the module type selection screen and provides
 * a custom wizard step for configuring all module parameters on one screen.
 */
class ComposeMultiplatformModuleGalleryEntry : ModuleGalleryEntry {
    
    override val icon: Icon
        get() = IconLoader.getIcon("/META-INF/compose.svg", javaClass)
    
    override val name: String
        get() = "Compose Multiplatform"
    
    override val description: String
        get() = "Create a new Compose Multiplatform module for Android, iOS, Desktop, and Web"
    
    override fun createStep(
        project: Project,
        moduleParent: String,
        projectSyncInvoker: ProjectSyncInvoker
    ): SkippableWizardStep<*> {
        val basePackage = NewProjectModel.getSuggestedProjectPackage()
        val model = ComposeMultiplatformModuleModel(project, moduleParent, projectSyncInvoker)
        return ComposeMultiplatformConfigureStep(model, basePackage, name)
    }
}

