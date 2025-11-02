package io.github.heisiar.composewizard.androidstudio.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.StringValueProperty
import com.android.tools.idea.wizard.model.WizardModel
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import io.github.heisiar.composewizard.androidstudio.composeMultiplatformModuleRecipe
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import java.io.File

/**
 * Model for Compose Multiplatform module creation.
 * 
 * This model holds all configuration data and handles module generation
 * when the wizard is completed.
 */
class ComposeMultiplatformModuleModel(
    val project: Project,
    val moduleParent: String,
    val projectSyncInvoker: ProjectSyncInvoker
) : WizardModel() {
    
    // Module settings
    val moduleName: StringValueProperty = StringValueProperty(WizardDefaults.PROJECT_NAME)
    val packageName: StringValueProperty = StringValueProperty(WizardDefaults.PACKAGE_NAME)
    
    // Target platforms
    val includeAndroid: BoolValueProperty = BoolValueProperty(WizardDefaults.TARGET_ANDROID)
    val includeIos: BoolValueProperty = BoolValueProperty(WizardDefaults.TARGET_IOS)
    val includeDesktop: BoolValueProperty = BoolValueProperty(WizardDefaults.TARGET_DESKTOP)
    val includeWeb: BoolValueProperty = BoolValueProperty(WizardDefaults.TARGET_WEB)
    
    // Additional options
    val includeTests: BoolValueProperty = BoolValueProperty(WizardDefaults.INCLUDE_TESTS)
    val composeVersion: StringValueProperty = StringValueProperty(ComposeVersions.DEFAULT_VERSION)
    
    // Build configuration - always Kotlin DSL for Compose Multiplatform
    val useKotlinDsl: Boolean = true
    
    override fun handleFinished() {
        val moduleDir = File(project.basePath, moduleName.get())
        
        // Collect statistics
        val platformsCount = listOf(
            includeAndroid.get(),
            includeIos.get(),
            includeDesktop.get(),
            includeWeb.get()
        ).count { it }
        
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = platformsCount,
            includeTests = includeTests.get(),
            includeGit = false, // Not applicable for module wizard
            usedDevVersions = false,
            timeSpentMs = 0 // Not tracked for module wizard
        )
        
        // Generate module files using recipe
        runWriteAction {
            // Create module directory
            val projectVfs = VfsUtil.findFileByIoFile(File(project.basePath!!), true)
            val moduleVfs = projectVfs?.createChildDirectory(this, moduleName.get())
            
            if (moduleVfs != null) {
                // Call recipe to generate files
                composeMultiplatformModuleRecipe(
                    moduleDir = moduleDir,
                    moduleName = moduleName.get(),
                    packageName = packageName.get(),
                    includeAndroid = includeAndroid.get(),
                    includeIos = includeIos.get(),
                    includeDesktop = includeDesktop.get(),
                    includeWeb = includeWeb.get(),
                    includeTests = includeTests.get(),
                    composeVersion = composeVersion.get()
                )
            }
        }
        
        // Trigger Gradle sync
        projectSyncInvoker.syncProject(project)
    }
}

