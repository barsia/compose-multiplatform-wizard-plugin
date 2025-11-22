package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.LibraryType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LibraryMetadataTest {

    @Test
    fun `LibraryRegistry contains all library types`() {
        val expectedTypes = listOf(
            LibraryType.LIFECYCLE,
            LibraryType.MATERIAL3,
            LibraryType.MATERIAL3_ADAPTIVE,
            LibraryType.NAVIGATION,
            LibraryType.NAVIGATION3,
            LibraryType.NAVIGATION_EVENT,
            LibraryType.SAVED_STATE,
            LibraryType.WINDOW,
            LibraryType.HOT_RELOAD
        )
        
        val actualTypes = LibraryRegistry.getAllTypes()
        
        expectedTypes.forEach { type ->
            assertTrue(actualTypes.contains(type), "Registry should contain $type")
        }
    }

    @Test
    fun `getMetadata returns correct metadata for Material3`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.MATERIAL3)
        
        assertEquals(LibraryType.MATERIAL3, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("material3/material3"))
        assertTrue(metadata.mavenMetadataUrl.endsWith("maven-metadata.xml"))
    }

    @Test
    fun `getMetadata returns correct metadata for Navigation`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.NAVIGATION)
        
        assertEquals(LibraryType.NAVIGATION, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("navigation/navigation-compose"))
    }

    @Test
    fun `getMetadata returns correct metadata for Lifecycle`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.LIFECYCLE)
        
        assertEquals(LibraryType.LIFECYCLE, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("lifecycle/lifecycle-runtime"))
    }

    @Test
    fun `getMetadata returns correct metadata for Material3 Adaptive`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.MATERIAL3_ADAPTIVE)
        
        assertEquals(LibraryType.MATERIAL3_ADAPTIVE, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("material3/adaptive/adaptive"))
    }

    @Test
    fun `getMetadata returns correct metadata for Navigation3`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.NAVIGATION3)
        
        assertEquals(LibraryType.NAVIGATION3, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("navigation3/navigation3-ui"))
    }

    @Test
    fun `getMetadata returns correct metadata for NavigationEvent`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.NAVIGATION_EVENT)
        
        assertEquals(LibraryType.NAVIGATION_EVENT, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("navigationevent/navigationevent-compose"))
    }

    @Test
    fun `getMetadata returns correct metadata for SavedState`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.SAVED_STATE)
        
        assertEquals(LibraryType.SAVED_STATE, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("savedstate/savedstate"))
    }

    @Test
    fun `getMetadata returns correct metadata for Window`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.WINDOW)
        
        assertEquals(LibraryType.WINDOW, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("window/window-core"))
    }

    @Test
    fun `getMetadata returns correct metadata for HotReload`() {
        val metadata = LibraryRegistry.getMetadata(LibraryType.HOT_RELOAD)
        
        assertEquals(LibraryType.HOT_RELOAD, metadata.type)
        assertTrue(metadata.mavenMetadataUrl.contains("hot-reload"))
    }

    @Test
    fun `all Maven URLs use HTTPS`() {
        LibraryRegistry.getAllTypes().forEach { type ->
            val metadata = LibraryRegistry.getMetadata(type)
            assertTrue(
                metadata.mavenMetadataUrl.startsWith("https://"),
                "Maven URL for $type should use HTTPS"
            )
        }
    }

    @Test
    fun `all Maven URLs point to repo1 maven org`() {
        LibraryRegistry.getAllTypes().forEach { type ->
            val metadata = LibraryRegistry.getMetadata(type)
            assertTrue(
                metadata.mavenMetadataUrl.contains("repo1.maven.org"),
                "Maven URL for $type should point to Maven Central"
            )
        }
    }

    @Test
    fun `all Maven URLs end with maven-metadata xml`() {
        LibraryRegistry.getAllTypes().forEach { type ->
            val metadata = LibraryRegistry.getMetadata(type)
            assertTrue(
                metadata.mavenMetadataUrl.endsWith("maven-metadata.xml"),
                "Maven URL for $type should end with maven-metadata.xml"
            )
        }
    }

    @Test
    fun `LibraryMetadata can be created with custom values`() {
        val customMetadata = LibraryMetadata(
            type = LibraryType.MATERIAL3,
            mavenMetadataUrl = "https://custom.repo/maven-metadata.xml"
        )
        
        assertEquals(LibraryType.MATERIAL3, customMetadata.type)
        assertEquals("https://custom.repo/maven-metadata.xml", customMetadata.mavenMetadataUrl)
    }

    @Test
    fun `getAllTypes returns non-empty list`() {
        val types = LibraryRegistry.getAllTypes()
        
        assertTrue(types.isNotEmpty(), "Should have at least one library type")
        assertTrue(types.size >= 9, "Should have at least 9 library types")
    }

    @Test
    fun `getMetadata for each type returns non-null metadata`() {
        LibraryRegistry.getAllTypes().forEach { type ->
            val metadata = LibraryRegistry.getMetadata(type)
            assertNotNull(metadata, "Metadata for $type should not be null")
            assertEquals(type, metadata.type, "Metadata type should match requested type")
        }
    }
}


