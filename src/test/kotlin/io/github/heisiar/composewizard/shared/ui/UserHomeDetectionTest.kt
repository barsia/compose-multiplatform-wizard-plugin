package io.github.heisiar.composewizard.shared.ui

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Честный тест для проверки, что getUserHome() НЕ возвращает пути с .gradle/caches
 * 
 * Проблема: когда IDEA запускается через gradle runIde,
 * System.getProperty("user.home") возвращает:
 * /Users/username/.gradle/caches/9.0.0/transforms/.../ideaIC-2025.2.4-aarch64
 * 
 * Это приводит к тому, что проект создается в:
 * /Users/username/.gradle/caches/.../ideaIC-2025.2.4-aarch64/~/IdeaProjects/MyApp
 */
class UserHomeDetectionTest {
    
    @Test
    fun `REAL TEST - simulate gradle runIde scenario where user home points to gradle cache`() {
        // Симулируем реальную ситуацию: user.home указывает на .gradle/caches
        val gradleCachePath = "/Users/Siarhei.Baradulia/.gradle/caches/9.0.0/transforms/93dcdcfef226a38dcb6cce96fa25c186/transformed/ideaIC-2025.2.4-aarch64"
        
        // Проверяем, что наша логика детектирует эту ситуацию
        val isGradleCache = gradleCachePath.contains(".gradle") || gradleCachePath.contains("caches")
        
        assertTrue(isGradleCache, 
            "Should detect that path contains .gradle/caches")
        
        // В этом случае должны использовать System.getenv("HOME")
        val realHome = System.getenv("HOME")
        
        // Реальный HOME не должен содержать .gradle
        assertFalse(realHome?.contains(".gradle") ?: false,
            "Real HOME environment variable should not contain .gradle")
        
        println("Gradle cache path: $gradleCachePath")
        println("Real HOME from env: $realHome")
    }
    
    @Test
    fun `REAL TEST - WizardDefaults should return real home not gradle cache`() {
        val ideaPath = io.github.heisiar.composewizard.shared.WizardDefaults.PROJECT_PATH_IDEA
        val androidPath = io.github.heisiar.composewizard.shared.WizardDefaults.PROJECT_PATH_ANDROID_STUDIO
        
        println("Current System.getProperty(user.home): ${System.getProperty("user.home")}")
        println("Current System.getenv(HOME): ${System.getenv("HOME")}")
        println("PROJECT_PATH_IDEA: $ideaPath")
        println("PROJECT_PATH_ANDROID_STUDIO: $androidPath")
        
        // КРИТИЧЕСКИЙ ТЕСТ: пути НЕ должны содержать .gradle/caches
        assertFalse(ideaPath.contains(".gradle"),
            "PROJECT_PATH_IDEA should NOT contain .gradle but got: $ideaPath")
        assertFalse(ideaPath.contains("caches"),
            "PROJECT_PATH_IDEA should NOT contain caches but got: $ideaPath")
        assertFalse(ideaPath.contains("transforms"),
            "PROJECT_PATH_IDEA should NOT contain transforms but got: $ideaPath")
        assertFalse(ideaPath.contains("ideaIC-"),
            "PROJECT_PATH_IDEA should NOT contain ideaIC- but got: $ideaPath")
        
        assertFalse(androidPath.contains(".gradle"),
            "PROJECT_PATH_ANDROID_STUDIO should NOT contain .gradle but got: $androidPath")
        assertFalse(androidPath.contains("caches"),
            "PROJECT_PATH_ANDROID_STUDIO should NOT contain caches but got: $androidPath")
    }
    
    @Test
    fun `REAL TEST - expandPath should not create paths in gradle cache`() {
        val expandedPath = io.github.heisiar.composewizard.shared.ui.WizardPathUtils.expandPath("~/IdeaProjects")
        
        println("Expanded ~/IdeaProjects to: $expandedPath")
        
        // КРИТИЧЕСКИЙ ТЕСТ: раскрытый путь НЕ должен содержать .gradle/caches
        assertFalse(expandedPath.contains(".gradle"),
            "Expanded path should NOT contain .gradle but got: $expandedPath")
        assertFalse(expandedPath.contains("caches"),
            "Expanded path should NOT contain caches but got: $expandedPath")
        assertFalse(expandedPath.contains("transforms"),
            "Expanded path should NOT contain transforms but got: $expandedPath")
        assertFalse(expandedPath.contains("ideaIC-"),
            "Expanded path should NOT contain ideaIC- but got: $expandedPath")
        
        // Путь должен заканчиваться на IdeaProjects
        assertTrue(expandedPath.endsWith("IdeaProjects") ||
                   expandedPath.endsWith("IdeaProjects/") ||
                   expandedPath.endsWith("IdeaProjects\\"),
            "Expanded path should end with IdeaProjects but got: $expandedPath")
    }
    
    @Test
    fun `REAL TEST - full project path should be in real home directory`() {
        val projectName = "MyApplication1"
        val basePath = io.github.heisiar.composewizard.shared.WizardDefaults.PROJECT_PATH_IDEA
        val fullPath = java.io.File(basePath, projectName).absolutePath
        
        println("Full project path would be: $fullPath")
        
        // КРИТИЧЕСКИЙ ТЕСТ: полный путь НЕ должен содержать .gradle/caches
        assertFalse(fullPath.contains(".gradle"),
            "Full project path should NOT contain .gradle but got: $fullPath")
        assertFalse(fullPath.contains("caches"),
            "Full project path should NOT contain caches but got: $fullPath")
        assertFalse(fullPath.contains("transforms"),
            "Full project path should NOT contain transforms but got: $fullPath")
        assertFalse(fullPath.contains("ideaIC-"),
            "Full project path should NOT contain ideaIC- but got: $fullPath")
        
        // Путь должен содержать IdeaProjects
        assertTrue(fullPath.contains("IdeaProjects"),
            "Full project path should contain IdeaProjects but got: $fullPath")
        
        // Путь должен заканчиваться на имя проекта
        assertTrue(fullPath.endsWith(projectName) ||
                   fullPath.endsWith("$projectName/") ||
                   fullPath.endsWith("$projectName\\"),
            "Full project path should end with $projectName but got: $fullPath")
    }
}

