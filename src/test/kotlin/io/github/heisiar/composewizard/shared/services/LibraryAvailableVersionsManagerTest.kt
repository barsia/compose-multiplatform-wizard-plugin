package io.github.heisiar.composewizard.shared.services

import com.intellij.openapi.diagnostic.Logger
import io.github.heisiar.composewizard.shared.LibraryType
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryAvailableVersionsManagerTest {

    private lateinit var state: ComposeVersionCacheState
    private lateinit var loader: LibraryAvailableVersionsLoader
    private lateinit var manager: LibraryAvailableVersionsManager
    private lateinit var logger: Logger

    @BeforeEach
    fun setup() {
        mockkStatic(Logger::class)
        logger = mockk(relaxed = true)
        every { Logger.getInstance(LibraryAvailableVersionsManager::class.java) } returns logger

        state = mockk(relaxed = true)
        loader = mockk(relaxed = true)
        
        mockkObject(LibraryRegistry)
        every { LibraryRegistry.getAllTypes() } returns listOf(LibraryType.NAVIGATION, LibraryType.LIFECYCLE)
        
        manager = LibraryAvailableVersionsManager(state, loader)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initializeAvailableVersions loads expired types`() {
        // Given
        every { state.getAvailableLastLoadTime(any()) } returns 0L
        every { state.getAvailableVersions(any()) } returns emptyList()

        // When
        manager.initializeAvailableVersions()

        // Then
        verify { loader.loadLibraryVersions(LibraryType.NAVIGATION) }
        verify { loader.loadLibraryVersions(LibraryType.LIFECYCLE) }
    }

    @Test
    fun `getLibraryVersions triggers load if expired`() {
        // Given
        every { state.getAvailableLastLoadTime(LibraryType.NAVIGATION) } returns 0L
        every { state.getAvailableVersions(LibraryType.NAVIGATION) } returns emptyList()
        every { loader.isLoading(LibraryType.NAVIGATION) } returns false

        // When
        val versions = manager.getLibraryVersions(LibraryType.NAVIGATION)

        // Then
        verify { loader.loadLibraryVersions(LibraryType.NAVIGATION) }
        assertTrue(versions.isEmpty())
    }

    @Test
    fun `getLibraryVersions returns cached versions if valid`() {
        // Given
        val now = System.currentTimeMillis()
        every { state.getAvailableLastLoadTime(LibraryType.NAVIGATION) } returns now
        every { state.getAvailableVersions(LibraryType.NAVIGATION) } returns listOf("1.0.0")

        // When
        val versions = manager.getLibraryVersions(LibraryType.NAVIGATION)

        // Then
        verify(exactly = 0) { loader.loadLibraryVersions(any()) }
        assertEquals(listOf("1.0.0"), versions)
    }
    
    @Test
    fun `invalidateCache resets timestamp`() {
        // When
        manager.invalidateCache(LibraryType.NAVIGATION)
        
        // Then
        verify { state.setAvailableLastLoadTime(LibraryType.NAVIGATION, 0L) }
        verify { logger.info(any<String>()) }
    }
}
