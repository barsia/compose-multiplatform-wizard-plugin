package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.LibraryType

data class LibraryMetadata(
    val type: LibraryType,
    val mavenMetadataUrl: String
)

object LibraryRegistry {
    
    private val libraries = mapOf(
        LibraryType.LIFECYCLE to LibraryMetadata(
            type = LibraryType.LIFECYCLE,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime/maven-metadata.xml"
        ),
        LibraryType.MATERIAL3 to LibraryMetadata(
            type = LibraryType.MATERIAL3,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml"
        ),
        LibraryType.MATERIAL3_ADAPTIVE to LibraryMetadata(
            type = LibraryType.MATERIAL3_ADAPTIVE,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/maven-metadata.xml"
        ),
        LibraryType.NAVIGATION to LibraryMetadata(
            type = LibraryType.NAVIGATION,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation/navigation-compose/maven-metadata.xml"
        ),
        LibraryType.NAVIGATION3 to LibraryMetadata(
            type = LibraryType.NAVIGATION3,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui/maven-metadata.xml"
        ),
        LibraryType.NAVIGATION_EVENT to LibraryMetadata(
            type = LibraryType.NAVIGATION_EVENT,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/navigationevent/navigationevent-compose/maven-metadata.xml"
        ),
        LibraryType.SAVED_STATE to LibraryMetadata(
            type = LibraryType.SAVED_STATE,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/savedstate/savedstate/maven-metadata.xml"
        ),
        LibraryType.WINDOW to LibraryMetadata(
            type = LibraryType.WINDOW,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/androidx/window/window-core/maven-metadata.xml"
        ),
        LibraryType.HOT_RELOAD to LibraryMetadata(
            type = LibraryType.HOT_RELOAD,
            mavenMetadataUrl = "https://repo1.maven.org/maven2/org/jetbrains/compose/hot-reload/org.jetbrains.compose.hot-reload.gradle.plugin/maven-metadata.xml"
        )
    )
    
    fun getMetadata(type: LibraryType): LibraryMetadata {
        return libraries[type] ?: error("No metadata for library type: $type")
    }
    
    fun getAllTypes(): List<LibraryType> = libraries.keys.toList()
}

