package io.github.heisiar.composewizard.shared.ui

import io.github.heisiar.composewizard.shared.PlatformDetector
import io.github.heisiar.composewizard.shared.WizardStrings
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WizardValidationTest {

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        mockkObject(PlatformDetector)
        every { PlatformDetector.isAndroidStudio } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `validateProjectName returns error for empty name`() {
        assertEquals(WizardStrings.PROJECT_NAME_EMPTY, WizardValidation.validateProjectName(""))
    }

    @Test
    fun `validateProjectName returns null for valid name`() {
        assertNull(WizardValidation.validateProjectName("MyProject"))
    }

    @Test
    fun `validateProjectName returns error for invalid chars`() {
        assertEquals(WizardStrings.PROJECT_NAME_INVALID_CHARS, WizardValidation.validateProjectName("My@Project"))
    }

    @Test
    fun `validateProjectName returns error for invalid start`() {
        assertEquals(WizardStrings.PROJECT_NAME_INVALID_START, WizardValidation.validateProjectName("-MyProject"))
    }

    @Test
    fun `validateProjectName returns error for reserved words`() {
        assertEquals(WizardStrings.PROJECT_NAME_RESERVED_WORDS, WizardValidation.validateProjectName("con"))
    }

    @Test
    fun `validateProjectName in Android Studio allows more chars`() {
        every { PlatformDetector.isAndroidStudio } returns true
        assertNull(WizardValidation.validateProjectName("My-Project"))
        assertNotNull(WizardValidation.validateProjectName("My/Project"))
    }

    @Test
    fun `validateProjectPath returns error for empty path`() {
        assertEquals("Location must not be empty", WizardValidation.validateProjectPath("", { it }))
    }

    @Test
    fun `validateProjectPath returns null for valid path`() {
        assertNull(WizardValidation.validateProjectPath(tempDir.absolutePath, { it }))
    }

    @Test
    fun `validateProjectLocation returns error for non-empty directory`() {
        val projectDir = File(tempDir, "MyProject")
        projectDir.mkdir()
        File(projectDir, "file.txt").writeText("content")

        val error = WizardValidation.validateProjectLocation("MyProject", tempDir.absolutePath, { it })
        assertNotNull(error)
    }

    @Test
    fun `validateProjectLocation returns null for empty directory`() {
        val projectDir = File(tempDir, "MyProject")
        projectDir.mkdir()

        val error = WizardValidation.validateProjectLocation("MyProject", tempDir.absolutePath, { it })
        assertNull(error)
    }
    
    @Test
    fun `validateProjectLocation returns null for non-existent directory`() {
        val error = WizardValidation.validateProjectLocation("NewProject", tempDir.absolutePath, { it })
        assertNull(error)
    }
}
