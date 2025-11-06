package io.github.heisiar.composewizard.shared.ui

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for path expansion to ensure ~ is properly expanded before project creation
 * 
 * Critical: java.nio.file.Path.of() does NOT expand ~ and treats it as a literal directory name!
 * This caused projects to be created in paths like:
 * /Users/.../.gradle/caches/.../ideaIC-2025.2.4-aarch64/~/IdeaProjects/MyApp
 * 
 * The fix: always call WizardPathUtils.expandPath() before Path.of()
 */
class PathExpansionTest {
    
    @Test
    fun `expandPath should convert tilde to actual home directory`() {
        val path = "~/IdeaProjects"
        val expanded = WizardPathUtils.expandPath(path)
        
        // Should not contain literal ~
        assertFalse(expanded.contains("~"),
            "Expanded path should not contain ~ but got: $expanded")
        
        // Should contain IdeaProjects
        assertTrue(expanded.contains("IdeaProjects"),
            "Expanded path should contain IdeaProjects but got: $expanded")
        
        // Should be an absolute path
        assertTrue(expanded.startsWith("/") || expanded.matches(Regex("^[A-Z]:\\\\")),
            "Expanded path should be absolute but got: $expanded")
    }
    
    @Test
    fun `expandPath should handle paths without tilde`() {
        val path = "/absolute/path/to/project"
        val expanded = WizardPathUtils.expandPath(path)
        
        // Should return the same path
        assertEquals(path, expanded,
            "Paths without ~ should remain unchanged")
    }
    
    @Test
    fun `expandPath should use getUserHome which avoids gradle cache`() {
        val path = "~/IdeaProjects"
        val expanded = WizardPathUtils.expandPath(path)
        
        // Critical: should NOT contain .gradle/caches
        assertFalse(expanded.contains(".gradle"),
            "Expanded path should not contain .gradle but got: $expanded")
        assertFalse(expanded.contains("caches"),
            "Expanded path should not contain caches but got: $expanded")
        assertFalse(expanded.contains("transforms"),
            "Expanded path should not contain transforms but got: $expanded")
        assertFalse(expanded.contains("ideaIC-"),
            "Expanded path should not contain ideaIC- but got: $expanded")
    }
    
    @Test
    fun `Path_of does NOT expand tilde - demonstrating the bug`() {
        // This test documents the bug we're fixing
        val pathWithTilde = "~/IdeaProjects"
        val javaPath = java.nio.file.Path.of(pathWithTilde)
        
        // Path.of() treats ~ as a literal directory name!
        assertTrue(javaPath.toString().contains("~"),
            "Path.of() does NOT expand ~ - this is why we need expandPath()")
        
        // This would create a directory named ~ in the current working directory
        // When current dir is .gradle/caches/.../ideaIC-2025.2.4-aarch64,
        // it creates: .../ideaIC-2025.2.4-aarch64/~/IdeaProjects
        println("Path.of('~/IdeaProjects') = $javaPath")
        println("This is why we MUST call expandPath() first!")
    }
    
    @Test
    fun `full project path should be expanded correctly`() {
        val projectPath = "~/IdeaProjects"
        val projectName = "MyApplication"
        
        // WRONG way (causes the bug):
        // val wrongPath = java.nio.file.Path.of(projectPath).resolve(projectName)
        // This would create: <current-dir>/~/IdeaProjects/MyApplication
        
        // CORRECT way (our fix):
        val expandedPath = WizardPathUtils.expandPath(projectPath)
        val correctPath = java.nio.file.Path.of(expandedPath).resolve(projectName)
        
        // Should not contain literal ~
        assertFalse(correctPath.toString().contains("~"),
            "Final path should not contain ~ but got: $correctPath")
        
        // Should contain both IdeaProjects and project name
        assertTrue(correctPath.toString().contains("IdeaProjects"),
            "Final path should contain IdeaProjects but got: $correctPath")
        assertTrue(correctPath.toString().contains(projectName),
            "Final path should contain $projectName but got: $correctPath")
        
        // Should NOT contain .gradle/caches
        assertFalse(correctPath.toString().contains(".gradle"),
            "Final path should not contain .gradle but got: $correctPath")
    }
    
    @Test
    fun `collapsePath should convert absolute path back to tilde`() {
        // First expand
        val originalPath = "~/IdeaProjects/MyApp"
        val expanded = WizardPathUtils.expandPath(originalPath)
        
        // Then collapse back
        val collapsed = WizardPathUtils.collapsePath(expanded)
        
        // Should start with ~
        assertTrue(collapsed.startsWith("~"),
            "Collapsed path should start with ~ but got: $collapsed")
        
        // Should contain the rest of the path
        assertTrue(collapsed.contains("IdeaProjects"),
            "Collapsed path should contain IdeaProjects but got: $collapsed")
        assertTrue(collapsed.contains("MyApp"),
            "Collapsed path should contain MyApp but got: $collapsed")
    }
    
    @Test
    fun `expandPath should be idempotent`() {
        val path = "~/IdeaProjects"
        val expanded1 = WizardPathUtils.expandPath(path)
        val expanded2 = WizardPathUtils.expandPath(expanded1)
        
        // Expanding an already expanded path should return the same path
        assertEquals(expanded1, expanded2,
            "expandPath should be idempotent")
    }
}

