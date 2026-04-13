package io.github.barsia.composewizard.shared

/**
 * Single source of truth for all UI strings in the wizard.
 * Change here to apply across the entire application.
 */
object WizardStrings {
    // Field labels
    const val PACKAGE_NAME_LABEL = "Package Name"
    
    // Validation messages - Package name
    const val PACKAGE_NAME_EMPTY = "Package name must not be empty"
    const val PACKAGE_NAME_INVALID_START_END = "Package name cannot start or end with a dot"
    const val PACKAGE_NAME_CONSECUTIVE_DOTS = "Package name cannot contain consecutive dots"
    const val PACKAGE_NAME_INVALID_FORMAT = "Package name must be valid (e.g., com.example.project)"
    const val PACKAGE_NAME_NEEDS_SEPARATOR = "Package name must contain at least one '.' separator"
    const val PACKAGE_NAME_INVALID_END_CHAR = "Package name must end with lowercase latin character, digit or '_'"
    const val PACKAGE_NAME_EMPTY_PARTS = "Package name must not contain empty parts (consecutive dots)"
    const val PACKAGE_NAME_PART_START_LOWERCASE = "Each part of package name must start with lowercase letter"
    const val PACKAGE_NAME_INVALID_CHARS = "Package name can only contain lowercase letters, digits, '_' and '.'"
    const val PACKAGE_NAME_LOWERCASE_ONLY = "Package name must contain only lowercase letters"
    
    // Validation messages - Project name
    const val PROJECT_NAME_EMPTY = "Project name must not be empty"
    const val PROJECT_NAME_INVALID_CHARS = "Project name can only contain letters, digits, spaces, '_', '.' and '-'"
    const val PROJECT_NAME_INVALID_START = "Project name must start with a letter, digit or '_'"
    const val PROJECT_NAME_RESERVED_WORDS = "Project name contains reserved words"
}

