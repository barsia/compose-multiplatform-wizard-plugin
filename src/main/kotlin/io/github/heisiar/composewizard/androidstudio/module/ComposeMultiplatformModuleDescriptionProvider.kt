package io.github.heisiar.composewizard.androidstudio.module

import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.intellij.openapi.project.Project

/**
 * Provides Compose Multiplatform module in New Module wizard.
 * 
 * This provider registers a custom wizard entry that appears in the New Module dialog,
 * allowing users to create Compose Multiplatform modules with full control over
 * all parameters on a single screen.
 */
class ComposeMultiplatformModuleDescriptionProvider : ModuleDescriptionProvider {
    
    override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> {
        return listOf(ComposeMultiplatformModuleGalleryEntry())
    }
}

