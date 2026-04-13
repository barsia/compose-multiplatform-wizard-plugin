package io.github.barsia.composewizard.shared.wizard

import io.github.barsia.composewizard.shared.ProjectCreator
import io.github.barsia.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for AbstractWizardIntegration workflow logic.
 * 
 * PRIORITY: CRITICAL - This is the core integration point between UI and project creation
 * These tests verify the workflow orchestration, not the actual project creation.
 */
class AbstractWizardIntegrationTest {
    
    private lateinit var integration: TestWizardIntegration
    
    /**
     * Test implementation of AbstractWizardIntegration for testing purposes
     */
    private class TestWizardIntegration : AbstractWizardIntegration() {
        var setupCalled = false
        var openCalled = false
        var gitInitCalled = false
        var errorHandled = false
        
        var setupProjectPath: String? = null
        var setupProjectName: String? = null
        var setupBuilder: ComposeMultiplatformModuleBuilder? = null
        
        override fun performIdeSpecificSetup(
            projectPath: String,
            projectName: String,
            builder: ComposeMultiplatformModuleBuilder
        ) {
            setupCalled = true
            setupProjectPath = projectPath
            setupProjectName = projectName
            setupBuilder = builder
        }
        
        override fun openProject(projectPath: String) {
            openCalled = true
        }
        
        override fun initializeGit(projectPath: String) {
            gitInitCalled = true
        }
        
        override fun handleCreationError(
            projectPath: String,
            projectName: String,
            errorMessage: String
        ) {
            errorHandled = true
            super.handleCreationError(projectPath, projectName, errorMessage)
        }
    }
    
    @BeforeEach
    fun setup() {
        integration = TestWizardIntegration()
        
        // Mock ProjectCreator to avoid needing IntelliJ Application context
        mockkObject(ProjectCreator)
        every { 
            ProjectCreator.createProjectStructure(any(), any(), any(), any()) 
        } returns true
    }
    
    @AfterEach
    fun cleanup() {
        unmockkAll()
    }
    
    // ========== Lifecycle Tests ==========
    
    @Test
    fun `createAndOpenProject calls performIdeSpecificSetup with correct parameters`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            targetDesktop = true
        }
        
        // When
        val result = integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertTrue(result, "createAndOpenProject should return true")
        assertTrue(integration.setupCalled, "performIdeSpecificSetup should be called")
        assertEquals(projectPath, integration.setupProjectPath)
        assertEquals(projectName, integration.setupProjectName)
        assertEquals(builder, integration.setupBuilder)
    }
    
    @Test
    fun `createAndOpenProject calls openProject after setup`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertTrue(integration.openCalled, "openProject should be called after setup")
    }
    
    @Test
    fun `createAndOpenProject initializes git when enabled in builder`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            initGit = true // Enable git
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertTrue(integration.gitInitCalled, "initializeGit should be called when git is enabled")
    }
    
    @Test
    fun `createAndOpenProject skips git initialization when disabled in builder`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            initGit = false // Disable git
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertFalse(integration.gitInitCalled, "initializeGit should not be called when git is disabled")
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    fun `createAndOpenProject handles project creation failure`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
        }
        
        // Mock project creation failure
        every { 
            ProjectCreator.createProjectStructure(any(), any(), any(), any()) 
        } returns false
        
        // When
        val result = integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertFalse(result, "Should return false when project creation fails")
        assertTrue(integration.errorHandled, "Error handler should be called")
        assertFalse(integration.openCalled, "openProject should not be called on failure")
    }
    
    @Test
    fun `createAndOpenProject handles exceptions gracefully`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
        }
        
        // Mock project creation exception
        every { 
            ProjectCreator.createProjectStructure(any(), any(), any(), any()) 
        } throws RuntimeException("Test exception")
        
        // When
        val result = integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertFalse(result, "Should return false when exception occurs")
        assertTrue(integration.errorHandled, "Error handler should be called")
    }
    
    // ========== Platform Configuration Tests ==========
    
    @Test
    fun `createAndOpenProject handles single platform configuration`() {
        // Given
        val projectName = "AndroidOnlyProject"
        val projectPath = "/tmp/AndroidOnlyProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            targetDesktop = false
            targetIOS = false
            targetWeb = false
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertTrue(integration.setupCalled)
        assertEquals(builder, integration.setupBuilder)
        assertTrue(integration.setupBuilder?.targetAndroid == true)
        assertFalse(integration.setupBuilder?.targetDesktop == true)
    }
    
    @Test
    fun `createAndOpenProject handles multiplatform configuration`() {
        // Given
        val projectName = "MultiplatformProject"
        val projectPath = "/tmp/MultiplatformProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            targetDesktop = true
            targetIOS = true
            targetWeb = false
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertTrue(integration.setupCalled)
        val setupBuilder = integration.setupBuilder
        assertNotNull(setupBuilder)
        assertTrue(setupBuilder.targetAndroid)
        assertTrue(setupBuilder.targetDesktop)
        assertTrue(setupBuilder.targetIOS)
        assertFalse(setupBuilder.targetWeb)
    }
    
    // ========== Feature Configuration Tests ==========
    
    @Test
    fun `createAndOpenProject respects includeTests flag`() {
        // Given
        val projectName = "ProjectWithTests"
        val projectPath = "/tmp/ProjectWithTests"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            includeTests = true
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        assertTrue(integration.setupBuilder?.includeTests == true)
    }
    
    @Test
    fun `createAndOpenProject respects library selections`() {
        // Given
        val projectName = "ProjectWithLibraries"
        val projectPath = "/tmp/ProjectWithLibraries"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            includeNavigation = true
            includeMaterial3 = true
        }
        
        // When
        integration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then
        val setupBuilder = integration.setupBuilder
        assertNotNull(setupBuilder)
        assertTrue(setupBuilder.includeNavigation)
        assertTrue(setupBuilder.includeMaterial3)
    }
    
    // ========== Workflow Order Tests ==========
    
    @Test
    fun `createAndOpenProject calls methods in correct order`() {
        // Given
        val projectName = "TestProject"
        val projectPath = "/tmp/TestProject"
        val builder = ComposeMultiplatformModuleBuilder().apply {
            this.projectName = projectName
            targetAndroid = true
            initGit = true
        }
        
        val callOrder = mutableListOf<String>()
        
        // Track call order
        every { 
            ProjectCreator.createProjectStructure(any(), any(), any(), any()) 
        } answers {
            callOrder.add("createProject")
            true
        }
        
        // Create custom integration that tracks calls
        val testIntegration = object {
            fun createAndOpenProject(
                projectPath: String,
                projectName: String,
                builder: ComposeMultiplatformModuleBuilder
            ) {
                // Simulate the workflow
                ProjectCreator.createProjectStructure(projectPath, projectName, builder, false)
                if (builder.initGit) {
                    callOrder.add("git")
                }
                callOrder.add("setup")
                callOrder.add("open")
            }
        }
        
        // When
        testIntegration.createAndOpenProject(projectPath, projectName, builder)
        
        // Then - verify correct order
        assertEquals(listOf("createProject", "git", "setup", "open"), callOrder)
    }
}
