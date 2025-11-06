package io.github.heisiar.composewizard.shared.ui

import com.intellij.openapi.util.io.FileUtil
import io.github.heisiar.composewizard.shared.PlatformDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object WizardPathUtils {
    
    /**
     * Get user home directory, avoiding Gradle cache paths
     * 
     * When IDE is launched via gradle runIde, System.getProperty("user.home") 
     * points to .gradle/caches instead of the real user home.
     * We detect this and fall back to HOME environment variable.
     */
    private fun getUserHome(): String {
        val systemHome = com.intellij.util.SystemProperties.getUserHome()
        
        // If user.home points to .gradle/caches, use HOME environment variable instead
        return if (systemHome.contains(".gradle") || systemHome.contains("caches")) {
            System.getenv("HOME") ?: systemHome
        } else {
            systemHome
        }
    }
    
    fun expandPath(path: String): String {
        return if (path.startsWith("~/")) {
            // Use SystemProperties.getUserHome() to get the actual user's home directory
            // System.getProperty("user.home") can return wrong path when IDE is launched from Gradle
            val userHome = getUserHome()
            FileUtil.toSystemIndependentName("$userHome/${path.substring(2)}")
        } else {
            path
        }
    }
    
    fun collapsePath(path: String): String {
        // Use SystemProperties.getUserHome() to get the actual user's home directory
        // System.getProperty("user.home") can return wrong path when IDE is launched from Gradle
        val userHome = getUserHome()
        val normalizedHome = FileUtil.toSystemIndependentName(userHome)
        val normalizedPath = FileUtil.toSystemIndependentName(path)
        return if (normalizedPath.startsWith(normalizedHome)) {
            "~${normalizedPath.substring(normalizedHome.length)}"
        } else {
            path
        }
    }
    
    fun suggestUniqueName(baseName: String, path: String): String {
        val expandedPath = expandPath(path)
        val dir = File(expandedPath)
        if (!dir.exists()) return baseName
        
        return FileUtil.createSequentFileName(dir, baseName, "") { file ->
            !file.exists()
        }
    }
    
    fun getDefaultProjectPath(): String {
        return if (PlatformDetector.isAndroidStudio) {
            "~/AndroidStudioProjects"
        } else {
            "~/IdeaProjects"
        }
    }
    
    suspend fun loadComposeVersionsFromMaven(
        cache: io.github.heisiar.composewizard.shared.services.ComposeVersionCache,
        includeDevVersions: Boolean = false,
        timeoutMs: Long = 5000
    ): List<String> = withContext(Dispatchers.IO) {
        return@withContext if (includeDevVersions) {
            cache.getDevVersionsBlocking(timeoutMs)
        } else {
            cache.getStableVersionsBlocking(timeoutMs)
        }
    }
}

