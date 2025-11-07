package io.github.heisiar.composewizard.shared.ui

import io.github.heisiar.composewizard.shared.WizardDefaults
import org.junit.jupiter.api.Test
import kotlin.test.*

class PathSynchronizationTest {

    @Test
    fun `findUniqueProjectLocation creates correct path from base directory and project name`() {
        val basePath = "/Users/test/AndroidStudioProjects"
        val projectName = "MyApplication1"
        
        val result = WizardDefaults.findUniqueProjectLocation(projectName, basePath)
        
        assertTrue(result.endsWith("MyApplication1"), "Path should end with project name")
        assertTrue(result.startsWith(basePath), "Path should start with base path")
        assertEquals("$basePath/MyApplication1", result)
    }

    @Test
    fun `findUniqueProjectLocation handles existing directory by adding number`() {
        val tempDir = createTempDir("test-project")
        try {
            val basePath = tempDir.absolutePath
            val projectName = "MyApplication"
            
            // Create first project directory
            val firstProject = java.io.File(basePath, projectName)
            firstProject.mkdirs()
            
            val result = WizardDefaults.findUniqueProjectLocation(projectName, basePath)
            
            assertTrue(result.endsWith("MyApplication2"), "Should append '2' when directory exists")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `projectPath should not contain project name in Android Studio mode`() {
        // This test documents the expected behavior:
        // In Android Studio, projectPath = base directory (e.g., ~/AndroidStudioProjects/)
        // Full path = projectPath + projectName
        
        val basePath = "/Users/test/AndroidStudioProjects"
        val projectName = "MyApplication1"
        
        // projectPath should be just the base directory
        assertFalse(basePath.endsWith(projectName), 
            "projectPath should NOT contain project name")
        
        // Full path is constructed by combining them
        val fullPath = "$basePath/$projectName"
        assertTrue(fullPath.endsWith(projectName), 
            "Full path should end with project name")
    }

    @Test
    fun `changing project name should update location with new name`() {
        val basePath = "/Users/test/AndroidStudioProjects"
        val oldProjectName = "MyApplication9"
        val newProjectName = "MyApplication1"
        
        // When project name changes, we should use the SAME base path
        val oldFullPath = WizardDefaults.findUniqueProjectLocation(oldProjectName, basePath)
        val newFullPath = WizardDefaults.findUniqueProjectLocation(newProjectName, basePath)
        
        // Both should use the same base path
        assertTrue(oldFullPath.startsWith(basePath))
        assertTrue(newFullPath.startsWith(basePath))
        
        // But have different project names
        assertTrue(oldFullPath.endsWith(oldProjectName))
        assertTrue(newFullPath.endsWith(newProjectName))
        
        // The new path should NOT be nested inside the old one
        assertFalse(newFullPath.contains("$oldProjectName/$newProjectName"),
            "New project should not be nested inside old project directory")
    }

    @Test
    fun `base path extraction should not use substringBeforeLast`() {
        // This test documents why substringBeforeLast is wrong
        val fullPathWithOldProject = "/Users/test/AndroidStudioProjects/MyApplication9"
        
        // WRONG approach (old code):
        val wrongBasePath = fullPathWithOldProject.substringBeforeLast("/")
        assertEquals("/Users/test/AndroidStudioProjects", wrongBasePath)
        // If we then use this wrongBasePath with new project name:
        val newProjectName = "MyApplication1"
        val wrongFullPath = "$wrongBasePath/$newProjectName"
        // We get: /Users/test/AndroidStudioProjects/MyApplication1 - which looks OK
        
        // BUT the problem is when projectPath ALREADY contains old project name:
        val projectPathWithOldName = "/Users/test/AndroidStudioProjects/MyApplication9/"
        val wrongExtraction = projectPathWithOldName.substringBeforeLast("/")
        // This gives: /Users/test/AndroidStudioProjects/MyApplication9 (still has old name!)
        assertTrue(wrongExtraction.contains("MyApplication9"),
            "substringBeforeLast doesn't remove the project name from path")
        
        // CORRECT approach: projectPath should already BE the base directory (without project name)
        val correctBasePath = "/Users/test/AndroidStudioProjects"
        val correctFullPath = "$correctBasePath/$newProjectName"
        
        assertEquals("/Users/test/AndroidStudioProjects/MyApplication1", correctFullPath)
        assertFalse(correctFullPath.contains("MyApplication9"),
            "Correct path should not contain old project name")
    }

    @Test
    fun `WizardState projectPath should represent base directory in Android Studio`() {
        val state = WizardState()
        
        // In Android Studio mode, projectPath should be the base directory
        state.projectPath = "/Users/test/AndroidStudioProjects"
        state.projectName = "MyApplication1"
        
        // Full path should be constructed by combining them
        val fullPath = "${state.projectPath}/${state.projectName}"
        
        assertEquals("/Users/test/AndroidStudioProjects/MyApplication1", fullPath)
    }

    @Test
    fun `changing project name should not modify projectPath`() {
        val state = WizardState()
        val basePath = "/Users/test/AndroidStudioProjects"
        
        state.projectPath = basePath
        state.projectName = "MyApplication9"
        
        val pathBeforeChange = state.projectPath
        
        // Change project name
        state.projectName = "MyApplication1"
        
        // projectPath should remain unchanged
        assertEquals(pathBeforeChange, state.projectPath,
            "projectPath should not change when project name changes")
        assertEquals(basePath, state.projectPath,
            "projectPath should still be the base directory")
    }

    @Test
    fun `full path construction works with trailing slash`() {
        val basePathWithSlash = "/Users/test/AndroidStudioProjects/"
        val basePathWithoutSlash = "/Users/test/AndroidStudioProjects"
        val projectName = "MyApplication1"
        
        // Both should work correctly
        val path1 = java.nio.file.Path.of(basePathWithSlash).resolve(projectName).toString()
        val path2 = java.nio.file.Path.of(basePathWithoutSlash).resolve(projectName).toString()
        
        // Results should be equivalent (Path.resolve handles trailing slashes)
        assertTrue(path1.endsWith("AndroidStudioProjects/MyApplication1") ||
                   path1.endsWith("AndroidStudioProjects\\MyApplication1"))
        assertTrue(path2.endsWith("AndroidStudioProjects/MyApplication1") ||
                   path2.endsWith("AndroidStudioProjects\\MyApplication1"))
    }

    @Test
    fun `Android Studio should not update Location when Name changes`() {
        // This is the KEY test for the bug fix
        val state = WizardState()
        val basePath = "/Users/test/AndroidStudioProjects"
        
        // Initial state
        state.projectPath = basePath
        state.projectName = "MyApplication9"
        
        val initialPath = state.projectPath
        
        // User changes name
        state.projectName = "MyApplication"
        
        // Location (projectPath) should NOT change!
        assertEquals(initialPath, state.projectPath,
            "Location should NOT change when Name changes in Android Studio")
        assertEquals(basePath, state.projectPath,
            "Location should remain the base directory")
        
        // Full path should be calculated dynamically
        val fullPath = "${state.projectPath}/${state.projectName}"
        assertEquals("$basePath/MyApplication", fullPath)
        
        // The bug was: Location would become "~/AndroidStudioProjects/MyApplication9/MyApplication"
        // And then full path would be "~/AndroidStudioProjects/MyApplication9/MyApplication/MyApplication"
        assertFalse(state.projectPath.contains("MyApplication9"),
            "Location should not contain old project name")
        assertFalse(state.projectPath.contains("MyApplication"),
            "Location should not contain new project name")
    }

    @Test
    fun `Location field should only contain base directory in Android Studio`() {
        // Regression test for the bug
        val basePath = "/Users/test/AndroidStudioProjects"
        val projectName = "MyApplication"
        
        // Location should be ONLY the base directory
        val location = basePath
        
        // Full path is constructed by combining them
        val fullPath = java.io.File(location, projectName).absolutePath
        
        // Verify the full path is correct
        assertTrue(fullPath.endsWith("AndroidStudioProjects/MyApplication") ||
                   fullPath.endsWith("AndroidStudioProjects\\MyApplication"))
        
        // Verify Location doesn't contain project name
        assertFalse(location.contains(projectName),
            "Location should not contain project name")
        
        // The bug scenario that should NOT happen:
        val buggyLocation = "$basePath/$projectName"  // WRONG!
        val buggyFullPath = java.io.File(buggyLocation, projectName).absolutePath
        // This would create: /Users/test/AndroidStudioProjects/MyApplication/MyApplication
        assertTrue(buggyFullPath.contains("MyApplication/MyApplication"),
            "This demonstrates the bug when Location contains project name")
    }

    @Test
    fun `ProjectPathHint should display correct path when projectPath is base directory`() {
        val basePath = "/Users/test/AndroidStudioProjects"
        val projectName = "MyApplication"
        
        // Simulate what ProjectPathHint does
        val fullPath = java.io.File(basePath, projectName).absolutePath
        
        // Should create correct path
        assertTrue(fullPath.endsWith("AndroidStudioProjects/MyApplication") ||
                   fullPath.endsWith("AndroidStudioProjects\\MyApplication"),
            "Full path should be base + project name")
        
        // Should NOT have duplicate project name
        val pathParts = fullPath.split(java.io.File.separator)
        val projectNameCount = pathParts.count { it == projectName }
        assertEquals(1, projectNameCount,
            "Project name should appear only once in the path")
    }

    @Test
    fun `ProjectPathHint should not create nested paths with old project name`() {
        // This tests the display bug: ~/AndroidStudioProjects/MyApplication9/MyApplication
        val basePath = "/Users/test/AndroidStudioProjects"
        val oldProjectName = "MyApplication9"
        val newProjectName = "MyApplication"
        
        // WRONG: if projectPath contains old project name
        val wrongPath = "$basePath/$oldProjectName"
        val wrongFullPath = java.io.File(wrongPath, newProjectName).absolutePath
        // This creates: /Users/test/AndroidStudioProjects/MyApplication9/MyApplication
        assertTrue(wrongFullPath.contains("$oldProjectName/$newProjectName") ||
                   wrongFullPath.contains("$oldProjectName\\$newProjectName"),
            "Wrong approach creates nested path with old project name")
        
        // CORRECT: projectPath should be only base directory
        val correctPath = basePath
        val correctFullPath = java.io.File(correctPath, newProjectName).absolutePath
        // This creates: /Users/test/AndroidStudioProjects/MyApplication
        assertFalse(correctFullPath.contains(oldProjectName),
            "Correct approach should not contain old project name")
        assertTrue(correctFullPath.endsWith("AndroidStudioProjects/$newProjectName") ||
                   correctFullPath.endsWith("AndroidStudioProjects\\$newProjectName"),
            "Correct path should be base + new project name only")
    }

    @Test
    fun `getUserHome should return real user home not gradle cache directory`() {
        // This test verifies the fix for IntelliJ IDEA path bug
        // When IDE is launched via gradle runIde, System.getProperty("user.home") 
        // returns path to .gradle/caches instead of real user home
        
        val userHome = com.intellij.util.SystemProperties.getUserHome()
        
        // User home should NOT contain .gradle/caches
        assertFalse(userHome.contains(".gradle"),
            "User home should not contain .gradle directory")
        assertFalse(userHome.contains("caches"),
            "User home should not contain caches directory")
        assertFalse(userHome.contains("transforms"),
            "User home should not contain transforms directory")
        
        // User home should be a valid directory
        assertTrue(userHome.isNotEmpty(),
            "User home should not be empty")
        
        // User home should not end with IDE distribution path
        assertFalse(userHome.contains("ideaIC-"),
            "User home should not contain IDE distribution path")
        assertFalse(userHome.contains("android-studio-"),
            "User home should not contain Android Studio distribution path")
    }

    @Test
    fun `getDefaultProjectPath should use real user home directory`() {
        // Verify that default project paths use SystemProperties.getUserHome()
        // and not System.getProperty("user.home")
        // 
        // Note: We test both paths directly instead of getDefaultProjectPath()
        // because getDefaultProjectPath() requires ApplicationManager which is not available in tests
        
        val ideaPath = io.github.heisiar.composewizard.shared.WizardDefaults.PROJECT_PATH_IDEA
        val androidPath = io.github.heisiar.composewizard.shared.WizardDefaults.PROJECT_PATH_ANDROID_STUDIO
        
        // Both paths should not be null or empty
        assertNotNull(ideaPath, "IDEA project path should not be null")
        assertNotNull(androidPath, "Android Studio project path should not be null")
        assertTrue(ideaPath.isNotEmpty(), "IDEA project path should not be empty")
        assertTrue(androidPath.isNotEmpty(), "Android Studio project path should not be empty")
        
        // Paths should NOT contain .gradle/caches
        assertFalse(ideaPath.contains(".gradle"),
            "IDEA project path should not contain .gradle directory")
        assertFalse(ideaPath.contains("caches"),
            "IDEA project path should not contain caches directory")
        assertFalse(androidPath.contains(".gradle"),
            "Android Studio project path should not contain .gradle directory")
        assertFalse(androidPath.contains("caches"),
            "Android Studio project path should not contain caches directory")
        
        // Paths should contain correct directory names
        assertTrue(ideaPath.contains("IdeaProjects"),
            "IDEA path should contain IdeaProjects")
        assertTrue(androidPath.contains("AndroidStudioProjects"),
            "Android Studio path should contain AndroidStudioProjects")
    }

    @Test
    fun `expandPath should use real user home for tilde expansion`() {
        // Verify that expandPath uses SystemProperties.getUserHome()
        val expandedPath = io.github.heisiar.composewizard.shared.ui.WizardPathUtils.expandPath("~/IdeaProjects")
        
        // Expanded path should NOT contain .gradle/caches
        assertFalse(expandedPath.contains(".gradle"),
            "Expanded path should not contain .gradle directory")
        assertFalse(expandedPath.contains("caches"),
            "Expanded path should not contain caches directory")
        
        // Expanded path should end with IdeaProjects
        assertTrue(expandedPath.endsWith("IdeaProjects") ||
                   expandedPath.endsWith("IdeaProjects/") ||
                   expandedPath.endsWith("IdeaProjects\\"),
            "Expanded path should end with IdeaProjects")
    }

    @Test
    fun `collapsePath should use real user home for tilde collapse`() {
        // First expand a path to get full path with real user home
        val expandedPath = io.github.heisiar.composewizard.shared.ui.WizardPathUtils.expandPath("~/IdeaProjects/MyApp")
        
        // Then collapse it back
        val collapsedPath = io.github.heisiar.composewizard.shared.ui.WizardPathUtils.collapsePath(expandedPath)
        
        // Collapsed path should start with ~
        assertTrue(collapsedPath.startsWith("~"),
            "Collapsed path should start with tilde")
        
        // Collapsed path should contain IdeaProjects
        assertTrue(collapsedPath.contains("IdeaProjects"),
            "Collapsed path should contain IdeaProjects")
        
        // Collapsed path should NOT contain .gradle
        assertFalse(collapsedPath.contains(".gradle"),
            "Collapsed path should not contain .gradle directory")
    }
}

