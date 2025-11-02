package io.github.heisiar.composewizard.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.WizardDefaults

/**
 * State holder for Compose Multiplatform Wizard.
 * Manages all wizard configuration in a single data class.
 */
data class WizardState(
    var projectName: String = WizardDefaults.PROJECT_NAME_DISPLAY,
    var projectPath: String = WizardDefaults.getDefaultProjectPath(),
    var projectId: String = WizardDefaults.PACKAGE_NAME,
    var composeVersion: String = ComposeVersions.DEFAULT_VERSION,
    var targetDesktop: Boolean = true,
    var targetAndroid: Boolean = true,
    var targetIOS: Boolean = true,
    var targetWeb: Boolean = true,
    var initGit: Boolean = true,
    var includeTests: Boolean = false,
    var enableDevVersions: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "projectName" to projectName,
            "projectPath" to projectPath,
            "projectId" to projectId,
            "composeVersion" to composeVersion,
            "targetDesktop" to targetDesktop,
            "targetAndroid" to targetAndroid,
            "targetIOS" to targetIOS,
            "targetWeb" to targetWeb,
            "initGit" to initGit,
            "includeTests" to includeTests,
            "enableDevVersions" to enableDevVersions
        )
    }
    
    fun getPlatformsCount(): Int {
        return listOf(targetDesktop, targetAndroid, targetIOS, targetWeb).count { it }
    }
    
    fun getFullProjectPath(): String {
        // Sanitize project name for file system (remove spaces)
        val sanitizedName = WizardDefaults.sanitizeProjectName(projectName)
        return java.io.File(projectPath, sanitizedName).absolutePath
    }
}

/**
 * Creates a remembered wizard state with default values.
 */
@Composable
fun rememberWizardState(
    projectName: String = WizardDefaults.PROJECT_NAME_DISPLAY,
    projectPath: String = WizardDefaults.getDefaultProjectPath(),
    projectId: String = WizardDefaults.PACKAGE_NAME,
    composeVersion: String = ComposeVersions.DEFAULT_VERSION
): MutableState<WizardState> {
    return remember {
        mutableStateOf(
            WizardState(
                projectName = projectName,
                projectPath = projectPath,
                projectId = projectId,
                composeVersion = composeVersion
            )
        )
    }
}



