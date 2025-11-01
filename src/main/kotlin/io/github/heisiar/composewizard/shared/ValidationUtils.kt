package io.github.heisiar.composewizard.shared

object ValidationUtils {
    private val namePattern = "[a-zA-Z\\d\\s_.-]*".toRegex()
    private val firstSymbolNamePattern = "[a-zA-Z\\d_].*".toRegex()
    private val reservedWordsPattern = "(^|[ .])(con|prn|aux|nul|com\\d|lpt\\d)($|[ .])".toRegex(RegexOption.IGNORE_CASE)
    
    private val packagePattern = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$".toRegex()
    
    fun validateProjectName(name: String): String? {
        if (name.isEmpty()) {
            return "Project name must not be empty"
        }
        
        if (!namePattern.matches(name)) {
            return "Project name can only contain letters, digits, spaces, '_', '.' and '-'"
        }
        
        if (!firstSymbolNamePattern.matches(name)) {
            return "Project name must start with a letter, digit or '_'"
        }
        
        if (reservedWordsPattern.find(name) != null) {
            return "Project name contains reserved words"
        }
        
        return null
    }
    
    fun validateProjectId(projectId: String): ValidationResult {
        if (projectId.isEmpty()) {
            return ValidationResult(false, listOf("Project ID must not be empty"))
        }
        
        if (projectId.startsWith(".") || projectId.endsWith(".")) {
            return ValidationResult(false, listOf("Project ID cannot start or end with a dot"))
        }
        
        if (projectId.contains("..")) {
            return ValidationResult(false, listOf("Project ID cannot contain consecutive dots"))
        }
        
        if (!packagePattern.matches(projectId)) {
            return ValidationResult(
                false, 
                listOf("Project ID must be a valid package name (e.g., com.example.project)")
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

