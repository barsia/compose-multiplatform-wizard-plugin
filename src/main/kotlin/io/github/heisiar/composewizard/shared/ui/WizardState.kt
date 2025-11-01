package io.github.heisiar.composewizard.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * State holder for Compose Multiplatform Wizard.
 * Manages all wizard configuration in a single data class.
 */
data class WizardState(
    var projectName: String = "ComposeProject",
    var projectPath: String = "~/IdeaProjects",
    var projectId: String = "org.example.project",
    var composeVersion: String = "1.7.1",
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
        return java.io.File(projectPath, projectName).absolutePath
    }
}

/**
 * Creates a remembered wizard state with default values.
 */
@Composable
fun rememberWizardState(
    projectName: String = "ComposeProject",
    projectPath: String = "~/IdeaProjects",
    projectId: String = "org.example.project",
    composeVersion: String = "1.7.1"
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



