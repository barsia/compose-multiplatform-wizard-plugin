package io.github.barsia.composewizard.shared

import org.junit.jupiter.api.Test
import kotlin.test.*

class ValidationUtilsTest {

    @Test
    fun `validateProjectId with valid package returns success`() {
        val result = ValidationUtils.validateProjectId("com.example.project")
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validateProjectId with empty string returns error`() {
        val result = ValidationUtils.validateProjectId("")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("must not be empty"))
    }

    @Test
    fun `validateProjectId starting with dot returns error`() {
        val result = ValidationUtils.validateProjectId(".com.example.project")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("cannot start or end with a dot"))
    }

    @Test
    fun `validateProjectId ending with dot returns error`() {
        val result = ValidationUtils.validateProjectId("com.example.project.")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("cannot start or end with a dot"))
    }

    @Test
    fun `validateProjectId with consecutive dots returns error`() {
        val result = ValidationUtils.validateProjectId("com..example.project")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("cannot contain consecutive dots"))
    }

    @Test
    fun `validateProjectId with single segment returns error because requires at least two segments`() {
        val result = ValidationUtils.validateProjectId("myproject")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("must be valid"))
    }

    @Test
    fun `validateProjectId with uppercase letters returns error because lowercase required`() {
        val result = ValidationUtils.validateProjectId("com.Example.Project")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("must be valid"))
    }

    @Test
    fun `validateProjectId with segment starting with digit returns error because must start with letter`() {
        val result = ValidationUtils.validateProjectId("com.example.123project")
        
        assertFalse(result.isValid)
        assertTrue(result.hasError("must be valid"))
    }

    @Test
    fun `validateProjectId with underscore is valid`() {
        val result = ValidationUtils.validateProjectId("com.example_app.project_name")
        
        assertTrue(result.isValid)
    }

    @Test
    fun `validateProjectId with numbers in middle is valid`() {
        val result = ValidationUtils.validateProjectId("com.example2.project3")
        
        assertTrue(result.isValid)
    }

    @Test
    fun `validateProjectId with special characters returns error`() {
        listOf(
            "com.example-project.app",
            "com.example@project.app",
            "com.example project.app",
            "com.example/project.app"
        ).forEach { invalidPackage ->
            val result = ValidationUtils.validateProjectId(invalidPackage)
            assertFalse(result.isValid, "Package '$invalidPackage' should be invalid")
        }
    }

    @Test
    fun `validateProjectId with valid two-segment package returns success`() {
        val result = ValidationUtils.validateProjectId("com.example")
        
        assertTrue(result.isValid)
    }

    @Test
    fun `validateProjectId with valid multi-segment package returns success`() {
        val result = ValidationUtils.validateProjectId("com.example.my.long.package.name")
        
        assertTrue(result.isValid)
    }

    @Test
    fun `validateProjectName with empty string returns error`() {
        val error = ValidationUtils.validateProjectName("")
        
        assertNotNull(error)
        assertTrue(error.contains("must not be empty"))
    }

    @Test
    fun `validateProjectName with valid name returns null`() {
        val error = ValidationUtils.validateProjectName("MyProject")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName with spaces is valid`() {
        val error = ValidationUtils.validateProjectName("My Application")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName with underscore is valid`() {
        val error = ValidationUtils.validateProjectName("My_Project")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName with dot is valid`() {
        val error = ValidationUtils.validateProjectName("My.Project")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName with hyphen is valid`() {
        val error = ValidationUtils.validateProjectName("My-Project")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName with digits is valid`() {
        val error = ValidationUtils.validateProjectName("MyProject2024")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName starting with digit is valid`() {
        val error = ValidationUtils.validateProjectName("2024Project")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName starting with underscore is valid`() {
        val error = ValidationUtils.validateProjectName("_MyProject")
        
        assertNull(error)
    }

    @Test
    fun `validateProjectName with invalid characters returns error`() {
        val error = ValidationUtils.validateProjectName("My/Project")
        
        assertNotNull(error)
        assertTrue(error.contains("can only contain"))
    }

    @Test
    fun `validateProjectName starting with special character returns error`() {
        listOf(".", "-", " ").forEach { prefix ->
            val error = ValidationUtils.validateProjectName("${prefix}MyProject")
            assertNotNull(error, "Name starting with '$prefix' should be invalid")
            assertTrue(error.contains("must start with"))
        }
    }

    @Test
    fun `validateProjectName with reserved word 'con' returns error`() {
        val error = ValidationUtils.validateProjectName("con")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word 'prn' returns error`() {
        val error = ValidationUtils.validateProjectName("prn")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word 'aux' returns error`() {
        val error = ValidationUtils.validateProjectName("aux")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word 'nul' returns error`() {
        val error = ValidationUtils.validateProjectName("nul")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word 'com1' returns error`() {
        val error = ValidationUtils.validateProjectName("com1")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word 'lpt5' returns error`() {
        val error = ValidationUtils.validateProjectName("lpt5")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word in path is invalid`() {
        val error = ValidationUtils.validateProjectName("my.con.project")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName with reserved word with space is invalid`() {
        val error = ValidationUtils.validateProjectName("my con project")
        
        assertNotNull(error)
        assertTrue(error.contains("reserved words"))
    }

    @Test
    fun `validateProjectName containing reserved word as substring is valid`() {
        val error = ValidationUtils.validateProjectName("console")
        
        assertNull(error)
    }
}

