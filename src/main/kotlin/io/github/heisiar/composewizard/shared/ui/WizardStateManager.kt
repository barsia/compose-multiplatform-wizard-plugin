package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import java.io.File

class WizardState {
    var projectName by mutableStateOf("")
    var projectPath by mutableStateOf("")
    var projectId by mutableStateOf("")
    var composeVersion by mutableStateOf("")
    var desktop by mutableStateOf(false)
    var android by mutableStateOf(false)
    var ios by mutableStateOf(false)
    var web by mutableStateOf(false)
    var git by mutableStateOf(false)
    var tests by mutableStateOf(false)
    var enableDevVersions by mutableStateOf(false)
    var devCheckboxVisible by mutableStateOf(false)
    var isLocationSynced by mutableStateOf(true)
    var isNameEditedByUser by mutableStateOf(false)
    
    var projectNameError by mutableStateOf<String?>(null)
    var projectNameWarning by mutableStateOf<String?>(null)
    var projectPathError by mutableStateOf<String?>(null)
    var projectLocationWarning by mutableStateOf<String?>(null)
    var projectIdError by mutableStateOf<String?>(null)
    
    val hasNoTargets: Boolean
        get() = !desktop && !android && !ios && !web
}

@Composable
fun rememberWizardState(): WizardState {
    return remember { WizardState() }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupValidation(
    state: WizardState,
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectIdState: androidx.compose.foundation.text.input.TextFieldState,
    builder: ComposeMultiplatformModuleBuilder,
    onValidationChanged: (Boolean) -> Unit
) {
    LaunchedEffect(projectNameState.text.toString()) {
        state.projectName = projectNameState.text.toString()
    }
    LaunchedEffect(projectPathState.text.toString()) {
        state.projectPath = projectPathState.text.toString()
    }
    LaunchedEffect(projectIdState.text.toString()) {
        state.projectId = projectIdState.text.toString()
    }
    
    LaunchedEffect(state.projectName) {
        val newError = WizardValidation.validateProjectName(state.projectName)
        if (newError != null && newError != state.projectNameError) {
            val errorType = when {
                newError.contains("empty", ignoreCase = true) -> "empty"
                newError.contains("invalid", ignoreCase = true) -> "invalid_chars"
                newError.contains("reserved", ignoreCase = true) -> "reserved_name"
                else -> "invalid_chars"
            }
            ComposeWizardUsageCollector.logValidationError("project_name", errorType)
        }
        
        // For Android Studio: set error directly
        // For IDEA: will be handled in LaunchedEffect(state.projectName, state.projectPath)
        if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            state.projectNameError = newError
        }
    }
    
    LaunchedEffect(state.projectPath) {
        val newError = WizardValidation.validateProjectPath(state.projectPath, WizardPathUtils::expandPath)
        if (newError != null && newError != state.projectPathError) {
            val errorType = when {
                newError.contains("invalid", ignoreCase = true) -> "invalid_path"
                newError.contains("write", ignoreCase = true) -> "no_write_access"
                else -> "invalid_path"
            }
            ComposeWizardUsageCollector.logValidationError("project_location", errorType)
        }
        state.projectPathError = newError
    }
    
    LaunchedEffect(state.projectName, state.projectPath) {
        val locationError = WizardValidation.validateProjectLocationError(state.projectName, state.projectPath, WizardPathUtils::expandPath)
        val locationWarning = WizardValidation.validateProjectLocationWarning(state.projectName, state.projectPath, WizardPathUtils::expandPath)
        
        if (locationError != null && locationError != state.projectNameError) {
            ComposeWizardUsageCollector.logValidationError("project_location", "already_open")
        }
        if (locationWarning != null && locationWarning != state.projectLocationWarning) {
            ComposeWizardUsageCollector.logValidationError("project_location", "directory_not_empty")
        }
        
        if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            // Android Studio: show warnings in Location field
            state.projectLocationWarning = locationWarning ?: locationError
        } else {
            // IntelliJ IDEA: show location errors/warnings in Name field ONLY if no name validation error
            val nameValidationError = WizardValidation.validateProjectName(state.projectName)
            if (nameValidationError != null) {
                // Priority: name validation errors first
                state.projectNameError = nameValidationError
                state.projectNameWarning = null
            } else if (locationError != null) {
                // Then location errors
                state.projectNameError = locationError
                state.projectNameWarning = null
            } else {
                // Finally location warnings
                state.projectNameError = null
                state.projectNameWarning = locationWarning
            }
            state.projectLocationWarning = null
        }
    }
    
    LaunchedEffect(state.projectId) {
        val validation = builder.validateProjectId(state.projectId)
        val newError = if (!validation.isValid) {
            validation.errors.firstOrNull()
        } else {
            null
        }
        if (newError != null && newError != state.projectIdError) {
            val errorType = when {
                newError.contains("empty", ignoreCase = true) -> "empty"
                newError.contains("invalid", ignoreCase = true) || 
                newError.contains("package", ignoreCase = true) -> "invalid_package_format"
                else -> "invalid_package_format"
            }
            ComposeWizardUsageCollector.logValidationError("project_id", errorType)
        }
        state.projectIdError = newError
    }

    LaunchedEffect(state.hasNoTargets) {
        if (state.hasNoTargets) {
            ComposeWizardUsageCollector.logValidationError("platforms", "no_platform_selected")
        }
    }

    // Initial validation on first composition
    // Use state as key to re-trigger validation when wizard is reopened with new state instance
    LaunchedEffect(state) {
        // Run validation immediately to populate error states
        val nameValidationError = WizardValidation.validateProjectName(state.projectName)
        state.projectPathError = WizardValidation.validateProjectPath(state.projectPath, WizardPathUtils::expandPath)
        state.projectIdError = builder.validateProjectId(state.projectId).errors.firstOrNull()
        
        val locationError = WizardValidation.validateProjectLocationError(state.projectName, state.projectPath, WizardPathUtils::expandPath)
        val locationWarning = WizardValidation.validateProjectLocationWarning(state.projectName, state.projectPath, WizardPathUtils::expandPath)
        
        if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            // Android Studio: show name errors and location warnings separately
            state.projectNameError = nameValidationError
            state.projectLocationWarning = locationWarning ?: locationError
        } else {
            // IntelliJ IDEA: show errors/warnings in Name field with priority
            if (nameValidationError != null) {
                state.projectNameError = nameValidationError
                state.projectNameWarning = null
            } else if (locationError != null) {
                state.projectNameError = locationError
                state.projectNameWarning = null
            } else {
                state.projectNameError = null
                state.projectNameWarning = locationWarning
            }
            state.projectLocationWarning = null
        }
        
        val isFormValid = !state.hasNoTargets && 
                         state.projectNameError == null && 
                         state.projectPathError == null && 
                         state.projectIdError == null
        
        println("[LaunchedEffect(state)] isFormValid = $isFormValid, hasNoTargets = ${state.hasNoTargets}, projectNameError = '${state.projectNameError}', projectPathError = '${state.projectPathError}', projectIdError = '${state.projectIdError}'")
        onValidationChanged(isFormValid)
    }
    
    // Validation on any change
    LaunchedEffect(state.projectName, state.projectPath, state.projectId, state.composeVersion, 
        state.desktop, state.android, state.ios, state.web, state.git, state.tests, 
        state.enableDevVersions, state.projectNameError, state.projectPathError, 
        state.projectIdError, state.projectNameWarning, state.projectLocationWarning) {
        
        val isFormValid = !state.hasNoTargets && 
                         state.projectNameError == null && 
                         state.projectPathError == null && 
                         state.projectIdError == null
        onValidationChanged(isFormValid)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupPathSynchronization(
    state: WizardState,
    projectNameState: androidx.compose.foundation.text.input.TextFieldState,
    projectPathState: androidx.compose.foundation.text.input.TextFieldState,
    projectNameValue: String
) {
    LaunchedEffect(state.projectName) {
        // Android Studio: Location (projectPath) is the BASE directory and should NOT change when name changes
        // Full path = projectPath + projectName (calculated in UI)
        // So we do NOTHING here for Android Studio
        
        // IDEA: mark that user edited name manually (to stop auto-increment)
        if (!io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio && 
            state.projectName != projectNameValue) {
            state.isNameEditedByUser = true
        }
        
        if (state.projectName != projectNameValue) {
            ComposeWizardUsageCollector.logFieldEdited("project_name", state.projectName.isNotEmpty())
        }
    }
    
    LaunchedEffect(state.projectPath) {
        val displayedLocation = WizardDefaults.collapsePath(state.projectPath)
        if (projectPathState.text.toString() != displayedLocation) {
            projectPathState.edit {
                replace(0, length, displayedLocation)
            }
        }
    }
    
    LaunchedEffect(projectPathState.text.toString()) {
        val currentText = projectPathState.text.toString()
        if (currentText != WizardDefaults.collapsePath(state.projectPath)) {
            val expandedPath = WizardDefaults.expandPath(currentText)
            if (expandedPath != state.projectPath) {
                state.projectPath = expandedPath
                
                // Android Studio: user manually edited location - stop auto-sync
                if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
                    state.isLocationSynced = false
                }
                
                // IDEA: auto-increment name if location/name exists (unless user edited name manually)
                if (!io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio && 
                    !state.isNameEditedByUser) {
                    val uniqueName = WizardPathUtils.suggestUniqueName(state.projectName, state.projectPath)
                    if (uniqueName != state.projectName) {
                        projectNameState.edit {
                            replace(0, length, uniqueName)
                        }
                    }
                }
                
                ComposeWizardUsageCollector.logFieldEdited("project_location", state.projectPath.isNotEmpty())
            }
        }
    }
}

@Composable
fun SetupAnalytics(
    state: WizardState,
    projectIdValue: String
) {
    LaunchedEffect(Unit) {
        ComposeWizardUsageCollector.logWizardOpened()
    }
    
    LaunchedEffect(state.projectId) {
        if (state.projectId != projectIdValue) {
            ComposeWizardUsageCollector.logFieldEdited("project_id", state.projectId.isNotEmpty())
        }
    }
}

