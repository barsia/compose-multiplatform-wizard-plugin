package io.github.heisiar.composewizard.shared.ui

import com.intellij.ide.impl.ProjectUtil
import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.WizardDefaults
import io.github.heisiar.composewizard.shared.WizardStrings
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path

object WizardValidation {
    private val namePattern = "[a-zA-Z\\d\\s_.-]*".toRegex()
    private val firstSymbolNamePattern = "[a-zA-Z\\d_].*".toRegex()
    private val reservedWordsPattern = "(^|[ .])(con|prn|aux|nul|com\\d|lpt\\d)($|[ .])".toRegex(RegexOption.IGNORE_CASE)
    
    fun validateProjectName(name: String): String? {
        if (name.isEmpty()) {
            return WizardStrings.PROJECT_NAME_EMPTY
        }
        
        if (PlatformDetector.isAndroidStudio) {
            val bannedSymbols = "/\\:<>\"?*|"
            val firstIllegalChar = name.firstOrNull { it in bannedSymbols }
            if (firstIllegalChar != null) {
                return "Illegal character in project name '$name': '$firstIllegalChar'"
            }
            
            if (name.isNotEmpty() && !name[0].isUpperCase()) {
            }
        } else {
            if (!namePattern.matches(name)) {
                return WizardStrings.PROJECT_NAME_INVALID_CHARS
            }
            
            if (!firstSymbolNamePattern.matches(name)) {
                return WizardStrings.PROJECT_NAME_INVALID_START
            }
            
            if (reservedWordsPattern.find(name) != null) {
                return WizardStrings.PROJECT_NAME_RESERVED_WORDS
            }
        }
        
        return null
    }
    
    fun validateProjectPath(path: String, expandPath: (String) -> String): String? {
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
    
    fun validateProjectLocation(projectName: String, projectPath: String, expandPath: (String) -> String): String? {
        if (projectName.isEmpty() || projectPath.isEmpty()) {
            return null
        }
        
        val expandedPath = expandPath(projectPath)
        
        val fullPath = if (PlatformDetector.isAndroidStudio) {
            File(expandedPath)
        } else {
            val sanitizedName = WizardDefaults.sanitizeProjectName(projectName)
            File(expandedPath, sanitizedName)
        }
        
        if (fullPath.exists() && fullPath.isDirectory) {
            val entries = fullPath.listFiles()
            if (entries != null && entries.isNotEmpty()) {
                return "Directory '${fullPath.name}' is not empty"
            }
        }
        
        return null
    }
    
    fun validateProjectLocationBlocking(projectName: String, projectPath: String, expandPath: (String) -> String): String? {
        if (projectName.isEmpty() || projectPath.isEmpty()) {
            return null
        }
        
        val expandedPath = expandPath(projectPath)
        
        val fullPath = if (PlatformDetector.isAndroidStudio) {
            File(expandedPath)
        } else {
            val sanitizedName = WizardDefaults.sanitizeProjectName(projectName)
            File(expandedPath, sanitizedName)
        }
        
        try {
            val existingProject = ProjectUtil.findProject(fullPath.toPath())
            if (existingProject != null) {
                return "Project directory is already taken by project '${existingProject.name}'"
            }
        } catch (e: Exception) {
        }
        
        return null
    }
}

