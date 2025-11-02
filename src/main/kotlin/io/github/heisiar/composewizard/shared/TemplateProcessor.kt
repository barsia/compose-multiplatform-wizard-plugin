package io.github.heisiar.composewizard.shared

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URL

class TemplateProcessor(
    private val projectName: String,
    private val projectId: String,
    private val composeVersion: String,
    private val includeTests: Boolean,
    private val targetDesktop: Boolean,
    private val targetAndroid: Boolean,
    private val targetIOS: Boolean,
    private val targetWeb: Boolean,
    private val enableDevVersions: Boolean = false
) {
    
    private val projectIdPath = projectId.replace('.', '/')
    
    fun determineTemplate(): String {
        val selectedCount = listOf(targetDesktop, targetAndroid, targetIOS, targetWeb).count { it }
        
        return when {
            selectedCount == 4 -> "All"
            selectedCount == 1 && targetAndroid -> "Android"
            selectedCount == 1 && targetIOS -> "iOS"
            selectedCount == 1 && targetDesktop -> "Desktop"
            selectedCount == 1 && targetWeb -> "Web"
            else -> "All"
        }
    }
    
    fun copyTemplateToProject(targetPath: String) {
        val modularProcessor = ModularTemplateProcessor(
            projectName = projectName,
            projectId = projectId,
            composeVersion = composeVersion,
            includeTests = includeTests,
            targetDesktop = targetDesktop,
            targetAndroid = targetAndroid,
            targetIOS = targetIOS,
            targetWeb = targetWeb,
            enableDevVersions = enableDevVersions
        )
        
        modularProcessor.copyTemplateToProject(targetPath)
    }
    
}

