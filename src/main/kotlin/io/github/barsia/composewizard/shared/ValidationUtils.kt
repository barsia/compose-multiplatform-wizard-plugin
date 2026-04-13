package io.github.barsia.composewizard.shared

object ValidationUtils {
    private val namePattern = "[a-zA-Z\\d\\s_.-]*".toRegex()
    private val firstSymbolNamePattern = "[a-zA-Z\\d_].*".toRegex()
    private val reservedWordsPattern = "(^|[ .])(con|prn|aux|nul|com\\d|lpt\\d)($|[ .])".toRegex(RegexOption.IGNORE_CASE)
    
    private val packagePattern = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$".toRegex()
    
    fun validateProjectName(name: String): String? {
        if (name.isEmpty()) {
            return WizardStrings.PROJECT_NAME_EMPTY
        }
        
        if (!namePattern.matches(name)) {
            return WizardStrings.PROJECT_NAME_INVALID_CHARS
        }
        
        if (!firstSymbolNamePattern.matches(name)) {
            return WizardStrings.PROJECT_NAME_INVALID_START
        }
        
        if (reservedWordsPattern.find(name) != null) {
            return WizardStrings.PROJECT_NAME_RESERVED_WORDS
        }
        
        return null
    }
    
    fun validateProjectId(projectId: String): ValidationResult {
        if (projectId.isEmpty()) {
            return ValidationResult(false, listOf(WizardStrings.PACKAGE_NAME_EMPTY))
        }
        
        if (projectId.startsWith(".") || projectId.endsWith(".")) {
            return ValidationResult(false, listOf(WizardStrings.PACKAGE_NAME_INVALID_START_END))
        }
        
        if (projectId.contains("..")) {
            return ValidationResult(false, listOf(WizardStrings.PACKAGE_NAME_CONSECUTIVE_DOTS))
        }
        
        if (!packagePattern.matches(projectId)) {
            return ValidationResult(
                false, 
                listOf(WizardStrings.PACKAGE_NAME_INVALID_FORMAT)
            )
        }
        
        return ValidationResult(true, emptyList())
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun hasError(message: String): Boolean {
        return errors.any { it.contains(message, ignoreCase = true) }
    }
}

