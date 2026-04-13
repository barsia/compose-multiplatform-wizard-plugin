package io.github.barsia.composewizard.shared.ui

import com.intellij.openapi.diagnostic.Logger
import io.github.barsia.composewizard.shared.LibraryType
import io.github.barsia.composewizard.shared.services.ComposeVersionCache
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibrariesStateTest {

    private lateinit var cache: ComposeVersionCache
    private lateinit var wizardState: WizardState
    private lateinit var librariesState: LibrariesState
    private lateinit var logger: Logger

    @BeforeEach
    fun setup() {
        mockkStatic(Logger::class)
        logger = mockk(relaxed = true)
        every { Logger.getInstance(LibrariesState::class.java) } returns logger

        cache = mockk(relaxed = true)
        wizardState = WizardState()
        librariesState = LibrariesState(cache, wizardState)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state is correct`() {
        assertTrue(librariesState.libraryVersions.isEmpty())
        assertTrue(librariesState.isFromFallback.isEmpty())
        assertEquals("", librariesState.versionForLibraries)
        assertNull(librariesState.hotReloadGithubVersion)
        assertFalse(librariesState.isLoadingVersions)
    }

    @Test
    fun `clearAllVersions resets state`() {
        // Given
        librariesState.libraryVersions[LibraryType.NAVIGATION] = "1.0.0"
        librariesState.isFromFallback[LibraryType.NAVIGATION] = true
        librariesState.versionForLibraries = "1.6.0"
        librariesState.hotReloadGithubVersion = "1.0.0"
        librariesState.isLoadingVersions = true

        // When
        librariesState.clearAllVersions()

        // Then
        assertTrue(librariesState.libraryVersions.isEmpty())
        assertTrue(librariesState.isFromFallback.isEmpty())
        assertEquals("", librariesState.versionForLibraries)
        assertNull(librariesState.hotReloadGithubVersion)
        assertFalse(librariesState.isLoadingVersions)
        
        verify { logger.info(any<String>(), any<Throwable>()) }
    }
}
