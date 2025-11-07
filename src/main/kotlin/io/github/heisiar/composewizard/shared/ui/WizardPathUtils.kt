package io.github.heisiar.composewizard.shared.ui

import com.intellij.openapi.util.io.FileUtil
import io.github.heisiar.composewizard.shared.PlatformDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object WizardPathUtils {
    
    fun expandPath(path: String): String {
        return if (path.startsWith("~/")) {
            val userHome = System.getProperty("user.home")
            FileUtil.toSystemIndependentName("$userHome/${path.substring(2)}")
        } else {
            path
        }
    }
    
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

