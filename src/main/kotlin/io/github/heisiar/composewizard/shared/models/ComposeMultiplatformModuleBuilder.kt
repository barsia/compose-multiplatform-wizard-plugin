package io.github.heisiar.composewizard.shared.models

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.EmptyModuleType
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.WizardStrings
import javax.swing.Icon

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun hasError(message: String): Boolean {
        return errors.any { it.contains(message, ignoreCase = true) }
    }
}

class ComposeMultiplatformModuleBuilder : ModuleBuilder() {

    lateinit var upProject: Project
    var projectName: String = WizardDefaults.PROJECT_NAME_DISPLAY
    var projectId: String = WizardDefaults.PACKAGE_NAME
    var composeVersion: String = WizardDefaults.COMPOSE_VERSION
    var targetDesktop: Boolean = WizardDefaults.TARGET_DESKTOP
    var targetAndroid: Boolean = WizardDefaults.TARGET_ANDROID
    var targetIOS: Boolean = WizardDefaults.TARGET_IOS
    var targetWeb: Boolean = WizardDefaults.TARGET_WEB
    var initGit: Boolean = WizardDefaults.INIT_GIT
    var includeTests: Boolean = WizardDefaults.INCLUDE_TESTS
    var enableDevVersions: Boolean = WizardDefaults.ENABLE_DEV_VERSIONS
    
    var kotlinVersion: String = ""
    var lifecycleVersion: String? = null
    var material3Version: String? = null
    var material3AdaptiveVersion: String? = null
    var navigationVersion: String? = null
    var navigationEventVersion: String? = null
    var savedStateVersion: String? = null
    var windowVersion: String? = null
    var hotReloadVersion: String? = null
    
    var includeMaterial3: Boolean = false
    var includeMaterial3Adaptive: Boolean = false
    var includeNavigation: Boolean = false
    var includeNavigationEvent: Boolean = false
    var includeSavedState: Boolean = false
    var includeWindow: Boolean = false
    var includeHotReload: Boolean = false

    override fun getModuleType(): ModuleType<*> = EmptyModuleType.getInstance()

    override fun getPresentableName(): String = "Compose Multiplatform"

    override fun getDescription(): String =
        "Create a Compose Multiplatform project for desktop, Android, iOS and web"

    override fun getNodeIcon(): Icon = IconLoader.getIcon("/META-INF/compose.svg", ComposeMultiplatformModuleBuilder::class.java)

    override fun getGroupName(): String = "Compose Multiplatform"

    override fun getBuilderId(): String = "COMPOSE_MULTIPLATFORM"

    override fun getWeight(): Int = 1

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val path = contentEntryPath ?: return
        val rootPath = FileUtil.toSystemIndependentName(path)

        doAddContentEntry(modifiableRootModel)

        createProjectStructure(path, projectName)

        val root = LocalFileSystem.getInstance().refreshAndFindFileByPath(rootPath)
        root?.refresh(false, true)
    }

    override fun getCustomOptionsStep(context: com.intellij.ide.util.projectWizard.WizardContext?, parentDisposable: com.intellij.openapi.Disposable?): com.intellij.ide.util.projectWizard.ModuleWizardStep {
        return io.github.heisiar.composewizard.shared.ui.ComposeWizardStep(this)
    }

    override fun createWizardSteps(context: com.intellij.ide.util.projectWizard.WizardContext, modulesProvider: com.intellij.openapi.roots.ui.configuration.ModulesProvider): Array<com.intellij.ide.util.projectWizard.ModuleWizardStep> {
        return emptyArray()
    }

    override fun createFinishingSteps(wizardContext: com.intellij.ide.util.projectWizard.WizardContext, modulesProvider: com.intellij.openapi.roots.ui.configuration.ModulesProvider): Array<com.intellij.ide.util.projectWizard.ModuleWizardStep> {
        return emptyArray()
    }

    override fun modifySettingsStep(settingsStep: com.intellij.ide.util.projectWizard.SettingsStep): com.intellij.ide.util.projectWizard.ModuleWizardStep? {
        return null
    }

    override fun modifyProjectTypeStep(settingsStep: com.intellij.ide.util.projectWizard.SettingsStep): com.intellij.ide.util.projectWizard.ModuleWizardStep? {
        return null
    }

    override fun isTemplateBased(): Boolean = false

    override fun isAvailable(): Boolean = true
    
    fun createProjectStructure(rootPath: String, projectName: String) {
        io.github.heisiar.composewizard.shared.ProjectCreator.createProjectStructure(
            projectPath = rootPath,
                projectName = projectName,
            builder = this
        )
    }
    
    fun validateProjectId(id: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (id.isBlank()) {
            errors.add(WizardStrings.PACKAGE_NAME_EMPTY)
            return ValidationResult(false, errors)
        }
        
        if (!id.contains(".")) {
            errors.add(WizardStrings.PACKAGE_NAME_NEEDS_SEPARATOR)
            return ValidationResult(false, errors)
        }
        
        val lastChar = id.last()
        if (!lastChar.isLetterOrDigit() && lastChar != '_') {
            errors.add(WizardStrings.PACKAGE_NAME_INVALID_END_CHAR)
        }
        
        val parts = id.split(".")
        
        if (parts.any { it.isEmpty() }) {
            errors.add(WizardStrings.PACKAGE_NAME_EMPTY_PARTS)
        }
        
        parts.forEach { part ->
            if (part.isEmpty()) return@forEach
            
            if (!part[0].isLowerCase()) {
                errors.add(WizardStrings.PACKAGE_NAME_PART_START_LOWERCASE)
            }
            
            if (!part.all { it.isLetterOrDigit() || it == '_' }) {
                errors.add(WizardStrings.PACKAGE_NAME_INVALID_CHARS)
            }
            
            if (part.any { it.isUpperCase() }) {
                errors.add(WizardStrings.PACKAGE_NAME_LOWERCASE_ONLY)
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors.distinct()
        )
    }
}
