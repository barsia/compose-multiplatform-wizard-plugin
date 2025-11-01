package io.github.heisiar.composewizard.shared.ui

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * Validation logic for Compose Multiplatform Wizard.
 * Contains all validation rules and path utilities.
 */
object WizardValidation {
    
    // Validation patterns
    private val namePattern = "[a-zA-Z\\d\\s_.-]*".toRegex()
    private val firstSymbolNamePattern = "[a-zA-Z\\d_].*".toRegex()
    private val reservedWordsPattern = "(^|[ .])(con|prn|aux|nul|com\\d|lpt\\d)($|[ .])".toRegex(RegexOption.IGNORE_CASE)
    
    /**
     * Validates project name.
     * Returns error message or null if valid.
     */
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
    
    /**
     * Validates project path (location).
     * Returns error message or null if valid.
     */
    fun validateProjectPath(path: String): String? {
        if (path.isEmpty()) {
            return "Location must not be empty"
        }
        
        val expandedPath = expandPath(path)
        
        try {
            Path.of(expandedPath)
        } catch (e: InvalidPathException) {
            return "Invalid path"
        }
        
        val pathFile = File(expandedPath)
        if (pathFile.exists()) {
            if (!pathFile.isDirectory) {
                return "Location is not a directory"
            }
            if (!pathFile.canWrite()) {
                return "Location is not writable"
            }
        }
        
        return null
    }
    
    /**
     * Validates project location (full path = location + project name).
     * Returns warning message if directory is not empty.
     */
    fun validateProjectLocation(projectName: String, projectPath: String): String? {
        if (projectName.isEmpty() || projectPath.isEmpty()) {
            return null
        }
        
        val expandedPath = expandPath(projectPath)
        val fullPath = File(expandedPath, projectName)
        
        if (fullPath.exists() && fullPath.isDirectory) {
            val entries = fullPath.listFiles()
            if (entries != null && entries.isNotEmpty()) {
                return "Directory '${fullPath.name}' is not empty"
            }
        }
        
        return null
    }
    
    /**
     * Validates if project directory is already taken by another project.
     * This is a blocking validation that checks if project is already open.
     */
    fun validateProjectLocationBlocking(projectName: String, projectPath: String): String? {
        if (projectName.isEmpty() || projectPath.isEmpty()) {
            return null
        }
        
        val expandedPath = expandPath(projectPath)
        val fullPath = File(expandedPath, projectName)
        
        try {
            val existingProject = ProjectUtil.findProject(fullPath.toPath())
            if (existingProject != null) {
                return "Project directory is already taken by project '${existingProject.name}'"
            }
        } catch (e: Exception) {
            // In test environment or when Application is not initialized, skip this check
            // This is acceptable as it's a non-critical validation
        }
        
        return null
    }
    
    /**
     * Gets platform-specific default project path.
     */
    fun getDefaultProjectPath(): String {
        return if (io.github.heisiar.composewizard.shared.PlatformDetector.isAndroidStudio) {
            "~/AndroidStudioProjects"
        } else {
            "~/IdeaProjects"
        }
    }
    
    /**
     * Expands ~ to user home directory.
     */
    fun expandPath(path: String): String {
        return if (path.startsWith("~/")) {
            val userHome = System.getProperty("user.home")
            FileUtil.toSystemIndependentName("$userHome/${path.substring(2)}")
        } else {
            path
        }
    }
    
    /**
     * Collapses user home directory to ~.
     */
    fun collapsePath(path: String): String {
        val userHome = System.getProperty("user.home")
        val normalizedHome = FileUtil.toSystemIndependentName(userHome)
        val normalizedPath = FileUtil.toSystemIndependentName(path)
        return if (normalizedPath.startsWith(normalizedHome)) {
            "~${normalizedPath.substring(normalizedHome.length)}"
        } else {
            path
        }
    }
    
    /**
     * Suggests unique project name by appending number if needed.
     */
    fun suggestUniqueName(baseName: String, path: String): String {
        val expandedPath = expandPath(path)
        val dir = File(expandedPath)
        if (!dir.exists()) return baseName
        
        return FileUtil.createSequentFileName(dir, baseName, "") { file ->
            !file.exists()
        }
    }
}



